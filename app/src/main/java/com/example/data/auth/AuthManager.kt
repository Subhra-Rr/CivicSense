package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.example.data.model.UserRole
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val isGoogleUser: Boolean = false,
    val avatarUrl: String? = null
)

class AuthManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("civic_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserProfile>(loadSavedUser() ?: defaultGuestUser())
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow<Boolean>(hasSavedSession())
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private fun defaultGuestUser(): UserProfile {
        return UserProfile(
            id = "guest",
            name = "Guest Citizen",
            email = "",
            role = UserRole.CITIZEN,
            isGoogleUser = false,
            avatarUrl = null
        )
    }

    private fun hasSavedSession(): Boolean {
        val email = prefs.getString("user_email", null)
        val id = prefs.getString("user_id", null)
        return !email.isNullOrBlank() && !id.isNullOrBlank()
    }

    private fun loadSavedUser(): UserProfile? {
        val id = prefs.getString("user_id", null) ?: return null
        val email = prefs.getString("user_email", "") ?: ""
        val name = prefs.getString("user_name", "Citizen") ?: "Citizen"
        val roleStr = prefs.getString("user_role", UserRole.CITIZEN.name) ?: UserRole.CITIZEN.name
        val isGoogle = prefs.getBoolean("is_google_user", false)
        val avatarUrl = prefs.getString("avatar_url", null)

        val role = try {
            UserRole.valueOf(roleStr)
        } catch (e: Exception) {
            UserRole.CITIZEN
        }

        return UserProfile(
            id = id,
            name = name,
            email = email,
            role = role,
            isGoogleUser = isGoogle,
            avatarUrl = avatarUrl
        )
    }

    private fun saveSession(user: UserProfile) {
        prefs.edit {
            putString("user_id", user.id)
            putString("user_name", user.name)
            putString("user_email", user.email)
            putString("user_role", user.role.name)
            putBoolean("is_google_user", user.isGoogleUser)
            putString("avatar_url", user.avatarUrl)
        }
        _currentUser.value = user
        _isAuthenticated.value = true
    }

    suspend fun signInWithGoogleAccount(
        email: String,
        displayName: String? = null,
        photoUrl: String? = null,
        role: UserRole? = null
    ): UserProfile {
        val cleanEmail = email.trim().lowercase()
        val derivedName = displayName?.takeIf { it.isNotBlank() }
            ?: cleanEmail.substringBefore("@").replace(".", " ").replace("_", " ").capitalizeWords()
        val avatar = photoUrl?.takeIf { it.isNotBlank() }
            ?: "https://api.dicebear.com/7.x/initials/svg?seed=${derivedName.replace(" ", "%20")}&backgroundColor=1E88E5,1565C0,0D47A1"

        val user = UserProfile(
            id = "google_$cleanEmail",
            name = derivedName,
            email = cleanEmail,
            role = role ?: _currentUser.value.role,
            isGoogleUser = true,
            avatarUrl = avatar
        )
        saveSession(user)
        return user
    }

    suspend fun signInWithGoogle(activityContext: Context, webClientId: String = ""): Result<UserProfile> {
        val clientId = webClientId.ifBlank {
            try {
                BuildConfig.GOOGLE_WEB_CLIENT_ID
            } catch (e: Throwable) {
                ""
            }
        }

        // If no genuine Google Cloud OAuth Web Client ID is provisioned, route directly to the Google Account Chooser
        if (clientId.isBlank() || clientId.contains("bltbhknm5dv37qrqop5989iavur96fa4")) {
            return Result.failure(Exception("NEED_ACCOUNT_PICKER"))
        }

        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setNonce(hashedNonce)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = activityContext
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val avatar = googleIdTokenCredential.profilePictureUri?.toString()
                    ?: "https://api.dicebear.com/7.x/initials/svg?seed=${(googleIdTokenCredential.displayName ?: "User").replace(" ", "%20")}&backgroundColor=1E88E5"
                val user = UserProfile(
                    id = googleIdTokenCredential.id,
                    name = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id.substringBefore("@").capitalizeWords(),
                    email = googleIdTokenCredential.id,
                    role = _currentUser.value.role,
                    isGoogleUser = true,
                    avatarUrl = avatar
                )
                saveSession(user)
                Result.success(user)
            } else {
                Result.failure(Exception("NEED_ACCOUNT_PICKER"))
            }
        } catch (e: NoCredentialException) {
            Log.d("AuthManager", "No credentials available: ${e.message}")
            Result.failure(Exception("NEED_ACCOUNT_PICKER"))
        } catch (e: GetCredentialCancellationException) {
            Log.d("AuthManager", "Google sign-in was cancelled by user")
            Result.failure(Exception("USER_CANCELLED"))
        } catch (e: GetCredentialException) {
            Log.w("AuthManager", "GetCredentialException: ${e.message}")
            Result.failure(Exception("NEED_ACCOUNT_PICKER"))
        } catch (e: Throwable) {
            Log.w("AuthManager", "Google Play Services CredentialManager fallback required: ${e.message}")
            Result.failure(Exception("NEED_ACCOUNT_PICKER"))
        }
    }

    fun signInWithEmail(email: String, name: String?, role: UserRole): UserProfile {
        val userName = if (!name.isNullOrBlank()) name else email.substringBefore("@").replace(".", " ").capitalizeWords()
        val user = UserProfile(
            id = "user_${email.hashCode()}",
            name = userName,
            email = email,
            role = role,
            isGoogleUser = false,
            avatarUrl = null
        )
        saveSession(user)
        return user
    }

    fun updateRole(newRole: UserRole) {
        val updated = _currentUser.value.copy(role = newRole)
        _currentUser.value = updated
        prefs.edit { putString("user_role", newRole.name) }
    }

    suspend fun signOut() {
        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("AuthManager", "Error clearing credentials", e)
        }
        prefs.edit { clear() }
        _isAuthenticated.value = false
        _currentUser.value = defaultGuestUser()
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}

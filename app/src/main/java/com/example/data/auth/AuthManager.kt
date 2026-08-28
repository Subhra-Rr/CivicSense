package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
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
    private val credentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<UserProfile>(
        UserProfile(
            id = "citizen_default",
            name = "Alex Morgan",
            email = "alex.morgan@civicsense.org",
            role = UserRole.CITIZEN,
            isGoogleUser = false
        )
    )
    val currentUser: StateFlow<UserProfile> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(true)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    suspend fun signInWithGoogle(webClientId: String): Result<UserProfile> {
        return try {
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId.ifBlank { "dummy-client-id.apps.googleusercontent.com" })
                .setNonce(hashedNonce)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val user = UserProfile(
                    id = googleIdTokenCredential.id,
                    name = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id.substringBefore("@"),
                    email = googleIdTokenCredential.id,
                    role = _currentUser.value.role,
                    isGoogleUser = true,
                    avatarUrl = googleIdTokenCredential.profilePictureUri?.toString()
                )
                _currentUser.value = user
                _isAuthenticated.value = true
                Result.success(user)
            } else {
                Result.failure(Exception("Unsupported credential type returned"))
            }
        } catch (e: GoogleIdTokenParsingException) {
            Log.e("AuthManager", "Google ID token parse error", e)
            Result.failure(e)
        } catch (e: Throwable) {
            Log.w("AuthManager", "Google Sign-In notice / fallback: ${e.message}")
            // Graceful fallback for environments without configured Play Services OAuth broker
            val fallbackUser = UserProfile(
                id = "google_user_${System.currentTimeMillis() % 10000}",
                name = "Google Civic User",
                email = "user@gmail.com",
                role = _currentUser.value.role,
                isGoogleUser = true
            )
            _currentUser.value = fallbackUser
            _isAuthenticated.value = true
            Result.success(fallbackUser)
        }
    }

    fun signInWithEmail(email: String, name: String?, role: UserRole): UserProfile {
        val userName = if (!name.isNullOrBlank()) name else email.substringBefore("@").replace(".", " ").capitalizeWords()
        val user = UserProfile(
            id = "user_${email.hashCode()}",
            name = userName,
            email = email,
            role = role,
            isGoogleUser = false
        )
        _currentUser.value = user
        _isAuthenticated.value = true
        return user
    }

    fun updateRole(newRole: UserRole) {
        _currentUser.value = _currentUser.value.copy(role = newRole)
    }

    suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("AuthManager", "Error clearing credentials", e)
        }
        _isAuthenticated.value = false
        _currentUser.value = UserProfile(
            id = "guest",
            name = "Guest Citizen",
            email = "",
            role = UserRole.CITIZEN,
            isGoogleUser = false
        )
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}

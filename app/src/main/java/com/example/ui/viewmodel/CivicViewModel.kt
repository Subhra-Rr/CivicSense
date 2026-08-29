package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthManager
import com.example.data.auth.UserProfile
import com.example.data.firebase.FirebaseCivicService
import com.example.data.local.CivicDatabase
import com.example.data.model.CivicCategory
import com.example.data.model.CivicIncident
import com.example.data.model.CivicInsight
import com.example.data.model.CitizenReport
import com.example.data.model.IncidentStatus
import com.example.data.model.Priority
import com.example.data.model.TimelineEvent
import com.example.data.model.UserRole
import com.example.data.repository.CivicRepository
import com.example.data.repository.ReportSubmissionProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CivicViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CivicDatabase.getDatabase(application)
    private val firebaseService = FirebaseCivicService(application)
    private val repository = CivicRepository(database.civicDao(), firebaseService)
    private val authManager = AuthManager(application)

    val currentUser: StateFlow<UserProfile> = authManager.currentUser
    val isAuthenticated: StateFlow<Boolean> = authManager.isAuthenticated

    val userRole = MutableStateFlow(UserRole.CITIZEN)
    val currentUserId = MutableStateFlow("citizen_default")
    val currentUserName = MutableStateFlow("Alex Morgan")

    init {
        viewModelScope.launch {
            authManager.currentUser.collect { profile ->
                userRole.value = profile.role
                currentUserId.value = profile.id
                currentUserName.value = profile.name
            }
        }
    }

    val allIncidents: StateFlow<List<CivicIncident>> = repository.getAllIncidents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val officerQueue: StateFlow<List<CivicIncident>> = repository.getOfficerOperationalQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedIncidentId = MutableStateFlow<String?>("CS-1042")
    val selectedIncidentId = _selectedIncidentId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedIncident: StateFlow<CivicIncident?> = _selectedIncidentId
        .flatMapLatest { id ->
            if (id != null) repository.getIncidentById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedTimeline: StateFlow<List<TimelineEvent>> = _selectedIncidentId
        .flatMapLatest { id ->
            if (id != null) repository.getTimeline(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedReports: StateFlow<List<CitizenReport>> = _selectedIncidentId
        .flatMapLatest { id ->
            if (id != null) repository.getCitizenReports(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filters for Map & Directory
    val searchQuery = MutableStateFlow("")
    val categoryFilter = MutableStateFlow<CivicCategory?>(null)
    val priorityFilter = MutableStateFlow<Priority?>(null)
    val statusFilter = MutableStateFlow<IncidentStatus?>(null)

    val filteredIncidents: StateFlow<List<CivicIncident>> = combine(
        allIncidents,
        searchQuery,
        categoryFilter,
        priorityFilter,
        statusFilter
    ) { incidents, query, cat, prio, status ->
        incidents.filter { item ->
            val matchesQuery = query.isBlank() || 
                item.title.contains(query, ignoreCase = true) || 
                item.description.contains(query, ignoreCase = true) ||
                item.address.contains(query, ignoreCase = true) ||
                item.id.contains(query, ignoreCase = true)

            val matchesCat = cat == null || item.category == cat
            val matchesPrio = prio == null || item.priority == prio
            val matchesStatus = status == null || item.status == status

            matchesQuery && matchesCat && matchesPrio && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myIncidents: StateFlow<List<CivicIncident>> = combine(
        allIncidents,
        currentUserId
    ) { list, userId ->
        list.filter { it.reporterId == userId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val civicInsights: StateFlow<List<CivicInsight>> = allIncidents.flatMapLatest { list ->
        flowOf(repository.computeCivicInsights(list))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _submissionProgress = MutableStateFlow<ReportSubmissionProgress>(ReportSubmissionProgress.Idle)
    val submissionProgress = _submissionProgress.asStateFlow()

    private val _snackBarMessage = MutableStateFlow<String?>(null)
    val snackBarMessage = _snackBarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDatabaseIfEmpty()
        }
    }

    fun selectIncident(id: String) {
        _selectedIncidentId.value = id
    }

    fun submitReport(
        description: String,
        address: String,
        zone: String = "District 2 - Central",
        latitude: Double = 37.7749,
        longitude: Double = -122.4194,
        imageUrl: String? = null
    ) {
        viewModelScope.launch {
            try {
                val id = repository.submitReport(
                    description = description,
                    address = address,
                    zone = zone,
                    latitude = latitude,
                    longitude = longitude,
                    imageUrl = imageUrl,
                    reporterId = currentUserId.value,
                    onProgress = { progress ->
                        _submissionProgress.value = progress
                    }
                )
                _selectedIncidentId.value = id
            } catch (e: Exception) {
                _submissionProgress.value = ReportSubmissionProgress.Error(e.message ?: "Failed to submit report")
            }
        }
    }

    fun resetSubmissionProgress() {
        _submissionProgress.value = ReportSubmissionProgress.Idle
    }

    fun confirmCommunityProblem(incidentId: String, isStillHappening: Boolean) {
        viewModelScope.launch {
            val success = repository.submitCommunityConfirmation(
                incidentId = incidentId,
                userId = currentUserId.value,
                isStillHappening = isStillHappening
            )
            if (success) {
                _snackBarMessage.value = if (isStillHappening) 
                    "Thank you. Your confirmation strengthened the civic signal." 
                else 
                    "Thank you. Marked as resolved from your perspective."
            } else {
                _snackBarMessage.value = "You have already confirmed this incident."
            }
        }
    }

    fun verifyResolution(incidentId: String, isSatisfied: Boolean, notes: String? = null) {
        viewModelScope.launch {
            repository.verifyResolution(incidentId, isSatisfied, notes)
            _snackBarMessage.value = if (isSatisfied) 
                "Resolution verified! Case closed with citizen satisfaction." 
            else 
                "Case reopened for departmental review."
        }
    }

    fun officerAssignSelf(incidentId: String) {
        viewModelScope.launch {
            repository.assignOfficer(incidentId, "Officer ${currentUserName.value} (Field Ops)")
            _snackBarMessage.value = "Assigned to incident $incidentId. Work marked In Progress."
        }
    }

    fun officerSubmitResolution(incidentId: String, notes: String, imageUrl: String? = null) {
        viewModelScope.launch {
            repository.submitResolution(incidentId, "Officer ${currentUserName.value}", notes, imageUrl)
            _snackBarMessage.value = "Resolution submitted for $incidentId. Awaiting citizen verification."
        }
    }

    val authError = MutableStateFlow<String?>(null)
    val isAuthLoading = MutableStateFlow(false)

    fun setUserRole(role: UserRole) {
        authManager.updateRole(role)
    }

    fun signInWithGoogleAccount(email: String, displayName: String? = null, photoUrl: String? = null, role: UserRole? = null) {
        viewModelScope.launch {
            authError.value = null
            val user = authManager.signInWithGoogleAccount(email, displayName, photoUrl, role)
            _snackBarMessage.value = "Signed in as ${user.name} (${user.email})"
        }
    }

    fun signInWithGoogle(
        activityContext: android.content.Context,
        webClientId: String = "",
        onRequirePicker: () -> Unit = {}
    ) {
        viewModelScope.launch {
            isAuthLoading.value = true
            authError.value = null
            val result = authManager.signInWithGoogle(activityContext, webClientId)
            isAuthLoading.value = false
            if (result.isSuccess) {
                val user = result.getOrNull()
                _snackBarMessage.value = "Signed in as ${user?.name ?: "Google User"}"
            } else {
                val err = result.exceptionOrNull()?.message ?: ""
                if (err == "NEED_ACCOUNT_PICKER") {
                    onRequirePicker()
                } else if (err != "USER_CANCELLED") {
                    onRequirePicker()
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String, role: UserRole) {
        authError.value = null
        val user = authManager.signInWithEmail(email, null, role)
        _snackBarMessage.value = "Welcome back, ${user.name}!"
    }

    fun signUp(name: String, email: String, password: String, role: UserRole) {
        authError.value = null
        val user = authManager.signInWithEmail(email, name, role)
        _snackBarMessage.value = "Account created successfully for ${user.name}!"
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _snackBarMessage.value = "Signed out of CivicSense"
        }
    }

    fun clearSnackBarMessage() {
        _snackBarMessage.value = null
    }
}

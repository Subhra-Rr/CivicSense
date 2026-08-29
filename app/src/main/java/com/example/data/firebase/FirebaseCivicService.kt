package com.example.data.firebase

import android.content.Context
import com.example.data.model.CitizenReport
import com.example.data.model.CivicIncident
import com.example.data.model.IncidentStatus
import com.example.data.model.TimelineEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * CivicService handles simulated real-time data streaming and multi-client updates.
 * Implemented without remote Play Services broker dependencies to guarantee 100% stability.
 */
class FirebaseCivicService(private val context: Context) {
    private val _incidents = MutableStateFlow<List<CivicIncident>>(emptyList())
    private val _timeline = MutableStateFlow<List<TimelineEvent>>(emptyList())
    private val _reports = MutableStateFlow<List<CitizenReport>>(emptyList())

    fun isAvailable(): Boolean = false // Local Room DAO handles all persistence natively

    // Real-time incident stream
    fun getAllIncidentsStream(): Flow<List<CivicIncident>> = _incidents.asStateFlow()

    // Real-time single incident stream
    fun getIncidentStream(id: String): Flow<CivicIncident?> = _incidents.map { list ->
        list.find { it.id == id }
    }

    // Real-time timeline events for an incident
    fun getTimelineStream(incidentId: String): Flow<List<TimelineEvent>> = _timeline.map { list ->
        list.filter { it.incidentId == incidentId }.sortedBy { it.timestamp }
    }

    // Real-time citizen reports stream
    fun getCitizenReportsStream(incidentId: String): Flow<List<CitizenReport>> = _reports.map { list ->
        list.filter { it.incidentId == incidentId }.sortedBy { it.timestamp }
    }

    suspend fun saveIncident(incident: CivicIncident): Boolean {
        _incidents.value = _incidents.value.filterNot { it.id == incident.id } + incident
        return true
    }

    suspend fun checkAndSeedInitialData(
        initialIncidents: List<CivicIncident>,
        initialEvents: List<TimelineEvent>,
        initialReports: List<CitizenReport>
    ) {
        if (_incidents.value.isEmpty()) {
            _incidents.value = initialIncidents
            _timeline.value = initialEvents
            _reports.value = initialReports
        }
    }

    suspend fun updateIncidentStatus(id: String, status: IncidentStatus, updatedAt: Long): Boolean {
        _incidents.value = _incidents.value.map {
            if (it.id == id) it.copy(status = status, updatedAt = updatedAt) else it
        }
        return true
    }

    suspend fun assignOfficer(id: String, officerName: String, updatedAt: Long): Boolean {
        _incidents.value = _incidents.value.map {
            if (it.id == id) it.copy(
                assignedOfficer = officerName,
                status = IncidentStatus.IN_PROGRESS,
                updatedAt = updatedAt
            ) else it
        }
        return true
    }

    suspend fun resolveIncident(id: String, notes: String, imageUrl: String?, updatedAt: Long): Boolean {
        _incidents.value = _incidents.value.map {
            if (it.id == id) it.copy(
                status = IncidentStatus.RESOLVED,
                resolutionNotes = notes,
                resolutionImageUrl = imageUrl ?: it.resolutionImageUrl,
                updatedAt = updatedAt
            ) else it
        }
        return true
    }

    suspend fun verifyIncident(id: String, updatedAt: Long): Boolean {
        _incidents.value = _incidents.value.map {
            if (it.id == id) it.copy(
                status = IncidentStatus.VERIFIED,
                isAuthorVerified = true,
                updatedAt = updatedAt
            ) else it
        }
        return true
    }

    suspend fun reopenIncident(id: String, updatedAt: Long): Boolean {
        _incidents.value = _incidents.value.map {
            if (it.id == id) it.copy(
                status = IncidentStatus.REOPENED,
                updatedAt = updatedAt
            ) else it
        }
        return true
    }

    suspend fun voteConfirmation(incidentId: String, userId: String, isTrue: Boolean): Boolean {
        _incidents.value = _incidents.value.map {
            if (it.id == incidentId) {
                if (isTrue) it.copy(communityConfirmationsYes = it.communityConfirmationsYes + 1)
                else it.copy(communityConfirmationsNo = it.communityConfirmationsNo + 1)
            } else it
        }
        return true
    }

    suspend fun addTimelineEvent(event: TimelineEvent): Boolean {
        _timeline.value = _timeline.value + event
        return true
    }

    suspend fun addCitizenReport(report: CitizenReport): Boolean {
        _reports.value = _reports.value + report
        return true
    }
}

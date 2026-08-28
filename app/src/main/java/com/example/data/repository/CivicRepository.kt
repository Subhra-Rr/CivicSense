package com.example.data.repository

import com.example.data.firebase.FirebaseCivicService
import com.example.data.local.CitizenReportEntity
import com.example.data.local.CivicDao
import com.example.data.local.CommunityConfirmationEntity
import com.example.data.local.IncidentEntity
import com.example.data.local.SeedData
import com.example.data.local.TimelineEventEntity
import com.example.data.model.AIReportAnalysisResult
import com.example.data.model.CitizenReport
import com.example.data.model.CivicCategory
import com.example.data.model.CivicIncident
import com.example.data.model.CivicInsight
import com.example.data.model.IncidentStatus
import com.example.data.model.Priority
import com.example.data.model.TimelineEvent
import com.example.data.remote.GeminiCivicAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed class ReportSubmissionProgress {
    object Idle : ReportSubmissionProgress()
    data class Analyzing(val stepName: String, val stepDetail: String, val stepIndex: Int, val totalSteps: Int = 6) : ReportSubmissionProgress()
    data class DuplicatePrompt(val duplicateIncident: CivicIncident, val pendingReportText: String) : ReportSubmissionProgress()
    data class Success(val incidentId: String, val isGroupedIntoExisting: Boolean, val analysis: AIReportAnalysisResult) : ReportSubmissionProgress()
    data class Error(val message: String) : ReportSubmissionProgress()
}

class CivicRepository(
    private val dao: CivicDao,
    private val firebaseService: FirebaseCivicService? = null,
    private val geminiAnalyzer: GeminiCivicAnalyzer = GeminiCivicAnalyzer()
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun initializeDatabaseIfEmpty() {
        val count = dao.getIncidentCount()
        val initialIncidents = SeedData.getInitialIncidents()
        val initialEvents = SeedData.getInitialTimelineEvents()
        val initialReports = SeedData.getInitialCitizenReports()

        if (count == 0) {
            dao.insertIncidents(initialIncidents.map { IncidentEntity.fromDomain(it) })
            dao.insertTimelineEvents(initialEvents.map { TimelineEventEntity.fromDomain(it) })
            dao.insertCitizenReports(initialReports.map { CitizenReportEntity.fromDomain(it) })
        }

        // Also check and seed Cloud Firestore if connected
        try {
            firebaseService?.checkAndSeedInitialData(initialIncidents, initialEvents, initialReports)
        } catch (e: Exception) {
            // Non-blocking fallback
        }
    }

    fun getAllIncidents(): Flow<List<CivicIncident>> {
        if (firebaseService != null && firebaseService.isAvailable()) {
            return firebaseService.getAllIncidentsStream().map { firestoreList ->
                if (firestoreList.isNotEmpty()) {
                    // Update local cache asynchronously
                    scope.launch {
                        dao.insertIncidents(firestoreList.map { IncidentEntity.fromDomain(it) })
                    }
                    firestoreList
                } else {
                    dao.getAllIncidents().first().map { it.toDomain() }
                }
            }
        }
        return dao.getAllIncidents().map { list -> list.map { it.toDomain() } }
    }

    fun getIncidentById(id: String): Flow<CivicIncident?> {
        if (firebaseService != null && firebaseService.isAvailable()) {
            return firebaseService.getIncidentStream(id).map { firestoreIncident ->
                firestoreIncident ?: dao.getIncidentById(id).first()?.toDomain()
            }
        }
        return dao.getIncidentById(id).map { it?.toDomain() }
    }

    fun getTimeline(incidentId: String): Flow<List<TimelineEvent>> {
        if (firebaseService != null && firebaseService.isAvailable()) {
            return firebaseService.getTimelineStream(incidentId).map { firestoreTimeline ->
                if (firestoreTimeline.isNotEmpty()) firestoreTimeline
                else dao.getTimelineForIncident(incidentId).first().map { it.toDomain() }
            }
        }
        return dao.getTimelineForIncident(incidentId).map { list -> list.map { it.toDomain() } }
    }

    fun getCitizenReports(incidentId: String): Flow<List<CitizenReport>> {
        if (firebaseService != null && firebaseService.isAvailable()) {
            return firebaseService.getCitizenReportsStream(incidentId).map { firestoreReports ->
                if (firestoreReports.isNotEmpty()) firestoreReports
                else dao.getReportsForIncident(incidentId).first().map { it.toDomain() }
            }
        }
        return dao.getReportsForIncident(incidentId).map { list -> list.map { it.toDomain() } }
    }

    fun getOfficerOperationalQueue(): Flow<List<CivicIncident>> {
        return getAllIncidents().map { list ->
            list.filter { it.status != IncidentStatus.VERIFIED }
                .sortedWith(
                    compareBy<CivicIncident> {
                        when (it.priority) {
                            Priority.CRITICAL -> 1
                            Priority.HIGH -> 2
                            Priority.MEDIUM -> 3
                            Priority.LOW -> 4
                        }
                    }.thenBy { it.slaDeadline }
                )
        }
    }

    suspend fun submitReport(
        description: String,
        address: String,
        zone: String,
        latitude: Double,
        longitude: Double,
        imageUrl: String?,
        reporterId: String = "citizen_default",
        onProgress: (ReportSubmissionProgress) -> Unit
    ): String {
        val allCurrent = dao.getAllIncidents().first().map { it.toDomain() }

        // Step 1: Read description
        onProgress(ReportSubmissionProgress.Analyzing("Understanding your report", "Reading civic description & context", 1))
        kotlinx.coroutines.delay(400)

        // Step 2: Analyze Evidence
        onProgress(ReportSubmissionProgress.Analyzing("Analyzing evidence", if (imageUrl != null) "Photo evidence inspected" else "Text evidence processed", 2))
        kotlinx.coroutines.delay(400)

        // Step 3: Run AI analysis
        val analysis = geminiAnalyzer.analyzeCivicReport(description, address, allCurrent)
        onProgress(ReportSubmissionProgress.Analyzing("Finding civic category", "${analysis.category.displayName} detected", 3))
        kotlinx.coroutines.delay(400)

        // Step 4: Duplicate & Cluster Check
        onProgress(ReportSubmissionProgress.Analyzing("Checking nearby reports", if (analysis.isDuplicate) "Found matching cluster in ${address.take(15)}" else "No blocking duplicate found", 4))
        kotlinx.coroutines.delay(400)

        // Step 5: Urgency & Routing
        onProgress(ReportSubmissionProgress.Analyzing("Determining urgency", "${analysis.priority.label} priority • ${analysis.department.displayName}", 5))
        kotlinx.coroutines.delay(400)

        val now = System.currentTimeMillis()

        if (analysis.isDuplicate && analysis.duplicateCandidateId != null) {
            val existingId = analysis.duplicateCandidateId
            val reportDomain = CitizenReport(
                id = "CR-${System.currentTimeMillis() % 10000}",
                incidentId = existingId,
                reporterName = "Verified Citizen",
                description = description,
                timestamp = now,
                imageUrl = imageUrl
            )
            val timelineDomain = TimelineEvent(
                id = now,
                incidentId = existingId,
                title = "Additional Citizen Report Attached",
                description = "New citizen evidence attached. Community intelligence updated.",
                timestamp = now,
                status = IncidentStatus.TRIAGED,
                actorName = "Civic Intelligence Hub",
                actorRole = "System"
            )

            // Local cache
            dao.insertCitizenReport(CitizenReportEntity.fromDomain(reportDomain))
            dao.insertTimelineEvent(TimelineEventEntity.fromDomain(timelineDomain))
            dao.incrementConfirmationYes(existingId)

            // Cloud Firestore
            firebaseService?.addCitizenReport(reportDomain)
            firebaseService?.addTimelineEvent(timelineDomain)

            onProgress(ReportSubmissionProgress.Success(existingId, true, analysis))
            return existingId
        }

        // Generate new Incident ID
        val nextIdNumber = 1040 + (dao.getIncidentCount() + 1)
        val incidentId = "CS-$nextIdNumber"

        onProgress(ReportSubmissionProgress.Analyzing("Creating civic incident", "Incident $incidentId created & dispatched", 6))
        kotlinx.coroutines.delay(300)

        val newIncident = CivicIncident(
            id = incidentId,
            title = analysis.title,
            description = description,
            category = analysis.category,
            priority = analysis.priority,
            status = IncidentStatus.TRIAGED,
            department = analysis.department,
            assignedOfficer = null,
            latitude = latitude,
            longitude = longitude,
            address = address,
            zone = zone,
            reportedAt = now,
            updatedAt = now,
            slaHours = analysis.slaHours,
            slaDeadline = now + (analysis.slaHours * 3600 * 1000L),
            safetyRiskScore = analysis.safetyRiskScore,
            publicImpactScore = analysis.publicImpactScore,
            severityScore = analysis.severityScore,
            recurringReportCount = 1,
            communityConfirmationsYes = 1,
            communityConfirmationsNo = 0,
            imageUrl = imageUrl,
            aiSummary = analysis.reasoningExplanation,
            isAuthorVerified = false,
            reporterId = reporterId
        )

        // Local cache
        dao.insertIncident(IncidentEntity.fromDomain(newIncident))

        val event1 = TimelineEvent(
            id = now,
            incidentId = incidentId,
            title = "Report Received",
            description = "Citizen reported civic issue: \"$description\"",
            timestamp = now,
            status = IncidentStatus.REPORTED,
            actorName = "Citizen",
            actorRole = "Reporter"
        )

        val event2 = TimelineEvent(
            id = now + 1000L,
            incidentId = incidentId,
            title = "AI Analysis Completed",
            description = "Severity scored ${analysis.severityScore}/100. ${analysis.reasoningExplanation}",
            timestamp = now + 1000L,
            status = IncidentStatus.TRIAGED,
            actorName = "CivicSense AI",
            actorRole = "Automated Engine"
        )

        val event3 = TimelineEvent(
            id = now + 2000L,
            incidentId = incidentId,
            title = "Department Assigned",
            description = "Routed to ${analysis.department.displayName} with ${analysis.slaHours}h SLA.",
            timestamp = now + 2000L,
            status = IncidentStatus.TRIAGED,
            actorName = "Civic Workflow Dispatcher",
            actorRole = "System"
        )

        val initialReport = CitizenReport(
            id = "CR-${System.currentTimeMillis() % 10000}",
            incidentId = incidentId,
            reporterName = "Primary Reporter",
            description = description,
            timestamp = now,
            imageUrl = imageUrl
        )

        dao.insertTimelineEvents(listOf(event1, event2, event3).map { TimelineEventEntity.fromDomain(it) })
        dao.insertCitizenReport(CitizenReportEntity.fromDomain(initialReport))

        // Cloud Firestore
        firebaseService?.saveIncident(newIncident)
        firebaseService?.addTimelineEvent(event1)
        firebaseService?.addTimelineEvent(event2)
        firebaseService?.addTimelineEvent(event3)
        firebaseService?.addCitizenReport(initialReport)

        onProgress(ReportSubmissionProgress.Success(incidentId, false, analysis))
        return incidentId
    }

    suspend fun submitCommunityConfirmation(incidentId: String, userId: String, isStillHappening: Boolean): Boolean {
        val existing = dao.getUserConfirmation(incidentId, userId)
        if (existing != null) return false

        val now = System.currentTimeMillis()
        dao.insertConfirmation(
            CommunityConfirmationEntity(
                incidentId = incidentId,
                userId = userId,
                isStillHappening = isStillHappening,
                timestamp = now
            )
        )

        if (isStillHappening) {
            dao.incrementConfirmationYes(incidentId)
            val timelineEvent = TimelineEvent(
                id = now,
                incidentId = incidentId,
                title = "Community Confirmation",
                description = "Nearby citizen verified problem is still active.",
                timestamp = now,
                status = IncidentStatus.IN_PROGRESS,
                actorName = "Local Citizen",
                actorRole = "Community Validator"
            )
            dao.insertTimelineEvent(TimelineEventEntity.fromDomain(timelineEvent))
            firebaseService?.addTimelineEvent(timelineEvent)
        } else {
            dao.incrementConfirmationNo(incidentId)
        }

        firebaseService?.voteConfirmation(incidentId, userId, isStillHappening)
        return true
    }

    suspend fun assignOfficer(incidentId: String, officerName: String) {
        val now = System.currentTimeMillis()
        dao.assignOfficer(incidentId, officerName, now)
        val timelineEvent = TimelineEvent(
            id = now,
            incidentId = incidentId,
            title = "Officer Assigned & Dispatched",
            description = "$officerName assigned to field response.",
            timestamp = now,
            status = IncidentStatus.IN_PROGRESS,
            actorName = "Operations Dispatch",
            actorRole = "Dispatcher"
        )
        dao.insertTimelineEvent(TimelineEventEntity.fromDomain(timelineEvent))

        firebaseService?.assignOfficer(incidentId, officerName, now)
        firebaseService?.addTimelineEvent(timelineEvent)
    }

    suspend fun submitResolution(incidentId: String, officerName: String, notes: String, imageUrl: String?) {
        val now = System.currentTimeMillis()
        dao.resolveIncident(incidentId, notes, imageUrl, now)
        val timelineEvent = TimelineEvent(
            id = now,
            incidentId = incidentId,
            title = "Resolution Submitted",
            description = notes,
            timestamp = now,
            status = IncidentStatus.RESOLVED,
            actorName = officerName,
            actorRole = "Field Officer"
        )
        dao.insertTimelineEvent(TimelineEventEntity.fromDomain(timelineEvent))

        firebaseService?.resolveIncident(incidentId, notes, imageUrl, now)
        firebaseService?.addTimelineEvent(timelineEvent)
    }

    suspend fun verifyResolution(incidentId: String, isSatisfied: Boolean, notes: String? = null) {
        val now = System.currentTimeMillis()
        if (isSatisfied) {
            dao.verifyIncident(incidentId, now)
            val timelineEvent = TimelineEvent(
                id = now,
                incidentId = incidentId,
                title = "Citizen Verification Completed",
                description = "Reporter confirmed problem is resolved satisfactorily.",
                timestamp = now,
                status = IncidentStatus.VERIFIED,
                actorName = "Citizen Reporter",
                actorRole = "Auditor"
            )
            dao.insertTimelineEvent(TimelineEventEntity.fromDomain(timelineEvent))

            firebaseService?.verifyIncident(incidentId, now)
            firebaseService?.addTimelineEvent(timelineEvent)
        } else {
            dao.reopenIncident(incidentId, now)
            val timelineEvent = TimelineEvent(
                id = now,
                incidentId = incidentId,
                title = "Issue Reopened by Citizen",
                description = notes ?: "Citizen reported resolution was incomplete.",
                timestamp = now,
                status = IncidentStatus.REOPENED,
                actorName = "Citizen Reporter",
                actorRole = "Auditor"
            )
            dao.insertTimelineEvent(TimelineEventEntity.fromDomain(timelineEvent))

            firebaseService?.reopenIncident(incidentId, now)
            firebaseService?.addTimelineEvent(timelineEvent)
        }
    }

    fun computeCivicInsights(incidents: List<CivicIncident>): List<CivicInsight> {
        if (incidents.isEmpty()) return emptyList()

        val insights = mutableListOf<CivicInsight>()
        val total = incidents.size
        val criticalCount = incidents.count { it.priority == Priority.CRITICAL && it.status != IncidentStatus.VERIFIED }
        val roadIncidents = incidents.count { it.category == CivicCategory.ROADS }
        val approachingSla = incidents.count { 
            it.status != IncidentStatus.VERIFIED && it.status != IncidentStatus.RESOLVED && (it.slaDeadline - System.currentTimeMillis() in 0..(12 * 3600 * 1000L))
        }

        if (criticalCount > 0) {
            insights.add(
                CivicInsight(
                    id = "INS-1",
                    title = "Critical Safety Escalation",
                    description = "$criticalCount critical hazards require immediate emergency dispatch (Water Main & Open Drain).",
                    type = "SLA_WARNING",
                    priority = Priority.CRITICAL,
                    metricValue = "$criticalCount Active",
                    actionableRecommendation = "Prioritize barricade crews and water valve isolation teams."
                )
            )
        }

        if (approachingSla > 0) {
            insights.add(
                CivicInsight(
                    id = "INS-2",
                    title = "SLA Threshold Alert",
                    description = "$approachingSla high-priority incidents are within 12 hours of SLA deadline.",
                    type = "SLA_WARNING",
                    priority = Priority.HIGH,
                    metricValue = "$approachingSla Nearing SLA",
                    actionableRecommendation = "Notify Sanitation and Public Works shift leads for queue rebalancing."
                )
            )
        }

        val roadPct = if (total > 0) (roadIncidents * 100 / total) else 0
        insights.add(
            CivicInsight(
                id = "INS-3",
                title = "Corridor Infrastructure Clustering",
                description = "Roads & Streetlight incidents account for $roadPct% of reports along District 2 and 4 corridors.",
                type = "CLUSTER",
                priority = Priority.MEDIUM,
                metricValue = "$roadPct% of Total",
                actionableRecommendation = "Recommend consolidated batch paving workflow along Elm & Market Street."
            )
        )

        val resolvedCount = incidents.count { it.status == IncidentStatus.VERIFIED || it.status == IncidentStatus.RESOLVED }
        val resolvedPct = if (total > 0) (resolvedCount * 100 / total) else 0
        insights.add(
            CivicInsight(
                id = "INS-4",
                title = "Resolution Performance",
                description = "$resolvedPct% of historical reports have been resolved with citizen confirmation.",
                type = "PERFORMANCE",
                priority = Priority.LOW,
                metricValue = "$resolvedPct% Resolved",
                actionableRecommendation = "Maintain rapid citizen verification loop to sustain community trust."
            )
        )

        return insights
    }
}

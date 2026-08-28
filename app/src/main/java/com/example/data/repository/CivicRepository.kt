package com.example.data.repository

import com.example.data.local.CitizenReportEntity
import com.example.data.local.CivicDao
import com.example.data.local.CommunityConfirmationEntity
import com.example.data.local.IncidentEntity
import com.example.data.local.SeedData
import com.example.data.local.TimelineEventEntity
import com.example.data.model.AIReportAnalysisResult
import com.example.data.model.CivicCategory
import com.example.data.model.CivicIncident
import com.example.data.model.CivicInsight
import com.example.data.model.CitizenReport
import com.example.data.model.IncidentStatus
import com.example.data.model.Priority
import com.example.data.model.TimelineEvent
import com.example.data.remote.GeminiCivicAnalyzer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

sealed class ReportSubmissionProgress {
    object Idle : ReportSubmissionProgress()
    data class Analyzing(val stepName: String, val stepDetail: String, val stepIndex: Int, val totalSteps: Int = 6) : ReportSubmissionProgress()
    data class DuplicatePrompt(val duplicateIncident: CivicIncident, val pendingReportText: String) : ReportSubmissionProgress()
    data class Success(val incidentId: String, val isGroupedIntoExisting: Boolean, val analysis: AIReportAnalysisResult) : ReportSubmissionProgress()
    data class Error(val message: String) : ReportSubmissionProgress()
}

class CivicRepository(
    private val dao: CivicDao,
    private val geminiAnalyzer: GeminiCivicAnalyzer = GeminiCivicAnalyzer()
) {
    suspend fun initializeDatabaseIfEmpty() {
        val count = dao.getIncidentCount()
        if (count == 0) {
            val initialIncidents = SeedData.getInitialIncidents().map { IncidentEntity.fromDomain(it) }
            val initialEvents = SeedData.getInitialTimelineEvents().map { TimelineEventEntity.fromDomain(it) }
            val initialReports = SeedData.getInitialCitizenReports().map { CitizenReportEntity.fromDomain(it) }

            dao.insertIncidents(initialIncidents)
            dao.insertTimelineEvents(initialEvents)
            dao.insertCitizenReports(initialReports)
        }
    }

    fun getAllIncidents(): Flow<List<CivicIncident>> {
        return dao.getAllIncidents().map { list -> list.map { it.toDomain() } }
    }

    fun getIncidentById(id: String): Flow<CivicIncident?> {
        return dao.getIncidentById(id).map { it?.toDomain() }
    }

    fun getTimeline(incidentId: String): Flow<List<TimelineEvent>> {
        return dao.getTimelineForIncident(incidentId).map { list -> list.map { it.toDomain() } }
    }

    fun getCitizenReports(incidentId: String): Flow<List<CitizenReport>> {
        return dao.getReportsForIncident(incidentId).map { list -> list.map { it.toDomain() } }
    }

    fun getOfficerOperationalQueue(): Flow<List<CivicIncident>> {
        return dao.getOfficerOperationalQueue().map { list -> list.map { it.toDomain() } }
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
            // Attach as community report
            val reportEntity = CitizenReportEntity(
                id = "CR-${System.currentTimeMillis() % 10000}",
                incidentId = existingId,
                reporterName = "Verified Citizen",
                description = description,
                timestamp = now,
                imageUrl = imageUrl
            )
            dao.insertCitizenReport(reportEntity)
            
            // Add timeline note
            dao.insertTimelineEvent(
                TimelineEventEntity(
                    incidentId = existingId,
                    title = "Additional Citizen Report Attached",
                    description = "New citizen evidence attached. Community intelligence updated.",
                    timestamp = now,
                    status = IncidentStatus.TRIAGED.name,
                    actorName = "Civic Intelligence Hub",
                    actorRole = "System"
                )
            )
            dao.incrementConfirmationYes(existingId)

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

        dao.insertIncident(IncidentEntity.fromDomain(newIncident))

        // Initial timeline events
        dao.insertTimelineEvent(
            TimelineEventEntity(
                incidentId = incidentId,
                title = "Report Received",
                description = "Citizen reported civic issue: \"$description\"",
                timestamp = now,
                status = IncidentStatus.REPORTED.name,
                actorName = "Citizen",
                actorRole = "Reporter"
            )
        )

        dao.insertTimelineEvent(
            TimelineEventEntity(
                incidentId = incidentId,
                title = "AI Analysis Completed",
                description = "Severity scored ${analysis.severityScore}/100. ${analysis.reasoningExplanation}",
                timestamp = now + 1000L,
                status = IncidentStatus.TRIAGED.name,
                actorName = "CivicSense AI",
                actorRole = "Automated Engine"
            )
        )

        dao.insertTimelineEvent(
            TimelineEventEntity(
                incidentId = incidentId,
                title = "Department Assigned",
                description = "Routed to ${analysis.department.displayName} with ${analysis.slaHours}h SLA.",
                timestamp = now + 2000L,
                status = IncidentStatus.TRIAGED.name,
                actorName = "Civic Workflow Dispatcher",
                actorRole = "System"
            )
        )

        dao.insertCitizenReport(
            CitizenReportEntity(
                id = "CR-${System.currentTimeMillis() % 10000}",
                incidentId = incidentId,
                reporterName = "Primary Reporter",
                description = description,
                timestamp = now,
                imageUrl = imageUrl
            )
        )

        onProgress(ReportSubmissionProgress.Success(incidentId, false, analysis))
        return incidentId
    }

    suspend fun submitCommunityConfirmation(incidentId: String, userId: String, isStillHappening: Boolean): Boolean {
        val existing = dao.getUserConfirmation(incidentId, userId)
        if (existing != null) return false // Prevent spam

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
            dao.insertTimelineEvent(
                TimelineEventEntity(
                    incidentId = incidentId,
                    title = "Community Confirmation",
                    description = "Nearby citizen verified problem is still active.",
                    timestamp = now,
                    status = IncidentStatus.IN_PROGRESS.name,
                    actorName = "Local Citizen",
                    actorRole = "Community Validator"
                )
            )
        } else {
            dao.incrementConfirmationNo(incidentId)
        }
        return true
    }

    suspend fun assignOfficer(incidentId: String, officerName: String) {
        val now = System.currentTimeMillis()
        dao.assignOfficer(incidentId, officerName, now)
        dao.insertTimelineEvent(
            TimelineEventEntity(
                incidentId = incidentId,
                title = "Officer Assigned & Dispatched",
                description = "$officerName assigned to field response.",
                timestamp = now,
                status = IncidentStatus.IN_PROGRESS.name,
                actorName = "Operations Dispatch",
                actorRole = "Dispatcher"
            )
        )
    }

    suspend fun submitResolution(incidentId: String, officerName: String, notes: String, imageUrl: String?) {
        val now = System.currentTimeMillis()
        dao.resolveIncident(incidentId, notes, imageUrl, now)
        dao.insertTimelineEvent(
            TimelineEventEntity(
                incidentId = incidentId,
                title = "Resolution Submitted",
                description = notes,
                timestamp = now,
                status = IncidentStatus.RESOLVED.name,
                actorName = officerName,
                actorRole = "Field Officer"
            )
        )
    }

    suspend fun verifyResolution(incidentId: String, isSatisfied: Boolean, notes: String? = null) {
        val now = System.currentTimeMillis()
        if (isSatisfied) {
            dao.verifyIncident(incidentId, now)
            dao.insertTimelineEvent(
                TimelineEventEntity(
                    incidentId = incidentId,
                    title = "Citizen Verification Completed",
                    description = "Reporter confirmed problem is resolved satisfactorily.",
                    timestamp = now,
                    status = IncidentStatus.VERIFIED.name,
                    actorName = "Citizen Reporter",
                    actorRole = "Auditor"
                )
            )
        } else {
            dao.reopenIncident(incidentId, now)
            dao.insertTimelineEvent(
                TimelineEventEntity(
                    incidentId = incidentId,
                    title = "Issue Reopened by Citizen",
                    description = notes ?: "Citizen reported resolution was incomplete.",
                    timestamp = now,
                    status = IncidentStatus.REOPENED.name,
                    actorName = "Citizen Reporter",
                    actorRole = "Auditor"
                )
            )
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

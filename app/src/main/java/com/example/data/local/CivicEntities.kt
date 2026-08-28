package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CivicCategory
import com.example.data.model.CivicIncident
import com.example.data.model.CitizenReport
import com.example.data.model.Department
import com.example.data.model.IncidentStatus
import com.example.data.model.Priority
import com.example.data.model.TimelineEvent

@Entity(tableName = "incidents")
data class IncidentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val priority: String,
    val status: String,
    val department: String,
    val assignedOfficer: String? = null,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val zone: String,
    val reportedAt: Long,
    val updatedAt: Long,
    val slaHours: Int,
    val slaDeadline: Long,
    val safetyRiskScore: Int,
    val publicImpactScore: Int,
    val severityScore: Int,
    val recurringReportCount: Int,
    val communityConfirmationsYes: Int = 0,
    val communityConfirmationsNo: Int = 0,
    val imageUrl: String? = null,
    val resolutionNotes: String? = null,
    val resolutionImageUrl: String? = null,
    val aiSummary: String? = null,
    val isAuthorVerified: Boolean = false,
    val reporterId: String = "citizen_default"
) {
    fun toDomain(): CivicIncident {
        return CivicIncident(
            id = id,
            title = title,
            description = description,
            category = runCatching { CivicCategory.valueOf(category) }.getOrDefault(CivicCategory.ROADS),
            priority = runCatching { Priority.valueOf(priority) }.getOrDefault(Priority.MEDIUM),
            status = runCatching { IncidentStatus.valueOf(status) }.getOrDefault(IncidentStatus.REPORTED),
            department = runCatching { Department.valueOf(department) }.getOrDefault(Department.PUBLIC_WORKS),
            assignedOfficer = assignedOfficer,
            latitude = latitude,
            longitude = longitude,
            address = address,
            zone = zone,
            reportedAt = reportedAt,
            updatedAt = updatedAt,
            slaHours = slaHours,
            slaDeadline = slaDeadline,
            safetyRiskScore = safetyRiskScore,
            publicImpactScore = publicImpactScore,
            severityScore = severityScore,
            recurringReportCount = recurringReportCount,
            communityConfirmationsYes = communityConfirmationsYes,
            communityConfirmationsNo = communityConfirmationsNo,
            imageUrl = imageUrl,
            resolutionNotes = resolutionNotes,
            resolutionImageUrl = resolutionImageUrl,
            aiSummary = aiSummary,
            isAuthorVerified = isAuthorVerified,
            reporterId = reporterId
        )
    }

    companion object {
        fun fromDomain(domain: CivicIncident): IncidentEntity {
            return IncidentEntity(
                id = domain.id,
                title = domain.title,
                description = domain.description,
                category = domain.category.name,
                priority = domain.priority.name,
                status = domain.status.name,
                department = domain.department.name,
                assignedOfficer = domain.assignedOfficer,
                latitude = domain.latitude,
                longitude = domain.longitude,
                address = domain.address,
                zone = domain.zone,
                reportedAt = domain.reportedAt,
                updatedAt = domain.updatedAt,
                slaHours = domain.slaHours,
                slaDeadline = domain.slaDeadline,
                safetyRiskScore = domain.safetyRiskScore,
                publicImpactScore = domain.publicImpactScore,
                severityScore = domain.severityScore,
                recurringReportCount = domain.recurringReportCount,
                communityConfirmationsYes = domain.communityConfirmationsYes,
                communityConfirmationsNo = domain.communityConfirmationsNo,
                imageUrl = domain.imageUrl,
                resolutionNotes = domain.resolutionNotes,
                resolutionImageUrl = domain.resolutionImageUrl,
                aiSummary = domain.aiSummary,
                isAuthorVerified = domain.isAuthorVerified,
                reporterId = domain.reporterId
            )
        }
    }
}

@Entity(tableName = "timeline_events")
data class TimelineEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val incidentId: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val status: String,
    val actorName: String,
    val actorRole: String
) {
    fun toDomain(): TimelineEvent = TimelineEvent(
        id = id,
        incidentId = incidentId,
        title = title,
        description = description,
        timestamp = timestamp,
        status = runCatching { IncidentStatus.valueOf(status) }.getOrDefault(IncidentStatus.REPORTED),
        actorName = actorName,
        actorRole = actorRole
    )

    companion object {
        fun fromDomain(domain: TimelineEvent): TimelineEventEntity = TimelineEventEntity(
            id = domain.id,
            incidentId = domain.incidentId,
            title = domain.title,
            description = domain.description,
            timestamp = domain.timestamp,
            status = domain.status.name,
            actorName = domain.actorName,
            actorRole = domain.actorRole
        )
    }
}

@Entity(tableName = "citizen_reports")
data class CitizenReportEntity(
    @PrimaryKey val id: String,
    val incidentId: String,
    val reporterName: String,
    val description: String,
    val timestamp: Long,
    val imageUrl: String? = null
) {
    fun toDomain(): CitizenReport = CitizenReport(
        id = id,
        incidentId = incidentId,
        reporterName = reporterName,
        description = description,
        timestamp = timestamp,
        imageUrl = imageUrl
    )

    companion object {
        fun fromDomain(domain: CitizenReport): CitizenReportEntity = CitizenReportEntity(
            id = domain.id,
            incidentId = domain.incidentId,
            reporterName = domain.reporterName,
            description = domain.description,
            timestamp = domain.timestamp,
            imageUrl = domain.imageUrl
        )
    }
}

@Entity(tableName = "community_confirmations", primaryKeys = ["incidentId", "userId"])
data class CommunityConfirmationEntity(
    val incidentId: String,
    val userId: String,
    val isStillHappening: Boolean,
    val timestamp: Long
)

package com.example.data.model

enum class Priority(val label: String, val slaDefaultHours: Int) {
    CRITICAL("Critical", 12),
    HIGH("High", 24),
    MEDIUM("Medium", 48),
    LOW("Low", 96)
}

enum class IncidentStatus(val label: String) {
    REPORTED("Report Received"),
    TRIAGED("AI Triaged & Routed"),
    IN_PROGRESS("Work Started"),
    RESOLVED("Resolution Submitted"),
    VERIFIED("Citizen Verified"),
    REOPENED("Reopened for Review")
}

enum class CivicCategory(val displayName: String, val iconName: String) {
    ROADS("Roads & Potholes", "traffic"),
    SANITATION("Sanitation & Waste", "delete"),
    WATER_SEWAGE("Water & Sewage", "water_drop"),
    LIGHTING("Streetlights & Grid", "lightbulb"),
    TRAFFIC_SAFETY("Traffic & Safety", "warning"),
    PARKS("Parks & Public Spaces", "park"),
    PUBLIC_HAZARD("Emergency & Hazard", "emergency")
}

enum class Department(val displayName: String, val contactEmail: String) {
    PUBLIC_WORKS("Public Works Department", "publicworks@city.gov"),
    SANITATION("Sanitation & Waste Management", "waste@city.gov"),
    WATER_BOARD("Water & Sewage Authority", "water@city.gov"),
    TRANSPORTATION("Transportation & Traffic Management", "traffic@city.gov"),
    ENERGY("Municipal Energy & Lighting", "energy@city.gov"),
    PARKS_REC("Parks & Recreation", "parks@city.gov"),
    CIVIC_RESPONSE("Rapid Civic Action Team", "rapidresponse@city.gov")
}

enum class UserRole(val displayName: String, val subtitle: String) {
    CITIZEN("Citizen", "Report, track and verify community issues"),
    FIELD_OFFICER("Field Officer", "Inspect, assign and submit resolutions"),
    CIVIC_ADMIN("Civic Administrator", "Monitor citywide SLA and AI intelligence")
}

data class CivicIncident(
    val id: String,
    val title: String,
    val description: String,
    val category: CivicCategory,
    val priority: Priority,
    val status: IncidentStatus,
    val department: Department,
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
)

data class TimelineEvent(
    val id: Long = 0,
    val incidentId: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val status: IncidentStatus,
    val actorName: String,
    val actorRole: String
)

data class CitizenReport(
    val id: String,
    val incidentId: String,
    val reporterName: String,
    val description: String,
    val timestamp: Long,
    val imageUrl: String? = null
)

data class CivicInsight(
    val id: String,
    val title: String,
    val description: String,
    val type: String, // "TREND", "SLA_WARNING", "CLUSTER", "PERFORMANCE"
    val priority: Priority,
    val metricValue: String,
    val actionableRecommendation: String
)

data class AIReportAnalysisResult(
    val title: String,
    val category: CivicCategory,
    val priority: Priority,
    val department: Department,
    val safetyRiskScore: Int,
    val publicImpactScore: Int,
    val severityScore: Int,
    val reasoningExplanation: String,
    val slaHours: Int,
    val duplicateCandidateId: String? = null,
    val isDuplicate: Boolean = false
)

package com.example.data.local

import com.example.data.model.CivicCategory
import com.example.data.model.CivicIncident
import com.example.data.model.CitizenReport
import com.example.data.model.Department
import com.example.data.model.IncidentStatus
import com.example.data.model.Priority
import com.example.data.model.TimelineEvent

object SeedData {
    private val now = System.currentTimeMillis()
    private const val HOUR = 3600 * 1000L

    fun getInitialIncidents(): List<CivicIncident> = listOf(
        CivicIncident(
            id = "CS-1042",
            title = "Severe Pothole Cluster on 4th & Elm Ave",
            description = "Multiple deep potholes near the pedestrian crossing. Several vehicles swerving dangerously into the oncoming lane.",
            category = CivicCategory.ROADS,
            priority = Priority.HIGH,
            status = IncidentStatus.IN_PROGRESS,
            department = Department.PUBLIC_WORKS,
            assignedOfficer = "Officer Marcus Vance (Badge #412)",
            latitude = 37.7749,
            longitude = -122.4194,
            address = "420 Elm Street, District 4",
            zone = "North Corridor - Zone 2",
            reportedAt = now - 3 * HOUR,
            updatedAt = now - 45 * 60 * 1000L,
            slaHours = 24,
            slaDeadline = now + 21 * HOUR,
            safetyRiskScore = 82,
            publicImpactScore = 71,
            severityScore = 80,
            recurringReportCount = 4,
            communityConfirmationsYes = 14,
            communityConfirmationsNo = 1,
            imageUrl = null,
            aiSummary = "High safety risk identified due to multi-lane vehicular swerving near designated pedestrian zone.",
            isAuthorVerified = false,
            reporterId = "citizen_default"
        ),
        CivicIncident(
            id = "CS-1043",
            title = "Unattended Garbage Accumulation near High School",
            description = "Large accumulation of commercial & residential trash bags obstructing sidewalk and attracting pests near student entrance.",
            category = CivicCategory.SANITATION,
            priority = Priority.HIGH,
            status = IncidentStatus.TRIAGED,
            department = Department.SANITATION,
            assignedOfficer = "Sanitation Crew Alpha-3",
            latitude = 37.7790,
            longitude = -122.4130,
            address = "850 School Lane, District 3",
            zone = "Downtown East - Zone 3",
            reportedAt = now - 5 * HOUR,
            updatedAt = now - 1 * HOUR,
            slaHours = 24,
            slaDeadline = now + 19 * HOUR,
            safetyRiskScore = 68,
            publicImpactScore = 85,
            severityScore = 75,
            recurringReportCount = 7,
            communityConfirmationsYes = 22,
            communityConfirmationsNo = 0,
            imageUrl = null,
            aiSummary = "Elevated public health impact. Proximity to school requires prioritized 24-hour collection sweep.",
            isAuthorVerified = false,
            reporterId = "citizen_other"
        ),
        CivicIncident(
            id = "CS-1044",
            title = "Dark Corridor: 4 Consecutive Streetlights Extinguished",
            description = "Streetlights completely off from 12th to 14th Ave. Dark corridor creates severe low-visibility risk for night commuters.",
            category = CivicCategory.LIGHTING,
            priority = Priority.MEDIUM,
            status = IncidentStatus.TRIAGED,
            department = Department.ENERGY,
            assignedOfficer = null,
            latitude = 37.7680,
            longitude = -122.4250,
            address = "1300 Oak Ridge Ave, District 5",
            zone = "Westside Residential - Zone 5",
            reportedAt = now - 12 * HOUR,
            updatedAt = now - 2 * HOUR,
            slaHours = 48,
            slaDeadline = now + 36 * HOUR,
            safetyRiskScore = 60,
            publicImpactScore = 65,
            severityScore = 60,
            recurringReportCount = 3,
            communityConfirmationsYes = 9,
            communityConfirmationsNo = 0,
            imageUrl = null,
            aiSummary = "Corridor pattern detected. 3 independent citizen reports automatically clustered into single grid circuit case.",
            isAuthorVerified = false,
            reporterId = "citizen_default"
        ),
        CivicIncident(
            id = "CS-1045",
            title = "Critical Water Main Fracture & Sidewalk Flooding",
            description = "High-pressure potable water escaping from beneath sidewalk slab. Flow eroding asphalt foundation rapidly.",
            category = CivicCategory.WATER_SEWAGE,
            priority = Priority.CRITICAL,
            status = IncidentStatus.IN_PROGRESS,
            department = Department.WATER_BOARD,
            assignedOfficer = "Supervisor Elena Rostova (Water Dispatch)",
            latitude = 37.7810,
            longitude = -122.4080,
            address = "210 Greenfield Plaza, District 1",
            zone = "Central Business - Zone 1",
            reportedAt = now - 2 * HOUR,
            updatedAt = now - 20 * 60 * 1000L,
            slaHours = 12,
            slaDeadline = now + 10 * HOUR,
            safetyRiskScore = 94,
            publicImpactScore = 90,
            severityScore = 92,
            recurringReportCount = 6,
            communityConfirmationsYes = 31,
            communityConfirmationsNo = 0,
            imageUrl = null,
            aiSummary = "Critical infrastructure damage with active structural erosion. Emergency rapid response dispatched under 12h SLA.",
            isAuthorVerified = false,
            reporterId = "citizen_other"
        ),
        CivicIncident(
            id = "CS-1046",
            title = "Open Storm Drain Manhole on Main Crosswalk",
            description = "Heavy cast-iron cover dislodged and missing. Extreme hazard for cyclists and night pedestrians.",
            category = CivicCategory.PUBLIC_HAZARD,
            priority = Priority.CRITICAL,
            status = IncidentStatus.IN_PROGRESS,
            department = Department.CIVIC_RESPONSE,
            assignedOfficer = "Officer Ray Chen (Rapid Response)",
            latitude = 37.7715,
            longitude = -122.4170,
            address = "7th & Market Blvd Crosswalk",
            zone = "Market Transit - Zone 2",
            reportedAt = now - 1 * HOUR,
            updatedAt = now - 15 * 60 * 1000L,
            slaHours = 12,
            slaDeadline = now + 11 * HOUR,
            safetyRiskScore = 98,
            publicImpactScore = 88,
            severityScore = 95,
            recurringReportCount = 8,
            communityConfirmationsYes = 19,
            communityConfirmationsNo = 0,
            imageUrl = null,
            aiSummary = "Immediate severe fall/collision hazard. Barricade crew deployed pending replacement lid installation.",
            isAuthorVerified = false,
            reporterId = "citizen_default"
        ),
        CivicIncident(
            id = "CS-1041",
            title = "Large Fallen Tree Limb Blocking Bike Lane",
            description = "Storm limb cleared and path restored by Parks & Recreation maintenance crew.",
            category = CivicCategory.PARKS,
            priority = Priority.LOW,
            status = IncidentStatus.VERIFIED,
            department = Department.PARKS_REC,
            assignedOfficer = "Crew Leader Dave Miller",
            latitude = 37.7650,
            longitude = -122.4300,
            address = "Maple Park Perimeter Trail",
            zone = "South Park - Zone 4",
            reportedAt = now - 28 * HOUR,
            updatedAt = now - 4 * HOUR,
            slaHours = 96,
            slaDeadline = now - 4 * HOUR,
            safetyRiskScore = 30,
            publicImpactScore = 40,
            severityScore = 35,
            recurringReportCount = 2,
            communityConfirmationsYes = 18,
            communityConfirmationsNo = 0,
            imageUrl = null,
            resolutionNotes = "Limb sawed, mulched and removed. Bike path fully reopened and swept.",
            aiSummary = "Incident successfully resolved and confirmed through citizen photo verification.",
            isAuthorVerified = true,
            reporterId = "citizen_default"
        )
    )

    fun getInitialTimelineEvents(): List<TimelineEvent> = listOf(
        TimelineEvent(
            incidentId = "CS-1042",
            title = "Report Received",
            description = "Citizen reported pothole hazard with photo evidence.",
            timestamp = now - 3 * HOUR,
            status = IncidentStatus.REPORTED,
            actorName = "Citizen App User",
            actorRole = "Reporter"
        ),
        TimelineEvent(
            incidentId = "CS-1042",
            title = "AI Analysis Completed",
            description = "Severity scored 80/100, Safety Risk 82%. Categorized as Roads & Infrastructure. High Priority assigned.",
            timestamp = now - 3 * HOUR + 2000L,
            status = IncidentStatus.TRIAGED,
            actorName = "CivicSense AI Engine",
            actorRole = "Automated System"
        ),
        TimelineEvent(
            incidentId = "CS-1042",
            title = "Department Routed",
            description = "Incident routed to Public Works Department. 24-hour resolution SLA activated.",
            timestamp = now - 3 * HOUR + 5000L,
            status = IncidentStatus.TRIAGED,
            actorName = "Civic Workflow Dispatcher",
            actorRole = "System"
        ),
        TimelineEvent(
            incidentId = "CS-1042",
            title = "Officer Assigned & Dispatched",
            description = "Officer Marcus Vance assigned to patch crew with cold-mix asphalt equipment.",
            timestamp = now - 2 * HOUR,
            status = IncidentStatus.IN_PROGRESS,
            actorName = "Public Works Dispatch",
            actorRole = "Operations Desk"
        ),
        TimelineEvent(
            incidentId = "CS-1042",
            title = "Work Started On Site",
            description = "Repair truck on scene. Safety cones placed and excavation started.",
            timestamp = now - 45 * 60 * 1000L,
            status = IncidentStatus.IN_PROGRESS,
            actorName = "Officer Marcus Vance",
            actorRole = "Field Officer"
        ),
        // CS-1045 Events
        TimelineEvent(
            incidentId = "CS-1045",
            title = "Critical Report Logged",
            description = "Water main fracture detected with high flow rate.",
            timestamp = now - 2 * HOUR,
            status = IncidentStatus.REPORTED,
            actorName = "Citizen App User",
            actorRole = "Reporter"
        ),
        TimelineEvent(
            incidentId = "CS-1045",
            title = "Emergency AI Classification",
            description = "Critical 12h SLA triggered. Automatic priority escalation to Water & Sewage Authority.",
            timestamp = now - 2 * HOUR + 1000L,
            status = IncidentStatus.TRIAGED,
            actorName = "CivicSense AI Engine",
            actorRole = "Automated System"
        ),
        TimelineEvent(
            incidentId = "CS-1045",
            title = "Emergency Valve Crew En Route",
            description = "Supervisor Elena Rostova en route with shut-off isolation equipment.",
            timestamp = now - 20 * 60 * 1000L,
            status = IncidentStatus.IN_PROGRESS,
            actorName = "Water Board Control Room",
            actorRole = "Dispatch"
        )
    )

    fun getInitialCitizenReports(): List<CitizenReport> = listOf(
        CitizenReport(
            id = "CR-101",
            incidentId = "CS-1042",
            reporterName = "A. Miller",
            description = "Pothole is getting deeper every day, hit it this morning on commute.",
            timestamp = now - 3 * HOUR
        ),
        CitizenReport(
            id = "CR-102",
            incidentId = "CS-1042",
            reporterName = "S. Davis",
            description = "Saw two cars pop tires here. Needs urgent repair.",
            timestamp = now - 2 * HOUR - 20 * 60 * 1000L
        ),
        CitizenReport(
            id = "CR-103",
            incidentId = "CS-1042",
            reporterName = "T. Johnson",
            description = "Traffic swerving into opposite lane to avoid hole.",
            timestamp = now - 1 * HOUR - 15 * 60 * 1000L
        ),
        CitizenReport(
            id = "CR-201",
            incidentId = "CS-1044",
            reporterName = "R. Lee",
            description = "Light pole #12 is dark, very pitch black at night.",
            timestamp = now - 12 * HOUR
        ),
        CitizenReport(
            id = "CR-202",
            incidentId = "CS-1044",
            reporterName = "M. Gomez",
            description = "Entire 3-block stretch of streetlights has been out for 2 nights.",
            timestamp = now - 6 * HOUR
        )
    )
}

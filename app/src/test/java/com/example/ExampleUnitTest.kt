package com.example

import com.example.data.model.CivicCategory
import com.example.data.model.CivicIncident
import com.example.data.model.Department
import com.example.data.model.IncidentStatus
import com.example.data.model.Priority
import com.example.data.remote.GeminiCivicAnalyzer
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    private val analyzer = GeminiCivicAnalyzer()

    @Test
    fun testPotholeKeywordRoutesToRoadsAndPublicWorks() {
        val result = analyzer.fallbackCivicAnalysis(
            description = "Huge deep pothole damaging car tires near school",
            address = "742 Evergreen Terrace",
            existing = emptyList()
        )
        assertEquals(CivicCategory.ROADS, result.category)
        assertEquals(Department.PUBLIC_WORKS, result.department)
        assertEquals(Priority.HIGH, result.priority)
        assertEquals(24, result.slaHours)
        assertTrue(result.severityScore > 70)
    }

    @Test
    fun testHazardRoutesToCriticalPriority() {
        val result = analyzer.fallbackCivicAnalysis(
            description = "Open manhole cover with danger of pedestrians falling in",
            address = "500 Main Street",
            existing = emptyList()
        )
        assertEquals(CivicCategory.PUBLIC_HAZARD, result.category)
        assertEquals(Priority.CRITICAL, result.priority)
        assertEquals(12, result.slaHours)
        assertTrue(result.safetyRiskScore >= 90)
    }

    @Test
    fun testSanitationKeywordRoutesToSanitation() {
        val result = analyzer.fallbackCivicAnalysis(
            description = "Overflowing garbage dump on corner",
            address = "12 Oak Ave",
            existing = emptyList()
        )
        assertEquals(CivicCategory.SANITATION, result.category)
        assertEquals(Department.SANITATION, result.department)
    }

    @Test
    fun testDuplicateDetectionMatch() {
        val existingIncident = CivicIncident(
            id = "CS-1001",
            title = "Existing Pothole",
            description = "Road crack",
            category = CivicCategory.ROADS,
            priority = Priority.MEDIUM,
            status = IncidentStatus.TRIAGED,
            department = Department.PUBLIC_WORKS,
            assignedOfficer = null,
            latitude = 37.77,
            longitude = -122.41,
            address = "742 Evergreen Terr",
            zone = "Zone A",
            reportedAt = 1000L,
            updatedAt = 1000L,
            slaHours = 48,
            slaDeadline = 2000L,
            safetyRiskScore = 60,
            publicImpactScore = 60,
            severityScore = 60,
            recurringReportCount = 1,
            communityConfirmationsYes = 1,
            communityConfirmationsNo = 0
        )

        val result = analyzer.fallbackCivicAnalysis(
            description = "Cracked road asphalt",
            address = "742 Evergreen Terr",
            existing = listOf(existingIncident)
        )
        assertTrue(result.isDuplicate)
        assertEquals("CS-1001", result.duplicateCandidateId)
    }
}

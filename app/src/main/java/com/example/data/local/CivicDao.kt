package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CivicDao {
    @Query("SELECT * FROM incidents ORDER BY updatedAt DESC")
    fun getAllIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE id = :id LIMIT 1")
    fun getIncidentById(id: String): Flow<IncidentEntity?>

    @Query("SELECT * FROM incidents WHERE id = :id LIMIT 1")
    suspend fun getIncidentByIdOnce(id: String): IncidentEntity?

    @Query("SELECT * FROM incidents WHERE reporterId = :reporterId ORDER BY reportedAt DESC")
    fun getIncidentsByReporter(reporterId: String): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE status != 'VERIFIED' ORDER BY CASE priority WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END, slaDeadline ASC")
    fun getOfficerOperationalQueue(): Flow<List<IncidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncidents(incidents: List<IncidentEntity>)

    @Update
    suspend fun updateIncident(incident: IncidentEntity)

    @Query("UPDATE incidents SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateIncidentStatus(id: String, status: String, updatedAt: Long)

    @Query("UPDATE incidents SET assignedOfficer = :officerName, status = 'IN_PROGRESS', updatedAt = :updatedAt WHERE id = :id")
    suspend fun assignOfficer(id: String, officerName: String, updatedAt: Long)

    @Query("UPDATE incidents SET status = 'RESOLVED', resolutionNotes = :notes, resolutionImageUrl = :imageUrl, updatedAt = :updatedAt WHERE id = :id")
    suspend fun resolveIncident(id: String, notes: String, imageUrl: String?, updatedAt: Long)

    @Query("UPDATE incidents SET status = 'VERIFIED', isAuthorVerified = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun verifyIncident(id: String, updatedAt: Long)

    @Query("UPDATE incidents SET status = 'REOPENED', updatedAt = :updatedAt WHERE id = :id")
    suspend fun reopenIncident(id: String, updatedAt: Long)

    @Query("UPDATE incidents SET communityConfirmationsYes = communityConfirmationsYes + 1 WHERE id = :id")
    suspend fun incrementConfirmationYes(id: String)

    @Query("UPDATE incidents SET communityConfirmationsNo = communityConfirmationsNo + 1 WHERE id = :id")
    suspend fun incrementConfirmationNo(id: String)

    // Timeline Events
    @Query("SELECT * FROM timeline_events WHERE incidentId = :incidentId ORDER BY timestamp ASC")
    fun getTimelineForIncident(incidentId: String): Flow<List<TimelineEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimelineEvent(event: TimelineEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimelineEvents(events: List<TimelineEventEntity>)

    // Citizen Reports
    @Query("SELECT * FROM citizen_reports WHERE incidentId = :incidentId ORDER BY timestamp ASC")
    fun getReportsForIncident(incidentId: String): Flow<List<CitizenReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCitizenReport(report: CitizenReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCitizenReports(reports: List<CitizenReportEntity>)

    // Community Confirmations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfirmation(confirmation: CommunityConfirmationEntity)

    @Query("SELECT * FROM community_confirmations WHERE incidentId = :incidentId AND userId = :userId LIMIT 1")
    suspend fun getUserConfirmation(incidentId: String, userId: String): CommunityConfirmationEntity?

    @Query("SELECT COUNT(*) FROM incidents")
    suspend fun getIncidentCount(): Int
}

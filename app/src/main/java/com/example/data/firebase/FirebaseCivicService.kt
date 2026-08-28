package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.CitizenReport
import com.example.data.model.CivicCategory
import com.example.data.model.CivicIncident
import com.example.data.model.Department
import com.example.data.model.IncidentStatus
import com.example.data.model.Priority
import com.example.data.model.TimelineEvent
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseCivicService(private val context: Context) {
    private val tag = "FirebaseCivicService"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            ensureFirebaseInitialized()
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize Firebase Firestore: ${e.message}", e)
            null
        }
    }

    private fun ensureFirebaseInitialized() {
        if (FirebaseApp.getApps(context).isEmpty()) {
            try {
                // Try initializing with available system resources or fallback options
                FirebaseApp.initializeApp(context)
                Log.d(tag, "FirebaseApp initialized via default context.")
            } catch (e: Exception) {
                Log.w(tag, "Default FirebaseApp init failed, checking manual config: ${e.message}")
            }
        }
    }

    fun isAvailable(): Boolean = firestore != null

    // Collections
    private val incidentsCollection get() = firestore?.collection("incidents")
    private val timelineCollection get() = firestore?.collection("timeline_events")
    private val reportsCollection get() = firestore?.collection("citizen_reports")
    private val confirmationsCollection get() = firestore?.collection("community_confirmations")

    // Real-time incident stream
    fun getAllIncidentsStream(): Flow<List<CivicIncident>> = callbackFlow {
        val collection = incidentsCollection
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = collection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Firestore getAllIncidents notice: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc -> docToIncident(doc) }
                        .sortedByDescending { it.updatedAt }
                    trySend(list)
                }
            }

        awaitClose { registration.remove() }
    }

    // Real-time single incident stream
    fun getIncidentStream(id: String): Flow<CivicIncident?> = callbackFlow {
        val collection = incidentsCollection
        if (collection == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = collection.document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Firestore getIncident notice for $id: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(docToIncident(snapshot))
                } else {
                    trySend(null)
                }
            }

        awaitClose { registration.remove() }
    }

    // Real-time timeline events for an incident
    fun getTimelineStream(incidentId: String): Flow<List<TimelineEvent>> = callbackFlow {
        val collection = timelineCollection
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = collection
            .whereEqualTo("incidentId", incidentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Firestore getTimeline notice for $incidentId: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc -> docToTimelineEvent(doc) }
                        .sortedBy { it.timestamp }
                    trySend(list)
                }
            }

        awaitClose { registration.remove() }
    }

    // Real-time citizen reports stream
    fun getCitizenReportsStream(incidentId: String): Flow<List<CitizenReport>> = callbackFlow {
        val collection = reportsCollection
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = collection
            .whereEqualTo("incidentId", incidentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Firestore getCitizenReports notice for $incidentId: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc -> docToCitizenReport(doc) }
                        .sortedBy { it.timestamp }
                    trySend(list)
                }
            }

        awaitClose { registration.remove() }
    }

    // Save or update an incident in Firestore
    suspend fun saveIncident(incident: CivicIncident): Boolean {
        return try {
            incidentsCollection?.document(incident.id)?.set(incidentToMap(incident), SetOptions.merge())?.await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error saving incident ${incident.id} to Firestore", e)
            false
        }
    }

    // Save batch of initial seed incidents if collection is empty
    suspend fun checkAndSeedInitialData(
        initialIncidents: List<CivicIncident>,
        initialEvents: List<TimelineEvent>,
        initialReports: List<CitizenReport>
    ) {
        val collection = incidentsCollection ?: return
        try {
            val countSnapshot = collection.limit(1).get().await()
            if (countSnapshot.isEmpty) {
                Log.d(tag, "Firestore empty, seeding ${initialIncidents.size} default civic incidents...")
                for (incident in initialIncidents) {
                    collection.document(incident.id).set(incidentToMap(incident)).await()
                }
                for (event in initialEvents) {
                    timelineCollection?.document("${event.incidentId}_${event.timestamp}")
                        ?.set(timelineToMap(event))?.await()
                }
                for (report in initialReports) {
                    reportsCollection?.document(report.id)?.set(reportToMap(report))?.await()
                }
                Log.d(tag, "Firestore seeding complete.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error seeding Firestore", e)
        }
    }

    suspend fun updateIncidentStatus(id: String, status: IncidentStatus, updatedAt: Long): Boolean {
        return try {
            incidentsCollection?.document(id)?.update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to updatedAt
                )
            )?.await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error updating status in Firestore", e)
            false
        }
    }

    suspend fun assignOfficer(id: String, officerName: String, updatedAt: Long): Boolean {
        return try {
            incidentsCollection?.document(id)?.update(
                mapOf(
                    "assignedOfficer" to officerName,
                    "status" to IncidentStatus.IN_PROGRESS.name,
                    "updatedAt" to updatedAt
                )
            )?.await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error assigning officer in Firestore", e)
            false
        }
    }

    suspend fun resolveIncident(id: String, notes: String, imageUrl: String?, updatedAt: Long): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to IncidentStatus.RESOLVED.name,
                "resolutionNotes" to notes,
                "updatedAt" to updatedAt
            )
            if (imageUrl != null) updates["resolutionImageUrl"] = imageUrl
            incidentsCollection?.document(id)?.update(updates)?.await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error resolving incident in Firestore", e)
            false
        }
    }

    suspend fun verifyIncident(id: String, updatedAt: Long): Boolean {
        return try {
            incidentsCollection?.document(id)?.update(
                mapOf(
                    "status" to IncidentStatus.VERIFIED.name,
                    "isAuthorVerified" to true,
                    "updatedAt" to updatedAt
                )
            )?.await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error verifying incident in Firestore", e)
            false
        }
    }

    suspend fun reopenIncident(id: String, updatedAt: Long): Boolean {
        return try {
            incidentsCollection?.document(id)?.update(
                mapOf(
                    "status" to IncidentStatus.REOPENED.name,
                    "updatedAt" to updatedAt
                )
            )?.await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error reopening incident in Firestore", e)
            false
        }
    }

    suspend fun voteConfirmation(incidentId: String, userId: String, isTrue: Boolean): Boolean {
        return try {
            val fieldName = if (isTrue) "communityConfirmationsYes" else "communityConfirmationsNo"
            incidentsCollection?.document(incidentId)?.update(
                fieldName, FieldValue.increment(1),
                "updatedAt", System.currentTimeMillis()
            )?.await()

            confirmationsCollection?.document("${incidentId}_$userId")?.set(
                mapOf(
                    "incidentId" to incidentId,
                    "userId" to userId,
                    "confirmed" to isTrue,
                    "timestamp" to System.currentTimeMillis()
                )
            )?.await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error voting confirmation in Firestore", e)
            false
        }
    }

    suspend fun addTimelineEvent(event: TimelineEvent): Boolean {
        return try {
            val docId = "${event.incidentId}_${event.timestamp}_${(0..9999).random()}"
            timelineCollection?.document(docId)?.set(timelineToMap(event))?.await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error adding timeline event in Firestore", e)
            false
        }
    }

    suspend fun addCitizenReport(report: CitizenReport): Boolean {
        return try {
            reportsCollection?.document(report.id)?.set(reportToMap(report))?.await()
            incidentsCollection?.document(report.incidentId)?.update(
                "recurringReportCount", FieldValue.increment(1),
                "updatedAt", System.currentTimeMillis()
            )?.await()
            true
        } catch (e: Exception) {
            Log.e(tag, "Error adding citizen report in Firestore", e)
            false
        }
    }

    // Mapping Utilities
    private fun incidentToMap(i: CivicIncident): Map<String, Any?> = mapOf(
        "id" to i.id,
        "title" to i.title,
        "description" to i.description,
        "category" to i.category.name,
        "priority" to i.priority.name,
        "status" to i.status.name,
        "department" to i.department.name,
        "assignedOfficer" to i.assignedOfficer,
        "latitude" to i.latitude,
        "longitude" to i.longitude,
        "address" to i.address,
        "zone" to i.zone,
        "reportedAt" to i.reportedAt,
        "updatedAt" to i.updatedAt,
        "slaHours" to i.slaHours,
        "slaDeadline" to i.slaDeadline,
        "safetyRiskScore" to i.safetyRiskScore,
        "publicImpactScore" to i.publicImpactScore,
        "severityScore" to i.severityScore,
        "recurringReportCount" to i.recurringReportCount,
        "communityConfirmationsYes" to i.communityConfirmationsYes,
        "communityConfirmationsNo" to i.communityConfirmationsNo,
        "imageUrl" to i.imageUrl,
        "resolutionNotes" to i.resolutionNotes,
        "resolutionImageUrl" to i.resolutionImageUrl,
        "aiSummary" to i.aiSummary,
        "isAuthorVerified" to i.isAuthorVerified,
        "reporterId" to i.reporterId
    )

    private fun docToIncident(doc: DocumentSnapshot): CivicIncident? {
        return try {
            val categoryStr = doc.getString("category") ?: CivicCategory.ROADS.name
            val priorityStr = doc.getString("priority") ?: Priority.MEDIUM.name
            val statusStr = doc.getString("status") ?: IncidentStatus.REPORTED.name
            val deptStr = doc.getString("department") ?: Department.PUBLIC_WORKS.name

            CivicIncident(
                id = doc.getString("id") ?: doc.id,
                title = doc.getString("title") ?: "Civic Issue",
                description = doc.getString("description") ?: "",
                category = try { CivicCategory.valueOf(categoryStr) } catch (e: Exception) { CivicCategory.ROADS },
                priority = try { Priority.valueOf(priorityStr) } catch (e: Exception) { Priority.MEDIUM },
                status = try { IncidentStatus.valueOf(statusStr) } catch (e: Exception) { IncidentStatus.REPORTED },
                department = try { Department.valueOf(deptStr) } catch (e: Exception) { Department.PUBLIC_WORKS },
                assignedOfficer = doc.getString("assignedOfficer"),
                latitude = doc.getDouble("latitude") ?: 37.7749,
                longitude = doc.getDouble("longitude") ?: -122.4194,
                address = doc.getString("address") ?: "Downtown District",
                zone = doc.getString("zone") ?: "Central Zone",
                reportedAt = doc.getLong("reportedAt") ?: System.currentTimeMillis(),
                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                slaHours = (doc.getLong("slaHours") ?: 24L).toInt(),
                slaDeadline = doc.getLong("slaDeadline") ?: (System.currentTimeMillis() + 86400000L),
                safetyRiskScore = (doc.getLong("safetyRiskScore") ?: 50L).toInt(),
                publicImpactScore = (doc.getLong("publicImpactScore") ?: 50L).toInt(),
                severityScore = (doc.getLong("severityScore") ?: 50L).toInt(),
                recurringReportCount = (doc.getLong("recurringReportCount") ?: 1L).toInt(),
                communityConfirmationsYes = (doc.getLong("communityConfirmationsYes") ?: 0L).toInt(),
                communityConfirmationsNo = (doc.getLong("communityConfirmationsNo") ?: 0L).toInt(),
                imageUrl = doc.getString("imageUrl"),
                resolutionNotes = doc.getString("resolutionNotes"),
                resolutionImageUrl = doc.getString("resolutionImageUrl"),
                aiSummary = doc.getString("aiSummary"),
                isAuthorVerified = doc.getBoolean("isAuthorVerified") ?: false,
                reporterId = doc.getString("reporterId") ?: "citizen_default"
            )
        } catch (e: Exception) {
            Log.e(tag, "Error parsing incident doc ${doc.id}", e)
            null
        }
    }

    private fun timelineToMap(e: TimelineEvent): Map<String, Any?> = mapOf(
        "incidentId" to e.incidentId,
        "title" to e.title,
        "description" to e.description,
        "timestamp" to e.timestamp,
        "status" to e.status.name,
        "actorName" to e.actorName,
        "actorRole" to e.actorRole
    )

    private fun docToTimelineEvent(doc: DocumentSnapshot): TimelineEvent? {
        return try {
            val statusStr = doc.getString("status") ?: IncidentStatus.REPORTED.name
            TimelineEvent(
                id = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                incidentId = doc.getString("incidentId") ?: "",
                title = doc.getString("title") ?: "Event",
                description = doc.getString("description") ?: "",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                status = try { IncidentStatus.valueOf(statusStr) } catch (e: Exception) { IncidentStatus.REPORTED },
                actorName = doc.getString("actorName") ?: "Civic System",
                actorRole = doc.getString("actorRole") ?: "System"
            )
        } catch (e: Exception) {
            Log.e(tag, "Error parsing timeline doc ${doc.id}", e)
            null
        }
    }

    private fun reportToMap(r: CitizenReport): Map<String, Any?> = mapOf(
        "id" to r.id,
        "incidentId" to r.incidentId,
        "reporterName" to r.reporterName,
        "description" to r.description,
        "timestamp" to r.timestamp,
        "imageUrl" to r.imageUrl
    )

    private fun docToCitizenReport(doc: DocumentSnapshot): CitizenReport? {
        return try {
            CitizenReport(
                id = doc.getString("id") ?: doc.id,
                incidentId = doc.getString("incidentId") ?: "",
                reporterName = doc.getString("reporterName") ?: "Anonymous Citizen",
                description = doc.getString("description") ?: "",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                imageUrl = doc.getString("imageUrl")
            )
        } catch (e: Exception) {
            Log.e(tag, "Error parsing report doc ${doc.id}", e)
            null
        }
    }
}

package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AIReportAnalysisResult
import com.example.data.model.CivicCategory
import com.example.data.model.CivicIncident
import com.example.data.model.Department
import com.example.data.model.Priority
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = "user"
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent?
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

class GeminiCivicAnalyzer {
    private val client: GeminiApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun analyzeCivicReport(
        userDescription: String,
        locationAddress: String,
        existingIncidents: List<CivicIncident>
    ): AIReportAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val prompt = buildCivicAnalysisPrompt(userDescription, locationAddress, existingIncidents)
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = prompt))
                        )
                    )
                )

                val response = client.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!responseText.isNullOrBlank()) {
                    val parsed = parseJsonResponse(responseText)
                    if (parsed != null) return@withContext parsed
                }
            } catch (e: Exception) {
                Log.w("CivicSenseAI", "Gemini API call failed, using local civic action engine: ${e.message}")
            }
        }

        // Resilient Fallback Civic Action Engine
        fallbackCivicAnalysis(userDescription, locationAddress, existingIncidents)
    }

    private fun buildCivicAnalysisPrompt(
        description: String,
        address: String,
        existingIncidents: List<CivicIncident>
    ): String {
        val activeIncidentsSummary = existingIncidents.take(10).joinToString("\n") {
            "- ID: ${it.id}, Title: ${it.title}, Category: ${it.category.name}, Address: ${it.address}"
        }

        return """
        You are the CivicSense AI Civic Action Engine. Analyze this citizen report:
        Problem Description: "$description"
        Report Location: "$address"
        
        Active Nearby Incidents:
        $activeIncidentsSummary
        
        Output strictly a single valid JSON object (no markdown quotes, no triple backticks) with:
        {
          "title": "Concise professional title under 8 words",
          "category": "One of: ROADS, SANITATION, WATER_SEWAGE, LIGHTING, TRAFFIC_SAFETY, PARKS, PUBLIC_HAZARD",
          "priority": "One of: CRITICAL, HIGH, MEDIUM, LOW",
          "department": "One of: PUBLIC_WORKS, SANITATION, WATER_BOARD, TRANSPORTATION, ENERGY, PARKS_REC, CIVIC_RESPONSE",
          "safetyRiskScore": integer 0-100,
          "publicImpactScore": integer 0-100,
          "severityScore": integer 0-100,
          "reasoningExplanation": "1-2 sentences explaining why this priority and department were chosen based on safety, location and impact.",
          "slaHours": integer (12 for critical, 24 for high, 48 for medium, 96 for low),
          "duplicateCandidateId": "Incident ID if this matches an existing active incident, or null",
          "isDuplicate": boolean true if matches an active incident
        }
        """.trimIndent()
    }

    private fun parseJsonResponse(raw: String): AIReportAnalysisResult? {
        return try {
            val clean = raw.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val json = JSONObject(clean)
            val categoryStr = json.optString("category", "ROADS")
            val priorityStr = json.optString("priority", "MEDIUM")
            val departmentStr = json.optString("department", "PUBLIC_WORKS")
            
            val category = runCatching { CivicCategory.valueOf(categoryStr) }.getOrDefault(CivicCategory.ROADS)
            val priority = runCatching { Priority.valueOf(priorityStr) }.getOrDefault(Priority.MEDIUM)
            val department = runCatching { Department.valueOf(departmentStr) }.getOrDefault(Department.PUBLIC_WORKS)

            AIReportAnalysisResult(
                title = json.optString("title", "Civic Problem Report"),
                category = category,
                priority = priority,
                department = department,
                safetyRiskScore = json.optInt("safetyRiskScore", 65).coerceIn(0, 100),
                publicImpactScore = json.optInt("publicImpactScore", 70).coerceIn(0, 100),
                severityScore = json.optInt("severityScore", 68).coerceIn(0, 100),
                reasoningExplanation = json.optString("reasoningExplanation", "Analyzed by CivicSense engine based on public safety risk."),
                slaHours = json.optInt("slaHours", priority.slaDefaultHours),
                duplicateCandidateId = if (json.isNull("duplicateCandidateId")) null else json.optString("duplicateCandidateId"),
                isDuplicate = json.optBoolean("isDuplicate", false)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun fallbackCivicAnalysis(
        description: String,
        address: String,
        existing: List<CivicIncident>
    ): AIReportAnalysisResult {
        val lower = description.lowercase()

        // Category & Department determination
        val (category, department, defaultTitle) = when {
            lower.contains("pothole") || lower.contains("road") || lower.contains("asphalt") || lower.contains("cracked street") -> 
                Triple(CivicCategory.ROADS, Department.PUBLIC_WORKS, "Road Surface & Pothole Damage")
            lower.contains("garbage") || lower.contains("trash") || lower.contains("waste") || lower.contains("dump") || lower.contains("litter") -> 
                Triple(CivicCategory.SANITATION, Department.SANITATION, "Sanitation & Trash Accumulation")
            lower.contains("water") || lower.contains("leak") || lower.contains("sewage") || lower.contains("drain") || lower.contains("pipe") || lower.contains("flooding") -> 
                Triple(CivicCategory.WATER_SEWAGE, Department.WATER_BOARD, "Water Main & Drainage Issue")
            lower.contains("light") || lower.contains("dark") || lower.contains("lamp") || lower.contains("pole") || lower.contains("electric") -> 
                Triple(CivicCategory.LIGHTING, Department.ENERGY, "Streetlight & Power Failure")
            lower.contains("traffic") || lower.contains("signal") || lower.contains("sign") || lower.contains("speed") || lower.contains("intersection") -> 
                Triple(CivicCategory.TRAFFIC_SAFETY, Department.TRANSPORTATION, "Traffic Control & Signage Issue")
            lower.contains("manhole") || lower.contains("hazard") || lower.contains("sinkhole") || lower.contains("danger") || lower.contains("wire") -> 
                Triple(CivicCategory.PUBLIC_HAZARD, Department.CIVIC_RESPONSE, "Critical Public Safety Hazard")
            lower.contains("park") || lower.contains("tree") || lower.contains("bench") || lower.contains("playground") || lower.contains("grass") -> 
                Triple(CivicCategory.PARKS, Department.PARKS_REC, "Park Facility & Grounds Issue")
            else -> 
                Triple(CivicCategory.ROADS, Department.PUBLIC_WORKS, "Civic Infrastructure Issue")
        }

        // Priority Scoring
        val isEmergency = lower.contains("manhole") || lower.contains("danger") || lower.contains("flood") || lower.contains("urgent") || lower.contains("fire") || lower.contains("electric")
        val isHigh = lower.contains("deep") || lower.contains("school") || lower.contains("hospital") || lower.contains("busy") || lower.contains("swerving") || lower.contains("broken")

        val priority = when {
            isEmergency -> Priority.CRITICAL
            isHigh -> Priority.HIGH
            category == CivicCategory.PUBLIC_HAZARD -> Priority.CRITICAL
            category == CivicCategory.PARKS -> Priority.LOW
            else -> Priority.MEDIUM
        }

        val safetyRisk = when (priority) {
            Priority.CRITICAL -> 92
            Priority.HIGH -> 80
            Priority.MEDIUM -> 60
            Priority.LOW -> 35
        }

        val publicImpact = when (priority) {
            Priority.CRITICAL -> 88
            Priority.HIGH -> 74
            Priority.MEDIUM -> 62
            Priority.LOW -> 40
        }

        val severity = (safetyRisk * 0.6 + publicImpact * 0.4).toInt()

        // Check Duplicate
        val duplicate = existing.firstOrNull {
            it.category == category && (it.address.take(10).equals(address.take(10), ignoreCase = true) || lower.contains(it.category.name.lowercase()))
        }

        val title = if (description.length in 5..50) description.replaceFirstChar { it.uppercase() } else defaultTitle

        return AIReportAnalysisResult(
            title = title,
            category = category,
            priority = priority,
            department = department,
            safetyRiskScore = safetyRisk,
            publicImpactScore = publicImpact,
            severityScore = severity,
            reasoningExplanation = "Classified as ${priority.label} priority based on ${safetyRisk}% safety risk and public traffic exposure.",
            slaHours = priority.slaDefaultHours,
            duplicateCandidateId = duplicate?.id,
            isDuplicate = duplicate != null
        )
    }
}

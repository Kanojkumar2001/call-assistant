package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class GeminiPart(
    val text: String? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiRequest(
    val contents: List<GeminiContent>
)

data class GeminiCandidate(
    val content: GeminiContent?
)

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

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    suspend fun processVoicemailTranscript(callerName: String, transcriptText: String): AiAnalysisResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return generateLocalFallbackAnalysis(callerName, transcriptText)
        }

        val prompt = """
            You are an AI Missed Call & Voicemail Processing Engine.
            Analyze the following incoming caller voicemail transcript from '$callerName':
            "$transcriptText"

            Provide a response strictly formatted as JSON without markdown backticks:
            {
              "aiSummary": "<Concise 1-2 sentence summary of what caller needs>",
              "urgencyLevel": "<HIGH | MEDIUM | LOW>",
              "category": "<Emergency | Business | Personal | Doctor | General>",
              "detectedKeywords": "<3-5 key terms separated by comma>",
              "actionSuggested": "<Immediate recommended action for the user>",
              "sentiment": "<Urgent | Concerned | Neutral | Positive>",
              "suggestedReplySms": "<Short polite SMS draft to send back to caller>"
            }
        """.trimIndent()

        return try {
            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
            )
            val response = service.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (rawText != null) {
                parseAiJsonResponse(rawText, callerName, transcriptText)
            } else {
                generateLocalFallbackAnalysis(callerName, transcriptText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            generateLocalFallbackAnalysis(callerName, transcriptText)
        }
    }

    private fun parseAiJsonResponse(jsonRaw: String, callerName: String, transcriptText: String): AiAnalysisResult {
        try {
            val cleaned = jsonRaw.replace("```json", "").replace("```", "").trim()
            val jsonAdapter = moshi.adapter(AiAnalysisResult::class.java)
            val parsed = jsonAdapter.fromJson(cleaned)
            if (parsed != null) return parsed
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return generateLocalFallbackAnalysis(callerName, transcriptText)
    }

    fun generateLocalFallbackAnalysis(callerName: String, transcript: String): AiAnalysisResult {
        val lower = transcript.lowercase()
        val isEmergency = lower.contains("emergency") || lower.contains("hospital") || lower.contains("prescription") || lower.contains("doctor") || lower.contains("asap") || lower.contains("urgent") || lower.contains("immediately")
        val isBusiness = lower.contains("meeting") || lower.contains("contract") || lower.contains("client") || lower.contains("project") || lower.contains("budget") || lower.contains("payment")

        val urgency = if (isEmergency) "HIGH" else if (isBusiness) "MEDIUM" else "LOW"
        val category = if (lower.contains("prescription") || lower.contains("doctor") || lower.contains("hospital")) "Doctor"
            else if (isEmergency) "Emergency"
            else if (isBusiness) "Business"
            else "Personal"

        val keywords = when {
            lower.contains("prescription") -> "Prescription, Pharmacy, Urgent Doctor"
            lower.contains("contract") -> "Contract Review, Deadline, Client"
            lower.contains("meeting") -> "Meeting Reschedule, Project Sync"
            lower.contains("delivery") -> "Package Delivery, Gate Code"
            else -> "Voice Note, General Message"
        }

        val action = when (urgency) {
            "HIGH" -> "Call back immediately. Urgent request detected."
            "MEDIUM" -> "Review details and schedule a callback before end of day."
            else -> "Listen when convenient or send a quick SMS."
        }

        val sentiment = when (urgency) {
            "HIGH" -> "Urgent"
            "MEDIUM" -> "Concerned"
            else -> "Neutral"
        }

        val suggestedReply = "Hi $callerName, received your message regarding $category. Will follow up shortly!"

        return AiAnalysisResult(
            aiSummary = "Caller $callerName left a $urgency priority message regarding: \"${transcript.take(60)}...\"",
            urgencyLevel = urgency,
            category = category,
            detectedKeywords = keywords,
            actionSuggested = action,
            sentiment = sentiment,
            suggestedReplySms = suggestedReply
        )
    }
}

data class AiAnalysisResult(
    val aiSummary: String = "",
    val urgencyLevel: String = "MEDIUM",
    val category: String = "General",
    val detectedKeywords: String = "",
    val actionSuggested: String = "",
    val sentiment: String = "Neutral",
    val suggestedReplySms: String = ""
)

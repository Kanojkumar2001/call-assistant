package com.example.data.repository

import com.example.data.local.AssistantSettingsDao
import com.example.data.local.AssistantSettingsEntity
import com.example.data.local.VoicemailDao
import com.example.data.local.VoicemailEntity
import com.example.data.remote.GeminiApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class VoicemailRepository(
    private val voicemailDao: VoicemailDao,
    private val settingsDao: AssistantSettingsDao
) {
    val allVoicemails: Flow<List<VoicemailEntity>> = voicemailDao.getAllVoicemails()
    val highUrgencyVoicemails: Flow<List<VoicemailEntity>> = voicemailDao.getHighUrgencyVoicemails()
    val settings: Flow<AssistantSettingsEntity?> = settingsDao.getSettings()

    suspend fun prepopulateInitialDataIfNeeded() {
        val current = voicemailDao.getAllVoicemails().firstOrNull()
        if (current.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            val sampleVoicemails = listOf(
                VoicemailEntity(
                    callerName = "Dr. Robert Davis",
                    phoneNumber = "+1 (555) 019-2834",
                    timestamp = now - (15 * 60 * 1000), // 15 mins ago
                    durationSeconds = 48,
                    transcript = "Hello, this is Dr. Davis from St. Jude Medical Clinic calling regarding your prescription refill request. Please call our triage desk immediately before 5 PM to confirm dosage adjustment or we cannot release the prescription today.",
                    aiSummary = "Urgent call from Dr. Davis regarding prescription dosage approval needed before 5 PM.",
                    urgencyLevel = "HIGH",
                    category = "Doctor",
                    detectedKeywords = "Prescription, Medical Clinic, Dosage, Triage Desk",
                    isRead = false,
                    actionSuggested = "Call back Dr. Davis at St. Jude Clinic before 5 PM.",
                    sentiment = "Urgent"
                ),
                VoicemailEntity(
                    callerName = "Marcus Vance (Vance Corp)",
                    phoneNumber = "+1 (555) 438-9201",
                    timestamp = now - (2 * 3600 * 1000), // 2 hours ago
                    durationSeconds = 64,
                    transcript = "Hi there, Marcus here. We reviewed the Q3 software architecture proposal and agreed on the terms. The legal team signed off on the contract. Please confirm if we can initiate onboarding on Monday morning.",
                    aiSummary = "Marcus Vance approved the Q3 proposal contract terms and wants onboarding confirmation for Monday.",
                    urgencyLevel = "MEDIUM",
                    category = "Business",
                    detectedKeywords = "Proposal, Q3 Contract, Onboarding, Legal Approval",
                    isRead = false,
                    actionSuggested = "Confirm Monday onboarding schedule with Marcus.",
                    sentiment = "Positive"
                ),
                VoicemailEntity(
                    callerName = "Apex Express Courier",
                    phoneNumber = "+1 (800) 229-4911",
                    timestamp = now - (5 * 3600 * 1000), // 5 hours ago
                    durationSeconds = 28,
                    transcript = "Express delivery update: Driver attempted delivery at your front entrance but gate code was required. Package #8921 is held at local hub.",
                    aiSummary = "Courier delivery attempted; gate code required for package #8921.",
                    urgencyLevel = "LOW",
                    category = "General",
                    detectedKeywords = "Delivery, Gate Code, Courier, Package #8921",
                    isRead = true,
                    actionSuggested = "Provide gate code or reschedule delivery online.",
                    sentiment = "Neutral"
                ),
                VoicemailEntity(
                    callerName = "Sarah Jenkins (Sister)",
                    phoneNumber = "+1 (555) 782-3341",
                    timestamp = now - (24 * 3600 * 1000), // 1 day ago
                    durationSeconds = 85,
                    transcript = "Hey! Just wanted to check in about Sunday family dinner. Let me know if you are bringing the dessert or if I should order the cake from the bakery. Call me whenever you're free!",
                    aiSummary = "Sarah checking in about Sunday family dinner dessert plans.",
                    urgencyLevel = "LOW",
                    category = "Personal",
                    detectedKeywords = "Family Dinner, Sunday, Dessert, Bakery",
                    isRead = true,
                    actionSuggested = "Text or call Sarah regarding Sunday dinner plans.",
                    sentiment = "Positive"
                )
            )

            sampleVoicemails.forEach { voicemailDao.insertVoicemail(it) }
        }

        val currentSettings = settingsDao.getSettingsOnce()
        if (currentSettings == null) {
            settingsDao.insertOrUpdateSettings(AssistantSettingsEntity())
        }
    }

    suspend fun simulateIncomingCallAndVoicemail(
        callerName: String,
        phoneNumber: String,
        category: String,
        rawVoiceText: String
    ): VoicemailEntity {
        // Run AI speech-to-text processing & Gemini analysis
        val aiAnalysis = GeminiApiClient.processVoicemailTranscript(callerName, rawVoiceText)

        val newEntity = VoicemailEntity(
            callerName = callerName,
            phoneNumber = phoneNumber,
            timestamp = System.currentTimeMillis(),
            durationSeconds = (rawVoiceText.length / 15).coerceAtLeast(15),
            transcript = rawVoiceText,
            aiSummary = aiAnalysis.aiSummary,
            urgencyLevel = aiAnalysis.urgencyLevel,
            category = if (aiAnalysis.category.isNotBlank()) aiAnalysis.category else category,
            detectedKeywords = aiAnalysis.detectedKeywords,
            isRead = false,
            actionSuggested = aiAnalysis.actionSuggested,
            sentiment = aiAnalysis.sentiment
        )

        val newId = voicemailDao.insertVoicemail(newEntity)
        return newEntity.copy(id = newId)
    }

    suspend fun markAsRead(id: Long) {
        voicemailDao.markAsRead(id)
    }

    suspend fun deleteVoicemail(id: Long) {
        voicemailDao.deleteVoicemail(id)
    }

    suspend fun updateSettings(settings: AssistantSettingsEntity) {
        settingsDao.insertOrUpdateSettings(settings)
    }
}

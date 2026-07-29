package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assistant_settings")
data class AssistantSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val assistantMode: String = "ACTIVE", // "ACTIVE", "MEETING", "DO_NOT_DISTURB"
    val greetingText: String = "Hello! I am currently unable to take your call. Please leave a detailed message, and my AI assistant will transcribe and prioritize your voice note.",
    val autoSmsReplyEnabled: Boolean = true,
    val autoSmsTemplate: String = "Hi! I received your voice message. I will review the AI summary and call you back shortly.",
    val urgencyAlertsEnabled: Boolean = true,
    val notifyHighPriorityOnly: Boolean = false
)

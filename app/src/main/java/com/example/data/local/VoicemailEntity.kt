package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voicemails")
data class VoicemailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val callerName: String,
    val phoneNumber: String,
    val timestamp: Long,
    val durationSeconds: Int,
    val audioPath: String? = null,
    val transcript: String,
    val aiSummary: String,
    val urgencyLevel: String, // "HIGH", "MEDIUM", "LOW"
    val category: String,     // "Emergency", "Business", "Personal", "Doctor", "General"
    val detectedKeywords: String, // Comma separated, e.g. "Prescription, Immediate, Pharmacy"
    val isRead: Boolean = false,
    val actionSuggested: String, // E.g. "Call back immediately regarding prescription"
    val sentiment: String = "Neutral" // "Urgent", "Concerned", "Neutral", "Positive"
)

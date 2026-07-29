package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VoicemailDao {
    @Query("SELECT * FROM voicemails ORDER BY timestamp DESC")
    fun getAllVoicemails(): Flow<List<VoicemailEntity>>

    @Query("SELECT * FROM voicemails WHERE urgencyLevel = 'HIGH' ORDER BY timestamp DESC")
    fun getHighUrgencyVoicemails(): Flow<List<VoicemailEntity>>

    @Query("SELECT * FROM voicemails WHERE id = :id")
    suspend fun getVoicemailById(id: Long): VoicemailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoicemail(voicemail: VoicemailEntity): Long

    @Update
    suspend fun updateVoicemail(voicemail: VoicemailEntity)

    @Query("UPDATE voicemails SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("DELETE FROM voicemails WHERE id = :id")
    suspend fun deleteVoicemail(id: Long)

    @Query("DELETE FROM voicemails")
    suspend fun deleteAllVoicemails()
}

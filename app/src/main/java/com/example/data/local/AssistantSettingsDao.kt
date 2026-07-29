package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantSettingsDao {
    @Query("SELECT * FROM assistant_settings WHERE id = 1")
    fun getSettings(): Flow<AssistantSettingsEntity?>

    @Query("SELECT * FROM assistant_settings WHERE id = 1")
    suspend fun getSettingsOnce(): AssistantSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: AssistantSettingsEntity)
}

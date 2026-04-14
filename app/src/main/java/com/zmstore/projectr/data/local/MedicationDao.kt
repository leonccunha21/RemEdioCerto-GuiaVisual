package com.zmstore.projectr.data.local

import androidx.room.*
import com.zmstore.projectr.data.model.DoseHistory
import com.zmstore.projectr.data.model.Medication
import com.zmstore.projectr.data.model.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE profileId = :profileId ORDER BY name ASC")
    fun getMedicationsByProfile(profileId: Int): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: Int): Medication?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    // Profiles
    @Query("SELECT * FROM profiles ORDER BY isDefault DESC, name ASC")
    fun getAllProfiles(): Flow<List<Profile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    @Update
    suspend fun updateProfile(profile: Profile)

    @Delete
    suspend fun deleteProfile(profile: Profile)

    // History
    @Query("SELECT * FROM dose_history ORDER BY timestamp DESC")
    fun getAllDoseHistory(): Flow<List<DoseHistory>>

    @Query("SELECT * FROM dose_history WHERE medicationId IN (SELECT id FROM medications WHERE profileId = :profileId) ORDER BY timestamp DESC")
    fun getDoseHistoryByProfile(profileId: Int): Flow<List<DoseHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseHistory(doseHistory: DoseHistory)

    @Delete
    suspend fun deleteDoseHistory(doseHistory: DoseHistory)

    @Query("DELETE FROM dose_history WHERE medicationId = :medicationId")
    suspend fun deleteHistoryByMedication(medicationId: Int)

    @Query("DELETE FROM dose_history")
    suspend fun clearAllHistory()

    @Query("SELECT * FROM medications WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchMedications(query: String): Flow<List<Medication>>
}

package com.zmstore.projectr.data.repository

import com.zmstore.projectr.data.local.MedicationDao
import com.zmstore.projectr.data.model.DoseHistory
import com.zmstore.projectr.data.model.Medication
import com.zmstore.projectr.data.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MedicationRepository(private val medicationDao: MedicationDao) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()
    val allDoseHistory: Flow<List<DoseHistory>> = medicationDao.getAllDoseHistory()
    val allProfiles: Flow<List<Profile>> = medicationDao.getAllProfiles()

    fun getMedicationsByProfile(profileId: Int): Flow<List<Medication>> {
        return medicationDao.getMedicationsByProfile(profileId)
    }

    suspend fun getMedicationById(id: Int): Medication? {
        return medicationDao.getMedicationById(id)
    }

    suspend fun insertMedication(medication: Medication): Long {
        return medicationDao.insertMedication(medication)
    }

    suspend fun updateMedication(medication: Medication) {
        medicationDao.updateMedication(medication)
    }

    suspend fun deleteMedication(medication: Medication) {
        medicationDao.deleteMedication(medication)
    }

    suspend fun insertDoseHistory(history: DoseHistory) {
        medicationDao.insertDoseHistory(history)
    }

    suspend fun deleteDoseHistory(history: DoseHistory) {
        medicationDao.deleteDoseHistory(history)
    }

    suspend fun deleteHistoryByMedication(medicationId: Int) {
        medicationDao.deleteHistoryByMedication(medicationId)
    }

    suspend fun clearAllHistory() {
        medicationDao.clearAllHistory()
    }

    fun getDoseHistoryByProfile(profileId: Int): Flow<List<DoseHistory>> {
        return medicationDao.getDoseHistoryByProfile(profileId)
    }

    suspend fun insertProfile(profile: Profile): Long {
        return medicationDao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: Profile) {
        medicationDao.updateProfile(profile)
    }

    suspend fun deleteProfile(profile: Profile) {
        medicationDao.deleteProfile(profile)
    }

    fun searchMedications(query: String): Flow<List<Medication>> {
        return allMedications.map { list ->
            list.filter { it.name.contains(query, ignoreCase = true) }
        }
    }
}

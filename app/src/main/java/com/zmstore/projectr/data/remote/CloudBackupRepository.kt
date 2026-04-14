package com.zmstore.projectr.data.remote

import android.content.Context
import com.zmstore.projectr.data.model.DoseHistory
import com.zmstore.projectr.data.model.Medication
import com.zmstore.projectr.data.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

import com.google.firebase.database.FirebaseDatabase
import com.zmstore.projectr.data.repository.AuthRepository
import kotlinx.coroutines.tasks.await

class CloudBackupRepository @Inject constructor(
    private val context: Context,
    private val authRepository: AuthRepository
) {
    private val database = FirebaseDatabase.getInstance("https://zm-remedio-certo-guia-visual-default-rtdb.firebaseio.com/").reference

    suspend fun syncMedications(medications: List<Medication>) {
        val user = authRepository.currentUser ?: return
        if (user.isAnonymous) return
        
        try {
            database.child("users").child(user.uid).child("medications").setValue(medications).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncProfiles(profiles: List<Profile>) {
        val user = authRepository.currentUser ?: return
        if (user.isAnonymous) return
        
        try {
            database.child("users").child(user.uid).child("profiles").setValue(profiles).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun syncHistory(history: List<DoseHistory>) {
        val user = authRepository.currentUser ?: return
        if (user.isAnonymous) return
        
        try {
            database.child("users").child(user.uid).child("history").setValue(history).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCloudStatus(): Flow<String> = flow {
        val user = authRepository.currentUser
        if (user == null || user.isAnonymous) {
            emit("Backup na Nuvem Desativado (Modo Convidado)")
        } else {
            emit("Sincronizando com a Nuvem...")
        }
    }
}

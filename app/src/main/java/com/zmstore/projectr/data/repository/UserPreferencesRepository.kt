package com.zmstore.projectr.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore by preferencesDataStore(name = "user_prefs")

data class UserPreferences(
    val name: String,
    val weight: String,
    val height: String,
    val emergencyContact: String,
    val geminiApiKey: String = "",
    val isBiometricEnabled: Boolean = false,
    val isFirstRun: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {
    private object PreferencesKeys {
        val NAME = stringPreferencesKey("user_name")
        val WEIGHT = stringPreferencesKey("user_weight")
        val HEIGHT = stringPreferencesKey("user_height")
        val EMERGENCY_CONTACT = stringPreferencesKey("emergency_contact")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")
        val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            UserPreferences(
                name = preferences[PreferencesKeys.NAME] ?: "",
                weight = preferences[PreferencesKeys.WEIGHT] ?: "",
                height = preferences[PreferencesKeys.HEIGHT] ?: "",
                emergencyContact = preferences[PreferencesKeys.EMERGENCY_CONTACT] ?: "",
                geminiApiKey = preferences[PreferencesKeys.GEMINI_API_KEY] ?: "",
                isBiometricEnabled = preferences[PreferencesKeys.IS_BIOMETRIC_ENABLED] ?: false,
                isFirstRun = preferences[PreferencesKeys.IS_FIRST_RUN] ?: true
            )
        }

    suspend fun updatePreferences(
        name: String,
        weight: String,
        height: String,
        emergencyContact: String,
        geminiApiKey: String,
        isBiometricEnabled: Boolean
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NAME] = name
            preferences[PreferencesKeys.WEIGHT] = weight
            preferences[PreferencesKeys.HEIGHT] = height
            preferences[PreferencesKeys.EMERGENCY_CONTACT] = emergencyContact
            preferences[PreferencesKeys.GEMINI_API_KEY] = geminiApiKey
            preferences[PreferencesKeys.IS_BIOMETRIC_ENABLED] = isBiometricEnabled
        }
    }

    suspend fun setFirstRunCompleted() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_RUN] = false
        }
    }
}

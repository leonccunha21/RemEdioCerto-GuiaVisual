package com.zmstore.projectr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zmstore.projectr.data.model.DoseHistory
import com.zmstore.projectr.data.model.Medication
import com.zmstore.projectr.data.model.Profile
import com.zmstore.projectr.data.repository.MedicationRepository
import com.zmstore.projectr.data.repository.UserPreferencesRepository
import com.zmstore.projectr.data.repository.UserPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.google.ai.client.generativeai.GenerativeModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import com.zmstore.projectr.util.MedicationAlarmHelper
import com.zmstore.projectr.data.remote.CloudBackupRepository
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthCredential

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: MedicationRepository,
    private val userPrefsRepository: UserPreferencesRepository,
    private val authRepository: com.zmstore.projectr.data.repository.AuthRepository,
    private val cloudBackupRepository: CloudBackupRepository,
    private val application: android.app.Application
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _aiResearchResult = MutableStateFlow<Medication?>(null)
    val aiResearchResult: StateFlow<Medication?> = _aiResearchResult.asStateFlow()

    private val _isAiSearching = MutableStateFlow(false)
    val isAiSearching: StateFlow<Boolean> = _isAiSearching.asStateFlow()

    private val _interactionWarning = MutableStateFlow<String?>(null)
    val interactionWarning: StateFlow<String?> = _interactionWarning.asStateFlow()

    val currentUser = authRepository.currentUserFlow

    private val _selectedProfile = MutableStateFlow<Profile?>(null)
    val selectedProfile: StateFlow<Profile?> = _selectedProfile.asStateFlow()

    val allProfiles: StateFlow<List<Profile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allMedications: StateFlow<List<Medication>> = repository.allMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val medications: StateFlow<List<Medication>> = _selectedProfile.flatMapLatest { profile ->
        if (profile != null) {
            repository.getMedicationsByProfile(profile.id)
        } else {
            repository.allMedications
        }
    }.combine(_searchQuery) { list, query ->
        if (query.isBlank()) list else list.filter { it.name.contains(query, ignoreCase = true) }
    }.combine(_selectedCategory) { list, category ->
        if (category == "Todos") list else list.filter { it.category == category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val doseHistory: StateFlow<List<DoseHistory>> = _selectedProfile.flatMapLatest { profile ->
        if (profile != null) {
            repository.getDoseHistoryByProfile(profile.id)
        } else {
            repository.allDoseHistory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaysAdherence: StateFlow<Pair<Int, Int>> = medications.combine(doseHistory) { meds, history ->
        val activeMeds = meds.filter { it.isActive }
        if (activeMeds.isEmpty()) return@combine 0 to 0
        
        val now = java.util.Calendar.getInstance()
        val startOfDay = now.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val takenTodayIds = history
            .filter { it.timestamp >= startOfDay }
            .map { it.medicationId }
            .distinct()
        
        val takenCount = activeMeds.count { it.id in takenTodayIds }
        takenCount to activeMeds.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0 to 0)

    init {
        viewModelScope.launch {
            _isLoading.value = false
            allProfiles.collect { profiles ->
                if (_selectedProfile.value == null && profiles.isNotEmpty()) {
                    _selectedProfile.value = profiles.find { it.isDefault } ?: profiles.first()
                }
            }
        }
    }

    fun selectProfile(profile: Profile) {
        _selectedProfile.value = profile
    }

    fun insertProfile(name: String, color: Int) {
        viewModelScope.launch {
            repository.insertProfile(Profile(name = name, color = color))
            cloudBackupRepository.syncProfiles(allProfiles.value) 
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            repository.updateProfile(profile)
            cloudBackupRepository.syncProfiles(allProfiles.value.map { if (it.id == profile.id) profile else it })
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            cloudBackupRepository.syncProfiles(allProfiles.value.filter { it.id != profile.id })
        }
    }

    val userPreferences: StateFlow<UserPreferences> = userPrefsRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences("", "", "", "", ""))

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateProfile(name: String, weight: String, height: String, emergency: String, geminiApiKey: String, isBiometricEnabled: Boolean) {
        viewModelScope.launch {
            userPrefsRepository.updatePreferences(name, weight, height, emergency, geminiApiKey, isBiometricEnabled)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            userPrefsRepository.setFirstRunCompleted()
        }
    }

    fun signInAnonymously(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signInAnonymously()
            onComplete(result.isSuccess)
        }
    }

    fun signInWithGoogle(activityContext: android.app.Activity, webClientId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(activityContext, webClientId)
            onComplete(result.isSuccess)
        }
    }

    fun signInWithEmail(email: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signInWithEmail(email, password)
            onComplete(result.isSuccess)
        }
    }

    fun signUpWithEmail(email: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(email, password)
            onComplete(result.isSuccess)
        }
    }

    fun verifyPhoneNumber(
        phoneNumber: String,
        activity: android.app.Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        authRepository.verifyPhoneNumber(phoneNumber, activity, callbacks)
    }

    fun signInWithPhoneCredential(credential: PhoneAuthCredential, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signInWithPhoneCredential(credential)
            onComplete(result.isSuccess)
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun insertMedication(medication: Medication) {
        viewModelScope.launch {
            // Limpa busca para garantir que o remédio apareça
            _searchQuery.value = ""
            
            val currentProfile = _selectedProfile.value ?: allProfiles.value.find { it.isDefault } ?: allProfiles.value.firstOrNull()
            
            // Verifica se já existe um medicamento com o mesmo nome e dosagem para este perfil (usando a lista completa)
            val existingMed = if (currentProfile != null) {
                allMedications.value.find { it.name.equals(medication.name, ignoreCase = true) && it.dosage == medication.dosage && it.profileId == currentProfile.id }
            } else {
                allMedications.value.find { it.name.equals(medication.name, ignoreCase = true) && it.dosage == medication.dosage }
            }

            if (existingMed != null) {
                // Se já existe, atualiza em vez de inserir novo preservando o histórico e perfil
                val updatedMed = medication.copy(
                    id = existingMed.id, 
                    profileId = existingMed.profileId,
                    lastTakenTimestamp = existingMed.lastTakenTimestamp
                )
                repository.updateMedication(updatedMed)
                try {
                    MedicationAlarmHelper.scheduleAlarm(application, updatedMed)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Sincroniza a lista completa atualizada
                cloudBackupRepository.syncMedications(allMedications.value.map { if (it.id == updatedMed.id) updatedMed else it })
            } else {
                // Insere novo
                val medicationWithProfile = if (currentProfile != null) {
                    medication.copy(profileId = currentProfile.id)
                } else {
                    medication
                }
                val id = repository.insertMedication(medicationWithProfile)
                val insertedMedication = medicationWithProfile.copy(id = id.toInt())
                try {
                    MedicationAlarmHelper.scheduleAlarm(application, insertedMedication)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Sincroniza a lista completa atualizada
                cloudBackupRepository.syncMedications(allMedications.value + insertedMedication)
            }
        }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            repository.updateMedication(medication)
            try {
                MedicationAlarmHelper.scheduleAlarm(application, medication)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Sincroniza a lista completa atualizada
            cloudBackupRepository.syncMedications(allMedications.value.map { if (it.id == medication.id) medication else it })
        }
    }

    suspend fun getMedicationById(id: Int): Medication? {
        return repository.getMedicationById(id)
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
            repository.deleteHistoryByMedication(medication.id)
            // Sincroniza a lista completa atualizada
            cloudBackupRepository.syncMedications(allMedications.value.filter { it.id != medication.id })
        }
    }

    fun deleteDoseHistory(doseHistory: DoseHistory) {
        viewModelScope.launch {
            repository.deleteDoseHistory(doseHistory)
        }
    }

    fun confirmDose(medicationId: Int, medicationName: String, note: String? = null) {
        viewModelScope.launch {
            repository.insertDoseHistory(DoseHistory(
                medicationId = medicationId, 
                medicationName = medicationName,
                note = note
            ))
            repository.getMedicationById(medicationId)?.let { med ->
                val updatedStock = if (med.stockCount > 0) med.stockCount - 1 else 0
                val updatedMed = med.copy(
                    lastTakenTimestamp = System.currentTimeMillis(),
                    stockCount = updatedStock
                )
                repository.updateMedication(updatedMed)
                try {
                    MedicationAlarmHelper.scheduleAlarm(application, updatedMed)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // Sincroniza a lista completa atualizada
                cloudBackupRepository.syncMedications(allMedications.value.map { if (it.id == updatedMed.id) updatedMed else it })
            }
        }
    }

    /**
     * Tenta confirmar uma dose baseada em um comando de voz.
     */
    fun confirmDoseByVoice(spokenText: String, onResult: (String, Boolean) -> Unit) {
        viewModelScope.launch {
            val meds = medications.value.filter { it.isActive }
            // Busca simplificada: verifica se o nome do remédio está contido no texto falado
            val match = meds.find { spokenText.contains(it.name, ignoreCase = true) }
            
            if (match != null) {
                confirmDose(match.id, match.name, "Confirmado via voz")
                onResult("${match.name} confirmado!", true)
            } else {
                onResult("Não entendi '$spokenText'. Tente dizer apenas o nome do remédio.", false)
            }
        }
    }

    fun fetchMedicationInfo(medicationName: String) {
        val apiKey = userPreferences.value.geminiApiKey
        if (apiKey.isBlank()) return

        viewModelScope.launch {
            _isAiSearching.value = true
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = apiKey
                )

                val prompt = """
                    Forneça informações detalhadas sobre o medicamento: $medicationName
                    Responda em formato JSON rigoroso com as seguintes chaves (em português):
                    - "name": Nome oficial
                    - "dosage": Dosagem comum
                    - "purpose": Para que serve
                    - "instructions": Como tomar
                    - "sideEffects": Efeitos colaterais comuns
                    - "alerts": Advertências importantes
                    SEJA CONCISO.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val text = response.text ?: ""
                
                val jsonMatch = """\{[\s\S]*\}""".toRegex().find(text)
                val jsonString = jsonMatch?.value ?: text

                try {
                    val json = org.json.JSONObject(jsonString)
                    _aiResearchResult.value = Medication(
                        name = json.optString("name", medicationName),
                        dosage = json.optString("dosage", ""),
                        purpose = json.optString("purpose", ""),
                        instructions = json.optString("instructions", ""),
                        sideEffects = json.optString("sideEffects", ""),
                        alerts = json.optString("alerts", "")
                    )
                } catch (e: Exception) {
                    _aiResearchResult.value = Medication(
                        name = parseJsonPart(text, "name") ?: medicationName,
                        dosage = parseJsonPart(text, "dosage") ?: "",
                        purpose = parseJsonPart(text, "purpose") ?: "",
                        instructions = parseJsonPart(text, "instructions") ?: "",
                        sideEffects = parseJsonPart(text, "sideEffects") ?: "",
                        alerts = parseJsonPart(text, "alerts") ?: ""
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isAiSearching.value = false
            }
        }
    }

    private fun parseJsonPart(json: String, key: String): String? {
        val pattern = "\"$key\":\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    fun clearAiResearch() {
        _aiResearchResult.value = null
    }

    fun checkInteractions(newMedication: String) {
        val apiKey = userPreferences.value.geminiApiKey
        if (apiKey.isBlank()) return
        
        val currentMeds = medications.value.filter { it.isActive }.map { it.name }
        if (currentMeds.isEmpty()) return

        viewModelScope.launch {
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = apiKey
                )

                val prompt = """
                    O usuário está adicionando o medicamento: $newMedication
                    Ele já toma os seguintes medicamentos: ${currentMeds.joinToString(", ")}
                    
                    Existe alguma interação medicamentosa conhecida entre o novo medicamento e os que ele já toma?
                    Responda em português.
                    Se houver risco, descreva-o de forma concisa.
                    Se não houver risco conhecido, responda apenas "NENHUM".
                    IMPORTANTE: Adicione um aviso de que esta é uma análise por IA e o médico deve ser consultado.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val text = response.text ?: ""
                
                if (text.trim().uppercase() != "NENHUM") {
                    _interactionWarning.value = text
                } else {
                    _interactionWarning.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearInteractionWarning() {
        _interactionWarning.value = null
    }

    fun getMedicationCountdown(medication: Medication): String {
        if (!medication.isActive) return "Pausado"

        val currentTime = System.currentTimeMillis()
        var diff = 0L

        if (!medication.customTimes.isNullOrBlank()) {
            val times = medication.customTimes.split(",").map { it.trim() }
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = currentTime
            
            var nextDoseTimeInMillis = Long.MAX_VALUE
            var timeFound = false

            for (timeStr in times) {
                val parts = timeStr.split(":")
                if (parts.size == 2) {
                    val h = parts[0].toIntOrNull() ?: 0
                    val m = parts[1].toIntOrNull() ?: 0
                    
                    val testCal = java.util.Calendar.getInstance()
                    testCal.timeInMillis = currentTime
                    testCal.set(java.util.Calendar.HOUR_OF_DAY, h)
                    testCal.set(java.util.Calendar.MINUTE, m)
                    testCal.set(java.util.Calendar.SECOND, 0)
                    
                    var testTime = testCal.timeInMillis
                    if (testTime < currentTime) {
                        testTime += 24 * 3600 * 1000
                    }

                    if (testTime < nextDoseTimeInMillis) {
                        nextDoseTimeInMillis = testTime
                        timeFound = true
                    }
                }
            }

            if (!timeFound) return "Horário Inválido"
            diff = nextDoseTimeInMillis - currentTime
        } else {
            if (medication.intervalHours <= 0) return "S/ horário"
            if (medication.lastTakenTimestamp == 0L) return "Pendente"
            val nextDoseTimestamp = medication.lastTakenTimestamp + (medication.intervalHours * 3600 * 1000)
            diff = nextDoseTimestamp - currentTime
        }

        return if (diff < 0) {
            "Atrasado"
        } else {
            val hours = diff / (3600 * 1000)
            val minutes = (diff % (3600 * 1000)) / (60 * 1000)
            if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
    }
}

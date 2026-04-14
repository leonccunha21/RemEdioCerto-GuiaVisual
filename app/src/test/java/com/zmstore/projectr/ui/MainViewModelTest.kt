package com.zmstore.projectr.ui

import android.app.Application
import app.cash.turbine.test
import com.zmstore.projectr.data.model.Medication
import com.zmstore.projectr.data.repository.MedicationRepository
import com.zmstore.projectr.data.repository.UserPreferencesRepository
import com.zmstore.projectr.data.repository.UserPreferences
import com.zmstore.projectr.data.repository.AuthRepository
import com.zmstore.projectr.data.remote.CloudBackupRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val repository: MedicationRepository = mockk()
    private val userPrefsRepository: UserPreferencesRepository = mockk()
    private val authRepository: AuthRepository = mockk()
    private val cloudBackupRepository: CloudBackupRepository = mockk()
    private val application: Application = mockk()
    private lateinit var viewModel: MainViewModel
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.allMedications } returns flowOf(emptyList())
        coEvery { repository.allDoseHistory } returns flowOf(emptyList())
        coEvery { repository.allProfiles } returns flowOf(emptyList())
        coEvery { userPrefsRepository.userPreferencesFlow } returns flowOf(UserPreferences("", "", "", "", ""))
        coEvery { authRepository.currentUserFlow } returns flowOf(null)
        
        viewModel = MainViewModel(repository, userPrefsRepository, authRepository, cloudBackupRepository, application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial search query is empty`() = runTest {
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `updateSearchQuery updates state`() = runTest {
        viewModel.updateSearchQuery("Paracetamol")
        assertEquals("Paracetamol", viewModel.searchQuery.value)
    }

    @Test
    fun `getMedicationCountdown returns Atrasado when dose missed`() {
        val medication = Medication(
            name = "Test",
            dosage = "10mg",
            purpose = "",
            instructions = "",
            sideEffects = "",
            alerts = "",
            intervalHours = 8,
            lastTakenTimestamp = System.currentTimeMillis() - (10 * 3600 * 1000), // 10h ago
            isActive = true,
            category = "Outros",
            customTimes = null
        )
        
        val result = viewModel.getMedicationCountdown(medication)
        assertEquals("Atrasado", result)
    }

    @Test
    fun `getMedicationCountdown returns Pendente when never taken`() {
        val medication = Medication(
            name = "Test",
            dosage = "10mg",
            purpose = "",
            instructions = "",
            sideEffects = "",
            alerts = "",
            intervalHours = 8,
            lastTakenTimestamp = 0L,
            isActive = true,
            category = "Outros",
            customTimes = null
        )
        
        val result = viewModel.getMedicationCountdown(medication)
        assertEquals("Pendente", result)
    }
}

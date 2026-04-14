package com.zmstore.projectr.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.zmstore.projectr.data.local.AppDatabase
import com.zmstore.projectr.data.model.Medication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MedicationRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: MedicationRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MedicationRepository(database.medicationDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insertMedication persists medication in db`() = runTest {
        val medication = Medication(name = "Paracetamol", dosage = "500mg", purpose = "", instructions = "", sideEffects = "", alerts = "", category = "Outros", customTimes = null)

        repository.insertMedication(medication)

        val all = repository.allMedications.first()
        assertEquals(1, all.size)
        assertEquals("Paracetamol", all[0].name)
    }

    @Test
    fun `getMedicationById returns correct medication`() = runTest {
        val medication = Medication(name = "Dipirona", dosage = "500mg", purpose = "", instructions = "", sideEffects = "", alerts = "", category = "Outros", customTimes = null)
        repository.insertMedication(medication)

        val inserted = repository.allMedications.first()[0]
        val result = repository.getMedicationById(inserted.id)

        assertNotNull(result)
        assertEquals("Dipirona", result?.name)
    }

    @Test
    fun `searchMedications filters by name`() = runTest {
        repository.insertMedication(Medication(name = "Paracetamol", dosage = "", purpose = "", instructions = "", sideEffects = "", alerts = "", category = "Outros", customTimes = null))
        repository.insertMedication(Medication(name = "Dipirona", dosage = "", purpose = "", instructions = "", sideEffects = "", alerts = "", category = "Outros", customTimes = null))

        val results = repository.searchMedications("Para").first()

        assertEquals(1, results.size)
        assertEquals("Paracetamol", results[0].name)
    }
}

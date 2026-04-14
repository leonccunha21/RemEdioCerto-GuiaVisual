package com.zmstore.projectr

import android.app.Application
import com.zmstore.projectr.data.local.AppDatabase
import com.zmstore.projectr.data.repository.MedicationRepository
import com.zmstore.projectr.data.repository.UserPreferencesRepository

import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ProjectRApplication : Application()

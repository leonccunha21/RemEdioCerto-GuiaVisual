package com.zmstore.projectr.di

import android.content.Context
import com.zmstore.projectr.data.local.AppDatabase
import com.zmstore.projectr.data.local.MedicationDao
import com.zmstore.projectr.data.repository.MedicationRepository
import com.zmstore.projectr.data.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import com.zmstore.projectr.data.remote.CloudBackupRepository
import com.google.firebase.auth.FirebaseAuth
import com.zmstore.projectr.data.repository.AuthRepository

@Module
@InstallIn(SingletonComponent::class)
object DiModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        @ApplicationContext context: Context
    ): AuthRepository {
        return AuthRepository(auth, context)
    }

    @Provides
    @Singleton
    fun provideCloudBackupRepository(
        @ApplicationContext context: Context,
        authRepository: AuthRepository
    ): CloudBackupRepository {
        return CloudBackupRepository(context, authRepository)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideMedicationDao(database: AppDatabase): MedicationDao {
        return database.medicationDao()
    }

    @Provides
    @Singleton
    fun provideMedicationRepository(medicationDao: MedicationDao): MedicationRepository {
        return MedicationRepository(medicationDao)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }
}

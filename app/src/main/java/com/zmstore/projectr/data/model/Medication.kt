package com.zmstore.projectr.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val purpose: String,
    val instructions: String,
    val sideEffects: String,
    val alerts: String,
    val imageUrl: String? = null,
    val stockCount: Int = 0,
    val intervalHours: Int = 0,
    val lastTakenTimestamp: Long = 0,
    val isActive: Boolean = true,
    val category: String = "Outros",
    val customTimes: String? = null, // comma-separated HH:mm times
    val profileId: Int = 0, // Default profile
    val iconType: String = "pill", // pill, capsule, drops, liquid
    val iconColor: Int = 0xFF008080.toInt() // MedicleanTeal
)

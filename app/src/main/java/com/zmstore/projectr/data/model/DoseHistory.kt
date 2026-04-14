package com.zmstore.projectr.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dose_history")
data class DoseHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int,
    val medicationName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)

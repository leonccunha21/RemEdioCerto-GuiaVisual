package com.zmstore.projectr.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: Int = 0xFF008080.toInt(), // Default MedicleanTeal
    val isDefault: Boolean = false
)

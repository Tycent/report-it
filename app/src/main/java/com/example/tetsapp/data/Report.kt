package com.example.tetsapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String = "",
    val department: String = "",
    val reporterPhone: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val status: String = "Pending",
    val timestamp: Long = System.currentTimeMillis()
)

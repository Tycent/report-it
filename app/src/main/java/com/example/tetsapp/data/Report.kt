package com.example.tetsapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val department: String,
    val reporterPhone: String,
    val timestamp: Long = System.currentTimeMillis()
)

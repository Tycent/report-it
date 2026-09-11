package com.example.tetsapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admins")
data class Admin(
    @PrimaryKey
    val forceNumber: String,
    val name: String,
    val department: String,
    val password: String,
    val phoneNumber: String
)

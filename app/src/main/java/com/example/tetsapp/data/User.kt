package com.example.tetsapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val phoneNumber: String,
    val name: String,
    val address: String,
    val password: String
)

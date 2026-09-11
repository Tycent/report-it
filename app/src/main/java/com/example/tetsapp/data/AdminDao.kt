package com.example.tetsapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AdminDao {
    @Query("SELECT * FROM admins WHERE forceNumber = :forceNumber LIMIT 1")
    suspend fun getAdminByForceNumber(forceNumber: String): Admin?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAdmin(admin: Admin)
}

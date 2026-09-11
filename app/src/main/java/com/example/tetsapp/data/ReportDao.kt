package com.example.tetsapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports WHERE department = :department ORDER BY timestamp DESC")
    suspend fun getReportsByDepartment(department: String): List<Report>

    @Insert
    suspend fun insertReport(report: Report)
}

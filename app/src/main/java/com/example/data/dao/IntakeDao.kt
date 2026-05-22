package com.example.data.dao

import androidx.room.*
import com.example.data.model.IntakeRecord
import com.example.data.model.DeficiencyAnalysis
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeDao {
    @Query("SELECT * FROM intake_records ORDER BY timestamp DESC")
    fun getAllIntakeRecords(): Flow<List<IntakeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntakeRecord(record: IntakeRecord)

    @Delete
    suspend fun deleteIntakeRecord(record: IntakeRecord)

    @Query("DELETE FROM intake_records WHERE id = :id")
    suspend fun deleteIntakeRecordById(id: Int)

    @Query("SELECT * FROM intake_records WHERE timestamp >= :startOfDay")
    fun getIntakesSince(startOfDay: Long): Flow<List<IntakeRecord>>

    // Deficiency Analysis History
    @Query("SELECT * FROM deficiency_analyses ORDER BY timestamp DESC")
    fun getAllDeficiencyAnalyses(): Flow<List<DeficiencyAnalysis>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeficiencyAnalysis(analysis: DeficiencyAnalysis)

    @Delete
    suspend fun deleteDeficiencyAnalysis(analysis: DeficiencyAnalysis)
}

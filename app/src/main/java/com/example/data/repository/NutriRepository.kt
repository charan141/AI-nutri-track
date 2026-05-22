package com.example.data.repository

import com.example.data.dao.IntakeDao
import com.example.data.model.IntakeRecord
import com.example.data.model.DeficiencyAnalysis
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class NutriRepository(private val intakeDao: IntakeDao) {

    val allIntakeRecords: Flow<List<IntakeRecord>> = intakeDao.getAllIntakeRecords()
    val allDeficiencyAnalyses: Flow<List<DeficiencyAnalysis>> = intakeDao.getAllDeficiencyAnalyses()

    suspend fun insertIntake(record: IntakeRecord) {
        intakeDao.insertIntakeRecord(record)
    }

    suspend fun deleteIntake(record: IntakeRecord) {
        intakeDao.deleteIntakeRecord(record)
    }

    suspend fun deleteIntakeById(id: Int) {
        intakeDao.deleteIntakeRecordById(id)
    }

    suspend fun insertDeficiencyAnalysis(analysis: DeficiencyAnalysis) {
        intakeDao.insertDeficiencyAnalysis(analysis)
    }

    suspend fun deleteDeficiencyAnalysis(analysis: DeficiencyAnalysis) {
        intakeDao.deleteDeficiencyAnalysis(analysis)
    }

    fun getIntakeForToday(): Flow<List<IntakeRecord>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return intakeDao.getIntakesSince(calendar.timeInMillis)
    }
}

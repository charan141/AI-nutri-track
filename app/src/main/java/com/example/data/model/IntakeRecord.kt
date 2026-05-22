package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "intake_records")
data class IntakeRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodName: String,
    val proteinGrams: Double,
    val fiberGrams: Double,
    val caloriesKcal: Double,
    val vitamins: String = "",
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

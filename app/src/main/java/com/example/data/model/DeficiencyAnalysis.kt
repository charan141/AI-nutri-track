package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "deficiency_analyses")
data class DeficiencyAnalysis(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symptoms: String,
    val predictedDeficiencies: String, // multiline or custom JSON representation
    val recommendedFoods: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

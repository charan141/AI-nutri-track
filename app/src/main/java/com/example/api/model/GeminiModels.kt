package com.example.api.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class PartResponse(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ContentResponse(
    val parts: List<PartResponse>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: ContentResponse? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

// Custom parsed entities for Food label extraction
@JsonClass(generateAdapter = true)
data class NutritionLabelResult(
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val fiber: Double
)

// Custom parsed entities for Vitamin Deficiency analysis
@JsonClass(generateAdapter = true)
data class DeficiencyItem(
    val vitaminName: String,
    val confidence: String, // High, Medium, Low
    val symptomLink: String,
    val recommendedFoods: List<String>
)

@JsonClass(generateAdapter = true)
data class VitaminDeficiencyResult(
    val deficiencies: List<DeficiencyItem>
)

@JsonClass(generateAdapter = true)
data class FoodAnalysisResult(
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val fiber: Double,
    val vitamins: String
)

// Dynamic Indian Weekly Meal Chart Models
@JsonClass(generateAdapter = true)
data class MealDay(
    val day: String,
    val breakfast: String,
    val lunch: String,
    val dinner: String,
    val snacks: String
)

@JsonClass(generateAdapter = true)
data class WeeklyMealChartResult(
    val title: String,
    val description: String,
    val days: List<MealDay>
)

// Disease and Deficiency Recommendation Models
@JsonClass(generateAdapter = true)
data class DiseaseFoodRecommendation(
    val condition: String,
    val recommendedFoods: List<String>,
    val reason: String
)

@JsonClass(generateAdapter = true)
data class DiseaseFoodRecommendationsResult(
    val recommendations: List<DiseaseFoodRecommendation>
)

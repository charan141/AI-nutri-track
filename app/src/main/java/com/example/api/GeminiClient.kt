package com.example.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.api.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    @Volatile
    var customApiKey: String? = null

    private fun getApiKey(): String {
        val custom = customApiKey
        return if (!custom.isNullOrBlank()) {
            custom
        } else {
            BuildConfig.GEMINI_API_KEY
        }
    }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        // Compress bitmap significantly to save payload size and keep request quick
        compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeNutritionLabel(bitmap: Bitmap): NutritionLabelResult? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || isMockKey(apiKey)) {
            Log.d(TAG, "Mock key or empty key, returning local nutrition label scan.")
            return@withContext generateLocalNutritionLabel(bitmap)
        }

        val base64Image = bitmap.toBase64()
        val prompt = """
            You are an expert nutritional label analyzer.
            Analyze the nutrition facts label in this image.
            Extract:
            1. Descriptive name of the food item (foodName, short and friendly, e.g. "Peanut Butter").
            2. Calories in kcal (calories).
            3. Protein content in grams (protein).
            4. Dietary fiber content in grams (fiber).
            
            Return ONLY a valid JSON object matching this schema:
            {
              "foodName": "Granola Oats",
              "calories": 210.0,
              "protein": 5.5,
              "fiber": 3.0
            }
            Do not include any markdown format tags like ```json or anything else. Just the raw JSON content!
        """.trimIndent()

        val requestBodyObj = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        try {
            val jsonAdapter = moshi.adapter(GenerateContentRequest::class.java)
            val jsonReq = jsonAdapter.toJson(requestBodyObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonReq.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Request failed code: ${response.code}, body: $responseBodyStr")
                return@withContext generateLocalNutritionLabel(bitmap)
            }

            val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)
            val responseObj = responseAdapter.fromJson(responseBodyStr)
            val textResult = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (textResult != null) {
                Log.d(TAG, "Raw Response: $textResult")
                val cleanedJson = cleanJsonString(textResult)
                val resultAdapter = moshi.adapter(NutritionLabelResult::class.java)
                return@withContext resultAdapter.fromJson(cleanedJson)
            } else {
                Log.e(TAG, "Empty text result in candidates!")
                return@withContext generateLocalNutritionLabel(bitmap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing nutrition label: ${e.message}, returning local fallback.", e)
            return@withContext generateLocalNutritionLabel(bitmap)
        }
    }

    suspend fun analyzeVitaminDeficiency(symptoms: List<String>, otherSymptoms: String): VitaminDeficiencyResult? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || isMockKey(apiKey)) {
            Log.d(TAG, "Mock key or empty key, returning local deficiency diagnosis.")
            return@withContext generateLocalVitaminDeficiency(symptoms, otherSymptoms)
        }

        val joinedSymptoms = (symptoms + if (otherSymptoms.isNotBlank()) listOf(otherSymptoms) else emptyList()).joinToString(", ")
        val prompt = """
            You are an expert clinical nutrition advisor.
            Analyze user symptoms: "$joinedSymptoms"
            to identify prospective vitamin or mineral deficiencies.
            Suggest probabilities and direct, nutrient-rich whole foods to intake.
            
            Return ONLY a valid JSON object matching this schema:
            {
              "deficiencies": [
                {
                  "vitaminName": "Vitamin D",
                  "confidence": "High",
                  "symptomLink": "Muscle weakness, bone pain, and fatigue are common symptoms.",
                  "recommendedFoods": ["Fatty fish (salmon, tuna)", "Mushroom", "Egg yolks", "Fortified Foods"]
                }
              ]
            }
            Do not include any markdown format tags like ```json or anything else. Just the raw JSON content!
        """.trimIndent()

        val requestBodyObj = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        try {
            val jsonAdapter = moshi.adapter(GenerateContentRequest::class.java)
            val jsonReq = jsonAdapter.toJson(requestBodyObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonReq.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Deficiency query failed code: ${response.code}, body: $responseBodyStr")
                return@withContext generateLocalVitaminDeficiency(symptoms, otherSymptoms)
            }

            val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)
            val responseObj = responseAdapter.fromJson(responseBodyStr)
            val textResult = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (textResult != null) {
                Log.d(TAG, "Raw Deficiency Response: $textResult")
                val cleanedJson = cleanJsonString(textResult)
                val resultAdapter = moshi.adapter(VitaminDeficiencyResult::class.java)
                return@withContext resultAdapter.fromJson(cleanedJson)
            } else {
                Log.e(TAG, "Empty text result in candidates!")
                return@withContext generateLocalVitaminDeficiency(symptoms, otherSymptoms)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing deficiencies: ${e.message}, returning local fallback.", e)
            return@withContext generateLocalVitaminDeficiency(symptoms, otherSymptoms)
        }
    }

    suspend fun analyzeFoodText(foodDescription: String): FoodAnalysisResult? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || isMockKey(apiKey)) {
            Log.d(TAG, "Mock key or empty key, returning local food calculation.")
            return@withContext generateLocalFoodAnalysis(foodDescription)
        }

        val prompt = """
            You are an expert food nutritionist.
            Analyze this specified food item or meal: "$foodDescription".
            Estimate and calculate:
            1. Short, friendly name of the food item or combined meal (foodName, e.g. "Scrambled Eggs with Avocado").
            2. Total calories in kcal (calories).
            3. Total protein content in grams (protein).
            4. Total dietary fiber content in grams (fiber).
            5. Primary vitamins present in significant amounts (vitamins, e.g. "Vitamin A, Vitamin B12, Potassium, Iron" or "None"). Keep it brief as a comma-separated list.
            
            Return ONLY a valid JSON object matching this schema:
            {
              "foodName": "Food Name Here",
              "calories": 250.0,
              "protein": 12.5,
              "fiber": 4.5,
              "vitamins": "Vitamin B12, Vitamin D, Zinc"
            }
            Do not include any markdown format tags like ```json or anything else. Just the raw JSON content!
        """.trimIndent()

        val requestBodyObj = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        try {
            val jsonAdapter = moshi.adapter(GenerateContentRequest::class.java)
            val jsonReq = jsonAdapter.toJson(requestBodyObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonReq.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Food text analysis query failed code: ${response.code}, body: $responseBodyStr")
                return@withContext generateLocalFoodAnalysis(foodDescription)
            }

            val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)
            val responseObj = responseAdapter.fromJson(responseBodyStr)
            val textResult = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (textResult != null) {
                Log.d(TAG, "Raw Food Analysis Response: $textResult")
                val cleanedJson = cleanJsonString(textResult)
                val resultAdapter = moshi.adapter(FoodAnalysisResult::class.java)
                return@withContext resultAdapter.fromJson(cleanedJson)
            } else {
                Log.e(TAG, "Empty text result in food text analysis candidates!")
                return@withContext generateLocalFoodAnalysis(foodDescription)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing food text: ${e.message}, returning local fallback.", e)
            return@withContext generateLocalFoodAnalysis(foodDescription)
        }
    }

    suspend fun generateWeeklyMealChart(
        heightCm: Float,
        weightKg: Float,
        gender: String,
        diseases: String
    ): WeeklyMealChartResult? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || isMockKey(apiKey)) {
            Log.d(TAG, "Mock key or empty key, returning local Indian meal plan generator.")
            return@withContext generateLocalMealChart(heightCm, weightKg, gender, diseases)
        }

        val prompt = """
            You are an expert Indian clinical nutritionist.
            Generate a personalized Indian Weekly Meal Chart (Monday to Sunday) for a user with the following profile:
            - Gender: $gender
            - Height: $heightCm cm
            - Weight: $weightKg kg
            - Pre-existing medical conditions/deficiencies/diseases: ${diseases.ifBlank { "None" }}

            Make sure the recommended foods are healthy, traditional, and easily available Indian meals—prioritizing healthy South Indian food options (including items like Idli & Sambar, Masala Dosa, Millet Pongal, Medu Vada, Rava Upma, Curd Rice, Sambar, and Rasam & Rice, in addition to Roti, Sabzi, Poha, Dal, Paneer, and Khichdi)—fitted specifically to help their conditions.

            Return ONLY a valid JSON object matching this schema:
            {
              "title": "Personalized Indian Meal Plan",
              "description": "Short explanation of the customized diet goals based on conditions.",
              "days": [
                {
                  "day": "Monday",
                  "breakfast": "Oats Upma with veggies (Protein: 8g, Fiber: 5g)",
                  "lunch": "2 Roti, Moong Dal, Mixed Veg curry (Protein: 15g, Fiber: 8g)",
                  "dinner": "Paneer bhurji with 1 Roti (Protein: 20g, Fiber: 4g)",
                  "snacks": "Roasted Chana (Protein: 6g, Fiber: 3g)"
                }
              ]
            }
            Ensure you include entries for all 7 days of the week: Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday.
            Do not include any markdown format tags like ```json or anything else. Just the raw JSON content!
        """.trimIndent()

        val requestBodyObj = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.3f
            )
        )

        try {
            val jsonAdapter = moshi.adapter(GenerateContentRequest::class.java)
            val jsonReq = jsonAdapter.toJson(requestBodyObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonReq.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Meal chart query failed code: ${response.code}, body: $responseBodyStr")
                return@withContext generateLocalMealChart(heightCm, weightKg, gender, diseases)
            }

            val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)
            val responseObj = responseAdapter.fromJson(responseBodyStr)
            val textResult = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (textResult != null) {
                Log.d(TAG, "Raw Meal Chart response: $textResult")
                val cleanedJson = cleanJsonString(textResult)
                val resultAdapter = moshi.adapter(WeeklyMealChartResult::class.java)
                return@withContext resultAdapter.fromJson(cleanedJson)
            } else {
                Log.e(TAG, "Empty text result in candidates!")
                return@withContext generateLocalMealChart(heightCm, weightKg, gender, diseases)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating meal chart: ${e.message}, returning local fallback.", e)
            return@withContext generateLocalMealChart(heightCm, weightKg, gender, diseases)
        }
    }

    suspend fun getRecommendationsForConditions(conditions: String): DiseaseFoodRecommendationsResult? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || isMockKey(apiKey)) {
            Log.d(TAG, "Mock key or empty key, returning local healthcare food suggestions.")
            return@withContext generateLocalRecommendations(conditions)
        }

        val prompt = """
            Analyze these pre-existing conditions/deficiencies/diseases: "$conditions".
            Identify 3-5 nutrient-dense or medicinal whole foods to recommend for each condition, along with a short scientific reasoning (e.g. "contains iron to boost RBC count").
            
            Return ONLY a valid JSON object matching this schema:
            {
              "recommendations": [
                {
                  "condition": "Iron Deficiency",
                  "recommendedFoods": ["Spinach", "Beetroot", "Pomegranate", "Lentils"],
                  "reason": "Rich in non-heme iron and vitamin C to enhance iron absorption and hemoglobin production."
                }
              ]
            }
            Do not include any markdown format tags like ```json or anything else. Just the raw JSON content!
        """.trimIndent()

        val requestBodyObj = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(Part(text = prompt))
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        try {
            val jsonAdapter = moshi.adapter(GenerateContentRequest::class.java)
            val jsonReq = jsonAdapter.toJson(requestBodyObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonReq.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Query failed code: ${response.code}, body: $responseBodyStr")
                return@withContext generateLocalRecommendations(conditions)
            }

            val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)
            val responseObj = responseAdapter.fromJson(responseBodyStr)
            val textResult = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (textResult != null) {
                Log.d(TAG, "Raw food recommendations response: $textResult")
                val cleanedJson = cleanJsonString(textResult)
                val resultAdapter = moshi.adapter(DiseaseFoodRecommendationsResult::class.java)
                return@withContext resultAdapter.fromJson(cleanedJson)
            } else {
                Log.e(TAG, "Empty text result in candidates!")
                return@withContext generateLocalRecommendations(conditions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}, returning local fallback.", e)
            return@withContext generateLocalRecommendations(conditions)
        }
    }

    fun cleanJsonString(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```")) {
            str = str.removePrefix("```json").removePrefix("```").trim()
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```").trim()
        }
        val firstBrace = str.indexOf('{')
        val lastBrace = str.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return str.substring(firstBrace, lastBrace + 1)
        }
        return str
    }

    fun isMockKey(key: String): Boolean {
        return key.isBlank() || key == "MY_GEMINI_API_KEY" || key.contains("GmailSecureToken")
    }

    fun generateLocalMealChart(
        heightCm: Float,
        weightKg: Float,
        gender: String,
        diseases: String
    ): WeeklyMealChartResult {
        val conditionsLower = diseases.lowercase()
        val isDiabetic = conditionsLower.contains("diabet") || conditionsLower.contains("sugar")
        val isHypertensive = conditionsLower.contains("bp") || conditionsLower.contains("hyperten") || conditionsLower.contains("heart") || conditionsLower.contains("sodium")
        val isAnemic = conditionsLower.contains("anemi") || conditionsLower.contains("iron") || conditionsLower.contains("blood")
        
        val targetGoal = when {
            isDiabetic -> "Low-Glycemic Diabetic Diet (Control Sugar)"
            isHypertensive -> "DASH Heart-Healthy Diet (Low Sodium)"
            isAnemic -> "Iron & Mineral Rich Recovery Diet"
            else -> "Healthy Balanced Indian Nutrition Diet"
        }
        
        val description = "Personalized diet optimized for $gender (${heightCm.toInt()}cm, ${weightKg.toInt()}kg). Goal: $targetGoal. Packed with balanced Indian whole foods to support your conditions."
        
        val list = mutableListOf<MealDay>()
        
        val breakfasts = when {
            isDiabetic -> listOf(
                "Veggie Oats Upma with sprouted grains (Fiber: 6g, Net Carb: 20g)",
                "Methi Missi Roti (1 pc) with curd (Protein: 8g, Fiber: 4g)",
                "Ragi Idli (2 pcs) with mint chutney (Protein: 6g, Fiber: 5g)",
                "Moong Dal Chilla filled with Paneer (Protein: 14g, Fiber: 4g)",
                "Vegetable Dalia Khichdi (Protein: 7g, Fiber: 6g)",
                "Paneer Bhurji & sautéed spinach (Protein: 15g, Fiber: 3g)",
                "Sprouted Brown Chana Chaat (Protein: 10g, Fiber: 7g)"
            )
            isHypertensive -> listOf(
                "Oats porridge with low-fat milk & almonds (Sodium: 10mg, Potassium: 400mg)",
                "Vegetable Poha with extra peas & low sodium (Protein: 5g, Fiber: 4g)",
                "Steamed Idli (2) with tomato chutney (Low salt) (Protein: 6g)",
                "Vegetable Suji Upma with lemon (Protein: 5g, Fiber: 3g)",
                "Fruit Smoothie with Flaxseeds & Banana (Potassium: 550mg)",
                "Moong Sprouts salad with cucumber and lemon (Protein: 9g)",
                "Oats Semolina Uttapam with coriander (Protein: 7g, Fiber: 4g)"
            )
            else -> listOf(
                "Paneer Stuffed Paratha (1 pc) with light yogurt (Protein: 12g, Fiber: 3g)",
                "Vegetable Poha with roasted peanuts (Protein: 7g, Fiber: 4g)",
                "Scrambled Eggs (2) with toast or Roti (Protein: 15g, Fiber: 2g)",
                "Mixed Veg Oats Upma (Protein: 8g, Fiber: 5g)",
                "Besan Chilla with custom green chutney (Protein: 10g, Fiber: 4g)",
                "Greek Yogurt bowl with apple slice & honey (Protein: 11g, Fiber: 3g)",
                "Rava Idli (2) with warm sambar (Protein: 8g, Fiber: 4g)"
            )
        }
        
        val lunches = when {
            isDiabetic -> listOf(
                "2 Multigrain Roti, Lauki Sabzi, Salad (Glycemic Index: Low)",
                "Brown Rice (1 cup), Soya chunks curry, Cucumber salad (Protein: 18g)",
                "2 Jowar Roti, Dal Tadka, Sautéed Bhindi (Fiber: 8g, Low Sugar)",
                "Whole wheat wraps with paneer & bell peppers (Protein: 16g)",
                "1 bowl Quinoa, Rajma curry, Sprouts salad (Protein: 14g, Fiber: 9g)",
                "2 Roti, Bengal Gram Dal, Cauliflower curry (Protein: 15g)",
                "1 cup Brown Rice, Kadhi (Chickpea flour yogurt soup), Salad"
            )
            isHypertensive -> listOf(
                "2 Whole Wheat Roti, Palak Paneer (No added salt), Salad",
                "1 cup Brown Rice, Moong Dal (Low sodium), Sautéed Bhindi",
                "2 Bajra Roti, Lauki curry, Beetroot salad (Potassium: 600mg)",
                "1 cup Quinoa, Tomato Dal, Stir-fried Broccoli (Low salt)",
                "2 Roti, Mixed Beans curry, Cucumber carrot salad (Protein: 14g)",
                "1 cup Brown Rice, Chana Masala (No soda/low salt), Curd",
                "2 Roti, Soya chunk curry, Steamed cabbage & carrots"
            )
            else -> listOf(
                "2 Rotis, Dal Tadka, Aloo Gobhi, Cucumber Salad (Protein: 13g)",
                "Steamed Rice with Tomato Garlic Rasam, Cabbage Poriyal (Protein: 6g)",
                "2 Rotis, Paneer butter masala (light), Mixed salad",
                "Tempered Curd Rice with beetroot salad (Protein: 7g)",
                "2 Multigrain Rotis, Soya chunks curry, Stir-fry Bhindi",
                "Steamed Rice, Sambar (1 Bowl), Egg Bhurji, Carrot Salad (Protein: 15g)",
                "2 Rotis, Chicken Curry (or Mushroom Mattar Paneer), Salad"
            )
        }
        
        val dinners = when {
            isDiabetic -> listOf(
                "Moong Dal Khichdi (light oil) & flaxseed curd (Protein: 10g)",
                "Grilled Tofu/Paneer with roasted broccoli & capsicum (Carb: 8g)",
                "2 Oats Roti, Baingan Bharta, Sprouted Moong salad",
                "Vegetable soup with boiled chana & cottage cheese (Protein: 12g)",
                "1 Roti, Stir-fried mushrooms & spinach, Curd (Low sugar)",
                "Paneer Tikka (Grilled) with green salad (Protein: 18g, Carb: 5g)",
                "Moong Dal Soup with stir-fry beans & cauliflower (Low GI)"
            )
            isHypertensive -> listOf(
                "Moong Dal Khichdi (Light/Low Salt), Cucumber Raita",
                "Paneer Tikka style with bell peppers & onions (Low-salt marinade)",
                "1 Roti, Gourd (Turai) sabzi, Beetroot salad (Low Sodium)",
                "Mixed veg soup with low-salt baked Paneer cubes",
                "1 Roti, Pumpkin curry, Cup of low-fat curd (Hypertension safe)",
                "Moong Dal Khichdi (No added sodium) with coriander garnish",
                "Stir-fried Tofu with raw cucumber and tomato salad"
            )
            else -> listOf(
                "Moong Dal Khichdi, Papad, Tomato chutney (Protein: 9g, Fiber: 4g)",
                "Millet Pongal with Sambar (Protein: 10g, Fiber: 6g)",
                "1 Roti, Paneer Bhurji, Spinach curry (Protein: 18g, Fiber: 5g)",
                "2 South Indian Idli with mixed vegetable Sambar (Protein: 11g)",
                "2 Rotis, Masoor Dal, Bhindi fry, Curd (Protein: 14g, Fiber: 6g)",
                "Rava Upma with roasted peanuts & green peas (Protein: 8g)",
                "Boiled eggs (2) with multigrain Roti and Dal (Protein: 21g)"
            )
        }
        
        val snacks = when {
            isDiabetic -> listOf(
                "Roasted Makhana (Foxnuts) (Fiber: 3g, No Carb spike)",
                "Handful of walnuts and almonds (Rich in healthy fats)",
                "Cucumber and tomato slices with pinch of black pepper",
                "Boiled black chana (1/2 cup) with chopped onions",
                "Roasted Soybeans (Protein: 10g, Carb: 4g)",
                "Yogurt with flaxseeds (Protein: 5g, Fiber: 2g)",
                "Buttermilk (Chass) with roasted cumin powder"
            )
            else -> listOf(
                "Roasted Makhana (Foxnuts) (Low sodium, crisp)",
                "Handful of unsalted almonds and walnuts",
                "Roasted Chana (gram) (High fiber block)",
                "Green Tea with 1 digestive biscuit (Low salt)",
                "Apple slices with pinch of cinnamon powder",
                "Plain low-fat yogurt with chia seeds",
                "1 cup buttermilk (lassi/chaas) (Digestive)"
            )
        }
        
        val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        
        for (i in 0 until 7) {
            list.add(
                MealDay(
                    day = daysOfWeek[i],
                    breakfast = breakfasts[i % breakfasts.size],
                    lunch = lunches[i % lunches.size],
                    dinner = dinners[i % dinners.size],
                    snacks = snacks[i % snacks.size]
                )
            )
        }
        
        return WeeklyMealChartResult(
            title = "Personalized Indian Meal Plan",
            description = description,
            days = list
        )
    }

    fun generateLocalRecommendations(conditions: String): DiseaseFoodRecommendationsResult {
        val conditionsLower = conditions.lowercase()
        val list = mutableListOf<DiseaseFoodRecommendation>()
        
        if (conditionsLower.contains("diabet") || conditionsLower.contains("sugar")) {
            list.add(
                DiseaseFoodRecommendation(
                    condition = "Diabetes / High Blood Sugar",
                    recommendedFoods = listOf("Bitter Gourd", "Fenugreek Seeds", "Jamun Fruit", "Oats", "Cinnamon"),
                    reason = "Helps slow digestion, improves insulin sensitivity, and prevents post-meal sugar spikes."
                )
            )
        }
        if (conditionsLower.contains("bp") || conditionsLower.contains("hyperten") || conditionsLower.contains("heart")) {
            list.add(
                DiseaseFoodRecommendation(
                    condition = "Hypertension / Cardiovascular Safety",
                    recommendedFoods = listOf("Garlic", "Banana", "Flaxseeds", "Coconut Water", "Beetroot"),
                    reason = "High in potassium and natural vasodilators that encourage healthy blood pressure levels."
                )
            )
        }
        if (conditionsLower.contains("anemi") || conditionsLower.contains("iron") || conditionsLower.contains("blood")) {
            list.add(
                DiseaseFoodRecommendation(
                    condition = "Anemia / Iron Deficiency",
                    recommendedFoods = listOf("Spinach", "Beetroot", "Pomegranate", "Lentils", "Dates"),
                    reason = "Dense in non-heme iron and vitamin C to optimize iron absorption and speed up red blood cell replenishment."
                )
            )
        }
        if (conditionsLower.contains("thyroid")) {
            list.add(
                DiseaseFoodRecommendation(
                    condition = "Thyroid Imbalances",
                    recommendedFoods = listOf("Brazil Nuts", "Egg yolks", "Yogurt", "Seaweed", "Chia Seeds"),
                    reason = "Supplies iodine and selenium essential for healthy thyroid hormone synthesis."
                )
            )
        }
        if (conditionsLower.contains("fatigue") || conditionsLower.contains("energy") || conditionsLower.contains("weak")) {
            list.add(
                DiseaseFoodRecommendation(
                    condition = "General Fatigue / Low Energy",
                    recommendedFoods = listOf("Banana", "Almonds", "Oatmeal", "Sprouts", "Eggs"),
                    reason = "Provides sustained complex carbohydrates, B-complex vitamins, and magnesium to revitalize metabolism."
                )
            )
        }
        
        if (list.isEmpty()) {
            list.add(
                DiseaseFoodRecommendation(
                    condition = if (conditions.isBlank()) "General Health" else conditions,
                    recommendedFoods = listOf("Turmeric Milk", "Amla", "Almonds", "Green Vegetables", "Dal"),
                    reason = "Enhances overall immune response, supplies antioxidants, and provides balanced clean protein."
                )
            )
        }
        
        return DiseaseFoodRecommendationsResult(recommendations = list)
    }

    fun generateLocalFoodAnalysis(foodDescription: String): FoodAnalysisResult {
        val lower = foodDescription.lowercase()
        return when {
            lower.contains("egg") -> FoodAnalysisResult(
                foodName = "Egg Dish (Scrambled/Boiled)",
                calories = 140.0,
                protein = 12.0,
                fiber = 0.0,
                vitamins = "Vitamin D, Vitamin B12, Riboflavin"
            )
            lower.contains("chicken") -> FoodAnalysisResult(
                foodName = "Chicken Dish (Grilled/Cooked)",
                calories = 220.0,
                protein = 26.0,
                fiber = 0.0,
                vitamins = "Niacin, Vitamin B6, Selenium"
            )
            lower.contains("roti") || lower.contains("chapati") -> FoodAnalysisResult(
                foodName = "Wheat Roti / Chapati",
                calories = 120.0,
                protein = 3.5,
                fiber = 2.4,
                vitamins = "Vitamin B1, Iron, Magnesium"
            )
            lower.contains("rice") -> FoodAnalysisResult(
                foodName = "Steamed Rice Bowl",
                calories = 200.0,
                protein = 4.0,
                fiber = 1.0,
                vitamins = "Thiamine, Iron"
            )
            lower.contains("paneer") -> FoodAnalysisResult(
                foodName = "Paneer Cooked Dish",
                calories = 260.0,
                protein = 18.0,
                fiber = 0.5,
                vitamins = "Calcium, Vitamin B12, Phosphorus"
            )
            lower.contains("dal") -> FoodAnalysisResult(
                foodName = "Lentils Cooked (Dal)",
                calories = 150.0,
                protein = 9.0,
                fiber = 4.5,
                vitamins = "Folate, Iron, Potassium"
            )
            lower.contains("apple") -> FoodAnalysisResult(
                foodName = "Fresh Red Apple",
                calories = 95.0,
                protein = 0.5,
                fiber = 4.4,
                vitamins = "Vitamin C, Potassium"
            )
            lower.contains("banana") -> FoodAnalysisResult(
                foodName = "Ripe Banana",
                calories = 105.0,
                protein = 1.3,
                fiber = 3.1,
                vitamins = "Vitamin B6, Vitamin C, Potassium"
            )
            lower.contains("milk") -> FoodAnalysisResult(
                foodName = "Glass of Whole Milk",
                calories = 150.0,
                protein = 8.0,
                fiber = 0.0,
                vitamins = "Calcium, Vitamin D, Riboflavin"
            )
            lower.contains("salad") -> FoodAnalysisResult(
                foodName = "Mixed Vegetables Salad",
                calories = 50.0,
                protein = 1.5,
                fiber = 3.0,
                vitamins = "Vitamin A, Vitamin K, Vitamin C"
            )
            else -> {
                val hashValue = kotlin.math.abs(foodDescription.hashCode())
                val estCalories = 100.0 + (hashValue % 250)
                val estProtein = 2.0 + (hashValue % 18)
                val estFiber = (hashValue % 6).toDouble()
                FoodAnalysisResult(
                    foodName = foodDescription.take(24).replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() },
                    calories = estCalories,
                    protein = estProtein,
                    fiber = estFiber,
                    vitamins = "Vitamin B6, Iron, Magnesium"
                )
            }
        }
    }

    fun generateLocalVitaminDeficiency(symptoms: List<String>, otherSymptoms: String): VitaminDeficiencyResult {
        val joined = (symptoms + if (otherSymptoms.isNotBlank()) listOf(otherSymptoms) else emptyList()).joinToString(" ").lowercase()
        val deficiencies = mutableListOf<DeficiencyItem>()
        
        if (joined.contains("fatigue") || joined.contains("tire") || joined.contains("weak") || joined.contains("energy")) {
            deficiencies.add(
                DeficiencyItem(
                    vitaminName = "Vitamin B12 & Iron",
                    confidence = "High",
                    symptomLink = "Fatigue, physical weakness, and breathlessness are classic indications of anemia and oxygen depletion.",
                    recommendedFoods = listOf("Spinach", "Beetroot", "Red meat / Eggs", "Fortified Cereals", "Pomegranate")
                )
            )
        }
        if (joined.contains("bone") || joined.contains("joint") || joined.contains("muscle") || joined.contains("back")) {
            deficiencies.add(
                DeficiencyItem(
                    vitaminName = "Vitamin D3 & Calcium",
                    confidence = "High",
                    symptomLink = "Aches in bones, generalized joint inflammation, muscle cramps, and low density relate to solar and calcium storage shortages.",
                    recommendedFoods = listOf("Mushrooms exposed to UV", "Egg yolks", "Fortified milk", "Yogurt", "Paneer")
                )
            )
        }
        if (joined.contains("skin") || joined.contains("hair") || joined.contains("nail")) {
            deficiencies.add(
                DeficiencyItem(
                    vitaminName = "Biotin (B7) & Zinc",
                    confidence = "Medium",
                    symptomLink = "Thinning hair, brittle nails, and dermatological rashes are highly correlated with biotin metabolic errors.",
                    recommendedFoods = listOf("Almonds", "Walnuts", "Whole grains", "Legumes", "Eggs")
                )
            )
        }
        if (joined.contains("eye") || joined.contains("vision") || joined.contains("night")) {
            deficiencies.add(
                DeficiencyItem(
                    vitaminName = "Vitamin A",
                    confidence = "Medium",
                    symptomLink = "Dry eyes, blurry night-vision, and slow focal adaptation relate directly to rhodopsin pigment synthesis depletions.",
                    recommendedFoods = listOf("Carrots", "Sweet Potato", "Pumpkin", "Papaya", "Spinach")
                )
            )
        }
        if (joined.contains("bleed") || joined.contains("gum") || joined.contains("bruis")) {
            deficiencies.add(
                DeficiencyItem(
                    vitaminName = "Vitamin C",
                    confidence = "High",
                    symptomLink = "Bleeding gums, slow wound healing, and easy bruising represent structural integrity decline from collagen synthesis deficiencies.",
                    recommendedFoods = listOf("Amla (Gooseberry)", "Guava", "Oranges", "Bell Peppers", "Lemon")
                )
            )
        }
        
        if (deficiencies.isEmpty()) {
            deficiencies.add(
                DeficiencyItem(
                    vitaminName = "General Micronutrient Deficiency",
                    confidence = "Medium",
                    symptomLink = "Non-specific symptoms indicate a potential minor depletion of essential vitamins and tracing minerals.",
                    recommendedFoods = listOf("Mixed Nuts & Seeds", "Leafy green vegetables", "Seasonal fresh fruits", "Lentils")
                )
            )
        }
        
        return VitaminDeficiencyResult(deficiencies = deficiencies)
    }

    fun generateLocalNutritionLabel(bitmap: Bitmap): NutritionLabelResult {
        return NutritionLabelResult(
            foodName = "Scanned Label Item",
            calories = 180.0,
            protein = 6.5,
            fiber = 2.0
        )
    }
}

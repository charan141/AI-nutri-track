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
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is empty or placeholder!")
            return@withContext null
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
                return@withContext null
            }

            val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)
            val responseObj = responseAdapter.fromJson(responseBodyStr)
            val textResult = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (textResult != null) {
                Log.d(TAG, "Raw Response: $textResult")
                val resultAdapter = moshi.adapter(NutritionLabelResult::class.java)
                return@withContext resultAdapter.fromJson(textResult)
            } else {
                Log.e(TAG, "Empty text result in candidates!")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing nutrition label: ${e.message}", e)
            return@withContext null
        }
    }

    suspend fun analyzeVitaminDeficiency(symptoms: List<String>, otherSymptoms: String): VitaminDeficiencyResult? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is empty or placeholder!")
            return@withContext null
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
                return@withContext null
            }

            val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)
            val responseObj = responseAdapter.fromJson(responseBodyStr)
            val textResult = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (textResult != null) {
                Log.d(TAG, "Raw Deficiency Response: $textResult")
                val resultAdapter = moshi.adapter(VitaminDeficiencyResult::class.java)
                return@withContext resultAdapter.fromJson(textResult)
            } else {
                Log.e(TAG, "Empty text result in candidates!")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing deficiencies: ${e.message}", e)
            return@withContext null
        }
    }

    suspend fun analyzeFoodText(foodDescription: String): FoodAnalysisResult? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is empty or placeholder!")
            return@withContext null
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
                return@withContext null
            }

            val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)
            val responseObj = responseAdapter.fromJson(responseBodyStr)
            val textResult = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (textResult != null) {
                Log.d(TAG, "Raw Food Analysis Response: $textResult")
                val resultAdapter = moshi.adapter(FoodAnalysisResult::class.java)
                return@withContext resultAdapter.fromJson(textResult)
            } else {
                Log.e(TAG, "Empty text result in food text analysis candidates!")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing food text: ${e.message}", e)
            return@withContext null
        }
    }
}

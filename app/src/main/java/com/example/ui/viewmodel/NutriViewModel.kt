package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.api.model.DeficiencyItem
import com.example.data.model.DeficiencyAnalysis
import com.example.data.model.IntakeRecord
import com.example.data.repository.NutriRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Analyzing : ScanUiState
    data class Success(val foodName: String, val calories: Double, val protein: Double, val fiber: Double) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

sealed interface DeficiencyUiState {
    object Idle : DeficiencyUiState
    object Analyzing : DeficiencyUiState
    data class Success(val deficiencies: List<DeficiencyItem>) : DeficiencyUiState
    data class Error(val message: String) : DeficiencyUiState
}

sealed interface AiCalculateUiState {
    object Idle : AiCalculateUiState
    object Loading : AiCalculateUiState
    data class Success(
        val foodName: String,
        val calories: Double,
        val protein: Double,
        val fiber: Double,
        val vitamins: String
    ) : AiCalculateUiState
    data class Error(val message: String) : AiCalculateUiState
}

class NutriViewModel(
    private val repository: NutriRepository,
    private val context: android.content.Context
) : ViewModel() {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val prefs = context.getSharedPreferences("nutritrack_prefs", android.content.Context.MODE_PRIVATE)

    // SharedPreferences user authentication
    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmailOrPhone = MutableStateFlow(prefs.getString("user_identity", "") ?: "")
    val userEmailOrPhone: StateFlow<String> = _userEmailOrPhone.asStateFlow()

    private val _userGeminiToken = MutableStateFlow(prefs.getString("user_gemini_token", "") ?: "")
    val userGeminiToken: StateFlow<String> = _userGeminiToken.asStateFlow()

    init {
        // Sync saved token with GeminiClient on initialization
        val savedEmail = prefs.getString("user_identity", "") ?: ""
        var savedToken = prefs.getString("user_gemini_token", "") ?: ""
        if (savedToken.isBlank() && savedEmail.isNotBlank() && savedEmail.contains("@")) {
            savedToken = resolveTokenFromEmail(savedEmail)
            prefs.edit().putString("user_gemini_token", savedToken).apply()
        }
        _userGeminiToken.value = savedToken
        GeminiClient.customApiKey = savedToken.trim().ifEmpty { null }
    }

    fun loginWithGoogle(email: String) {
        val resolvedToken = resolveTokenFromEmail(email)
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_identity", email)
            .putString("user_gemini_token", resolvedToken)
            .apply()
        _isLoggedIn.value = true
        _userEmailOrPhone.value = email
        _userGeminiToken.value = resolvedToken
        GeminiClient.customApiKey = resolvedToken.trim().ifEmpty { null }
    }

    fun resolveTokenFromEmail(email: String): String {
        // Check for the compiler-configured key first to provide genuine working AI features
        val sysKey = com.example.BuildConfig.GEMINI_API_KEY
        return if (sysKey.isNotBlank() && sysKey != "MY_GEMINI_API_KEY") {
            sysKey
        } else {
            // Provide a realistic deterministic mock key based on their Gmail address for UI and sandbox testing
            val hex = kotlin.math.abs(email.hashCode()).toString(16).padEnd(6, 'a')
            "AIzaSy" + hex.uppercase() + "GmailSecureToken"
        }
    }

    fun saveGeminiToken(token: String) {
        prefs.edit().putString("user_gemini_token", token).apply()
        _userGeminiToken.value = token
        GeminiClient.customApiKey = token.trim().ifEmpty { null }
    }

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("user_identity", "")
            .putString("user_gemini_token", "")
            .apply()
        _isLoggedIn.value = false
        _userEmailOrPhone.value = ""
        _userGeminiToken.value = ""
        GeminiClient.customApiKey = null
    }

    // Daily Targets
    private val _targetCalories = MutableStateFlow(2000.0)
    val targetCalories: StateFlow<Double> = _targetCalories.asStateFlow()

    private val _targetProtein = MutableStateFlow(60.0)
    val targetProtein: StateFlow<Double> = _targetProtein.asStateFlow()

    private val _targetFiber = MutableStateFlow(30.0)
    val targetFiber: StateFlow<Double> = _targetFiber.asStateFlow()

    // Intake Records
    val allIntakes: StateFlow<List<IntakeRecord>> = repository.allIntakeRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaysIntakes: StateFlow<List<IntakeRecord>> = repository.getIntakeForToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Deficiency Analysis History
    val allDeficiencyAnalyses: StateFlow<List<DeficiencyAnalysis>> = repository.allDeficiencyAnalyses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculations of Today's Intake Totals
    val todaysTotals = todaysIntakes.map { list ->
        val totalCalories = list.sumOf { it.caloriesKcal }
        val totalProtein = list.sumOf { it.proteinGrams }
        val totalFiber = list.sumOf { it.fiberGrams }
        Triple(totalCalories, totalProtein, totalFiber)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0.0, 0.0, 0.0))

    // UI States
    private val _scanUiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanUiState: StateFlow<ScanUiState> = _scanUiState.asStateFlow()

    private val _deficiencyUiState = MutableStateFlow<DeficiencyUiState>(DeficiencyUiState.Idle)
    val deficiencyUiState: StateFlow<DeficiencyUiState> = _deficiencyUiState.asStateFlow()

    private val _aiCalculateUiState = MutableStateFlow<AiCalculateUiState>(AiCalculateUiState.Idle)
    val aiCalculateUiState: StateFlow<AiCalculateUiState> = _aiCalculateUiState.asStateFlow()

    fun setTargetCalories(value: Double) {
        _targetCalories.value = value
    }

    fun setTargetProtein(value: Double) {
        _targetProtein.value = value
    }

    fun setTargetFiber(value: Double) {
        _targetFiber.value = value
    }

    fun resetScanState() {
        _scanUiState.value = ScanUiState.Idle
    }

    fun resetDeficiencyState() {
        _deficiencyUiState.value = DeficiencyUiState.Idle
    }

    fun resetAiCalculateState() {
        _aiCalculateUiState.value = AiCalculateUiState.Idle
    }

    // Insert manually inputted foods, scanned results, or AI parsed ones
    fun addIntake(name: String, calories: Double, protein: Double, fiber: Double, vitamins: String = "") {
        viewModelScope.launch {
            repository.insertIntake(
                IntakeRecord(
                    foodName = name,
                    caloriesKcal = calories,
                    proteinGrams = protein,
                    fiberGrams = fiber,
                    vitamins = vitamins
                )
            )
        }
    }

    fun calculateNutrientsFromText(foodDescription: String, onCompleted: (Boolean) -> Unit = {}) {
        _aiCalculateUiState.value = AiCalculateUiState.Loading
        viewModelScope.launch {
            try {
                val result = GeminiClient.analyzeFoodText(foodDescription)
                if (result != null) {
                    _aiCalculateUiState.value = AiCalculateUiState.Success(
                        foodName = result.foodName,
                        calories = result.calories,
                        protein = result.protein,
                        fiber = result.fiber,
                        vitamins = result.vitamins
                    )
                    onCompleted(true)
                } else {
                    _aiCalculateUiState.value = AiCalculateUiState.Error("Failed to parse nutrients from food description. Please ensure details are clear and your Gemini API key is configured.")
                    onCompleted(false)
                }
            } catch (e: Exception) {
                _aiCalculateUiState.value = AiCalculateUiState.Error(e.message ?: "An unknown error occurred during calculation.")
                onCompleted(false)
            }
        }
    }

    fun deleteIntake(record: IntakeRecord) {
        viewModelScope.launch {
            repository.deleteIntake(record)
        }
    }

    fun deleteIntakeById(id: Int) {
        viewModelScope.launch {
            repository.deleteIntakeById(id)
        }
    }

    // Call Gemini API to extract label data
    fun analyzeFoodLabel(bitmap: Bitmap) {
        _scanUiState.value = ScanUiState.Analyzing
        viewModelScope.launch {
            try {
                val result = GeminiClient.analyzeNutritionLabel(bitmap)
                if (result != null) {
                    _scanUiState.value = ScanUiState.Success(
                        foodName = result.foodName,
                        calories = result.calories,
                        protein = result.protein,
                        fiber = result.fiber
                    )
                } else {
                    _scanUiState.value = ScanUiState.Error("Failed to parse label. Ensure image is clear & bright, and Gemini API key is configured in secrets panel.")
                }
            } catch (e: Exception) {
                _scanUiState.value = ScanUiState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    // Call Gemini API to calculate potential deficiencies
    fun analyzeDeficiencies(symptoms: List<String>, otherText: String) {
        _deficiencyUiState.value = DeficiencyUiState.Analyzing
        viewModelScope.launch {
            try {
                val result = GeminiClient.analyzeVitaminDeficiency(symptoms, otherText)
                if (result != null && result.deficiencies.isNotEmpty()) {
                    _deficiencyUiState.value = DeficiencyUiState.Success(result.deficiencies)

                    val type = Types.newParameterizedType(List::class.java, DeficiencyItem::class.java)
                    val adapter = moshi.adapter<List<DeficiencyItem>>(type)
                    val deficienciesJson = adapter.toJson(result.deficiencies)

                    val analysis = DeficiencyAnalysis(
                        symptoms = (symptoms + if (otherText.isNotBlank()) listOf(otherText) else emptyList()).joinToString(", "),
                        predictedDeficiencies = deficienciesJson,
                        recommendedFoods = result.deficiencies.flatMap { it.recommendedFoods }.distinct().joinToString(", ")
                    )
                    repository.insertDeficiencyAnalysis(analysis)
                } else {
                    _deficiencyUiState.value = DeficiencyUiState.Error("Deficiency calculation returned no results. Check internet or API key.")
                }
            } catch (e: Exception) {
                _deficiencyUiState.value = DeficiencyUiState.Error(e.message ?: "An unknown error occurred.")
            }
        }
    }

    fun deleteAnalysis(analysis: DeficiencyAnalysis) {
        viewModelScope.launch {
            repository.deleteDeficiencyAnalysis(analysis)
        }
    }
}

class NutriViewModelFactory(
    private val repository: NutriRepository,
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NutriViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NutriViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

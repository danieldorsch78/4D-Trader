package com.fourdigital.marketintelligence.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdigital.marketintelligence.core.network.api.BrapiApi
import com.fourdigital.marketintelligence.core.network.api.CoinGeckoApi
import com.fourdigital.marketintelligence.core.network.api.FinnhubApi
import com.fourdigital.marketintelligence.core.network.api.OpenAIModelsApi
import com.fourdigital.marketintelligence.data.provider.ApiKeyManager
import com.fourdigital.marketintelligence.data.provider.GitHubAIAnalyst
import com.fourdigital.marketintelligence.domain.model.*
import com.fourdigital.marketintelligence.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ApiKeyState(
    val finnhubKey: String = "",
    val brapiKey: String = "",
    val twelveDataKey: String = "",
    val massiveKey: String = "",
    val githubToken: String = "",
    val openAiKey: String = ""
)

enum class ApiTestStatus { IDLE, TESTING, SUCCESS, ERROR }

data class ApiTestResult(
    val status: ApiTestStatus = ApiTestStatus.IDLE,
    val message: String = ""
)

data class ApiDiagnosticsState(
    val coinGecko: ApiTestResult = ApiTestResult(),
    val finnhub: ApiTestResult = ApiTestResult(),
    val brapi: ApiTestResult = ApiTestResult(),
    val githubModels: ApiTestResult = ApiTestResult(),
    val openAI: ApiTestResult = ApiTestResult()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepo: PreferencesRepository,
    private val apiKeyManager: ApiKeyManager,
    private val coinGeckoApi: CoinGeckoApi,
    private val finnhubApi: FinnhubApi,
    private val brapiApi: BrapiApi,
    private val openAIModelsApi: OpenAIModelsApi,
    private val gitHubAI: GitHubAIAnalyst
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepo
        .observePreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _apiKeyState = MutableStateFlow(ApiKeyState())
    val apiKeyState: StateFlow<ApiKeyState> = _apiKeyState.asStateFlow()

    val availableAIModels = GitHubAIAnalyst.AVAILABLE_MODELS

    init {
        viewModelScope.launch {
            loadApiKeysInternal()
            // Auto-test all connections after keys are loaded
            testAllApis()
        }
    }

    private suspend fun loadApiKeysInternal() {
        try {
            val finnhub = apiKeyManager.getKey(ApiKeyManager.FINNHUB) ?: ""
            val brapi = apiKeyManager.getKey(ApiKeyManager.BRAPI) ?: ""
            val twelveData = apiKeyManager.getKey(ApiKeyManager.TWELVE_DATA) ?: ""
            val massive = apiKeyManager.getKey(ApiKeyManager.MASSIVE) ?: ""
            val github = apiKeyManager.getKey(GitHubAIAnalyst.GITHUB_KEY) ?: ""
            val openAi = apiKeyManager.getKey(ApiKeyManager.OPENAI) ?: ""
            _apiKeyState.value = ApiKeyState(finnhub, brapi, twelveData, massive, github, openAi)
            // Wait for first real emission from preferences before syncing
            val current = preferences.first()
            val needsSync = current.finnhubKeyConfigured != finnhub.isNotBlank()
                    || current.brapiKeyConfigured != brapi.isNotBlank()
                    || current.openAiKeyConfigured != openAi.isNotBlank()
            if (needsSync) {
                preferencesRepo.updatePreferences(
                    current.copy(
                        finnhubKeyConfigured = finnhub.isNotBlank(),
                        brapiKeyConfigured = brapi.isNotBlank(),
                        openAiKeyConfigured = openAi.isNotBlank()
                    )
                )
            }
            gitHubAI.setProviderMode(current.aiProviderMode)
            gitHubAI.setSelectedModel(current.selectedAIModel)
        } catch (e: Exception) {
            // EncryptedSharedPreferences can throw if Keystore is corrupted
            _apiKeyState.value = ApiKeyState()
        }
    }

    fun updateTheme(theme: AppTheme) = update { it.copy(theme = theme) }
    fun updateDefaultMarket(market: String) = update { it.copy(defaultMarket = market) }
    fun updateRefreshInterval(seconds: Int) = update { it.copy(refreshIntervalSeconds = seconds) }
    fun updateStreaming(enabled: Boolean) = update { it.copy(streamingEnabled = enabled) }
    fun updateRiskProfile(profile: RiskProfile) = update { it.copy(riskProfile = profile) }
    fun updateNotifications(enabled: Boolean) = update { it.copy(notificationsEnabled = enabled) }
    fun updateAlertSound(enabled: Boolean) = update { it.copy(alertSoundEnabled = enabled) }
    fun updateDeveloperMode(enabled: Boolean) = update { it.copy(developerMode = enabled) }
    fun updateShowDebugPanel(enabled: Boolean) = update { it.copy(showDebugPanel = enabled) }

    fun updateAnalyticsWindows(short: Int, medium: Int, long: Int) = update {
        it.copy(analyticsWindowShort = short, analyticsWindowMedium = medium, analyticsWindowLong = long)
    }

    // AI Configuration
    fun updateSelectedAIModel(modelId: String) {
        gitHubAI.setSelectedModel(modelId)
        update { it.copy(selectedAIModel = modelId) }
    }

    fun updateAIProviderMode(mode: AIProviderMode) {
        gitHubAI.setProviderMode(mode)
        update { it.copy(aiProviderMode = mode) }
    }

    fun updateAIAutoRefresh(enabled: Boolean) = update { it.copy(aiAutoRefreshEnabled = enabled) }
    fun updateAIRefreshInterval(seconds: Int) = update { it.copy(aiAutoRefreshIntervalSeconds = seconds.coerceIn(15, 300)) }
    fun updateTargetReturn(percent: Double) = update { it.copy(targetReturnPercent = percent.coerceIn(1.0, 100.0)) }

    // Prediction horizons
    fun updateShowWeeklyPredictions(show: Boolean) = update { it.copy(showWeeklyPredictions = show) }
    fun updateShowMonthlyPredictions(show: Boolean) = update { it.copy(showMonthlyPredictions = show) }
    fun updateShowQuarterlyPredictions(show: Boolean) = update { it.copy(showQuarterlyPredictions = show) }
    fun updateShowYearlyPredictions(show: Boolean) = update { it.copy(showYearlyPredictions = show) }

    // Signal center
    fun updateMaxSignalAssets(max: Int) = update { it.copy(maxSignalAssets = max.coerceIn(5, 50)) }
    fun updateSignalAutoRefresh(enabled: Boolean) = update { it.copy(signalAutoRefresh = enabled) }
    fun updateSignalRefreshInterval(seconds: Int) = update { it.copy(signalRefreshIntervalSeconds = seconds.coerceIn(15, 300)) }

    // Cross-asset
    fun updateCrossAssetAutoRefresh(enabled: Boolean) = update { it.copy(crossAssetAutoRefresh = enabled) }
    fun updateCrossAssetRefreshInterval(seconds: Int) = update { it.copy(crossAssetRefreshIntervalSeconds = seconds.coerceIn(10, 120)) }

    fun saveFinnhubKey(key: String) {
        viewModelScope.launch {
            if (key.isBlank()) {
                apiKeyManager.deleteKey(ApiKeyManager.FINNHUB)
            } else {
                apiKeyManager.setKey(ApiKeyManager.FINNHUB, key.trim())
            }
            _apiKeyState.update { it.copy(finnhubKey = key.trim()) }
            update { it.copy(finnhubKeyConfigured = key.isNotBlank()) }
            testFinnhub()
        }
    }

    fun saveBrapiKey(key: String) {
        viewModelScope.launch {
            if (key.isBlank()) {
                apiKeyManager.deleteKey(ApiKeyManager.BRAPI)
            } else {
                apiKeyManager.setKey(ApiKeyManager.BRAPI, key.trim())
            }
            _apiKeyState.update { it.copy(brapiKey = key.trim()) }
            update { it.copy(brapiKeyConfigured = key.isNotBlank()) }
            testBrapi()
        }
    }

    fun saveTwelveDataKey(key: String) {
        viewModelScope.launch {
            if (key.isBlank()) {
                apiKeyManager.deleteKey(ApiKeyManager.TWELVE_DATA)
            } else {
                apiKeyManager.setKey(ApiKeyManager.TWELVE_DATA, key.trim())
            }
            _apiKeyState.update { it.copy(twelveDataKey = key.trim()) }
        }
    }

    fun saveMassiveKey(key: String) {
        viewModelScope.launch {
            if (key.isBlank()) {
                apiKeyManager.deleteKey(ApiKeyManager.MASSIVE)
            } else {
                apiKeyManager.setKey(ApiKeyManager.MASSIVE, key.trim())
            }
            _apiKeyState.update { it.copy(massiveKey = key.trim()) }
        }
    }

    fun saveGitHubToken(token: String) {
        viewModelScope.launch {
            if (token.isBlank()) {
                apiKeyManager.deleteKey(GitHubAIAnalyst.GITHUB_KEY)
            } else {
                apiKeyManager.setKey(GitHubAIAnalyst.GITHUB_KEY, token.trim())
            }
            _apiKeyState.update { it.copy(githubToken = token.trim()) }
            testGitHubModels()
        }
    }

    fun saveOpenAIKey(key: String) {
        viewModelScope.launch {
            if (key.isBlank()) {
                apiKeyManager.deleteKey(ApiKeyManager.OPENAI)
            } else {
                apiKeyManager.setKey(ApiKeyManager.OPENAI, key.trim())
            }
            _apiKeyState.update { it.copy(openAiKey = key.trim()) }
            update { it.copy(openAiKeyConfigured = key.isNotBlank()) }
            testOpenAI()
        }
    }

    private fun update(transform: (UserPreferences) -> UserPreferences) {
        viewModelScope.launch {
            val current = preferences.first()
            preferencesRepo.updatePreferences(transform(current))
        }
    }

    // ---- API Diagnostics ----

    private val _diagnostics = MutableStateFlow(ApiDiagnosticsState())
    val diagnostics: StateFlow<ApiDiagnosticsState> = _diagnostics.asStateFlow()

    fun testAllApis() {
        testCoinGecko()
        testFinnhub()
        testBrapi()
        testGitHubModels()
        testOpenAI()
    }

    fun testCoinGecko() {
        viewModelScope.launch {
            _diagnostics.update { it.copy(coinGecko = ApiTestResult(ApiTestStatus.TESTING, "Testing...")) }
            try {
                val result = coinGeckoApi.getSimplePrice(ids = "bitcoin", vsCurrencies = "usd")
                val btcPrice = result["bitcoin"]?.usd
                if (btcPrice != null && btcPrice > 0) {
                    _diagnostics.update {
                        it.copy(coinGecko = ApiTestResult(ApiTestStatus.SUCCESS, "OK — BTC = \$${String.format("%.0f", btcPrice)}"))
                    }
                } else {
                    _diagnostics.update {
                        it.copy(coinGecko = ApiTestResult(ApiTestStatus.ERROR, "Empty response"))
                    }
                }
            } catch (e: Exception) {
                _diagnostics.update {
                    it.copy(coinGecko = ApiTestResult(ApiTestStatus.ERROR, e.message?.take(80) ?: "Unknown error"))
                }
            }
        }
    }

    fun testFinnhub() {
        viewModelScope.launch {
            try {
                val key = apiKeyManager.getKey(ApiKeyManager.FINNHUB)
                if (key.isNullOrBlank()) {
                    _diagnostics.update { it.copy(finnhub = ApiTestResult(ApiTestStatus.ERROR, "No API key configured")) }
                    return@launch
                }
                _diagnostics.update { it.copy(finnhub = ApiTestResult(ApiTestStatus.TESTING, "Testing...")) }
                val quote = finnhubApi.getQuote(symbol = "AAPL", token = key)
                if (quote.current > 0) {
                    _diagnostics.update {
                        it.copy(finnhub = ApiTestResult(ApiTestStatus.SUCCESS, "OK — AAPL = \$${String.format("%.2f", quote.current)}"))
                    }
                } else {
                    _diagnostics.update {
                        it.copy(finnhub = ApiTestResult(ApiTestStatus.ERROR, "Invalid key or no data (price=0)"))
                    }
                }
            } catch (e: Exception) {
                _diagnostics.update {
                    it.copy(finnhub = ApiTestResult(ApiTestStatus.ERROR, e.message?.take(80) ?: "Unknown error"))
                }
            }
        }
    }

    fun testBrapi() {
        viewModelScope.launch {
            try {
                val key = apiKeyManager.getKey(ApiKeyManager.BRAPI)
                if (key.isNullOrBlank()) {
                    _diagnostics.update { it.copy(brapi = ApiTestResult(ApiTestStatus.ERROR, "No API key configured")) }
                    return@launch
                }
                _diagnostics.update { it.copy(brapi = ApiTestResult(ApiTestStatus.TESTING, "Testing...")) }
                val result = brapiApi.getQuotes(symbols = "PETR4", token = key)
                val quote = result.results.firstOrNull()
                if (quote != null && quote.regularMarketPrice > 0) {
                    _diagnostics.update {
                        it.copy(brapi = ApiTestResult(
                            ApiTestStatus.SUCCESS,
                            "OK — PETR4 = R\$${String.format("%.2f", quote.regularMarketPrice)}"
                        ))
                    }
                } else {
                    _diagnostics.update {
                        it.copy(brapi = ApiTestResult(ApiTestStatus.ERROR, "Invalid key or empty response"))
                    }
                }
            } catch (e: Exception) {
                _diagnostics.update {
                    it.copy(brapi = ApiTestResult(ApiTestStatus.ERROR, e.message?.take(80) ?: "Unknown error"))
                }
            }
        }
    }

    fun testGitHubModels() {
        viewModelScope.launch {
            try {
                val available = gitHubAI.isAvailable()
                if (!available) {
                    _diagnostics.update { it.copy(githubModels = ApiTestResult(ApiTestStatus.ERROR, "No GitHub PAT configured")) }
                    return@launch
                }
                _diagnostics.update { it.copy(githubModels = ApiTestResult(ApiTestStatus.TESTING, "Testing...")) }
                val models = gitHubAI.listAvailableModels()
                if (models.isNotEmpty()) {
                    _diagnostics.update {
                        it.copy(githubModels = ApiTestResult(
                            ApiTestStatus.SUCCESS,
                            "OK — ${models.size} models (${gitHubAI.getSelectedModel()})"
                        ))
                    }
                } else {
                    _diagnostics.update {
                        it.copy(githubModels = ApiTestResult(ApiTestStatus.SUCCESS, "OK — Connected"))
                    }
                }
            } catch (e: Exception) {
                _diagnostics.update {
                    it.copy(githubModels = ApiTestResult(ApiTestStatus.ERROR, e.message?.take(80) ?: "Unknown error"))
                }
            }
        }
    }

    fun testOpenAI() {
        viewModelScope.launch {
            try {
                val key = apiKeyManager.getKey(ApiKeyManager.OPENAI)
                if (key.isNullOrBlank()) {
                    _diagnostics.update { it.copy(openAI = ApiTestResult(ApiTestStatus.ERROR, "No OpenAI key configured")) }
                    return@launch
                }
                _diagnostics.update { it.copy(openAI = ApiTestResult(ApiTestStatus.TESTING, "Testing...")) }
                val models = openAIModelsApi.listModels("Bearer $key")
                if (models.data.isNotEmpty()) {
                    _diagnostics.update {
                        it.copy(openAI = ApiTestResult(ApiTestStatus.SUCCESS, "OK — ${models.data.size} models available"))
                    }
                } else {
                    _diagnostics.update {
                        it.copy(openAI = ApiTestResult(ApiTestStatus.SUCCESS, "OK — Connected"))
                    }
                }
            } catch (e: Exception) {
                _diagnostics.update {
                    it.copy(openAI = ApiTestResult(ApiTestStatus.ERROR, e.message?.take(80) ?: "Unknown error"))
                }
            }
        }
    }
}

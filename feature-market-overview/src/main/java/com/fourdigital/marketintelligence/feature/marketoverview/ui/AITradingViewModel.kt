package com.fourdigital.marketintelligence.feature.marketoverview.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdigital.marketintelligence.analytics.ai.*
import com.fourdigital.marketintelligence.analytics.prediction.PredictionEngine
import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.core.network.api.FinnhubApi
import com.fourdigital.marketintelligence.core.network.api.FinnhubNews
import com.fourdigital.marketintelligence.data.mock.DemoAssets
import com.fourdigital.marketintelligence.data.provider.ApiKeyManager
import com.fourdigital.marketintelligence.data.provider.GitHubAIAnalyst
import com.fourdigital.marketintelligence.domain.model.*
import com.fourdigital.marketintelligence.domain.repository.PreferencesRepository
import com.fourdigital.marketintelligence.domain.repository.QuoteRepository
import com.fourdigital.marketintelligence.domain.repository.WatchlistRepository
import com.fourdigital.marketintelligence.domain.repository.HistoricalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import timber.log.Timber
import javax.inject.Inject

data class AITradingState(
    val watchlists: List<Watchlist> = emptyList(),
    val selectedWatchlistId: String = "",
    val analyses: List<AIAnalysisResult> = emptyList(),
    val newsItems: List<FinnhubNews> = emptyList(),
    val selectedAnalysis: AIAnalysisResult? = null,
    val isLoading: Boolean = false,
    val isLoadingNews: Boolean = false,
    val error: String? = null,
    val selectedTab: Int = 0, // 0=AI Overview, 1=News, 2=Predictions, 3=AI Agent
    val lastRefresh: String = "",
    // AI Agent state
    val aiAgentAvailable: Boolean = false,
    val aiProviderMode: AIProviderMode = AIProviderMode.BOTH,
    val selectedAIModel: String = "gpt-4o-mini",
    val availableModels: List<GitHubAIAnalyst.AIModelOption> = GitHubAIAnalyst.AVAILABLE_MODELS,
    val aiMarketOutlook: String = "",
    val isLoadingAIOutlook: Boolean = false,
    val aiAssetAnalysis: String = "",
    val isLoadingAIAsset: Boolean = false,
    val aiPredictions: String = "",
    val isLoadingPredictions: Boolean = false,
    val targetReturnPercent: Double = 10.0,
    val aiChatMessages: List<AIChatMessage> = emptyList(),
    val aiChatInput: String = "",
    val isLoadingChat: Boolean = false,
    val autoRefreshEnabled: Boolean = true,
    val aiAutoRefreshIntervalSeconds: Int = 30,
    val analysisCount: Int = 0
)

data class AIChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class AITradingViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val quoteRepository: QuoteRepository,
    private val preferencesRepository: PreferencesRepository,
    private val apiKeyManager: ApiKeyManager,
    private val finnhubApi: FinnhubApi,
    private val aiEngine: AIMarketIntelligence,
    private val historicalRepository: HistoricalRepository,
    private val gitHubAI: GitHubAIAnalyst
) : ViewModel() {

    private val _state = MutableStateFlow(AITradingState())
    val state: StateFlow<AITradingState> = _state.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var lastAutomatedAiRunMs: Long = 0L

    init {
        syncAISettings()
        checkAIAvailability()
        runFullAnalysis()
        startAutoRefresh()
    }

    private fun syncAISettings() {
        viewModelScope.launch {
            preferencesRepository.observePreferences().collect { prefs ->
                gitHubAI.setProviderMode(prefs.aiProviderMode)
                gitHubAI.setSelectedModel(prefs.selectedAIModel)
                _state.update {
                    it.copy(
                        aiProviderMode = prefs.aiProviderMode,
                        selectedAIModel = prefs.selectedAIModel,
                        autoRefreshEnabled = prefs.aiAutoRefreshEnabled,
                        aiAutoRefreshIntervalSeconds = prefs.aiAutoRefreshIntervalSeconds,
                        selectedWatchlistId = if (it.selectedWatchlistId.isBlank()) prefs.defaultWatchlistId else it.selectedWatchlistId,
                        targetReturnPercent = prefs.targetReturnPercent
                    )
                }
                checkAIAvailability()
                // Restart auto-refresh with updated interval
                if (_state.value.autoRefreshEnabled) startAutoRefresh()
            }
        }

        viewModelScope.launch {
            watchlistRepository.observeWatchlists().collect { watchlists ->
                _state.update { state ->
                    val selected = when {
                        state.selectedWatchlistId.isBlank() -> watchlists.firstOrNull()?.id.orEmpty()
                        watchlists.any { it.id == state.selectedWatchlistId } -> state.selectedWatchlistId
                        else -> watchlists.firstOrNull()?.id.orEmpty()
                    }
                    state.copy(watchlists = watchlists, selectedWatchlistId = selected)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                val intervalMs = _state.value.aiAutoRefreshIntervalSeconds * 1000L
                delay(intervalMs.coerceAtLeast(15_000L))
                if (_state.value.autoRefreshEnabled && !_state.value.isLoading) {
                    Timber.d("AI Trading auto-refresh triggered (interval=${intervalMs/1000}s)")
                    runFullAnalysis()
                }
            }
        }
    }

    fun toggleAutoRefresh() {
        _state.update { it.copy(autoRefreshEnabled = !it.autoRefreshEnabled) }
    }

    private fun checkAIAvailability() {
        viewModelScope.launch {
            try {
                val available = gitHubAI.isAvailable()
                _state.update { it.copy(
                    aiAgentAvailable = available,
                    aiProviderMode = gitHubAI.getProviderMode(),
                    selectedAIModel = gitHubAI.getSelectedModel()
                )}
            } catch (e: Exception) {
                _state.update { it.copy(aiAgentAvailable = false) }
            }
        }
    }

    fun selectAIModel(modelId: String) {
        gitHubAI.setSelectedModel(modelId)
        _state.update { it.copy(selectedAIModel = modelId) }
    }

    fun setTargetReturn(percent: Double) {
        _state.update { it.copy(targetReturnPercent = percent.coerceIn(1.0, 100.0)) }
    }

    fun setSelectedWatchlist(id: String) {
        _state.update { it.copy(selectedWatchlistId = id) }
        runFullAnalysis()
    }

    fun runFullAnalysis() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Watchlists are the MASTER source for all AI analysis
                val watchlists = watchlistRepository.observeWatchlists().first()
                val selected = _state.value.selectedWatchlistId
                val scopedSymbols = if (selected.isNotBlank()) {
                    watchlists.firstOrNull { it.id == selected }?.items?.map { it.symbol }.orEmpty()
                } else emptyList()

                val allSymbols = (if (scopedSymbols.isNotEmpty()) scopedSymbols else watchlists
                    .flatMap { it.items.map { item -> item.symbol } })
                    .distinct()
                    .take(50) // Analyze top 50 assets from selected watchlist (or all if none selected)

                if (allSymbols.isEmpty()) {
                    _state.update { it.copy(isLoading = false, error = "Add symbols to a watchlist first") }
                    return@launch
                }

                val quotesResult = quoteRepository.refreshQuotes(allSymbols)
                val quotes = when (quotesResult) {
                    is DataResult.Success -> quotesResult.data.associateBy { it.symbol }
                    else -> emptyMap()
                }

                loadNews()

                // Chunked analysis: process 5 symbols at a time to respect Finnhub rate limits (60 req/min)
                val analyses = mutableListOf<AIAnalysisResult>()
                val chunks = allSymbols.chunked(5)
                for (chunk in chunks) {
                    val chunkResults = supervisorScope {
                        chunk.map { symbol ->
                            async {
                                try {
                                    val barsResult = historicalRepository.getDailyHistory(symbol, 200)
                                    val bars = when (barsResult) {
                                        is DataResult.Success -> barsResult.data
                                        else -> emptyList()
                                    }
                                    if (bars.size < 20) return@async null

                                    val quote = quotes[symbol]
                                    val knownAsset = DemoAssets.assetBySymbol(symbol)
                                    val asset = knownAsset ?: Asset(
                                        symbol = symbol,
                                        name = symbol,
                                        assetClass = inferAssetClass(symbol),
                                        exchange = Exchange.UNKNOWN,
                                        currency = "USD"
                                    )

                                    val symbolNews = _state.value.newsItems
                                        .filter { it.related.contains(symbol, ignoreCase = true) ||
                                            it.headline.contains(symbol, ignoreCase = true) }
                                        .map { NewsItem(it.headline, it.summary, it.source, it.datetime, it.url, it.related) }

                                    aiEngine.analyzeAsset(asset, bars, quote, symbolNews)
                                } catch (e: Exception) {
                                    Timber.w(e, "Analysis failed for $symbol")
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()
                    }
                    analyses.addAll(chunkResults)
                    if (chunk != chunks.last()) delay(1200) // Rate-limit pause between chunks
                }

                val sorted = analyses.sortedByDescending { kotlin.math.abs(it.compositeScore) }

                val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())

                _state.update { it.copy(
                    watchlists = watchlists,
                    analyses = sorted,
                    isLoading = false,
                    lastRefresh = now,
                    analysisCount = sorted.size
                )}

                maybeRunAutomatedAI()

                checkAIAvailability()
            } catch (e: Exception) {
                Timber.e(e, "Full analysis failed")
                _state.update { it.copy(isLoading = false, error = e.message?.take(100)) }
            }
        }
    }

    private fun maybeRunAutomatedAI() {
        viewModelScope.launch {
            if (!_state.value.aiAgentAvailable) return@launch
            if (_state.value.analyses.isEmpty()) return@launch

            val now = System.currentTimeMillis()
            val minIntervalMs = _state.value.aiAutoRefreshIntervalSeconds * 1000L
            if (now - lastAutomatedAiRunMs < minIntervalMs) return@launch

            lastAutomatedAiRunMs = now
            requestAIMarketOutlook()
            requestAIPredictions()
        }
    }

    fun loadNews() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingNews = true) }
            try {
                val key = apiKeyManager.getKey(ApiKeyManager.FINNHUB)
                if (!key.isNullOrBlank()) {
                    val news = finnhubApi.getMarketNews(token = key)
                    _state.update { it.copy(newsItems = news.take(50), isLoadingNews = false) }
                } else {
                    _state.update { it.copy(isLoadingNews = false) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingNews = false) }
            }
        }
    }

    fun selectAnalysis(analysis: AIAnalysisResult?) {
        _state.update { it.copy(selectedAnalysis = analysis, aiAssetAnalysis = "") }
    }

    fun setTab(tab: Int) {
        _state.update { it.copy(selectedTab = tab) }
    }

    // --- AI Agent methods ---

    fun requestAIMarketOutlook() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingAIOutlook = true) }
            val newsItems = _state.value.newsItems.map {
                NewsItem(it.headline, it.summary, it.source, it.datetime, it.url, it.related)
            }
            val result = gitHubAI.generateMarketOutlook(_state.value.analyses, newsItems)
            _state.update { it.copy(aiMarketOutlook = result, isLoadingAIOutlook = false) }
        }
    }

    fun requestAIAssetAnalysis(analysis: AIAnalysisResult) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingAIAsset = true) }
            val symbolNews = _state.value.newsItems
                .filter { it.related.contains(analysis.symbol, ignoreCase = true) ||
                    it.headline.contains(analysis.symbol, ignoreCase = true) }
                .map { NewsItem(it.headline, it.summary, it.source, it.datetime, it.url, it.related) }
            val result = gitHubAI.analyzeAsset(analysis, symbolNews)
            _state.update { it.copy(aiAssetAnalysis = result, isLoadingAIAsset = false) }
        }
    }

    fun requestAIPredictions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPredictions = true) }
            val newsItems = _state.value.newsItems.map {
                NewsItem(it.headline, it.summary, it.source, it.datetime, it.url, it.related)
            }
            val result = gitHubAI.generatePredictions(
                _state.value.analyses,
                newsItems,
                _state.value.targetReturnPercent
            )
            _state.update { it.copy(aiPredictions = result, isLoadingPredictions = false) }
        }
    }

    fun updateChatInput(input: String) {
        _state.update { it.copy(aiChatInput = input) }
    }

    fun sendChatMessage() {
        val question = _state.value.aiChatInput.trim()
        if (question.isBlank()) return

        val userMsg = AIChatMessage(role = "user", content = question)
        _state.update { it.copy(
            aiChatMessages = it.aiChatMessages + userMsg,
            aiChatInput = "",
            isLoadingChat = true
        )}

        viewModelScope.launch {
            val newsItems = _state.value.newsItems.map {
                NewsItem(it.headline, it.summary, it.source, it.datetime, it.url, it.related)
            }
            val response = gitHubAI.askQuestion(question, _state.value.analyses, newsItems)
            val assistantMsg = AIChatMessage(role = "assistant", content = response)
            _state.update { it.copy(
                aiChatMessages = it.aiChatMessages + assistantMsg,
                isLoadingChat = false
            )}
        }
    }

    private fun inferAssetClass(symbol: String): AssetClass {
        return when {
            symbol.startsWith("^") || symbol == "000001.SS" -> AssetClass.INDEX
            symbol.contains("-USD") || symbol.startsWith("BTC") ||
                symbol.startsWith("ETH") -> AssetClass.CRYPTO
            symbol.endsWith("=F") || symbol.startsWith("GC") ||
                symbol.startsWith("CL") || symbol.startsWith("SI") -> AssetClass.COMMODITY
            symbol.contains("/") -> AssetClass.FOREX
            symbol.length <= 5 && symbol.endsWith("11") -> AssetClass.ETF
            symbol.endsWith(".SA") -> AssetClass.EQUITY
            else -> AssetClass.EQUITY
        }
    }
}

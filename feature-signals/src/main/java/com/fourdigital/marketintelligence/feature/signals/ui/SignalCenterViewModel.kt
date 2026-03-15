package com.fourdigital.marketintelligence.feature.signals.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdigital.marketintelligence.analytics.signal.SignalEngine
import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.*
import com.fourdigital.marketintelligence.domain.provider.HistoricalDataProvider
import com.fourdigital.marketintelligence.domain.repository.QuoteRepository
import com.fourdigital.marketintelligence.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import javax.inject.Inject

data class SignalCenterUiState(
    val signals: List<SignalAnalysis> = emptyList(),
    val selectedSignal: SignalAnalysis? = null,
    val isLoading: Boolean = true,
    val showAdvanced: Boolean = false,
    val lastRefresh: String = "",
    val autoRefreshEnabled: Boolean = true,
    val analysisCount: Int = 0,
    val error: String? = null
)

@HiltViewModel
class SignalCenterViewModel @Inject constructor(
    private val signalEngine: SignalEngine,
    private val historicalProvider: HistoricalDataProvider,
    private val quoteRepository: QuoteRepository,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignalCenterUiState())
    val uiState: StateFlow<SignalCenterUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        analyzeKeyAssets()
        startAutoRefresh()
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(45_000) // Refresh every 45s
                if (_uiState.value.autoRefreshEnabled && !_uiState.value.isLoading) {
                    Timber.d("Signal Center auto-refresh triggered")
                    analyzeKeyAssets()
                }
            }
        }
    }

    fun toggleAutoRefresh() {
        _uiState.update { it.copy(autoRefreshEnabled = !it.autoRefreshEnabled) }
    }

    fun selectSignal(signal: SignalAnalysis?) {
        _uiState.update { it.copy(selectedSignal = signal) }
    }

    fun toggleAdvanced() {
        _uiState.update { it.copy(showAdvanced = !it.showAdvanced) }
    }

    fun refresh() {
        analyzeKeyAssets()
    }

    private fun analyzeKeyAssets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Gather assets from ALL user watchlists — watchlist is master
                val watchlists = watchlistRepository.observeWatchlists().first()
                val allItems = watchlists.flatMap { it.items }.distinctBy { it.symbol }.take(50)

                if (allItems.isEmpty()) {
                    _uiState.update { it.copy(signals = emptyList(), isLoading = false, error = "Add assets to a watchlist first") }
                    return@launch
                }

                // Fetch real quotes for all symbols
                val symbols = allItems.map { it.symbol }
                val quotesResult = quoteRepository.refreshQuotes(symbols)
                val quotesMap = when (quotesResult) {
                    is DataResult.Success -> quotesResult.data.associateBy { it.symbol }
                    else -> emptyMap()
                }

                // Parallel signal computation
                val signals = supervisorScope {
                    allItems.map { item ->
                        async {
                            try {
                                val result = historicalProvider.getDailyBars(item.symbol, 200)
                                val bars = when (result) {
                                    is DataResult.Success -> result.data
                                    else -> emptyList()
                                }
                                if (bars.size < 50) return@async null

                                val quote = quotesMap[item.symbol] ?: return@async null
                                signalEngine.analyze(item.asset, bars, quote)
                            } catch (e: Exception) {
                                Timber.w(e, "Signal analysis failed for ${item.symbol}")
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()
                }

                val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())

                _uiState.update { it.copy(
                    signals = signals,
                    isLoading = false,
                    lastRefresh = now,
                    analysisCount = signals.size
                )}
            } catch (e: Exception) {
                Timber.e(e, "Signal center analysis failed")
                _uiState.update { it.copy(isLoading = false, error = e.message?.take(100)) }
            }
        }
    }
}

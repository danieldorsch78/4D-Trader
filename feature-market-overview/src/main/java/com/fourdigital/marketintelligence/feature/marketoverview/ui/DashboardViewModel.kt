package com.fourdigital.marketintelligence.feature.marketoverview.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.MarketRegime
import com.fourdigital.marketintelligence.domain.model.Quote
import com.fourdigital.marketintelligence.domain.model.Watchlist
import com.fourdigital.marketintelligence.domain.repository.QuoteRepository
import com.fourdigital.marketintelligence.domain.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val watchlists: List<Watchlist> = emptyList(),
    val quotes: Map<String, Quote> = emptyMap(),
    val marketRegime: MarketRegime = MarketRegime.UNKNOWN,
    val isLoading: Boolean = true,
    val error: String? = null,
    val lastRefresh: String = ""
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val quoteRepository: QuoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        startAutoRefresh()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            try {
                watchlistRepository.observeWatchlists().collect { watchlists ->
                    _uiState.update { it.copy(watchlists = watchlists) }
                    val allSymbols = watchlists.flatMap { wl -> wl.items.map { it.symbol } }.distinct()
                    refreshQuotes(allSymbols)
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to load watchlists") }
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                val symbols = _uiState.value.watchlists.flatMap { wl -> wl.items.map { it.symbol } }.distinct()
                if (symbols.isNotEmpty()) refreshQuotes(symbols)
            }
        }
    }

    private suspend fun refreshQuotes(symbols: List<String>) {
        _uiState.update { it.copy(isLoading = true) }
        when (val result = quoteRepository.refreshQuotes(symbols)) {
            is DataResult.Success -> {
                val quoteMap = result.data.associateBy { it.symbol }
                val regime = inferRegime(quoteMap)
                _uiState.update {
                    it.copy(
                        quotes = quoteMap,
                        marketRegime = regime,
                        isLoading = false,
                        error = null,
                        lastRefresh = kotlinx.datetime.Clock.System.now().toString().take(19)
                    )
                }
            }
            is DataResult.Error -> {
                _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
            is DataResult.Loading -> {}
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val symbols = _uiState.value.watchlists.flatMap { wl -> wl.items.map { it.symbol } }.distinct()
            refreshQuotes(symbols)
        }
    }

    private fun inferRegime(quotes: Map<String, Quote>): MarketRegime {
        val goldChange = quotes["GC=F"]?.changePercent ?: 0.0
        val btcChange = quotes["BTC-USD"]?.changePercent ?: 0.0
        val spxChange = quotes["^GSPC"]?.changePercent ?: 0.0
        val daxChange = quotes["^GDAXI"]?.changePercent ?: 0.0
        val ibovChange = quotes["^BVSP"]?.changePercent ?: 0.0

        val equityAvg = listOf(spxChange, daxChange, ibovChange).filter { it != 0.0 }
            .takeIf { it.isNotEmpty() }?.average() ?: 0.0
        return when {
            equityAvg > 0.5 && btcChange > 0 -> MarketRegime.RISK_ON
            equityAvg < -0.5 && goldChange > 0 -> MarketRegime.RISK_OFF
            (equityAvg > 0 && goldChange > 0) || (equityAvg < 0 && btcChange > 0) -> MarketRegime.MIXED
            else -> MarketRegime.TRANSITIONING
        }
    }
}

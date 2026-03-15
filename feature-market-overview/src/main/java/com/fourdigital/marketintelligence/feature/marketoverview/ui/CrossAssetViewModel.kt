package com.fourdigital.marketintelligence.feature.marketoverview.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.Quote
import com.fourdigital.marketintelligence.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CrossAssetState(
    val quotes: Map<String, Quote> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastRefresh: String = "",
    val riskRegime: String = "LOADING",
    val riskRegimeDescription: String = ""
)

data class AssetGroup(
    val name: String,
    val symbols: List<String>
)

@HiltViewModel
class CrossAssetViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CrossAssetState())
    val state: StateFlow<CrossAssetState> = _state.asStateFlow()

    val assetGroups = listOf(
        AssetGroup("Indices", listOf("^GDAXI", "^BVSP", "^GSPC", "^IXIC", "^FTSE", "^N225", "^HSI", "^FCHI")),
        AssetGroup("Crypto", listOf("BTC-USD", "ETH-USD", "SOL-USD", "ADA-USD", "XRP-USD", "BNB-USD")),
        AssetGroup("Commodities", listOf("GC=F", "SI=F", "CL=F", "NG=F", "PL=F", "HG=F")),
        AssetGroup("Forex", listOf("EUR/USD", "GBP/USD", "USD/JPY", "USD/CHF", "AUD/USD", "USD/BRL"))
    )

    private val allSymbols = assetGroups.flatMap { it.symbols }

    init {
        loadQuotes()
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(20_000) // Refresh every 20s for near real-time
                loadQuotes()
            }
        }
    }

    fun loadQuotes() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val result = quoteRepository.refreshQuotes(allSymbols)
                when (result) {
                    is DataResult.Success -> {
                        val now = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            .format(java.util.Date())
                        val quoteMap = result.data.associateBy { q -> q.symbol }
                        val regime = computeRiskRegime(quoteMap)
                        _state.update { it.copy(
                            quotes = quoteMap,
                            isLoading = false,
                            lastRefresh = now,
                            riskRegime = regime.first,
                            riskRegimeDescription = regime.second
                        )}
                    }
                    is DataResult.Error -> {
                        _state.update { it.copy(isLoading = false, error = result.message) }
                    }
                    is DataResult.Loading -> { /* already showing loading */ }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message?.take(100)) }
            }
        }
    }

    fun getRiskRegime(): String = _state.value.riskRegime

    private fun computeRiskRegime(quotes: Map<String, Quote>): Pair<String, String> {
        val sp500 = quotes["^GSPC"]
        val dax = quotes["^GDAXI"]
        val ibov = quotes["^BVSP"]
        val gold = quotes["GC=F"]
        val btc = quotes["BTC-USD"]
        val oil = quotes["CL=F"]

        val equityChanges = listOfNotNull(
            sp500?.changePercent,
            dax?.changePercent,
            ibov?.changePercent
        )
        val equityAvg = if (equityChanges.isNotEmpty()) equityChanges.average() else 0.0
        val goldChange = gold?.changePercent ?: 0.0
        val btcChange = btc?.changePercent ?: 0.0
        val oilChange = oil?.changePercent ?: 0.0

        val riskOnSignals = mutableListOf<String>()
        val riskOffSignals = mutableListOf<String>()

        if (equityAvg > 0.3) riskOnSignals.add("Equities rising +${"%+.1f".format(equityAvg)}%")
        if (equityAvg < -0.3) riskOffSignals.add("Equities falling ${"%+.1f".format(equityAvg)}%")
        if (btcChange > 1.0) riskOnSignals.add("Bitcoin strong +${"%+.1f".format(btcChange)}%")
        if (btcChange < -1.0) riskOffSignals.add("Bitcoin weak ${"%+.1f".format(btcChange)}%")
        if (goldChange > 0.5) riskOffSignals.add("Gold rising (defensive) +${"%+.1f".format(goldChange)}%")
        if (goldChange < -0.3) riskOnSignals.add("Gold falling (risk appetite)")
        if (oilChange > 1.0) riskOnSignals.add("Oil rising (growth signal)")
        if (oilChange < -1.0) riskOffSignals.add("Oil falling (demand concern)")

        return when {
            riskOnSignals.size >= 3 && riskOffSignals.isEmpty() -> {
                "RISK ON" to "Strong risk appetite \u2014 ${riskOnSignals.joinToString(", ")}"
            }
            riskOffSignals.size >= 3 && riskOnSignals.isEmpty() -> {
                "RISK OFF" to "Defensive positioning \u2014 ${riskOffSignals.joinToString(", ")}"
            }
            riskOnSignals.size > riskOffSignals.size -> {
                "RISK ON" to "Leaning bullish \u2014 ${riskOnSignals.joinToString(", ")}"
            }
            riskOffSignals.size > riskOnSignals.size -> {
                "RISK OFF" to "Leaning defensive \u2014 ${riskOffSignals.joinToString(", ")}"
            }
            riskOnSignals.isNotEmpty() && riskOffSignals.isNotEmpty() -> {
                "MIXED" to "Mixed signals across asset classes \u2014 no clear directional bias"
            }
            else -> {
                "MIXED" to "Insufficient data for regime classification"
            }
        }
    }
}

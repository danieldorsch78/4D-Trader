package com.fourdigital.marketintelligence.feature.marketoverview.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fourdigital.marketintelligence.analytics.ai.*
import com.fourdigital.marketintelligence.analytics.signal.SignalEngine
import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.data.mock.DemoAssets
import com.fourdigital.marketintelligence.domain.model.*
import com.fourdigital.marketintelligence.domain.repository.HistoricalRepository
import com.fourdigital.marketintelligence.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssetDetailState(
    val symbol: String = "",
    val quote: Quote? = null,
    val analysis: AIAnalysisResult? = null,
    val signals: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AssetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quoteRepository: QuoteRepository,
    private val historicalRepository: HistoricalRepository,
    private val aiEngine: AIMarketIntelligence,
    private val signalEngine: SignalEngine
) : ViewModel() {

    private val symbol: String = savedStateHandle["symbol"] ?: ""
    private val _state = MutableStateFlow(AssetDetailState(symbol = symbol))
    val state: StateFlow<AssetDetailState> = _state.asStateFlow()

    init {
        loadAssetData()
    }

    fun loadAssetData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch quote
                val quotesResult = quoteRepository.refreshQuotes(listOf(symbol))
                val quote = when (quotesResult) {
                    is DataResult.Success -> quotesResult.data.firstOrNull()
                    else -> null
                }
                _state.update { it.copy(quote = quote) }

                // Fetch historical data & run analysis
                val barsResult = historicalRepository.getDailyHistory(symbol, 200)
                val bars = when (barsResult) {
                    is DataResult.Success -> barsResult.data
                    else -> emptyList()
                }

                if (bars.size >= 50) {
                    val asset = DemoAssets.assetBySymbol(symbol) ?: Asset(
                        symbol = symbol,
                        name = symbol,
                        assetClass = inferAssetClass(symbol),
                        exchange = Exchange.UNKNOWN,
                        currency = "USD"
                    )
                    val analysis = aiEngine.analyzeAsset(asset, bars, quote, emptyList())
                    _state.update { it.copy(analysis = analysis) }

                    // Run signal engine
                    val signalResult = signalEngine.analyze(asset, bars, quote)
                    val signalTexts = mutableListOf<String>()
                    signalTexts.add("Regime: ${signalResult.regime.name}")
                    signalTexts.add("Bias: ${signalResult.directionalBias.name}")
                    signalTexts.add("Momentum: ${"%.0f".format(signalResult.momentumScore)}")
                    signalTexts.add("Risk: ${"%.0f".format(signalResult.riskScore)}")
                    signalTexts.add("Confidence: ${signalResult.confidenceScore}%")
                    signalResult.keyDrivers.take(3).forEach { driver ->
                        signalTexts.add(driver)
                    }
                    _state.update { it.copy(signals = signalTexts) }
                }

                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message?.take(100)) }
            }
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
            else -> AssetClass.EQUITY
        }
    }
}

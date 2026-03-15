package com.fourdigital.marketintelligence.data.provider

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.*
import com.fourdigital.marketintelligence.domain.provider.HistoricalDataProvider
import com.fourdigital.marketintelligence.domain.provider.MarketDataProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.datetime.Instant
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregator that routes quote requests to the correct real API provider:
 * - Crypto → CoinGecko (free, no key)
 * - Brazilian stocks → Brapi.dev (free key)
 * - Commodities → Finnhub ETF proxies
 * - Everything else (DAX, US stocks, forex) → Finnhub (free key)
 *
 * Uses parallel fetching across providers for maximum speed.
 */
@Singleton
class RealMarketDataProvider @Inject constructor(
    private val coinGeckoProvider: CoinGeckoProvider,
    private val finnhubProvider: FinnhubProvider,
    private val brapiProvider: BrapiProvider,
    private val apiKeyManager: ApiKeyManager
) : MarketDataProvider, HistoricalDataProvider {

    override val providerName: String = "4D Multi-Source"

    private fun routeProvider(symbol: String): MarketDataProvider {
        return when {
            coinGeckoProvider.canHandle(symbol) -> coinGeckoProvider
            brapiProvider.canHandle(symbol) -> brapiProvider
            else -> finnhubProvider
        }
    }

    private fun routeHistoricalProvider(symbol: String): HistoricalDataProvider {
        return when {
            coinGeckoProvider.canHandle(symbol) -> coinGeckoProvider
            brapiProvider.canHandle(symbol) -> brapiProvider
            else -> finnhubProvider
        }
    }

    override suspend fun getQuote(symbol: String): DataResult<Quote> {
        val primary = routeProvider(symbol)
        val result = primary.getQuote(symbol)
        if (result is DataResult.Success) return result
        // Fallback to Finnhub if primary fails (works for B3 ADRs, indices via ETFs, etc.)
        if (primary != finnhubProvider) {
            return finnhubProvider.getQuote(symbol)
        }
        return result
    }

    override suspend fun getQuotes(symbols: List<String>): DataResult<List<Quote>> {
        // Group symbols by provider for batch calls
        val grouped = mutableMapOf<String, MutableList<String>>()
        for (sym in symbols) {
            val pName = routeProvider(sym).providerName
            grouped.getOrPut(pName) { mutableListOf() }.add(sym)
        }

        // Fetch all provider groups in PARALLEL
        val allQuotes = mutableListOf<Quote>()
        supervisorScope {
            val deferredResults = grouped.map { (_, syms) ->
                async {
                    try {
                        val provider = routeProvider(syms.first())
                        val result = provider.getQuotes(syms)
                        if (result is DataResult.Success) result.data else emptyList()
                    } catch (e: Exception) {
                        Timber.w(e, "Provider fetch failed for ${syms.take(3)}")
                        emptyList()
                    }
                }
            }
            deferredResults.awaitAll().forEach { allQuotes.addAll(it) }
        }

        // Parallel fallback for missing symbols — try Finnhub (includes B3 ADR mappings)
        val gotSymbols = allQuotes.map { it.symbol }.toSet()
        val missing = symbols.filter { it !in gotSymbols }
        if (missing.isNotEmpty()) {
            Timber.d("Falling back to Finnhub for ${missing.size} missing symbols: ${missing.take(5)}")
            supervisorScope {
                val fallbackResults = missing.map { symbol ->
                    async {
                        try {
                            // Try Finnhub even for symbols it doesn't normally handle
                            // (it has ADR mappings for B3 stocks and ETF proxies for indices)
                            val result = finnhubProvider.getQuote(symbol)
                            if (result is DataResult.Success) result.data else null
                        } catch (_: Exception) { null }
                    }
                }
                fallbackResults.awaitAll().filterNotNull().let { allQuotes.addAll(it) }
            }
        }

        return if (allQuotes.isNotEmpty()) {
            DataResult.success(allQuotes)
        } else {
            // Return partial success instead of blanket error
            Timber.w("No quotes fetched for ${symbols.size} symbols")
            DataResult.error("No market data available — check API keys in Settings")
        }
    }

    override suspend fun isAvailable(): Boolean {
        return coinGeckoProvider.isAvailable()
    }

    override suspend fun getRateLimitRemaining(): Int = 50

    override suspend fun getHistoricalBars(
        symbol: String,
        timeFrame: TimeFrame,
        from: Instant,
        to: Instant
    ): DataResult<List<HistoricalBar>> {
        val provider = routeHistoricalProvider(symbol)
        val result = provider.getHistoricalBars(symbol, timeFrame, from, to)
        if (result is DataResult.Success) return result
        // Fallback to Finnhub
        if (provider != finnhubProvider) {
            return finnhubProvider.getHistoricalBars(symbol, timeFrame, from, to)
        }
        return result
    }

    override suspend fun getDailyBars(symbol: String, days: Int): DataResult<List<HistoricalBar>> {
        val provider = routeHistoricalProvider(symbol)
        val result = provider.getDailyBars(symbol, days)
        if (result is DataResult.Success) return result
        if (provider != finnhubProvider) {
            return finnhubProvider.getDailyBars(symbol, days)
        }
        return result
    }
}

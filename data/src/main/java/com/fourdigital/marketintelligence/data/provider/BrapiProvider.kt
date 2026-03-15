package com.fourdigital.marketintelligence.data.provider

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.core.network.api.BrapiApi
import com.fourdigital.marketintelligence.domain.model.*
import com.fourdigital.marketintelligence.domain.provider.MarketDataProvider
import com.fourdigital.marketintelligence.domain.provider.HistoricalDataProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real data provider using Brapi.dev free tier.
 * Handles: Brazilian B3 stocks (.SA suffix) and Ibovespa index.
 */
@Singleton
class BrapiProvider @Inject constructor(
    private val api: BrapiApi,
    private val keyManager: ApiKeyManager
) : MarketDataProvider, HistoricalDataProvider {

    override val providerName: String = "Brapi"

    fun canHandle(symbol: String): Boolean =
        symbol.endsWith(".SA") || symbol == "^BVSP"

    private fun mapSymbol(symbol: String): String = when (symbol) {
        "^BVSP" -> "^BVSP"
        else -> symbol.removeSuffix(".SA")
    }

    private suspend fun getToken(): String? = keyManager.getKey(ApiKeyManager.BRAPI)

    override suspend fun getQuote(symbol: String): DataResult<Quote> {
        val token = getToken() ?: return DataResult.error("Brapi API key not configured")
        val brapiSymbol = mapSymbol(symbol)
        return try {
            val response = api.getQuotes(brapiSymbol, token)
            val q = response.results.firstOrNull()
                ?: return DataResult.error("No data for $symbol")

            DataResult.success(
                Quote(
                    symbol = symbol,
                    price = q.regularMarketPrice,
                    previousClose = q.regularMarketPreviousClose ?: q.regularMarketPrice,
                    open = q.regularMarketOpen ?: q.regularMarketPrice,
                    dayHigh = q.regularMarketDayHigh ?: q.regularMarketPrice,
                    dayLow = q.regularMarketDayLow ?: q.regularMarketPrice,
                    volume = q.regularMarketVolume ?: 0L,
                    timestamp = Clock.System.now(),
                    dataQuality = QuoteDataQuality.DELAYED_15MIN,
                    providerName = providerName,
                    weekHigh52 = q.weekHigh52,
                    weekLow52 = q.weekLow52,
                    marketCap = q.marketCap?.toDouble()
                )
            )
        } catch (e: Exception) {
            DataResult.error("Brapi error for $symbol: ${e.message}")
        }
    }

    override suspend fun getQuotes(symbols: List<String>): DataResult<List<Quote>> {
        val b3Symbols = symbols.filter { canHandle(it) }
        if (b3Symbols.isEmpty()) return DataResult.success(emptyList())

        val token = getToken() ?: return DataResult.error("Brapi API key not configured")
        val brapiSymbols = b3Symbols.map { mapSymbol(it) }.joinToString(",")
        return try {
            val response = api.getQuotes(brapiSymbols, token)
            val quotes = response.results.map { q ->
                val originalSymbol = b3Symbols.find {
                    mapSymbol(it).equals(q.symbol, ignoreCase = true)
                } ?: "${q.symbol}.SA"

                Quote(
                    symbol = originalSymbol,
                    price = q.regularMarketPrice,
                    previousClose = q.regularMarketPreviousClose ?: q.regularMarketPrice,
                    open = q.regularMarketOpen ?: q.regularMarketPrice,
                    dayHigh = q.regularMarketDayHigh ?: q.regularMarketPrice,
                    dayLow = q.regularMarketDayLow ?: q.regularMarketPrice,
                    volume = q.regularMarketVolume ?: 0L,
                    timestamp = Clock.System.now(),
                    dataQuality = QuoteDataQuality.DELAYED_15MIN,
                    providerName = providerName,
                    weekHigh52 = q.weekHigh52,
                    weekLow52 = q.weekLow52,
                    marketCap = q.marketCap?.toDouble()
                )
            }
            DataResult.success(quotes)
        } catch (e: Exception) {
            DataResult.error("Brapi batch error: ${e.message}")
        }
    }

    override suspend fun isAvailable(): Boolean {
        val token = getToken() ?: return false
        return try {
            api.getAvailableStocks(token = token, search = "PETR4")
            true
        } catch (_: Exception) { false }
    }

    override suspend fun getRateLimitRemaining(): Int = 100

    override suspend fun getHistoricalBars(
        symbol: String,
        timeFrame: TimeFrame,
        from: Instant,
        to: Instant
    ): DataResult<List<HistoricalBar>> {
        val token = getToken() ?: return DataResult.error("Brapi API key not configured")
        val brapiSymbol = mapSymbol(symbol)
        val days = ((to.toEpochMilliseconds() - from.toEpochMilliseconds()) / 86_400_000).toInt().coerceAtLeast(1)
        val range = when {
            days <= 5 -> "5d"
            days <= 30 -> "1mo"
            days <= 90 -> "3mo"
            days <= 180 -> "6mo"
            days <= 365 -> "1y"
            days <= 730 -> "2y"
            else -> "5y"
        }
        return try {
            val response = api.getQuotes(
                symbols = brapiSymbol,
                token = token,
                range = range,
                interval = "1d"
            )
            val q = response.results.firstOrNull()
            val history = q?.historicalDataPrice ?: return DataResult.success(emptyList())
            val bars = history.map { h ->
                HistoricalBar(
                    symbol = symbol,
                    open = h.open,
                    high = h.high,
                    low = h.low,
                    close = h.close,
                    volume = h.volume,
                    timestamp = Instant.fromEpochSeconds(h.date),
                    adjustedClose = h.adjustedClose
                )
            }
            DataResult.success(bars)
        } catch (e: Exception) {
            DataResult.error("Brapi history error: ${e.message}")
        }
    }

    override suspend fun getDailyBars(symbol: String, days: Int): DataResult<List<HistoricalBar>> {
        val now = Clock.System.now()
        val from = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() - days.toLong() * 86_400_000L)
        return getHistoricalBars(symbol, TimeFrame.DAILY, from, now)
    }
}

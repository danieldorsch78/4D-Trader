package com.fourdigital.marketintelligence.data.provider

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.core.network.api.CoinGeckoApi
import com.fourdigital.marketintelligence.domain.model.HistoricalBar
import com.fourdigital.marketintelligence.domain.model.Quote
import com.fourdigital.marketintelligence.domain.model.QuoteDataQuality
import com.fourdigital.marketintelligence.domain.model.TimeFrame
import com.fourdigital.marketintelligence.domain.model.Asset
import com.fourdigital.marketintelligence.domain.model.AssetClass
import com.fourdigital.marketintelligence.domain.model.Exchange
import com.fourdigital.marketintelligence.domain.provider.HistoricalDataProvider
import com.fourdigital.marketintelligence.domain.provider.MarketDataProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real data provider using CoinGecko free API.
 * Handles: BTC-USD, ETH-USD and all crypto assets.
 * No API key required. Rate limit: 30 calls/min.
 */
@Singleton
class CoinGeckoProvider @Inject constructor(
    private val api: CoinGeckoApi
) : MarketDataProvider, HistoricalDataProvider {

    override val providerName: String = "CoinGecko"

    // Map our symbols to CoinGecko IDs — all 50 cryptos from DemoAssets
    private val symbolToId = mapOf(
        "BTC-USD" to "bitcoin",
        "ETH-USD" to "ethereum",
        "BNB-USD" to "binancecoin",
        "SOL-USD" to "solana",
        "ADA-USD" to "cardano",
        "XRP-USD" to "ripple",
        "DOGE-USD" to "dogecoin",
        "DOT-USD" to "polkadot",
        "AVAX-USD" to "avalanche-2",
        "MATIC-USD" to "matic-network",
        "LINK-USD" to "chainlink",
        "UNI-USD" to "uniswap",
        "ATOM-USD" to "cosmos",
        "LTC-USD" to "litecoin",
        "FIL-USD" to "filecoin",
        "NEAR-USD" to "near",
        "APT-USD" to "aptos",
        "ARB-USD" to "arbitrum",
        "OP-USD" to "optimism",
        "AAVE-USD" to "aave",
        "SHIB-USD" to "shiba-inu",
        "TRX-USD" to "tron",
        "TON-USD" to "the-open-network",
        "XLM-USD" to "stellar",
        "HBAR-USD" to "hedera-hashgraph",
        "ICP-USD" to "internet-computer",
        "IMX-USD" to "immutable-x",
        "INJ-USD" to "injective-protocol",
        "RENDER-USD" to "render-token",
        "FTM-USD" to "fantom",
        "MKR-USD" to "maker",
        "GRT-USD" to "the-graph",
        "ALGO-USD" to "algorand",
        "VET-USD" to "vechain",
        "SAND-USD" to "the-sandbox",
        "MANA-USD" to "decentraland",
        "AXS-USD" to "axie-infinity",
        "THETA-USD" to "theta-token",
        "EOS-USD" to "eos",
        "FLOW-USD" to "flow",
        "CRV-USD" to "curve-dao-token",
        "PEPE-USD" to "pepe",
        "SUI-USD" to "sui",
        "SEI-USD" to "sei-network",
        "RUNE-USD" to "thorchain",
        "EGLD-USD" to "elrond-erd-2",
        "GALA-USD" to "gala",
        "STX-USD" to "blockstack",
        "WLD-USD" to "worldcoin-wld",
        "FET-USD" to "fetch-ai"
    )

    fun canHandle(symbol: String): Boolean = symbolToId.containsKey(symbol)

    suspend fun searchCoins(query: String): DataResult<List<Asset>> {
        return try {
            val result = api.search(query)
            val assets = result.coins.take(15).map { coin ->
                val symbol = "${coin.symbol.uppercase()}-USD"
                Asset(
                    symbol = symbol,
                    name = coin.name,
                    assetClass = AssetClass.CRYPTO,
                    exchange = Exchange.CRYPTO_GLOBAL,
                    currency = "USD"
                )
            }
            DataResult.success(assets)
        } catch (e: Exception) {
            DataResult.error("CoinGecko search error: ${e.message}")
        }
    }

    override suspend fun getQuote(symbol: String): DataResult<Quote> {
        val id = symbolToId[symbol] ?: return DataResult.error("Unknown crypto symbol: $symbol")
        return try {
            val prices = api.getSimplePrice(ids = id)
            val entry = prices[id] ?: return DataResult.error("No data for $symbol")
            val market = try {
                api.getCoinMarkets(ids = id).firstOrNull()
            } catch (_: Exception) { null }

            val price = entry.usd
                ?: return DataResult.error("No price data for $symbol")
            val change24h = entry.usd24hChange ?: 0.0
            val previousClose = if (change24h != 0.0 && price != 0.0) {
                price / (1.0 + change24h / 100.0)
            } else price

            DataResult.success(
                Quote(
                    symbol = symbol,
                    price = price,
                    previousClose = previousClose,
                    open = market?.currentPrice ?: price,
                    dayHigh = market?.high24h ?: price,
                    dayLow = market?.low24h ?: price,
                    volume = market?.totalVolume?.toLong() ?: entry.usd24hVol?.toLong() ?: 0L,
                    timestamp = Clock.System.now(),
                    dataQuality = QuoteDataQuality.REALTIME_SNAPSHOT,
                    providerName = providerName,
                    marketCap = entry.usdMarketCap,
                    weekHigh52 = market?.allTimeHigh,
                    weekLow52 = market?.allTimeLow
                )
            )
        } catch (e: Exception) {
            DataResult.error("CoinGecko error for $symbol: ${e.message}")
        }
    }

    override suspend fun getQuotes(symbols: List<String>): DataResult<List<Quote>> {
        val cryptoSymbols = symbols.filter { canHandle(it) }
        if (cryptoSymbols.isEmpty()) return DataResult.success(emptyList())

        val ids = cryptoSymbols.mapNotNull { symbolToId[it] }.joinToString(",")
        return try {
            val prices = api.getSimplePrice(ids = ids)
            val quotes = cryptoSymbols.mapNotNull { symbol ->
                val id = symbolToId[symbol] ?: return@mapNotNull null
                val entry = prices[id] ?: return@mapNotNull null
                val price = entry.usd ?: return@mapNotNull null
                val change24h = entry.usd24hChange ?: 0.0
                val previousClose = if (change24h != 0.0) price / (1.0 + change24h / 100.0) else price

                Quote(
                    symbol = symbol,
                    price = price,
                    previousClose = previousClose,
                    open = price,
                    dayHigh = price,
                    dayLow = price,
                    volume = entry.usd24hVol?.toLong() ?: 0L,
                    timestamp = Clock.System.now(),
                    dataQuality = QuoteDataQuality.REALTIME_SNAPSHOT,
                    providerName = providerName,
                    marketCap = entry.usdMarketCap
                )
            }
            DataResult.success(quotes)
        } catch (e: Exception) {
            DataResult.error("CoinGecko batch error: ${e.message}")
        }
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            api.getSimplePrice(ids = "bitcoin")
            true
        } catch (_: Exception) { false }
    }

    override suspend fun getRateLimitRemaining(): Int = 25 // estimated

    override suspend fun getHistoricalBars(
        symbol: String,
        timeFrame: TimeFrame,
        from: Instant,
        to: Instant
    ): DataResult<List<HistoricalBar>> {
        val id = symbolToId[symbol] ?: return DataResult.error("Unknown crypto: $symbol")
        val days = ((to.toEpochMilliseconds() - from.toEpochMilliseconds()) / 86_400_000).toInt().coerceAtLeast(1)
        return try {
            val chart = api.getMarketChart(id = id, days = days)
            val bars = chart.prices.map { point ->
                val ts = point[0].toLong()
                val price = point[1]
                HistoricalBar(
                    symbol = symbol,
                    open = price,
                    high = price * 1.005,
                    low = price * 0.995,
                    close = price,
                    volume = 0L,
                    timestamp = Instant.fromEpochMilliseconds(ts)
                )
            }
            DataResult.success(bars)
        } catch (e: Exception) {
            DataResult.error("CoinGecko chart error: ${e.message}")
        }
    }

    override suspend fun getDailyBars(symbol: String, days: Int): DataResult<List<HistoricalBar>> {
        val now = Clock.System.now()
        val from = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() - days.toLong() * 86_400_000L)
        return getHistoricalBars(symbol, TimeFrame.DAILY, from, now)
    }
}

package com.fourdigital.marketintelligence.data.provider

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.core.network.api.FinnhubApi
import com.fourdigital.marketintelligence.domain.model.*
import com.fourdigital.marketintelligence.domain.provider.HistoricalDataProvider
import com.fourdigital.marketintelligence.domain.provider.MarketDataProvider
import com.fourdigital.marketintelligence.domain.provider.SymbolSearchProvider
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real data provider using Finnhub free tier.
 * Handles: DAX stocks (.DE), US stocks, ETFs, indices, forex, crypto.
 * API key required (free tier: 60 calls/min).
 */
@Singleton
class FinnhubProvider @Inject constructor(
    private val api: FinnhubApi,
    private val keyManager: ApiKeyManager
) : MarketDataProvider, HistoricalDataProvider, SymbolSearchProvider {

    override val providerName: String = "Finnhub"

    // Map our symbols to Finnhub-compatible symbols
    private val symbolMap = mapOf(
        // DAX equities (Finnhub uses US ADR tickers for German stocks)
        "SAP.DE" to "SAP",
        "SIE.DE" to "SIEGY",
        "ALV.DE" to "ALIZY",
        "DTE.DE" to "DTEGY",
        "MBG.DE" to "MBGYY",
        "BAS.DE" to "BASFY",
        "BMW.DE" to "BMWYY",
        "IFX.DE" to "IFNNY",
        "MUV2.DE" to "MURGY",
        "ADS.DE" to "ADDYY",
        "AIR.DE" to "EADSY",
        "DB1.DE" to "DBOEY",
        "DBK.DE" to "DB",
        "RWE.DE" to "RWEOY",
        "VOW3.DE" to "VWAGY",
        "HEN3.DE" to "HENKY",
        "EOAN.DE" to "EONGY",
        "FRE.DE" to "FSNUY",
        "HEI.DE" to "HDELY",
        "MTX.DE" to "MTUAY",
        "SHL.DE" to "SMMNY",
        "SY1.DE" to "SYIEY",
        "VNA.DE" to "VONOY",
        "BEI.DE" to "BDRFY",
        "CBK.DE" to "CRZBY",
        "CON.DE" to "CTTAY",
        "1COV.DE" to "COVTY",
        "DHL.DE" to "DPSGY",
        "ENR.DE" to "SMNEY",
        "FME.DE" to "FMS",
        "HNR1.DE" to "HVRRY",
        "MRK.DE" to "MKKGY",
        "P911.DE" to "DRPRY",
        "PAH3.DE" to "POAHY",
        "QIA.DE" to "QGEN",
        "RHM.DE" to "RNMBY",
        "SAR.DE" to "SARTF",
        "ZAL.DE" to "ZLNDY",
        "BNR.DE" to "BNTGY",
        "BAY.DE" to "BAYRY",
        // B3 stocks via US ADR equivalents (fallback when Brapi key not configured)
        "PETR4.SA" to "PBR",
        "PETR3.SA" to "PBR.A",
        "VALE3.SA" to "VALE",
        "ITUB4.SA" to "ITUB",
        "BBDC4.SA" to "BBD",
        "ABEV3.SA" to "ABEV",
        "BBAS3.SA" to "BDORY",
        "WEGE3.SA" to "WEGZY",
        "SUZB3.SA" to "SUZ",
        "B3SA3.SA" to "B3SAY",
        "ELET3.SA" to "EBR",
        "LREN3.SA" to "LRENY",
        "JBSS3.SA" to "JBSAY",
        "EMBR3.SA" to "ERJ",
        "VIVT3.SA" to "VIV",
        "GGBR4.SA" to "GGB",
        "CSNA3.SA" to "SID",
        "USIM5.SA" to "USNZY",
        "NTCO3.SA" to "NTCO",
        "AZUL4.SA" to "AZUL",
        "BRFS3.SA" to "BRFS",
        "^BVSP" to "EWZ",  // Ibovespa index via Brazil ETF
        // US stocks use same symbols
        // Commodities - proxy via ETFs
        "GC=F" to "GLD",   // Gold via SPDR Gold Trust
        "SI=F" to "SLV",   // Silver via iShares Silver Trust
        "CL=F" to "USO",   // Oil via United States Oil Fund
        "BZ=F" to "BNO",   // Brent via United States Brent Oil Fund
        "NG=F" to "UNG",   // Natural Gas via United States Natural Gas Fund
        "PL=F" to "PPLT",  // Platinum via abrdn Platinum ETF
        "HG=F" to "CPER",  // Copper via United States Copper Index Fund
        // Crypto on Finnhub
        "BTC-USD" to "BINANCE:BTCUSDT",
        "ETH-USD" to "BINANCE:ETHUSDT",
        // Indices - not directly available, use ETF proxies
        "^GDAXI" to "EWG",  // DAX via iShares Germany ETF
        "^BVSP" to "EWZ",   // Ibovespa via iShares Brazil ETF
        "^GSPC" to "SPY",   // S&P 500 via SPDR S&P 500 ETF
        "^IXIC" to "QQQ",   // NASDAQ via Invesco QQQ ETF
        "^DJI" to "DIA",    // Dow Jones via SPDR Dow Jones ETF
        "^FTSE" to "EWU",   // FTSE 100 via iShares UK ETF
        // Proxies
        "DX-Y.NYB" to "UUP",
        "EWG" to "EWG",
        "EWZ" to "EWZ",
        // New global indices via ETF proxies
        "^N225" to "EWJ",   // Nikkei 225 via iShares Japan ETF
        "^HSI" to "EWH",    // Hang Seng via iShares Hong Kong ETF
        "^FCHI" to "EWQ",   // CAC 40 via iShares France ETF
        "^STOXX50E" to "FEZ", // Euro Stoxx 50 via SPDR Euro Stoxx 50 ETF
        "^IBEX" to "EWP",   // IBEX 35 via iShares Spain ETF
        "^SSMI" to "EWL",   // Swiss Market Index via iShares Switzerland ETF
        "^AXJO" to "EWA",   // ASX 200 via iShares Australia ETF
        "^KS11" to "EWY",   // KOSPI via iShares South Korea ETF
        "^TWII" to "EWT",   // Taiwan Weighted via iShares Taiwan ETF
        "^BSESN" to "INDA",  // BSE Sensex via iShares MSCI India ETF
        "^NSEI" to "INDA",   // Nifty 50 via iShares MSCI India ETF
        "000001.SS" to "MCHI", // SSE Composite via iShares MSCI China ETF
        "^RUT" to "IWM",    // Russell 2000 via iShares Russell 2000 ETF
        "^VIX" to "VXX",    // VIX via iPath Series B S&P 500 VIX Short-Term Futures ETN
        // Forex pairs via Finnhub forex endpoint
        "EUR/USD" to "OANDA:EUR_USD",
        "GBP/USD" to "OANDA:GBP_USD",
        "USD/JPY" to "OANDA:USD_JPY",
        "USD/CHF" to "OANDA:USD_CHF",
        "AUD/USD" to "OANDA:AUD_USD",
        "USD/CAD" to "OANDA:USD_CAD",
        "NZD/USD" to "OANDA:NZD_USD",
        "EUR/GBP" to "OANDA:EUR_GBP",
        "EUR/JPY" to "OANDA:EUR_JPY",
        "GBP/JPY" to "OANDA:GBP_JPY",
        "EUR/CHF" to "OANDA:EUR_CHF",
        "AUD/JPY" to "OANDA:AUD_JPY",
        "EUR/AUD" to "OANDA:EUR_AUD",
        "USD/BRL" to "OANDA:USD_BRL",
        "USD/MXN" to "OANDA:USD_MXN",
        "USD/CNY" to "OANDA:USD_CNH",
        "USD/INR" to "OANDA:USD_INR",
        "USD/TRY" to "OANDA:USD_TRY",
        "EUR/BRL" to "OANDA:EUR_BRL",
        "GBP/EUR" to "OANDA:GBP_EUR"
    )

    fun canHandle(symbol: String): Boolean =
        symbolMap.containsKey(symbol) || (!symbol.endsWith(".SA") && !symbol.contains("-USD"))

    private fun mapSymbol(symbol: String): String = symbolMap[symbol] ?: symbol

    private suspend fun getToken(): String? = keyManager.getKey(ApiKeyManager.FINNHUB)

    override suspend fun getQuote(symbol: String): DataResult<Quote> {
        val token = getToken() ?: return DataResult.error("Finnhub API key not configured")
        val finnhubSymbol = mapSymbol(symbol)
        return try {
            val q = api.getQuote(finnhubSymbol, token)
            if (q.current == 0.0 && q.previousClose == 0.0) {
                return DataResult.error("No data for $symbol (mapped: $finnhubSymbol)")
            }
            DataResult.success(
                Quote(
                    symbol = symbol,
                    price = q.current,
                    previousClose = q.previousClose,
                    open = q.open,
                    dayHigh = q.high,
                    dayLow = q.low,
                    volume = 0L,
                    timestamp = if (q.timestamp > 0) Instant.fromEpochSeconds(q.timestamp) else Clock.System.now(),
                    dataQuality = QuoteDataQuality.REALTIME_SNAPSHOT,
                    providerName = providerName
                )
            )
        } catch (e: Exception) {
            DataResult.error("Finnhub error for $symbol: ${e.message}")
        }
    }

    override suspend fun getQuotes(symbols: List<String>): DataResult<List<Quote>> {
        val token = getToken() ?: return DataResult.error("Finnhub API key not configured")
        val quotes = mutableListOf<Quote>()
        for ((index, symbol) in symbols.withIndex()) {
            if (!canHandle(symbol)) continue
            try {
                val result = getQuote(symbol)
                if (result is DataResult.Success) {
                    quotes.add(result.data)
                }
                // Rate limit: Finnhub free tier allows 60 req/min → ~1 req/sec
                if (index < symbols.lastIndex) delay(120)
            } catch (_: Exception) { }
        }
        return DataResult.success(quotes)
    }

    override suspend fun isAvailable(): Boolean {
        val token = getToken() ?: return false
        return try {
            api.getQuote("AAPL", token)
            true
        } catch (_: Exception) { false }
    }

    override suspend fun getRateLimitRemaining(): Int = 55

    override suspend fun getHistoricalBars(
        symbol: String,
        timeFrame: TimeFrame,
        from: Instant,
        to: Instant
    ): DataResult<List<HistoricalBar>> {
        val token = getToken() ?: return DataResult.error("Finnhub API key not configured")
        val finnhubSymbol = mapSymbol(symbol)
        val resolution = when (timeFrame) {
            TimeFrame.INTRADAY_1M -> "1"
            TimeFrame.INTRADAY_5M -> "5"
            TimeFrame.INTRADAY_15M -> "15"
            TimeFrame.INTRADAY_1H -> "60"
            TimeFrame.DAILY -> "D"
            TimeFrame.WEEKLY -> "W"
            TimeFrame.MONTHLY -> "M"
        }
        return try {
            val candles = api.getCandles(
                symbol = finnhubSymbol,
                resolution = resolution,
                from = from.epochSeconds,
                to = to.epochSeconds,
                token = token
            )
            if (candles.status != "ok" || candles.close.isNullOrEmpty()) {
                return DataResult.error("No candle data for $symbol")
            }
            val closeList = candles.close ?: return DataResult.error("No candle data for $symbol")
            val bars = closeList.indices.map { i ->
                HistoricalBar(
                    symbol = symbol,
                    open = candles.open?.getOrNull(i) ?: 0.0,
                    high = candles.high?.getOrNull(i) ?: 0.0,
                    low = candles.low?.getOrNull(i) ?: 0.0,
                    close = closeList[i],
                    volume = candles.volume?.getOrNull(i)?.toLong() ?: 0L,
                    timestamp = Instant.fromEpochSeconds(candles.timestamp?.getOrNull(i) ?: 0L)
                )
            }
            DataResult.success(bars)
        } catch (e: Exception) {
            DataResult.error("Finnhub candles error: ${e.message}")
        }
    }

    override suspend fun getDailyBars(symbol: String, days: Int): DataResult<List<HistoricalBar>> {
        val now = Clock.System.now()
        val from = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() - days.toLong() * 86_400_000L)
        return getHistoricalBars(symbol, TimeFrame.DAILY, from, now)
    }

    override suspend fun searchSymbols(query: String, assetClassFilter: String?): DataResult<List<Asset>> {
        val token = getToken() ?: return DataResult.error("Finnhub API key not configured")
        return try {
            val result = api.symbolSearch(query, token)
            val assets = result.result.map { item ->
                Asset(
                    symbol = item.symbol,
                    name = item.description,
                    assetClass = when (item.type) {
                        "Common Stock" -> AssetClass.EQUITY
                        "ETF" -> AssetClass.ETF
                        "Crypto" -> AssetClass.CRYPTO
                        else -> AssetClass.UNKNOWN
                    },
                    exchange = Exchange.UNKNOWN,
                    currency = "USD"
                )
            }
            DataResult.success(assets)
        } catch (e: Exception) {
            DataResult.error("Finnhub search error: ${e.message}")
        }
    }

    override suspend fun getAssetDetails(symbol: String): DataResult<Asset> {
        val token = getToken() ?: return DataResult.error("Finnhub API key not configured")
        val finnhubSymbol = mapSymbol(symbol)
        return try {
            val profile = api.companyProfile(finnhubSymbol, token)
            DataResult.success(
                Asset(
                    symbol = symbol,
                    name = profile.name ?: symbol,
                    assetClass = AssetClass.EQUITY,
                    exchange = Exchange.UNKNOWN,
                    currency = profile.currency ?: "USD"
                )
            )
        } catch (e: Exception) {
            DataResult.error("Finnhub profile error: ${e.message}")
        }
    }
}

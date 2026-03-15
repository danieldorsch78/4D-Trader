package com.fourdigital.marketintelligence.data.mock

import com.fourdigital.marketintelligence.domain.model.Quote
import com.fourdigital.marketintelligence.domain.model.QuoteDataQuality
import kotlinx.datetime.Clock
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Generates realistic mock quote data with simulated price movement.
 */
object MockQuoteGenerator {

    private val baseQuotes = mapOf(
        // DAX equities
        "SAP.DE" to MockBase(218.50, 3_200_000),
        "SIE.DE" to MockBase(188.40, 2_100_000),
        "ALV.DE" to MockBase(285.30, 1_500_000),
        "DTE.DE" to MockBase(27.83, 8_000_000),
        "MBG.DE" to MockBase(58.72, 3_400_000),
        "BAS.DE" to MockBase(46.15, 4_200_000),
        "BMW.DE" to MockBase(82.56, 1_900_000),
        "IFX.DE" to MockBase(33.87, 5_100_000),
        "MUV2.DE" to MockBase(482.60, 600_000),
        "ADS.DE" to MockBase(232.20, 1_200_000),
        "^GDAXI" to MockBase(20856.0, 0),
        // B3 equities
        "PETR4.SA" to MockBase(36.42, 45_000_000),
        "VALE3.SA" to MockBase(62.18, 30_000_000),
        "ITUB4.SA" to MockBase(33.95, 25_000_000),
        "BBDC4.SA" to MockBase(14.82, 28_000_000),
        "ABEV3.SA" to MockBase(12.67, 18_000_000),
        "WEGE3.SA" to MockBase(38.45, 8_000_000),
        "RENT3.SA" to MockBase(44.30, 6_500_000),
        "BBAS3.SA" to MockBase(28.91, 12_000_000),
        "SUZB3.SA" to MockBase(55.73, 7_200_000),
        "MGLU3.SA" to MockBase(2.18, 55_000_000),
        "^BVSP" to MockBase(128495.0, 0),
        // Commodities
        "GC=F" to MockBase(2648.30, 180_000),
        "SI=F" to MockBase(31.42, 95_000),
        "CL=F" to MockBase(72.85, 350_000),
        "BZ=F" to MockBase(77.23, 200_000),
        // Crypto
        "BTC-USD" to MockBase(98450.0, 25_000),
        "ETH-USD" to MockBase(3520.0, 180_000),
        // Proxies
        "DX-Y.NYB" to MockBase(104.25, 50_000),
        "EWG" to MockBase(31.82, 3_500_000),
        "EWZ" to MockBase(27.45, 12_000_000),
    )

    fun generateQuote(symbol: String): Quote {
        val base = baseQuotes[symbol] ?: MockBase(100.0, 1_000_000)
        val now = Clock.System.now()
        val jitter = base.price * Random.nextDouble(-0.025, 0.025)
        val price = (base.price + jitter).coerceAtLeast(0.01)
        val previousClose = base.price * (1.0 + Random.nextDouble(-0.01, 0.01))
        val dayHigh = price * (1.0 + Random.nextDouble(0.001, 0.02))
        val dayLow = price * (1.0 - Random.nextDouble(0.001, 0.02))
        val open = previousClose * (1.0 + Random.nextDouble(-0.005, 0.005))
        val volume = (base.volume * Random.nextDouble(0.7, 1.4)).roundToLong()

        return Quote(
            symbol = symbol,
            price = price.round(2),
            previousClose = previousClose.round(2),
            open = open.round(2),
            dayHigh = dayHigh.round(2),
            dayLow = dayLow.round(2),
            volume = volume,
            timestamp = now,
            dataQuality = QuoteDataQuality.REALTIME_SNAPSHOT,
            providerName = "MockProvider",
            bid = (price - Random.nextDouble(0.01, 0.1)).round(2),
            ask = (price + Random.nextDouble(0.01, 0.1)).round(2),
            latencyMs = Random.nextLong(5, 50)
        )
    }

    fun generateQuotes(symbols: List<String>): List<Quote> =
        symbols.map { generateQuote(it) }

    private fun Double.round(decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return (this * factor).toLong() / factor
    }

    private data class MockBase(val price: Double, val volume: Long)
}

package com.fourdigital.marketintelligence.data.mock

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.HistoricalBar
import com.fourdigital.marketintelligence.domain.model.TimeFrame
import com.fourdigital.marketintelligence.domain.provider.HistoricalDataProvider
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

@Singleton
class MockHistoricalProvider @Inject constructor() : HistoricalDataProvider {

    override val providerName: String = "MockHistorical"

    override suspend fun getHistoricalBars(
        symbol: String,
        timeFrame: TimeFrame,
        from: Instant,
        to: Instant
    ): DataResult<List<HistoricalBar>> {
        val bars = generateBars(symbol, timeFrame, from, to)
        return DataResult.success(bars)
    }

    override suspend fun getDailyBars(symbol: String, days: Int): DataResult<List<HistoricalBar>> {
        val now = Clock.System.now()
        val from = now.minus(days.days)
        return getHistoricalBars(symbol, TimeFrame.DAILY, from, now)
    }

    private fun generateBars(
        symbol: String,
        timeFrame: TimeFrame,
        from: Instant,
        to: Instant
    ): List<HistoricalBar> {
        val basePrice = MockQuoteGenerator.generateQuote(symbol).price
        val stepMs = when (timeFrame) {
            TimeFrame.INTRADAY_1M -> 60_000L
            TimeFrame.INTRADAY_5M -> 300_000L
            TimeFrame.INTRADAY_15M -> 900_000L
            TimeFrame.INTRADAY_1H -> 3_600_000L
            TimeFrame.DAILY -> 86_400_000L
            TimeFrame.WEEKLY -> 604_800_000L
            TimeFrame.MONTHLY -> 2_592_000_000L
        }

        val bars = mutableListOf<HistoricalBar>()
        var currentMs = from.toEpochMilliseconds()
        val toMs = to.toEpochMilliseconds()
        var price = basePrice * 0.95

        while (currentMs <= toMs) {
            val trend = sin(currentMs.toDouble() / 86_400_000.0 * 0.05) * basePrice * 0.03
            val noise = Random.nextDouble(-0.02, 0.02) * basePrice
            price = (price + trend + noise).coerceAtLeast(basePrice * 0.7)

            val open = price
            val high = price * (1.0 + Random.nextDouble(0.001, 0.025))
            val low = price * (1.0 - Random.nextDouble(0.001, 0.025))
            val close = price * (1.0 + Random.nextDouble(-0.015, 0.015))
            val volume = Random.nextLong(100_000, 5_000_000)

            bars.add(
                HistoricalBar(
                    symbol = symbol,
                    open = open.roundTo(2),
                    high = high.roundTo(2),
                    low = low.roundTo(2),
                    close = close.roundTo(2),
                    volume = volume,
                    timestamp = Instant.fromEpochMilliseconds(currentMs)
                )
            )
            price = close
            currentMs += stepMs
        }
        return bars
    }

    private fun Double.roundTo(decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return (this * factor).toLong() / factor
    }
}

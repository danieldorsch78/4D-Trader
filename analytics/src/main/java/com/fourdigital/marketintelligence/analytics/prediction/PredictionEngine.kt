package com.fourdigital.marketintelligence.analytics.prediction

import com.fourdigital.marketintelligence.domain.model.*
import kotlinx.datetime.Clock
import kotlin.math.*

/**
 * Prediction engine: multi-horizon trend forecasting with entry/exit points,
 * price targets, stop-loss levels, and probability estimates.
 * Supports: intraday, weekly, monthly, quarterly, and yearly horizons.
 * Uses multi-timeframe analysis + mean reversion + momentum + volatility scaling.
 */
class PredictionEngine {

    data class Prediction(
        val symbol: String,
        val timestamp: kotlinx.datetime.Instant,
        val action: TradeAction,
        val entryPrice: Double,
        val targetPrice: Double,
        val stopLoss: Double,
        val riskRewardRatio: Double,
        val confidence: Double,
        val timeHorizon: String,
        val reasoning: List<String>,
        val priceTargets: List<PriceTarget>,
        val trendForecast: TrendForecast,
        val horizonPredictions: List<HorizonPrediction> = emptyList()
    )

    data class HorizonPrediction(
        val horizon: PredictionHorizon,
        val direction: TrendDirection,
        val estimatedChangePercent: Double,
        val targetPrice: Double,
        val stopLoss: Double,
        val confidence: Double,
        val keyFactor: String
    )

    enum class PredictionHorizon(val label: String, val tradingDays: Int) {
        ONE_WEEK("1 Week", 5),
        TWO_WEEKS("2 Weeks", 10),
        ONE_MONTH("1 Month", 22),
        THREE_MONTHS("3 Months", 66),
        SIX_MONTHS("6 Months", 132),
        ONE_YEAR("1 Year", 252)
    }

    data class PriceTarget(
        val label: String,
        val price: Double,
        val probability: Double
    )

    data class TrendForecast(
        val shortTerm: TrendDirection,
        val mediumTerm: TrendDirection,
        val longTerm: TrendDirection,
        val strength: Double
    )

    enum class TradeAction { BUY, SELL, HOLD, STRONG_BUY, STRONG_SELL }
    enum class TrendDirection { UP, DOWN, SIDEWAYS }

    fun predict(
        asset: Asset,
        bars: List<HistoricalBar>,
        currentQuote: Quote?
    ): Prediction? {
        if (bars.size < 20) return null

        val closes = bars.map { it.close }
        val highs = bars.map { it.high }
        val lows = bars.map { it.low }
        val volumes = bars.map { it.volume.toDouble() }

        val currentPrice = currentQuote?.price ?: closes.last()
        val reasoning = mutableListOf<String>()

        // Multi-timeframe moving averages
        val sma10 = sma(closes, 10)
        val sma20 = sma(closes, 20)
        val sma50 = if (closes.size >= 50) sma(closes, 50) else sma20
        val sma200 = if (closes.size >= 200) sma(closes, 200) else sma50
        val ema9 = ema(closes, 9)
        val ema21 = ema(closes, 21)

        // RSI
        val rsi = rsi(closes, 14)

        // MACD
        val macdLine = ema(closes, 12) - ema(closes, 26)
        val macdSignal = if (closes.size >= 35) ema(closes.takeLast(35).mapIndexed { i, _ ->
            if (i >= 26) ema(closes.take(closes.size - 35 + i + 1), 12) - ema(closes.take(closes.size - 35 + i + 1), 26)
            else 0.0
        }.takeLast(9), 9) else macdLine

        // ATR for volatility
        val atr = atr(highs, lows, closes, 14)
        val avgVolume = if (volumes.size >= 20) volumes.takeLast(20).average() else volumes.average()
        val currentVolume = volumes.lastOrNull() ?: 0.0
        val volumeRatio = if (avgVolume > 0) currentVolume / avgVolume else 1.0

        // Bollinger Bands
        val bbMiddle = sma20
        val stdDev = standardDeviation(closes.takeLast(20))
        val bbUpper = bbMiddle + 2.0 * stdDev
        val bbLower = bbMiddle - 2.0 * stdDev
        val bbPosition = if (bbUpper != bbLower) (currentPrice - bbLower) / (bbUpper - bbLower) else 0.5

        // Support & Resistance (simple pivot points)
        val recentHigh = highs.takeLast(20).maxOrNull() ?: currentPrice
        val recentLow = lows.takeLast(20).minOrNull() ?: currentPrice
        val pivot = (recentHigh + recentLow + closes.last()) / 3.0
        val r1 = 2 * pivot - recentLow
        val r2 = pivot + (recentHigh - recentLow)
        val s1 = 2 * pivot - recentHigh
        val s2 = pivot - (recentHigh - recentLow)

        // Trend classification
        val shortTrend = classifyTrend(currentPrice, sma10, ema9)
        val mediumTrend = classifyTrend(currentPrice, sma20, ema21)
        val longTrend = classifyTrend(currentPrice, sma50, sma200)

        // Trend alignment score (0 to 1)
        val trendScores = listOf(
            trendScore(shortTrend), trendScore(mediumTrend), trendScore(longTrend)
        )
        val avgTrendScore = trendScores.average()

        // Momentum components
        val rsiSignal = when {
            rsi < 25 -> 0.8   // Very oversold = bullish
            rsi < 30 -> 0.6
            rsi > 75 -> -0.8  // Very overbought = bearish
            rsi > 70 -> -0.6
            rsi > 50 -> 0.2
            else -> -0.2
        }

        val macdDelta = macdLine - macdSignal
        val macdSignalScore = when {
            macdDelta > 0 && macdLine > 0 -> 0.6
            macdDelta > 0 -> 0.3
            macdDelta < 0 && macdLine < 0 -> -0.6
            else -> -0.3
        }

        // Mean reversion component
        val meanReversionScore = when {
            bbPosition < 0.1 -> 0.7  // Near lower band = likely bounce
            bbPosition < 0.2 -> 0.4
            bbPosition > 0.9 -> -0.7 // Near upper band = likely pullback
            bbPosition > 0.8 -> -0.4
            else -> 0.0
        }

        // Volume confirmation
        val volumeBonus = when {
            volumeRatio > 2.0 -> 0.3
            volumeRatio > 1.5 -> 0.15
            volumeRatio < 0.5 -> -0.15
            else -> 0.0
        }

        // Composite score (-1 to +1)
        val compositeScore = (avgTrendScore * 0.35 +
            rsiSignal * 0.2 +
            macdSignalScore * 0.15 +
            meanReversionScore * 0.2 +
            volumeBonus * 0.1).coerceIn(-1.0, 1.0)

        // Determine action
        val action = when {
            compositeScore > 0.5 -> TradeAction.STRONG_BUY
            compositeScore > 0.2 -> TradeAction.BUY
            compositeScore < -0.5 -> TradeAction.STRONG_SELL
            compositeScore < -0.2 -> TradeAction.SELL
            else -> TradeAction.HOLD
        }

        // Calculate entry, target, stop
        val isBullish = compositeScore > 0
        val entryPrice = currentPrice
        val targetMultiplier = if (isBullish) 1.0 + atr / currentPrice * 3.0 else 1.0 - atr / currentPrice * 3.0
        val stopMultiplier = if (isBullish) 1.0 - atr / currentPrice * 1.5 else 1.0 + atr / currentPrice * 1.5
        val targetPrice = currentPrice * targetMultiplier
        val stopLoss = currentPrice * stopMultiplier

        val reward = abs(targetPrice - entryPrice)
        val risk = abs(stopLoss - entryPrice)
        val rrRatio = if (risk > 0) reward / risk else 0.0

        // Price targets with probabilities
        val targets = mutableListOf<PriceTarget>()
        if (isBullish) {
            targets.add(PriceTarget("Conservative", entryPrice + atr, 0.70))
            targets.add(PriceTarget("Target", targetPrice, 0.50))
            targets.add(PriceTarget("Aggressive", r2, 0.30))
            targets.add(PriceTarget("Resistance 1", r1, 0.60))
        } else {
            targets.add(PriceTarget("Conservative", entryPrice - atr, 0.70))
            targets.add(PriceTarget("Target", targetPrice, 0.50))
            targets.add(PriceTarget("Aggressive", s2, 0.30))
            targets.add(PriceTarget("Support 1", s1, 0.60))
        }

        // Multi-horizon predictions
        val dailyVolatility = if (stdDev > 0 && currentPrice > 0) stdDev / currentPrice else 0.02
        val horizonPredictions = buildHorizonPredictions(
            currentPrice = currentPrice,
            compositeScore = compositeScore,
            dailyVolatility = dailyVolatility,
            atr = atr,
            shortTrend = shortTrend,
            mediumTrend = mediumTrend,
            longTrend = longTrend,
            rsi = rsi,
            bbPosition = bbPosition
        )

        // Build reasoning
        reasoning.add("Trend: Short=${shortTrend.name}, Medium=${mediumTrend.name}, Long=${longTrend.name}")
        reasoning.add("RSI(14) = ${"%.1f".format(rsi)} → ${if (rsiSignal > 0) "Bullish" else "Bearish"}")
        reasoning.add("MACD histogram ${if (macdDelta > 0) "positive" else "negative"} → ${if (macdSignalScore > 0) "Bullish" else "Bearish"}")
        reasoning.add("Bollinger position: ${"%.0f".format(bbPosition * 100)}% → ${if (meanReversionScore > 0) "Oversold bounce likely" else if (meanReversionScore < 0) "Overbought pullback likely" else "Neutral"}")
        reasoning.add("Volume ratio: ${"%.1f".format(volumeRatio)}x average")
        reasoning.add("Risk/Reward: ${"%.1f".format(rrRatio)}:1")
        reasoning.add("Composite score: ${"%.2f".format(compositeScore)} → ${action.name}")
        reasoning.add("Daily volatility: ${"%.2f".format(dailyVolatility * 100)}%")
        reasoning.add("Horizons: ${horizonPredictions.joinToString { "${it.horizon.label}=${it.direction.name}(${"%+.1f".format(it.estimatedChangePercent)}%)" }}")

        val confidence = (abs(compositeScore) * 0.6 +
            (if (rrRatio > 2) 0.2 else rrRatio * 0.1) +
            (if (bars.size >= 200) 0.2 else bars.size / 1000.0)).coerceIn(0.1, 0.95)

        val trendForecast = TrendForecast(
            shortTerm = shortTrend,
            mediumTerm = mediumTrend,
            longTerm = longTrend,
            strength = abs(avgTrendScore)
        )

        return Prediction(
            symbol = asset.symbol,
            timestamp = Clock.System.now(),
            action = action,
            entryPrice = entryPrice,
            targetPrice = targetPrice,
            stopLoss = stopLoss,
            riskRewardRatio = rrRatio,
            confidence = confidence,
            timeHorizon = when {
                shortTrend == mediumTrend && mediumTrend == longTrend -> "Swing (1-4 weeks)"
                shortTrend != mediumTrend -> "Short-term (1-5 days)"
                else -> "Medium-term (1-2 weeks)"
            },
            reasoning = reasoning,
            priceTargets = targets,
            trendForecast = trendForecast,
            horizonPredictions = horizonPredictions
        )
    }

    private fun buildHorizonPredictions(
        currentPrice: Double,
        compositeScore: Double,
        dailyVolatility: Double,
        atr: Double,
        shortTrend: TrendDirection,
        mediumTrend: TrendDirection,
        longTrend: TrendDirection,
        rsi: Double,
        bbPosition: Double
    ): List<HorizonPrediction> {
        return PredictionHorizon.entries.map { horizon ->
            val sqrtDays = sqrt(horizon.tradingDays.toDouble())

            // Projected change scales with sqrt(time) * daily volatility
            val baseChange = compositeScore * dailyVolatility * sqrtDays * 100.0

            // Different horizons weight different signals
            val (direction, adjustedChange, keyFactor) = when (horizon) {
                PredictionHorizon.ONE_WEEK, PredictionHorizon.TWO_WEEKS -> {
                    // Short-term: momentum + mean reversion dominate
                    val meanRevAdj = when {
                        bbPosition < 0.15 -> baseChange + dailyVolatility * sqrtDays * 30
                        bbPosition > 0.85 -> baseChange - dailyVolatility * sqrtDays * 30
                        else -> baseChange
                    }
                    val dir = if (meanRevAdj > 0) TrendDirection.UP else if (meanRevAdj < -0.5) TrendDirection.DOWN else TrendDirection.SIDEWAYS
                    val factor = when {
                        rsi < 30 -> "Oversold RSI bounce"
                        rsi > 70 -> "Overbought RSI pullback"
                        shortTrend == TrendDirection.UP -> "Short-term momentum"
                        shortTrend == TrendDirection.DOWN -> "Short-term weakness"
                        else -> "Consolidation phase"
                    }
                    Triple(dir, meanRevAdj, factor)
                }
                PredictionHorizon.ONE_MONTH -> {
                    // Medium-term: trend alignment matters more
                    val trendAdj = baseChange * (if (shortTrend == mediumTrend) 1.3 else 0.7)
                    val dir = if (trendAdj > 0.5) TrendDirection.UP else if (trendAdj < -0.5) TrendDirection.DOWN else TrendDirection.SIDEWAYS
                    val factor = when {
                        shortTrend == mediumTrend && mediumTrend == TrendDirection.UP -> "Aligned bullish trends"
                        shortTrend == mediumTrend && mediumTrend == TrendDirection.DOWN -> "Aligned bearish trends"
                        shortTrend != mediumTrend -> "Mixed signals — caution"
                        else -> "Range-bound"
                    }
                    Triple(dir, trendAdj, factor)
                }
                PredictionHorizon.THREE_MONTHS, PredictionHorizon.SIX_MONTHS -> {
                    // Long-term: fundamental trend + MA alignment
                    val longAdj = baseChange * (if (mediumTrend == longTrend) 1.4 else 0.6)
                    val dir = if (longAdj > 1.0) TrendDirection.UP else if (longAdj < -1.0) TrendDirection.DOWN else TrendDirection.SIDEWAYS
                    val factor = when {
                        mediumTrend == longTrend && longTrend == TrendDirection.UP -> "Strong uptrend continuation"
                        mediumTrend == longTrend && longTrend == TrendDirection.DOWN -> "Persistent downtrend"
                        else -> "Trend transition / rotation"
                    }
                    Triple(dir, longAdj, factor)
                }
                PredictionHorizon.ONE_YEAR -> {
                    // Very long-term: macro trend, mean-revert extreme RSI
                    val macroAdj = baseChange * 0.5 + // Dampen projection
                        (if (rsi < 35) dailyVolatility * sqrtDays * 20 else 0.0) +
                        (if (rsi > 65) -dailyVolatility * sqrtDays * 15 else 0.0)
                    val dir = if (macroAdj > 2.0) TrendDirection.UP else if (macroAdj < -2.0) TrendDirection.DOWN else TrendDirection.SIDEWAYS
                    val factor = when (longTrend) {
                        TrendDirection.UP -> "Secular bull market"
                        TrendDirection.DOWN -> "Secular bear market"
                        TrendDirection.SIDEWAYS -> "Range-bound / uncertain"
                    }
                    Triple(dir, macroAdj, factor)
                }
            }

            // Confidence decays with longer horizons
            val baseConfidence = (0.85 - horizon.tradingDays * 0.001).coerceIn(0.3, 0.85)
            val confidence = (baseConfidence * (0.5 + abs(compositeScore) * 0.5)).coerceIn(0.15, 0.90)

            val changePercent = adjustedChange.coerceIn(-50.0, 50.0)
            val targetPrice = currentPrice * (1.0 + changePercent / 100.0)
            val stopLossPrice = if (direction == TrendDirection.UP) {
                currentPrice * (1.0 - abs(changePercent) / 200.0)
            } else {
                currentPrice * (1.0 + abs(changePercent) / 200.0)
            }

            HorizonPrediction(
                horizon = horizon,
                direction = direction,
                estimatedChangePercent = changePercent,
                targetPrice = targetPrice,
                stopLoss = stopLossPrice,
                confidence = confidence,
                keyFactor = keyFactor
            )
        }
    }

    private fun classifyTrend(price: Double, slowMa: Double, fastMa: Double): TrendDirection {
        val aboveSlow = price > slowMa
        val aboveFast = price > fastMa
        return when {
            aboveSlow && aboveFast -> TrendDirection.UP
            !aboveSlow && !aboveFast -> TrendDirection.DOWN
            else -> TrendDirection.SIDEWAYS
        }
    }

    private fun trendScore(dir: TrendDirection): Double = when (dir) {
        TrendDirection.UP -> 0.6
        TrendDirection.DOWN -> -0.6
        TrendDirection.SIDEWAYS -> 0.0
    }

    // --- Technical helpers ---

    private fun sma(data: List<Double>, period: Int): Double {
        if (data.size < period) return data.average()
        return data.takeLast(period).average()
    }

    private fun ema(data: List<Double>, period: Int): Double {
        if (data.isEmpty()) return 0.0
        val k = 2.0 / (period + 1)
        var ema = data.first()
        for (i in 1 until data.size) {
            ema = data[i] * k + ema * (1 - k)
        }
        return ema
    }

    private fun rsi(data: List<Double>, period: Int): Double {
        if (data.size < period + 1) return 50.0
        var avgGain = 0.0
        var avgLoss = 0.0
        for (i in 1..period) {
            val change = data[data.size - period - 1 + i] - data[data.size - period - 1 + i - 1]
            if (change > 0) avgGain += change else avgLoss += abs(change)
        }
        avgGain /= period
        avgLoss /= period
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    private fun atr(highs: List<Double>, lows: List<Double>, closes: List<Double>, period: Int): Double {
        if (highs.size < 2) return 0.0
        val trs = mutableListOf<Double>()
        for (i in 1 until highs.size) {
            val tr = maxOf(
                highs[i] - lows[i],
                abs(highs[i] - closes[i - 1]),
                abs(lows[i] - closes[i - 1])
            )
            trs.add(tr)
        }
        return if (trs.size >= period) trs.takeLast(period).average() else trs.average()
    }

    private fun standardDeviation(data: List<Double>): Double {
        if (data.size < 2) return 0.0
        val mean = data.average()
        val variance = data.sumOf { (it - mean).pow(2) } / (data.size - 1)
        return sqrt(variance)
    }
}

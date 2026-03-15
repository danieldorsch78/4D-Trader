package com.fourdigital.marketintelligence.analytics.signal

import com.fourdigital.marketintelligence.domain.model.*
import kotlinx.datetime.Clock
import kotlin.math.*

/**
 * Core signal engine: computes technical indicators, regime classification,
 * directional bias, volatility assessment, risk zones, and confidence scoring.
 * All computations are explainable — each signal carries reasoning steps.
 */
class SignalEngine {

    fun analyze(
        asset: Asset,
        bars: List<HistoricalBar>,
        currentQuote: Quote?
    ): SignalAnalysis {
        if (bars.size < 20) {
            return emptyAnalysis(asset, "Insufficient data: need at least 20 bars, have ${bars.size}")
        }

        val closes = bars.map { it.close }
        val highs = bars.map { it.high }
        val lows = bars.map { it.low }
        val volumes = bars.map { it.volume.toDouble() }

        val reasoning = mutableListOf<ReasoningStep>()
        val dataQualityWarnings = mutableListOf<String>()
        val featureContributions = mutableMapOf<String, Double>()
        val keyDrivers = mutableListOf<String>()

        // Data quality checks
        if (bars.size < 100) dataQualityWarnings.add("Limited history: ${bars.size} bars (200+ recommended)")
        if (currentQuote?.dataQuality == QuoteDataQuality.STALE) dataQualityWarnings.add("Current quote is stale")
        if (currentQuote?.dataQuality == QuoteDataQuality.DELAYED_15MIN || currentQuote?.dataQuality == QuoteDataQuality.DELAYED_30MIN) dataQualityWarnings.add("Quote data is delayed")

        // --- Technical Indicators ---
        val sma20 = sma(closes, 20)
        val sma50 = sma(closes, 50)
        val ema12 = ema(closes, 12)
        val ema26 = ema(closes, 26)
        val rsi14 = rsi(closes, 14)
        val macdResult = macd(closes)
        val atr14 = atr(highs, lows, closes, 14)
        val bb = bollingerBands(closes, 20, 2.0)
        val volumeSma20 = if (volumes.size >= 20) sma(volumes, 20) else volumes.average()

        val lastClose = closes.last()
        val lastAtr = atr14.lastOrNull() ?: 0.0

        // --- Regime Classification ---
        val regime = classifyRegime(closes, sma20, sma50, atr14, volumes, volumeSma20, reasoning, keyDrivers)
        featureContributions["regime"] = when (regime) {
            MarketRegime.RISK_ON -> 0.7
            MarketRegime.RISK_OFF -> -0.6
            MarketRegime.TRANSITIONING -> 0.0
            MarketRegime.MIXED -> 0.0
            MarketRegime.UNKNOWN -> 0.0
        }

        // --- Directional Bias ---
        val bias = computeDirectionalBias(lastClose, sma20, sma50, ema12, ema26, rsi14, macdResult, reasoning, keyDrivers)
        featureContributions["trend_sma"] = if (lastClose > sma20) 0.3 else -0.3
        featureContributions["rsi"] = when {
            rsi14 > 70 -> -0.4
            rsi14 < 30 -> 0.4
            else -> (rsi14 - 50.0) / 100.0
        }
        featureContributions["macd"] = if (macdResult.histogram > 0) 0.2 else -0.2

        // --- Volatility Regime ---
        val volatilityRegime = classifyVolatility(atr14, closes, reasoning)

        // --- Buy / Sell Zones ---
        val buyZone = computeBuyZone(lows, lastClose, lastAtr, reasoning)
        val sellZone = computeSellZone(highs, lastClose, lastAtr, reasoning)
        val invalidation = lastClose - lastAtr * 3.0

        // --- Risk Assessment ---
        val riskScore = computeRiskScore(rsi14, volatilityRegime, regime, lastAtr, lastClose)

        // --- Momentum Score ---
        val momentumScore = computeMomentumScore(rsi14, macdResult, lastClose, sma20, sma50)

        // --- Confidence ---
        val confidence = computeConfidence(bars.size, dataQualityWarnings.size, regime, rsi14)

        // Build explanations
        val concise = buildConciseExplanation(regime, bias, rsi14, macdResult, lastClose, sma20)
        val advanced = buildAdvancedExplanation(regime, bias, volatilityRegime, rsi14, macdResult, lastClose, sma20, sma50, lastAtr, bb)

        return SignalAnalysis(
            symbol = asset.symbol,
            timestamp = Clock.System.now(),
            regime = regime,
            directionalBias = bias,
            momentumScore = momentumScore,
            riskScore = riskScore,
            volatilityRegime = volatilityRegime,
            correlationContext = "Cross-asset analysis pending",
            keyDrivers = keyDrivers,
            buyZone = buyZone,
            sellZone = sellZone,
            invalidation = invalidation,
            confidenceScore = confidence,
            conciseExplanation = concise,
            advancedExplanation = advanced,
            featureContributions = featureContributions,
            dataQualityWarnings = dataQualityWarnings
        )
    }

    // ---- Regime Classification ----

    private fun classifyRegime(
        closes: List<Double>,
        sma20: Double,
        sma50: Double,
        atr14: List<Double>,
        volumes: List<Double>,
        volumeSma: Double,
        reasoning: MutableList<ReasoningStep>,
        keyDrivers: MutableList<String>
    ): MarketRegime {
        val last = closes.last()
        val atrPct = if (atr14.isNotEmpty() && last > 0) atr14.last() / last else 0.0
        val recentVol = volumes.takeLast(5).average()
        val volcExpanding = recentVol > volumeSma * 1.3

        val aboveSma20 = last > sma20
        val aboveSma50 = last > sma50
        val sma20AboveSma50 = sma20 > sma50

        reasoning.add(ReasoningStep(
            factor = "SMA Positioning",
            observation = "Price ${if (aboveSma20) "above" else "below"} SMA20 (${"%.2f".format(sma20)}), ${if (aboveSma50) "above" else "below"} SMA50 (${"%.2f".format(sma50)})",
            impact = if (aboveSma20 && aboveSma50) "Bullish structure" else if (!aboveSma20 && !aboveSma50) "Bearish structure" else "Mixed signals",
            weight = 0.3
        ))

        reasoning.add(ReasoningStep(
            factor = "Volatility",
            observation = "ATR/Price = ${"%.2f%%".format(atrPct * 100)}, Volume ${if (volcExpanding) "expanding" else "normal"}",
            impact = if (atrPct > 0.03) "Elevated risk" else "Manageable risk",
            weight = 0.2
        ))

        return when {
            atrPct > 0.04 && volcExpanding -> {
                keyDrivers.add("High volatility with expanding volume")
                MarketRegime.RISK_OFF
            }
            aboveSma20 && aboveSma50 && sma20AboveSma50 -> {
                keyDrivers.add("Bullish SMA alignment: price > SMA20 > SMA50")
                MarketRegime.RISK_ON
            }
            !aboveSma20 && !aboveSma50 && !sma20AboveSma50 -> {
                keyDrivers.add("Bearish SMA alignment: price < SMA20 < SMA50")
                MarketRegime.RISK_OFF
            }
            aboveSma20 != aboveSma50 -> {
                keyDrivers.add("Mixed SMA signals — transitioning")
                MarketRegime.TRANSITIONING
            }
            else -> {
                keyDrivers.add("No clear directional regime")
                MarketRegime.MIXED
            }
        }
    }

    // ---- Directional Bias ----

    private fun computeDirectionalBias(
        lastClose: Double,
        sma20: Double,
        sma50: Double,
        ema12: Double,
        ema26: Double,
        rsi14: Double,
        macd: MacdResult,
        reasoning: MutableList<ReasoningStep>,
        keyDrivers: MutableList<String>
    ): DirectionalBias {
        var score = 0.0

        if (lastClose > sma20) score += 1.0 else score -= 1.0
        if (lastClose > sma50) score += 1.0 else score -= 1.0
        if (sma20 > sma50) score += 0.5 else score -= 0.5
        if (ema12 > ema26) score += 0.5 else score -= 0.5
        if (macd.histogram > 0) score += 0.5 else score -= 0.5

        when {
            rsi14 > 60 -> score += 0.5
            rsi14 < 40 -> score -= 0.5
        }

        reasoning.add(ReasoningStep(
            factor = "Multi-factor Trend",
            observation = "Combined score: ${"%.1f".format(score)} (SMA/EMA/MACD/RSI)",
            impact = if (score > 1) "Bullish momentum" else if (score < -1) "Bearish momentum" else "Neutral",
            weight = 0.4
        ))

        val bias = when {
            score > 2.5 -> DirectionalBias.STRONG_BULLISH
            score > 1.0 -> DirectionalBias.BULLISH
            score > 0.3 -> DirectionalBias.NEUTRAL_BULLISH
            score < -2.5 -> DirectionalBias.STRONG_BEARISH
            score < -1.0 -> DirectionalBias.BEARISH
            score < -0.3 -> DirectionalBias.NEUTRAL_BEARISH
            else -> DirectionalBias.NEUTRAL
        }

        keyDrivers.add("RSI(${"%.0f".format(rsi14)}) + MACD(${if (macd.histogram > 0) "+" else "-"}) → $bias")
        return bias
    }

    // ---- Volatility Classification ----

    private fun classifyVolatility(
        atr14: List<Double>,
        closes: List<Double>,
        reasoning: MutableList<ReasoningStep>
    ): VolatilityRegime {
        if (atr14.isEmpty()) return VolatilityRegime.NORMAL
        val atrPct = atr14.last() / closes.last()
        val atrAvg = atr14.average() / closes.average()

        reasoning.add(ReasoningStep(
            factor = "ATR Analysis",
            observation = "Current ATR%: ${"%.2f%%".format(atrPct * 100)}, Avg ATR%: ${"%.2f%%".format(atrAvg * 100)}",
            impact = if (atrPct > atrAvg * 1.3) "Volatility expanding" else "Normal range",
            weight = 0.2
        ))

        return when {
            atrPct > atrAvg * 1.8 -> VolatilityRegime.EXTREME
            atrPct > atrAvg * 1.3 -> VolatilityRegime.HIGH
            atrPct > atrAvg * 1.1 -> VolatilityRegime.ELEVATED
            atrPct < atrAvg * 0.7 -> VolatilityRegime.LOW
            else -> VolatilityRegime.NORMAL
        }
    }

    // ---- Buy / Sell Zones ----

    private fun computeBuyZone(
        lows: List<Double>,
        lastClose: Double,
        atr: Double,
        reasoning: MutableList<ReasoningStep>
    ): PriceZone {
        val recentLow = lows.takeLast(20).min()
        val lower = minOf(recentLow, lastClose - atr * 1.5)
        val upper = lastClose - atr * 0.5

        reasoning.add(ReasoningStep(
            factor = "Buy Zone",
            observation = "Zone: ${"%.2f".format(lower)} - ${"%.2f".format(upper)}",
            impact = "Based on recent swing low + ATR pullback",
            weight = 0.15
        ))

        return PriceZone(lower = lower, upper = upper, label = "Buy Zone (ATR pullback)")
    }

    private fun computeSellZone(
        highs: List<Double>,
        lastClose: Double,
        atr: Double,
        reasoning: MutableList<ReasoningStep>
    ): PriceZone {
        val recentHigh = highs.takeLast(20).max()
        val lower = lastClose + atr * 0.5
        val upper = maxOf(recentHigh, lastClose + atr * 1.5)

        reasoning.add(ReasoningStep(
            factor = "Sell Zone",
            observation = "Zone: ${"%.2f".format(lower)} - ${"%.2f".format(upper)}",
            impact = "Based on recent swing high + ATR extension",
            weight = 0.15
        ))

        return PriceZone(lower = lower, upper = upper, label = "Sell Zone (ATR extension)")
    }

    // ---- Risk Score ----

    private fun computeRiskScore(
        rsi14: Double,
        volatilityRegime: VolatilityRegime,
        marketRegime: MarketRegime,
        atr: Double,
        lastClose: Double
    ): Double {
        var risk = 50.0

        when (volatilityRegime) {
            VolatilityRegime.EXTREME -> risk += 30
            VolatilityRegime.HIGH -> risk += 15
            VolatilityRegime.ELEVATED -> risk += 8
            VolatilityRegime.LOW -> risk -= 5
            VolatilityRegime.NORMAL -> {}
        }

        if (rsi14 > 80 || rsi14 < 20) risk += 15
        else if (rsi14 > 70 || rsi14 < 30) risk += 8

        when (marketRegime) {
            MarketRegime.RISK_OFF -> risk += 15
            MarketRegime.TRANSITIONING -> risk += 5
            else -> {}
        }

        return risk.coerceIn(0.0, 100.0)
    }

    // ---- Momentum Score ----

    private fun computeMomentumScore(
        rsi14: Double,
        macd: MacdResult,
        lastClose: Double,
        sma20: Double,
        sma50: Double
    ): Double {
        var score = 0.0

        // RSI contribution (centered at 50, mapped to -40..+40)
        score += (rsi14 - 50.0) * 0.8

        // MACD histogram sign
        score += if (macd.histogram > 0) 15.0 else -15.0

        // Price vs SMAs
        val pctAboveSma20 = (lastClose - sma20) / sma20 * 100
        score += pctAboveSma20.coerceIn(-20.0, 20.0)

        return score.coerceIn(-100.0, 100.0)
    }

    // ---- Confidence ----

    private fun computeConfidence(
        barCount: Int,
        warningCount: Int,
        regime: MarketRegime,
        rsi14: Double
    ): Int {
        var conf = 60

        when {
            barCount >= 200 -> conf += 20
            barCount >= 100 -> conf += 10
        }

        conf -= warningCount * 5

        when (regime) {
            MarketRegime.RISK_ON, MarketRegime.RISK_OFF -> conf += 10
            MarketRegime.TRANSITIONING -> conf += 3
            MarketRegime.UNKNOWN -> conf -= 15
            else -> {}
        }

        if (rsi14 > 70 || rsi14 < 30) conf += 5

        return conf.coerceIn(0, 100)
    }

    // ---- Technical Indicators ----

    fun sma(data: List<Double>, period: Int): Double {
        if (data.size < period) return data.average()
        return data.takeLast(period).average()
    }

    fun ema(data: List<Double>, period: Int): Double {
        if (data.isEmpty()) return 0.0
        val multiplier = 2.0 / (period + 1)
        var emaValue = data.take(period).average()
        for (i in period until data.size) {
            emaValue = (data[i] - emaValue) * multiplier + emaValue
        }
        return emaValue
    }

    fun rsi(closes: List<Double>, period: Int): Double {
        if (closes.size < period + 1) return 50.0
        val changes = closes.zipWithNext { a, b -> b - a }
        val gains = changes.takeLast(period).filter { it > 0 }
        val losses = changes.takeLast(period).filter { it < 0 }.map { -it }
        val avgGain = if (gains.isNotEmpty()) gains.average() else 0.001
        val avgLoss = if (losses.isNotEmpty()) losses.average() else 0.001
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    data class MacdResult(val macdLine: Double, val signalLine: Double, val histogram: Double)

    fun macd(closes: List<Double>): MacdResult {
        val ema12Val = ema(closes, 12)
        val ema26Val = ema(closes, 26)
        val macdLine = ema12Val - ema26Val
        val macdSeries = mutableListOf<Double>()
        var e12 = closes.take(12).average()
        var e26 = closes.take(26).average()
        val m12 = 2.0 / 13.0
        val m26 = 2.0 / 27.0
        for (i in 26 until closes.size) {
            e12 = (closes[i] - e12) * m12 + e12
            e26 = (closes[i] - e26) * m26 + e26
            macdSeries.add(e12 - e26)
        }
        val signalLine = if (macdSeries.size >= 9) ema(macdSeries, 9) else macdLine
        return MacdResult(macdLine, signalLine, macdLine - signalLine)
    }

    fun atr(highs: List<Double>, lows: List<Double>, closes: List<Double>, period: Int): List<Double> {
        if (highs.size < 2) return emptyList()
        val trueRanges = mutableListOf<Double>()
        for (i in 1 until highs.size) {
            val tr = maxOf(
                highs[i] - lows[i],
                abs(highs[i] - closes[i - 1]),
                abs(lows[i] - closes[i - 1])
            )
            trueRanges.add(tr)
        }
        if (trueRanges.size < period) return trueRanges
        val atrValues = mutableListOf<Double>()
        var atrVal = trueRanges.take(period).average()
        atrValues.add(atrVal)
        for (i in period until trueRanges.size) {
            atrVal = (atrVal * (period - 1) + trueRanges[i]) / period
            atrValues.add(atrVal)
        }
        return atrValues
    }

    fun bollingerBands(closes: List<Double>, period: Int, mult: Double): Triple<Double, Double, Double> {
        val mean = sma(closes, period)
        val recent = closes.takeLast(period)
        val variance = recent.sumOf { (it - mean).pow(2) } / recent.size
        val stdDev = sqrt(variance)
        return Triple(mean - mult * stdDev, mean, mean + mult * stdDev)
    }

    // ---- Explanation Builders ----

    private fun buildConciseExplanation(
        regime: MarketRegime,
        bias: DirectionalBias,
        rsi: Double,
        macd: MacdResult,
        close: Double,
        sma20: Double
    ): String {
        val regimeText = when (regime) {
            MarketRegime.RISK_ON -> "Risk-On"
            MarketRegime.RISK_OFF -> "Risk-Off"
            MarketRegime.TRANSITIONING -> "Transitioning"
            MarketRegime.MIXED -> "Mixed"
            MarketRegime.UNKNOWN -> "Unknown"
        }
        val biasText = when (bias) {
            DirectionalBias.STRONG_BULLISH -> "strongly bullish"
            DirectionalBias.BULLISH -> "bullish"
            DirectionalBias.NEUTRAL_BULLISH -> "lean bullish"
            DirectionalBias.NEUTRAL -> "neutral"
            DirectionalBias.NEUTRAL_BEARISH -> "lean bearish"
            DirectionalBias.BEARISH -> "bearish"
            DirectionalBias.STRONG_BEARISH -> "strongly bearish"
            DirectionalBias.INDETERMINATE -> "indeterminate"
        }
        return "$regimeText regime, $biasText bias. RSI ${"%.0f".format(rsi)}, MACD ${if (macd.histogram > 0) "positive" else "negative"}. Price ${if (close > sma20) "above" else "below"} SMA20."
    }

    private fun buildAdvancedExplanation(
        regime: MarketRegime,
        bias: DirectionalBias,
        vol: VolatilityRegime,
        rsi: Double,
        macd: MacdResult,
        close: Double,
        sma20: Double,
        sma50: Double,
        atr: Double,
        bb: Triple<Double, Double, Double>
    ): String {
        return buildString {
            appendLine("=== SIGNAL ANALYSIS ===")
            appendLine("Regime: ${regime.name} | Bias: ${bias.name} | Volatility: ${vol.name}")
            appendLine()
            appendLine("Price: ${"%.2f".format(close)} | SMA20: ${"%.2f".format(sma20)} | SMA50: ${"%.2f".format(sma50)}")
            appendLine("RSI(14): ${"%.1f".format(rsi)} | MACD: ${"%.4f".format(macd.macdLine)} | Signal: ${"%.4f".format(macd.signalLine)} | Hist: ${"%.4f".format(macd.histogram)}")
            appendLine("ATR(14): ${"%.2f".format(atr)} (${"%.2f%%".format(atr / close * 100)} of price)")
            appendLine("Bollinger: Lower=${"%.2f".format(bb.first)} Mid=${"%.2f".format(bb.second)} Upper=${"%.2f".format(bb.third)}")
            appendLine()
            append("⚠ This is a decision-support tool, not trading advice. Past performance does not guarantee future results.")
        }
    }

    private fun emptyAnalysis(asset: Asset, reason: String): SignalAnalysis {
        return SignalAnalysis(
            symbol = asset.symbol,
            timestamp = Clock.System.now(),
            regime = MarketRegime.UNKNOWN,
            directionalBias = DirectionalBias.INDETERMINATE,
            momentumScore = 0.0,
            riskScore = 50.0,
            volatilityRegime = VolatilityRegime.NORMAL,
            correlationContext = "",
            keyDrivers = listOf(reason),
            buyZone = null,
            sellZone = null,
            invalidation = null,
            confidenceScore = 10,
            conciseExplanation = reason,
            advancedExplanation = reason,
            featureContributions = emptyMap(),
            dataQualityWarnings = listOf(reason)
        )
    }
}

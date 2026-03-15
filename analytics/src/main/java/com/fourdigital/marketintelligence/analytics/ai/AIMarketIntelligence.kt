package com.fourdigital.marketintelligence.analytics.ai

import com.fourdigital.marketintelligence.analytics.prediction.PredictionEngine
import com.fourdigital.marketintelligence.analytics.signal.SignalEngine
import com.fourdigital.marketintelligence.domain.model.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * AI Market Intelligence Engine — combines technical analysis, news sentiment,
 * multi-timeframe signals, and price prediction into a unified analysis.
 * Produces real-time trading intelligence with confidence-weighted recommendations.
 */
@Singleton
class AIMarketIntelligence @Inject constructor() {

    private val predictionEngine = PredictionEngine()
    private val signalEngine = SignalEngine()

    /**
     * Full AI analysis for a single symbol across all timeframes.
     */
    fun analyzeAsset(
        asset: Asset,
        bars: List<HistoricalBar>,
        currentQuote: Quote?,
        newsItems: List<NewsItem> = emptyList()
    ): AIAnalysisResult {
        val closes = bars.map { it.close }
        val currentPrice = currentQuote?.price ?: closes.lastOrNull() ?: 0.0

        // Technical analysis
        val signal = signalEngine.analyze(asset, bars, currentQuote)
        val prediction = predictionEngine.predict(asset, bars, currentQuote)

        // News sentiment
        val sentiment = analyzeNewsSentiment(newsItems, asset.symbol)

        // Multi-timeframe forecast
        val timeframes = buildTimeframeForecasts(closes, bars, currentPrice)

        // AI composite score: combines technical + sentiment + momentum
        val technicalScore = (signal.momentumScore / 100.0).coerceIn(-1.0, 1.0)
        val sentimentScore = sentiment.score
        val trendScore = prediction?.let {
            when (it.action) {
                PredictionEngine.TradeAction.STRONG_BUY -> 0.9
                PredictionEngine.TradeAction.BUY -> 0.5
                PredictionEngine.TradeAction.HOLD -> 0.0
                PredictionEngine.TradeAction.SELL -> -0.5
                PredictionEngine.TradeAction.STRONG_SELL -> -0.9
            }
        } ?: 0.0

        val compositeAIScore = (technicalScore * 0.45 + sentimentScore * 0.25 + trendScore * 0.30)
            .coerceIn(-1.0, 1.0)

        val aiAction = when {
            compositeAIScore > 0.5 -> AIAction.STRONG_BUY
            compositeAIScore > 0.2 -> AIAction.BUY
            compositeAIScore < -0.5 -> AIAction.STRONG_SELL
            compositeAIScore < -0.2 -> AIAction.SELL
            else -> AIAction.HOLD
        }

        val confidence = (abs(compositeAIScore) * 60 +
            signal.confidenceScore * 0.3 +
            (if (newsItems.isNotEmpty()) 10 else 0)).coerceIn(5.0, 95.0)

        // Build AI insights
        val insights = buildInsights(
            signal, prediction, sentiment, timeframes, currentPrice, asset.symbol
        )

        // Price levels
        val keyLevels = buildKeyLevels(bars, currentPrice, prediction)

        return AIAnalysisResult(
            symbol = asset.symbol,
            assetName = asset.name,
            assetClass = asset.assetClass,
            currentPrice = currentPrice,
            timestamp = Clock.System.now(),
            action = aiAction,
            compositeScore = compositeAIScore,
            confidence = confidence,
            signal = signal,
            prediction = prediction,
            sentiment = sentiment,
            timeframes = timeframes,
            insights = insights,
            keyLevels = keyLevels,
            riskLevel = classifyRisk(signal.riskScore, signal.volatilityRegime),
            newsCount = newsItems.size
        )
    }

    private fun analyzeNewsSentiment(news: List<NewsItem>, symbol: String): SentimentResult {
        if (news.isEmpty()) return SentimentResult(0.0, "No news data", 0, 0, 0)

        val bullishWords = setOf(
            "surge", "rally", "bullish", "gain", "jump", "soar", "rise", "high",
            "upgrade", "outperform", "buy", "growth", "record", "strong", "boost",
            "profit", "revenue", "beat", "breakout", "recovery", "positive", "optimistic"
        )
        val bearishWords = setOf(
            "crash", "fall", "bearish", "drop", "plunge", "decline", "low", "sell",
            "downgrade", "underperform", "loss", "weak", "miss", "recession", "fear",
            "risk", "warning", "cut", "negative", "pessimistic", "bankruptcy", "default"
        )

        var bullCount = 0
        var bearCount = 0
        var neutralCount = 0

        for (item in news) {
            val text = "${item.headline} ${item.summary}".lowercase()
            val bulls = bullishWords.count { text.contains(it) }
            val bears = bearishWords.count { text.contains(it) }
            when {
                bulls > bears -> bullCount++
                bears > bulls -> bearCount++
                else -> neutralCount++
            }
        }

        val total = news.size.toDouble()
        val score = ((bullCount - bearCount) / total).coerceIn(-1.0, 1.0)

        val label = when {
            score > 0.3 -> "Bullish sentiment ($bullCount/${ news.size} positive)"
            score < -0.3 -> "Bearish sentiment ($bearCount/${news.size} negative)"
            else -> "Mixed sentiment (${bullCount}↑ ${bearCount}↓ ${neutralCount}→)"
        }

        return SentimentResult(score, label, bullCount, bearCount, neutralCount)
    }

    private fun buildTimeframeForecasts(
        closes: List<Double>,
        bars: List<HistoricalBar>,
        currentPrice: Double
    ): List<TimeframeForecast> {
        val forecasts = mutableListOf<TimeframeForecast>()

        // Intraday (using last 20 bars momentum)
        if (closes.size >= 20) {
            val shortMom = (currentPrice - closes[closes.size - 5]) / closes[closes.size - 5] * 100
            val intraChange = estimateChange(closes, 5, currentPrice)
            forecasts.add(TimeframeForecast(
                timeframe = "Intraday",
                direction = if (intraChange > 0) "UP" else if (intraChange < 0) "DOWN" else "SIDEWAYS",
                estimatedChange = intraChange,
                confidence = (40 + abs(shortMom).coerceAtMost(30.0)).toInt(),
                keyFactor = if (shortMom > 0) "Short-term momentum positive" else "Short-term momentum negative"
            ))
        }

        // Daily (5-day outlook)
        if (closes.size >= 50) {
            val dailyChange = estimateChange(closes, 20, currentPrice)
            val rsi = rsi(closes, 14)
            forecasts.add(TimeframeForecast(
                timeframe = "Daily (1-5 days)",
                direction = if (dailyChange > 0.5) "UP" else if (dailyChange < -0.5) "DOWN" else "SIDEWAYS",
                estimatedChange = dailyChange,
                confidence = (35 + (if (rsi < 30 || rsi > 70) 25 else 10)).coerceAtMost(85),
                keyFactor = "RSI(${"%.0f".format(rsi)}) + trend alignment"
            ))
        }

        // Weekly (5-20 day outlook)
        if (closes.size >= 100) {
            val weeklyChange = estimateChange(closes, 50, currentPrice)
            val sma20 = closes.takeLast(20).average()
            val sma50 = closes.takeLast(50).average()
            val trendAligned = (currentPrice > sma20 && sma20 > sma50) ||
                (currentPrice < sma20 && sma20 < sma50)
            forecasts.add(TimeframeForecast(
                timeframe = "Weekly (1-4 weeks)",
                direction = if (weeklyChange > 1.0) "UP" else if (weeklyChange < -1.0) "DOWN" else "SIDEWAYS",
                estimatedChange = weeklyChange,
                confidence = (30 + (if (trendAligned) 30 else 10)).coerceAtMost(80),
                keyFactor = if (trendAligned) "Trend aligned (SMA20 > SMA50)" else "Trend mixed signals"
            ))
        }

        // Monthly (20-60 day outlook)
        if (closes.size >= 200) {
            val monthlyChange = estimateChange(closes, 100, currentPrice)
            val sma50 = closes.takeLast(50).average()
            val sma200 = closes.takeLast(200).average()
            forecasts.add(TimeframeForecast(
                timeframe = "Monthly (1-3 months)",
                direction = if (monthlyChange > 3.0) "UP" else if (monthlyChange < -3.0) "DOWN" else "SIDEWAYS",
                estimatedChange = monthlyChange,
                confidence = (25 + (if (sma50 > sma200) 20 else 5)).coerceAtMost(70),
                keyFactor = if (sma50 > sma200) "Golden cross (SMA50 > SMA200)" else "Death cross (SMA50 < SMA200)"
            ))
        }

        return forecasts
    }

    private fun estimateChange(closes: List<Double>, lookback: Int, currentPrice: Double): Double {
        if (closes.size < lookback * 2) return 0.0
        val recentSlice = closes.takeLast(lookback)
        val avgRecentReturn = (recentSlice.last() - recentSlice.first()) / recentSlice.first() * 100

        // Mean reversion factor
        val sma = recentSlice.average()
        val deviation = (currentPrice - sma) / sma * 100
        val meanReversionPull = -deviation * 0.3

        // Momentum projection (damped)
        val projectedReturn = avgRecentReturn * 0.5 + meanReversionPull * 0.5

        return projectedReturn.coerceIn(-15.0, 15.0)
    }

    private fun buildInsights(
        signal: SignalAnalysis,
        prediction: PredictionEngine.Prediction?,
        sentiment: SentimentResult,
        timeframes: List<TimeframeForecast>,
        currentPrice: Double,
        symbol: String
    ): List<AIInsight> {
        val insights = mutableListOf<AIInsight>()

        // Market regime insight
        insights.add(AIInsight(
            category = "Market Regime",
            title = "${signal.regime.name} regime detected",
            detail = signal.conciseExplanation,
            impact = when (signal.regime) {
                MarketRegime.RISK_ON -> InsightImpact.POSITIVE
                MarketRegime.RISK_OFF -> InsightImpact.NEGATIVE
                else -> InsightImpact.NEUTRAL
            }
        ))

        // Directional bias
        insights.add(AIInsight(
            category = "Direction",
            title = "Bias: ${signal.directionalBias.name.replace("_", " ")}",
            detail = "Momentum score: ${"%.0f".format(signal.momentumScore)}/100",
            impact = when {
                signal.momentumScore > 30 -> InsightImpact.POSITIVE
                signal.momentumScore < -30 -> InsightImpact.NEGATIVE
                else -> InsightImpact.NEUTRAL
            }
        ))

        // Prediction insight
        prediction?.let {
            insights.add(AIInsight(
                category = "Prediction",
                title = "${it.action.name}: Target ${"%.2f".format(it.targetPrice)}",
                detail = "R:R ${"%.1f".format(it.riskRewardRatio)}:1, SL ${"%.2f".format(it.stopLoss)}, ${it.timeHorizon}",
                impact = when (it.action) {
                    PredictionEngine.TradeAction.STRONG_BUY, PredictionEngine.TradeAction.BUY -> InsightImpact.POSITIVE
                    PredictionEngine.TradeAction.STRONG_SELL, PredictionEngine.TradeAction.SELL -> InsightImpact.NEGATIVE
                    else -> InsightImpact.NEUTRAL
                }
            ))
        }

        // Sentiment insight
        if (sentiment.totalPositive + sentiment.totalNegative > 0) {
            insights.add(AIInsight(
                category = "Sentiment",
                title = sentiment.label,
                detail = "Based on ${sentiment.totalPositive + sentiment.totalNegative + sentiment.totalNeutral} news items",
                impact = when {
                    sentiment.score > 0.2 -> InsightImpact.POSITIVE
                    sentiment.score < -0.2 -> InsightImpact.NEGATIVE
                    else -> InsightImpact.NEUTRAL
                }
            ))
        }

        // Volatility insight
        insights.add(AIInsight(
            category = "Volatility",
            title = "${signal.volatilityRegime.name} volatility",
            detail = "Risk score: ${"%.0f".format(signal.riskScore)}/100",
            impact = when (signal.volatilityRegime) {
                VolatilityRegime.HIGH, VolatilityRegime.EXTREME -> InsightImpact.WARNING
                VolatilityRegime.ELEVATED -> InsightImpact.NEUTRAL
                else -> InsightImpact.POSITIVE
            }
        ))

        // Multi-timeframe insight
        val upCount = timeframes.count { it.direction == "UP" }
        val downCount = timeframes.count { it.direction == "DOWN" }
        if (timeframes.isNotEmpty()) {
            insights.add(AIInsight(
                category = "Timeframes",
                title = "$upCount/${timeframes.size} timeframes bullish",
                detail = timeframes.joinToString(" | ") { "${it.timeframe}: ${it.direction}" },
                impact = when {
                    upCount > downCount * 2 -> InsightImpact.POSITIVE
                    downCount > upCount * 2 -> InsightImpact.NEGATIVE
                    else -> InsightImpact.NEUTRAL
                }
            ))
        }

        return insights
    }

    private fun buildKeyLevels(
        bars: List<HistoricalBar>,
        currentPrice: Double,
        prediction: PredictionEngine.Prediction?
    ): List<PriceLevel> {
        val levels = mutableListOf<PriceLevel>()

        if (bars.size >= 20) {
            val high20 = bars.takeLast(20).maxOf { it.high }
            val low20 = bars.takeLast(20).minOf { it.low }
            levels.add(PriceLevel("20D High", high20, if (currentPrice >= high20 * 0.99) "AT" else "ABOVE"))
            levels.add(PriceLevel("20D Low", low20, if (currentPrice <= low20 * 1.01) "AT" else "BELOW"))
        }

        if (bars.size >= 50) {
            val high50 = bars.takeLast(50).maxOf { it.high }
            val low50 = bars.takeLast(50).minOf { it.low }
            levels.add(PriceLevel("50D High", high50, "RESISTANCE"))
            levels.add(PriceLevel("50D Low", low50, "SUPPORT"))
        }

        prediction?.let {
            levels.add(PriceLevel("Target", it.targetPrice, "TARGET"))
            levels.add(PriceLevel("Stop-Loss", it.stopLoss, "STOP"))
        }

        return levels.sortedByDescending { it.price }
    }

    private fun classifyRisk(riskScore: Double, volatility: VolatilityRegime): RiskLevel {
        return when {
            riskScore > 75 || volatility == VolatilityRegime.EXTREME -> RiskLevel.CRITICAL
            riskScore > 60 || volatility == VolatilityRegime.HIGH -> RiskLevel.HIGH
            riskScore > 40 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }
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
        return 100.0 - (100.0 / (1.0 + avgGain / avgLoss))
    }
}

// --- Data Models ---

data class AIAnalysisResult(
    val symbol: String,
    val assetName: String,
    val assetClass: AssetClass,
    val currentPrice: Double,
    val timestamp: kotlinx.datetime.Instant,
    val action: AIAction,
    val compositeScore: Double, // -1.0 to +1.0
    val confidence: Double,     // 0-95
    val signal: SignalAnalysis,
    val prediction: PredictionEngine.Prediction?,
    val sentiment: SentimentResult,
    val timeframes: List<TimeframeForecast>,
    val insights: List<AIInsight>,
    val keyLevels: List<PriceLevel>,
    val riskLevel: RiskLevel,
    val newsCount: Int
)

enum class AIAction {
    STRONG_BUY, BUY, HOLD, SELL, STRONG_SELL
}

data class SentimentResult(
    val score: Double,        // -1.0 to +1.0
    val label: String,
    val totalPositive: Int,
    val totalNegative: Int,
    val totalNeutral: Int
)

data class TimeframeForecast(
    val timeframe: String,
    val direction: String, // UP, DOWN, SIDEWAYS
    val estimatedChange: Double, // %
    val confidence: Int,
    val keyFactor: String
)

data class AIInsight(
    val category: String,
    val title: String,
    val detail: String,
    val impact: InsightImpact
)

enum class InsightImpact { POSITIVE, NEGATIVE, NEUTRAL, WARNING }

data class PriceLevel(
    val label: String,
    val price: Double,
    val type: String
)

enum class RiskLevel { LOW, MODERATE, HIGH, CRITICAL }

data class NewsItem(
    val headline: String,
    val summary: String,
    val source: String,
    val datetime: Long,
    val url: String,
    val related: String = ""
)

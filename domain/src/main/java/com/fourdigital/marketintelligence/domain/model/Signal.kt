package com.fourdigital.marketintelligence.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class MarketRegime {
    RISK_ON,
    RISK_OFF,
    TRANSITIONING,
    MIXED,
    UNKNOWN
}

@Serializable
enum class DirectionalBias {
    STRONG_BULLISH,
    BULLISH,
    NEUTRAL_BULLISH,
    NEUTRAL,
    NEUTRAL_BEARISH,
    BEARISH,
    STRONG_BEARISH,
    INDETERMINATE
}

@Serializable
enum class VolatilityRegime {
    LOW,
    NORMAL,
    ELEVATED,
    HIGH,
    EXTREME
}

@Serializable
data class SignalAnalysis(
    val symbol: String,
    val timestamp: Instant,
    val regime: MarketRegime,
    val directionalBias: DirectionalBias,
    val momentumScore: Double,     // -100 to +100
    val riskScore: Double,         // 0 to 100 (100 = highest risk)
    val volatilityRegime: VolatilityRegime,
    val correlationContext: String,
    val keyDrivers: List<String>,
    val buyZone: PriceZone?,
    val sellZone: PriceZone?,
    val invalidation: Double?,
    val confidenceScore: Int,      // 0 to 100
    val conciseExplanation: String,
    val advancedExplanation: String,
    val featureContributions: Map<String, Double> = emptyMap(),
    val dataQualityWarnings: List<String> = emptyList()
) {
    init {
        require(confidenceScore in 0..100) { "Confidence must be 0-100" }
        require(momentumScore in -100.0..100.0) { "Momentum must be -100 to +100" }
        require(riskScore in 0.0..100.0) { "Risk must be 0-100" }
    }

    val isActionable: Boolean get() = confidenceScore >= 40 && dataQualityWarnings.isEmpty()
}

@Serializable
data class PriceZone(
    val lower: Double,
    val upper: Double,
    val label: String = ""
) {
    val midpoint: Double get() = (lower + upper) / 2.0

    init {
        require(lower <= upper) { "Lower must be <= upper" }
    }
}

@Serializable
data class AIExplanation(
    val summary: String,
    val reasoning: List<ReasoningStep>,
    val disclaimers: List<String>,
    val modelVersion: String,
    val isLocalComputation: Boolean = true
)

@Serializable
data class ReasoningStep(
    val factor: String,
    val observation: String,
    val impact: String,
    val weight: Double
)

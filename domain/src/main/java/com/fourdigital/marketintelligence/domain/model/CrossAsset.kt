package com.fourdigital.marketintelligence.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CrossAssetSummary(
    val regime: MarketRegime,
    val riskOnOffScore: Double,  // -100 (risk-off) to +100 (risk-on)
    val goldStatus: AssetBehavior,
    val silverStatus: AssetBehavior,
    val oilStatus: AssetBehavior,
    val bitcoinStatus: AssetBehavior,
    val germanySummary: RegionSummary,
    val brazilSummary: RegionSummary,
    val insights: List<String>,
    val headline: String
)

@Serializable
data class AssetBehavior(
    val symbol: String,
    val role: String,
    val isConfirmingDefensiveBehavior: Boolean,
    val isLeadingEquities: Boolean,
    val trendDescription: String,
    val relativeStrength: Double
)

@Serializable
data class RegionSummary(
    val name: String,
    val representativeIndex: String,
    val performance: Double,
    val relativeStrength: Double,
    val isStronger: Boolean,
    val keyDrivers: List<String>
)

@Serializable
data class OpportunityRanking(
    val symbol: String,
    val score: Double,  // 0-100
    val factors: Map<String, Double>,
    val bias: DirectionalBias,
    val confidence: Int,
    val rank: Int
)

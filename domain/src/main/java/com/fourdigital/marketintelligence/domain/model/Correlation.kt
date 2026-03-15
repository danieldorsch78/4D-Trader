package com.fourdigital.marketintelligence.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CorrelationSnapshot(
    val assetA: String,
    val assetB: String,
    val pearson: Double,
    val spearman: Double,
    val windowDays: Int,
    val sampleSize: Int,
    val timestamp: Instant,
    val isStatisticallySignificant: Boolean,
    val stability: CorrelationStability = CorrelationStability.STABLE
)

@Serializable
enum class CorrelationStability {
    STABLE,
    WEAKENING,
    BREAKING,
    REVERTING,
    UNKNOWN
}

@Serializable
data class CorrelationSeries(
    val assetA: String,
    val assetB: String,
    val windowDays: Int,
    val dataPoints: List<CorrelationDataPoint>
)

@Serializable
data class CorrelationDataPoint(
    val timestamp: Instant,
    val pearson: Double,
    val spearman: Double
)

enum class CorrelationWindow(val days: Int, val displayName: String) {
    SHORT(20, "20-day"),
    MEDIUM(60, "60-day"),
    LONG(120, "120-day"),
    EXTENDED(252, "1-Year")
}

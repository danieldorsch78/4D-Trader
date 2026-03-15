package com.fourdigital.marketintelligence.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class HistoricalBar(
    val symbol: String,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val timestamp: Instant,
    val adjustedClose: Double? = null
)

@Serializable
enum class TimeFrame(val displayName: String) {
    INTRADAY_1M("1 Min"),
    INTRADAY_5M("5 Min"),
    INTRADAY_15M("15 Min"),
    INTRADAY_1H("1 Hour"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

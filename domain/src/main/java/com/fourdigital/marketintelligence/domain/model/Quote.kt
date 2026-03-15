package com.fourdigital.marketintelligence.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class QuoteDataQuality {
    REALTIME_STREAMING,
    REALTIME_SNAPSHOT,
    DELAYED_15MIN,
    DELAYED_30MIN,
    END_OF_DAY,
    CACHED,
    STALE,
    UNKNOWN
}

@Serializable
data class Quote(
    val symbol: String,
    val price: Double,
    val previousClose: Double,
    val open: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val volume: Long,
    val timestamp: Instant,
    val dataQuality: QuoteDataQuality,
    val providerName: String,
    val bid: Double? = null,
    val ask: Double? = null,
    val weekHigh52: Double? = null,
    val weekLow52: Double? = null,
    val marketCap: Double? = null,
    val avgVolume: Long? = null,
    val latencyMs: Long = 0
) {
    val change: Double get() = price - previousClose
    val changePercent: Double get() = if (previousClose != 0.0) (change / previousClose) * 100.0 else 0.0
    val isPositive: Boolean get() = change >= 0
    val spread: Double? get() = if (bid != null && ask != null) ask - bid else null
}

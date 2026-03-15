package com.fourdigital.marketintelligence.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ProviderStatus(
    val name: String,
    val isAvailable: Boolean,
    val latencyMs: Long,
    val lastSuccessfulCall: Instant?,
    val lastError: String?,
    val rateLimitRemaining: Int?,
    val capabilities: Set<ProviderCapability>
)

@Serializable
enum class ProviderCapability {
    REALTIME_QUOTES,
    STREAMING_QUOTES,
    HISTORICAL_DATA,
    INTRADAY_DATA,
    SYMBOL_SEARCH,
    MARKET_HOURS,
    NEWS_SENTIMENT,
    AI_ANALYSIS,
    CRYPTO,
    FOREX,
    COMMODITIES,
    EQUITIES
}

@Serializable
enum class DataProviderType {
    PRIMARY,
    FALLBACK,
    MOCK
}

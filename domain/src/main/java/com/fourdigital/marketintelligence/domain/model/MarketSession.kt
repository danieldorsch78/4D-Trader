package com.fourdigital.marketintelligence.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
enum class MarketStatus {
    OPEN,
    CLOSED,
    PRE_MARKET,
    POST_MARKET,
    HOLIDAY,
    WEEKEND,
    UNKNOWN
}

@Serializable
data class MarketSession(
    val exchange: Exchange,
    val status: MarketStatus,
    val openTime: LocalTime,
    val closeTime: LocalTime,
    val preMarketOpen: LocalTime? = null,
    val postMarketClose: LocalTime? = null,
    val tradingDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    ),
    val nextOpenTimestamp: Instant? = null,
    val nextCloseTimestamp: Instant? = null,
    val holidayName: String? = null,
    val is24x7: Boolean = false
)

@Serializable
data class MarketClock(
    val utcNow: Instant,
    val sessions: Map<String, MarketSession>,
    val berlinTime: String,
    val saoPauloTime: String,
    val newYorkTime: String,
    val utcTime: String
)

@Serializable
data class ExchangeInfo(
    val exchange: Exchange,
    val fullName: String,
    val country: String,
    val timezone: String,
    val currency: String,
    val mic: String = "",
    val website: String = ""
)

package com.fourdigital.marketintelligence.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
enum class AlertType {
    PRICE_ABOVE,
    PRICE_BELOW,
    PERCENT_MOVE,
    CORRELATION_THRESHOLD,
    VOLATILITY_THRESHOLD,
    SIGNAL_CONFIDENCE,
    MARKET_OPEN,
    MARKET_CLOSE,
    DIVERGENCE_DETECTED
}

@Serializable
data class AlertRule(
    val id: String,
    val symbol: String,
    val type: AlertType,
    val threshold: Double,
    val message: String,
    val isEnabled: Boolean = true,
    val isSnoozed: Boolean = false,
    val snoozeUntil: Instant? = null,
    val createdAt: Instant,
    val lastTriggeredAt: Instant? = null,
    val triggerCount: Int = 0,
    val maxTriggers: Int = -1  // -1 = unlimited
)

@Serializable
data class AlertEvent(
    val id: String,
    val ruleId: String,
    val symbol: String,
    val type: AlertType,
    val message: String,
    val triggeredValue: Double,
    val timestamp: Instant,
    val isRead: Boolean = false,
    val isDismissed: Boolean = false
)

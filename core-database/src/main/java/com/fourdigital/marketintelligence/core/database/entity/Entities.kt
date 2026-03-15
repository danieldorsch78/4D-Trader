package com.fourdigital.marketintelligence.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlists")
data class WatchlistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val sortOrder: Int = 0,
    val isDefault: Boolean = false
)

@Entity(tableName = "watchlist_items", primaryKeys = ["watchlistId", "symbol"])
data class WatchlistItemEntity(
    val watchlistId: String,
    val symbol: String,
    val assetName: String,
    val assetClass: String,
    val exchange: String,
    val currency: String,
    val isin: String? = null,
    val sortOrder: Int = 0,
    val notes: String = "",
    val addedTimestamp: Long = 0L
)

@Entity(tableName = "cached_quotes")
data class CachedQuoteEntity(
    @PrimaryKey val symbol: String,
    val price: Double,
    val previousClose: Double,
    val open: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val volume: Long,
    val timestamp: Long,
    val dataQuality: String,
    val providerName: String,
    val bid: Double? = null,
    val ask: Double? = null,
    val weekHigh52: Double? = null,
    val weekLow52: Double? = null,
    val marketCap: Double? = null,
    val avgVolume: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "alert_rules")
data class AlertRuleEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val type: String,
    val threshold: Double,
    val message: String,
    val isEnabled: Boolean = true,
    val isSnoozed: Boolean = false,
    val snoozeUntil: Long? = null,
    val createdAt: Long,
    val lastTriggeredAt: Long? = null,
    val triggerCount: Int = 0,
    val maxTriggers: Int = -1
)

@Entity(tableName = "alert_events")
data class AlertEventEntity(
    @PrimaryKey val id: String,
    val ruleId: String,
    val symbol: String,
    val type: String,
    val message: String,
    val triggeredValue: Double,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isDismissed: Boolean = false
)

@Entity(tableName = "portfolio_positions")
data class PortfolioPositionEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val assetName: String,
    val quantity: Double,
    val avgEntryPrice: Double,
    val currency: String,
    val openedAt: Long,
    val notes: String = ""
)

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey val provider: String,
    val apiKey: String,
    val updatedAt: Long = System.currentTimeMillis()
)

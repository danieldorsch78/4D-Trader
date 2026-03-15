package com.fourdigital.marketintelligence.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Watchlist(
    val id: String,
    val name: String,
    val description: String = "",
    val items: List<WatchlistItem> = emptyList(),
    val sortOrder: Int = 0,
    val isDefault: Boolean = false
)

@Serializable
data class WatchlistItem(
    val symbol: String,
    val asset: Asset,
    val sortOrder: Int = 0,
    val notes: String = "",
    val addedTimestamp: Long = 0L
)

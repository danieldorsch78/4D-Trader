package com.fourdigital.marketintelligence.domain.repository

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.Watchlist
import kotlinx.coroutines.flow.Flow

interface WatchlistRepository {
    fun observeWatchlists(): Flow<List<Watchlist>>
    fun observeWatchlist(id: String): Flow<Watchlist?>
    suspend fun getWatchlist(id: String): DataResult<Watchlist>
    suspend fun createWatchlist(watchlist: Watchlist): DataResult<Unit>
    suspend fun updateWatchlist(watchlist: Watchlist): DataResult<Unit>
    suspend fun deleteWatchlist(id: String): DataResult<Unit>
    suspend fun addSymbolToWatchlist(watchlistId: String, symbol: String): DataResult<Unit>
    suspend fun removeSymbolFromWatchlist(watchlistId: String, symbol: String): DataResult<Unit>
    suspend fun getDefaultWatchlists(): List<Watchlist>
}

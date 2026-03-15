package com.fourdigital.marketintelligence.core.database.dao

import androidx.room.*
import com.fourdigital.marketintelligence.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlists ORDER BY sortOrder")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlists WHERE id = :id")
    fun observeById(id: String): Flow<WatchlistEntity?>

    @Query("SELECT * FROM watchlists WHERE id = :id")
    suspend fun getById(id: String): WatchlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(watchlist: WatchlistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(watchlists: List<WatchlistEntity>)

    @Update
    suspend fun update(watchlist: WatchlistEntity)

    @Query("DELETE FROM watchlists WHERE id = :id")
    suspend fun delete(id: String)

    // Items
    @Query("SELECT * FROM watchlist_items WHERE watchlistId = :watchlistId ORDER BY sortOrder")
    fun observeItems(watchlistId: String): Flow<List<WatchlistItemEntity>>

    @Query("SELECT * FROM watchlist_items WHERE watchlistId = :watchlistId ORDER BY sortOrder")
    suspend fun getItems(watchlistId: String): List<WatchlistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WatchlistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<WatchlistItemEntity>)

    @Query("DELETE FROM watchlist_items WHERE watchlistId = :watchlistId AND symbol = :symbol")
    suspend fun deleteItem(watchlistId: String, symbol: String)

    @Query("DELETE FROM watchlist_items WHERE watchlistId = :watchlistId")
    suspend fun deleteAllItems(watchlistId: String)
}

@Dao
interface CachedQuoteDao {
    @Query("SELECT * FROM cached_quotes WHERE symbol = :symbol")
    suspend fun get(symbol: String): CachedQuoteEntity?

    @Query("SELECT * FROM cached_quotes WHERE symbol IN (:symbols)")
    suspend fun getAll(symbols: List<String>): List<CachedQuoteEntity>

    @Query("SELECT * FROM cached_quotes WHERE symbol IN (:symbols)")
    fun observeAll(symbols: List<String>): Flow<List<CachedQuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quote: CachedQuoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotes: List<CachedQuoteEntity>)

    @Query("DELETE FROM cached_quotes WHERE updatedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alert_rules ORDER BY createdAt DESC")
    fun observeRules(): Flow<List<AlertRuleEntity>>

    @Query("SELECT * FROM alert_events ORDER BY timestamp DESC")
    fun observeEvents(): Flow<List<AlertEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AlertRuleEntity)

    @Update
    suspend fun updateRule(rule: AlertRuleEntity)

    @Query("DELETE FROM alert_rules WHERE id = :id")
    suspend fun deleteRule(id: String)

    @Query("SELECT * FROM alert_rules WHERE id = :id")
    suspend fun getRuleById(id: String): AlertRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: AlertEventEntity)

    @Query("UPDATE alert_events SET isDismissed = 1 WHERE id = :id")
    suspend fun dismissEvent(id: String)
}

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio_positions ORDER BY openedAt DESC")
    fun observeAll(): Flow<List<PortfolioPositionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(position: PortfolioPositionEntity)

    @Update
    suspend fun update(position: PortfolioPositionEntity)

    @Query("DELETE FROM portfolio_positions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM portfolio_positions")
    suspend fun getAll(): List<PortfolioPositionEntity>
}

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys WHERE provider = :provider")
    suspend fun get(provider: String): ApiKeyEntity?

    @Query("SELECT * FROM api_keys")
    fun observeAll(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys")
    suspend fun getAll(): List<ApiKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apiKey: ApiKeyEntity)

    @Query("DELETE FROM api_keys WHERE provider = :provider")
    suspend fun delete(provider: String)
}

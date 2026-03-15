package com.fourdigital.marketintelligence.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.fourdigital.marketintelligence.core.database.dao.*
import com.fourdigital.marketintelligence.core.database.entity.*

@Database(
    entities = [
        WatchlistEntity::class,
        WatchlistItemEntity::class,
        CachedQuoteEntity::class,
        AlertRuleEntity::class,
        AlertEventEntity::class,
        PortfolioPositionEntity::class,
        ApiKeyEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun cachedQuoteDao(): CachedQuoteDao
    abstract fun alertDao(): AlertDao
    abstract fun portfolioDao(): PortfolioDao
    abstract fun apiKeyDao(): ApiKeyDao
}

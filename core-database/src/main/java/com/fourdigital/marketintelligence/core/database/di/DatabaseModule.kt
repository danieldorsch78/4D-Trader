package com.fourdigital.marketintelligence.core.database.di

import android.content.Context
import androidx.room.Room
import com.fourdigital.marketintelligence.core.database.AppDatabase
import com.fourdigital.marketintelligence.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "market_intelligence_db"
        ).fallbackToDestructiveMigration()
         .fallbackToDestructiveMigrationOnDowngrade()
         .build()
    }

    @Provides fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()
    @Provides fun provideCachedQuoteDao(db: AppDatabase): CachedQuoteDao = db.cachedQuoteDao()
    @Provides fun provideAlertDao(db: AppDatabase): AlertDao = db.alertDao()
    @Provides fun providePortfolioDao(db: AppDatabase): PortfolioDao = db.portfolioDao()
    @Provides fun provideApiKeyDao(db: AppDatabase): ApiKeyDao = db.apiKeyDao()
}

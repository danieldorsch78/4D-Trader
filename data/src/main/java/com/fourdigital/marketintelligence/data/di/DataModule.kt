package com.fourdigital.marketintelligence.data.di

import com.fourdigital.marketintelligence.data.mock.MockMarketHoursProvider
import com.fourdigital.marketintelligence.data.provider.FinnhubProvider
import com.fourdigital.marketintelligence.data.provider.RealMarketDataProvider
import com.fourdigital.marketintelligence.data.provider.RealStreamingProvider
import com.fourdigital.marketintelligence.data.repository.*
import com.fourdigital.marketintelligence.domain.provider.*
import com.fourdigital.marketintelligence.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindMarketDataProvider(impl: RealMarketDataProvider): MarketDataProvider

    @Binds
    @Singleton
    abstract fun bindStreamingProvider(impl: RealStreamingProvider): StreamingQuoteProvider

    @Binds
    @Singleton
    abstract fun bindHistoricalProvider(impl: RealMarketDataProvider): HistoricalDataProvider

    @Binds
    @Singleton
    abstract fun bindMarketHoursProvider(impl: MockMarketHoursProvider): MarketHoursProvider

    @Binds
    @Singleton
    abstract fun bindSymbolSearchProvider(impl: FinnhubProvider): SymbolSearchProvider

    @Binds
    @Singleton
    abstract fun bindSymbolSearchRepository(impl: SymbolSearchRepositoryImpl): SymbolSearchRepository

    @Binds
    @Singleton
    abstract fun bindQuoteRepository(impl: QuoteRepositoryImpl): QuoteRepository

    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(impl: WatchlistRepositoryImpl): WatchlistRepository

    @Binds
    @Singleton
    abstract fun bindHistoricalRepository(impl: HistoricalRepositoryImpl): HistoricalRepository

    @Binds
    @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}

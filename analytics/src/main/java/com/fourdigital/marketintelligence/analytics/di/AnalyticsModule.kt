package com.fourdigital.marketintelligence.analytics.di

import com.fourdigital.marketintelligence.analytics.signal.SignalEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * CorrelationEngine and CorrelationMatrixBuilder are provided via their
 * @Inject constructors (with @Singleton scope). Only SignalEngine needs
 * an explicit @Provides since it has no @Inject constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideSignalEngine(): SignalEngine = SignalEngine()
}

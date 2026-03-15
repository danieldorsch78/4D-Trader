package com.fourdigital.marketintelligence.domain.provider

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.HistoricalBar
import com.fourdigital.marketintelligence.domain.model.TimeFrame
import kotlinx.datetime.Instant

/**
 * Provider for historical OHLCV price data across timeframes.
 */
interface HistoricalDataProvider {

    val providerName: String

    suspend fun getHistoricalBars(
        symbol: String,
        timeFrame: TimeFrame,
        from: Instant,
        to: Instant
    ): DataResult<List<HistoricalBar>>

    suspend fun getDailyBars(
        symbol: String,
        days: Int
    ): DataResult<List<HistoricalBar>>
}

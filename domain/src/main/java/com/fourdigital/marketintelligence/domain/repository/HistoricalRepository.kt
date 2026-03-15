package com.fourdigital.marketintelligence.domain.repository

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.HistoricalBar
import com.fourdigital.marketintelligence.domain.model.TimeFrame
import kotlinx.datetime.Instant

interface HistoricalRepository {
    suspend fun getHistoricalData(
        symbol: String,
        timeFrame: TimeFrame,
        from: Instant,
        to: Instant
    ): DataResult<List<HistoricalBar>>

    suspend fun getDailyHistory(symbol: String, days: Int): DataResult<List<HistoricalBar>>

    suspend fun getCachedHistory(symbol: String, timeFrame: TimeFrame): List<HistoricalBar>
}

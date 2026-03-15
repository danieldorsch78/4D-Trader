package com.fourdigital.marketintelligence.data.repository

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.HistoricalBar
import com.fourdigital.marketintelligence.domain.model.TimeFrame
import com.fourdigital.marketintelligence.domain.provider.HistoricalDataProvider
import com.fourdigital.marketintelligence.domain.repository.HistoricalRepository
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoricalRepositoryImpl @Inject constructor(
    private val historicalProvider: HistoricalDataProvider
) : HistoricalRepository {

    private val cache = mutableMapOf<String, List<HistoricalBar>>()

    override suspend fun getHistoricalData(
        symbol: String,
        timeFrame: TimeFrame,
        from: Instant,
        to: Instant
    ): DataResult<List<HistoricalBar>> {
        val result = historicalProvider.getHistoricalBars(symbol, timeFrame, from, to)
        if (result is DataResult.Success) {
            cache["${symbol}_${timeFrame.name}"] = result.data
        }
        return result
    }

    override suspend fun getDailyHistory(symbol: String, days: Int): DataResult<List<HistoricalBar>> {
        val result = historicalProvider.getDailyBars(symbol, days)
        if (result is DataResult.Success) {
            cache["${symbol}_DAILY"] = result.data
        }
        return result
    }

    override suspend fun getCachedHistory(symbol: String, timeFrame: TimeFrame): List<HistoricalBar> =
        cache["${symbol}_${timeFrame.name}"].orEmpty()
}

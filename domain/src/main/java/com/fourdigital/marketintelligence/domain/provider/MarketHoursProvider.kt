package com.fourdigital.marketintelligence.domain.provider

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.Exchange
import com.fourdigital.marketintelligence.domain.model.MarketSession

/**
 * Provider for market hours, session info, and trading calendar.
 */
interface MarketHoursProvider {

    val providerName: String

    suspend fun getMarketSession(exchange: Exchange): DataResult<MarketSession>

    suspend fun getAllSessions(): DataResult<Map<Exchange, MarketSession>>

    suspend fun isMarketOpen(exchange: Exchange): DataResult<Boolean>
}

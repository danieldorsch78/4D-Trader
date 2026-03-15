package com.fourdigital.marketintelligence.domain.provider

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.Quote

/**
 * Abstraction for fetching real-time or near-real-time market quotes.
 * Implementations may use REST polling, WebSocket streaming, or mock data.
 */
interface MarketDataProvider {

    val providerName: String

    suspend fun getQuote(symbol: String): DataResult<Quote>

    suspend fun getQuotes(symbols: List<String>): DataResult<List<Quote>>

    suspend fun isAvailable(): Boolean

    suspend fun getRateLimitRemaining(): Int?
}

package com.fourdigital.marketintelligence.domain.repository

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.Quote
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    fun observeQuotes(symbols: List<String>): Flow<Map<String, Quote>>
    fun observeQuote(symbol: String): Flow<Quote?>
    suspend fun refreshQuote(symbol: String): DataResult<Quote>
    suspend fun refreshQuotes(symbols: List<String>): DataResult<List<Quote>>
    suspend fun getCachedQuote(symbol: String): Quote?
}

package com.fourdigital.marketintelligence.data.repository

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.Quote
import com.fourdigital.marketintelligence.domain.provider.MarketDataProvider
import com.fourdigital.marketintelligence.domain.provider.StreamingQuoteProvider
import com.fourdigital.marketintelligence.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuoteRepositoryImpl @Inject constructor(
    private val marketDataProvider: MarketDataProvider,
    private val streamingProvider: StreamingQuoteProvider
) : QuoteRepository {

    private val quoteCache = MutableStateFlow<Map<String, Quote>>(emptyMap())

    override fun observeQuotes(symbols: List<String>): Flow<Map<String, Quote>> =
        quoteCache.map { cache ->
            cache.filterKeys { it in symbols }
        }

    override fun observeQuote(symbol: String): Flow<Quote?> =
        quoteCache.map { it[symbol] }

    override suspend fun refreshQuote(symbol: String): DataResult<Quote> {
        val result = marketDataProvider.getQuote(symbol)
        if (result is DataResult.Success) {
            quoteCache.update { it + (symbol to result.data) }
        }
        return result
    }

    override suspend fun refreshQuotes(symbols: List<String>): DataResult<List<Quote>> {
        val result = marketDataProvider.getQuotes(symbols)
        if (result is DataResult.Success) {
            quoteCache.update { cache ->
                cache + result.data.associateBy { it.symbol }
            }
        }
        return result
    }

    override suspend fun getCachedQuote(symbol: String): Quote? =
        quoteCache.value[symbol]
}

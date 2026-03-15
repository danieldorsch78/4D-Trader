package com.fourdigital.marketintelligence.data.mock

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.Quote
import com.fourdigital.marketintelligence.domain.provider.MarketDataProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockMarketDataProvider @Inject constructor() : MarketDataProvider {

    override val providerName: String = "MockProvider"

    override suspend fun getQuote(symbol: String): DataResult<Quote> {
        return DataResult.success(MockQuoteGenerator.generateQuote(symbol))
    }

    override suspend fun getQuotes(symbols: List<String>): DataResult<List<Quote>> {
        return DataResult.success(MockQuoteGenerator.generateQuotes(symbols))
    }

    override suspend fun isAvailable(): Boolean = true

    override suspend fun getRateLimitRemaining(): Int = Int.MAX_VALUE
}

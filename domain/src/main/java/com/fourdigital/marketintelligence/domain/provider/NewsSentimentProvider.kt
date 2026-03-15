package com.fourdigital.marketintelligence.domain.provider

/**
 * Placeholder for news sentiment analysis integration.
 * To be implemented when a news data provider is selected.
 */
interface NewsSentimentProvider {

    val providerName: String

    suspend fun getSentiment(symbol: String): SentimentResult

    suspend fun getHeadlines(symbol: String, limit: Int = 10): List<HeadlineItem>
}

data class SentimentResult(
    val symbol: String,
    val score: Double,    // -1.0 to +1.0
    val label: String,    // "Bullish", "Bearish", "Neutral"
    val articleCount: Int,
    val providerName: String
)

data class HeadlineItem(
    val title: String,
    val source: String,
    val url: String,
    val sentiment: Double,
    val timestamp: Long
)

package com.fourdigital.marketintelligence.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Brapi.dev — FREE tier for Brazilian B3 stocks.
 * Base URL: https://brapi.dev/api/
 * Covers: Bovespa, IBOVESPA, Brazilian equities + ETFs.
 */
interface BrapiApi {

    @GET("quote/{symbols}")
    suspend fun getQuotes(
        @retrofit2.http.Path("symbols") symbols: String, // comma-separated: "PETR4,VALE3"
        @Query("token") token: String,
        @Query("range") range: String? = null, // "1d","5d","1mo","3mo","6mo","1y","2y","5y","10y","ytd","max"
        @Query("interval") interval: String? = null, // "1d","1wk","1mo"
        @Query("fundamental") fundamental: Boolean = false
    ): BrapiResponse

    @GET("available")
    suspend fun getAvailableStocks(
        @Query("token") token: String,
        @Query("search") search: String? = null
    ): BrapiAvailableResponse
}

@Serializable
data class BrapiResponse(
    val results: List<BrapiQuote> = emptyList(),
    val requestedAt: String? = null,
    val took: String? = null
)

@Serializable
data class BrapiQuote(
    val symbol: String = "",
    val shortName: String? = null,
    val longName: String? = null,
    val currency: String? = null,
    val regularMarketPrice: Double = 0.0,
    val regularMarketDayHigh: Double? = null,
    val regularMarketDayLow: Double? = null,
    val regularMarketDayRange: String? = null,
    val regularMarketChange: Double? = null,
    val regularMarketChangePercent: Double? = null,
    val regularMarketTime: String? = null,
    val regularMarketOpen: Double? = null,
    val regularMarketVolume: Long? = null,
    val regularMarketPreviousClose: Double? = null,
    @SerialName("fiftyTwoWeekHigh") val weekHigh52: Double? = null,
    @SerialName("fiftyTwoWeekLow") val weekLow52: Double? = null,
    val marketCap: Long? = null,
    val averageDailyVolume3Month: Long? = null,
    val averageDailyVolume10Day: Long? = null,
    val historicalDataPrice: List<BrapiHistoricalPrice>? = null
)

@Serializable
data class BrapiHistoricalPrice(
    val date: Long = 0,
    val open: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val close: Double = 0.0,
    val volume: Long = 0,
    val adjustedClose: Double = 0.0
)

@Serializable
data class BrapiAvailableResponse(
    val stocks: List<String> = emptyList()
)

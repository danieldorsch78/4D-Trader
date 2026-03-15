package com.fourdigital.marketintelligence.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Finnhub API — FREE tier: 60 calls/min, real-time WebSocket.
 * Base URL: https://finnhub.io/api/v1/
 * Covers: US stocks, DAX, global indices, crypto, forex.
 */
interface FinnhubApi {

    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Header("X-Finnhub-Token") token: String
    ): FinnhubQuote

    @GET("stock/candle")
    suspend fun getCandles(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String, // 1, 5, 15, 30, 60, D, W, M
        @Query("from") from: Long,   // UNIX timestamp
        @Query("to") to: Long,       // UNIX timestamp
        @Header("X-Finnhub-Token") token: String
    ): FinnhubCandles

    @GET("search")
    suspend fun symbolSearch(
        @Query("q") query: String,
        @Header("X-Finnhub-Token") token: String
    ): FinnhubSearchResult

    @GET("stock/profile2")
    suspend fun companyProfile(
        @Query("symbol") symbol: String,
        @Header("X-Finnhub-Token") token: String
    ): FinnhubCompanyProfile

    @GET("crypto/candle")
    suspend fun getCryptoCandles(
        @Query("symbol") symbol: String,   // e.g. "BINANCE:BTCUSDT"
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Header("X-Finnhub-Token") token: String
    ): FinnhubCandles

    @GET("forex/candle")
    suspend fun getForexCandles(
        @Query("symbol") symbol: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Header("X-Finnhub-Token") token: String
    ): FinnhubCandles

    @GET("news")
    suspend fun getMarketNews(
        @Query("category") category: String = "general",
        @Query("minId") minId: Long = 0,
        @Header("X-Finnhub-Token") token: String
    ): List<FinnhubNews>

    @GET("company-news")
    suspend fun getCompanyNews(
        @Query("symbol") symbol: String,
        @Query("from") from: String,  // YYYY-MM-DD
        @Query("to") to: String,
        @Header("X-Finnhub-Token") token: String
    ): List<FinnhubNews>
}

@Serializable
data class FinnhubQuote(
    @SerialName("c") val current: Double = 0.0,
    @SerialName("d") val change: Double? = null,
    @SerialName("dp") val percentChange: Double? = null,
    @SerialName("h") val high: Double = 0.0,
    @SerialName("l") val low: Double = 0.0,
    @SerialName("o") val open: Double = 0.0,
    @SerialName("pc") val previousClose: Double = 0.0,
    @SerialName("t") val timestamp: Long = 0
)

@Serializable
data class FinnhubCandles(
    @SerialName("c") val close: List<Double>? = null,
    @SerialName("h") val high: List<Double>? = null,
    @SerialName("l") val low: List<Double>? = null,
    @SerialName("o") val open: List<Double>? = null,
    @SerialName("v") val volume: List<Double>? = null,
    @SerialName("t") val timestamp: List<Long>? = null,
    @SerialName("s") val status: String = "ok"
)

@Serializable
data class FinnhubSearchResult(
    val count: Int = 0,
    val result: List<FinnhubSearchItem> = emptyList()
)

@Serializable
data class FinnhubSearchItem(
    val description: String = "",
    val displaySymbol: String = "",
    val symbol: String = "",
    val type: String = ""
)

@Serializable
data class FinnhubCompanyProfile(
    val country: String? = null,
    val currency: String? = null,
    val exchange: String? = null,
    @SerialName("finnhubIndustry") val industry: String? = null,
    val ipo: String? = null,
    val logo: String? = null,
    @SerialName("marketCapitalization") val marketCap: Double? = null,
    val name: String? = null,
    val ticker: String? = null,
    val weburl: String? = null
)

@Serializable
data class FinnhubNews(
    val category: String = "",
    val datetime: Long = 0,
    val headline: String = "",
    val id: Long = 0,
    val image: String = "",
    val related: String = "",
    val source: String = "",
    val summary: String = "",
    val url: String = ""
)

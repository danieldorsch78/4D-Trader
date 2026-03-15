package com.fourdigital.marketintelligence.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * CoinGecko API v3 — FREE, no API key required.
 * Base URL: https://api.coingecko.com/api/v3/
 * Rate limit: 30 calls/min
 */
interface CoinGeckoApi {

    @GET("simple/price")
    suspend fun getSimplePrice(
        @Query("ids") ids: String, // "bitcoin,ethereum"
        @Query("vs_currencies") vsCurrencies: String = "usd,eur,brl",
        @Query("include_24hr_vol") include24hVol: Boolean = true,
        @Query("include_24hr_change") include24hChange: Boolean = true,
        @Query("include_market_cap") includeMarketCap: Boolean = true,
        @Query("include_last_updated_at") includeLastUpdated: Boolean = true
    ): Map<String, CoinGeckoPriceEntry>

    @GET("coins/markets")
    suspend fun getCoinMarkets(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("ids") ids: String? = null,
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 50,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = false,
        @Query("price_change_percentage") priceChangePercentage: String = "1h,24h,7d"
    ): List<CoinGeckoMarket>

    @GET("coins/{id}/market_chart")
    suspend fun getMarketChart(
        @retrofit2.http.Path("id") id: String,
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("days") days: Int = 30,
        @Query("interval") interval: String? = null // "daily"
    ): CoinGeckoChart

    @GET("search")
    suspend fun search(
        @Query("query") query: String
    ): CoinGeckoSearchResult
}

@Serializable
data class CoinGeckoPriceEntry(
    val usd: Double? = null,
    val eur: Double? = null,
    val brl: Double? = null,
    @SerialName("usd_24h_vol") val usd24hVol: Double? = null,
    @SerialName("usd_24h_change") val usd24hChange: Double? = null,
    @SerialName("usd_market_cap") val usdMarketCap: Double? = null,
    @SerialName("last_updated_at") val lastUpdatedAt: Long? = null
)

@Serializable
data class CoinGeckoMarket(
    val id: String,
    val symbol: String,
    val name: String,
    @SerialName("current_price") val currentPrice: Double,
    @SerialName("market_cap") val marketCap: Double? = null,
    @SerialName("total_volume") val totalVolume: Double? = null,
    @SerialName("high_24h") val high24h: Double? = null,
    @SerialName("low_24h") val low24h: Double? = null,
    @SerialName("price_change_24h") val priceChange24h: Double? = null,
    @SerialName("price_change_percentage_24h") val priceChangePercentage24h: Double? = null,
    @SerialName("price_change_percentage_1h_in_currency") val priceChangePercentage1h: Double? = null,
    @SerialName("price_change_percentage_7d_in_currency") val priceChangePercentage7d: Double? = null,
    @SerialName("circulating_supply") val circulatingSupply: Double? = null,
    @SerialName("total_supply") val totalSupply: Double? = null,
    @SerialName("ath") val allTimeHigh: Double? = null,
    @SerialName("ath_change_percentage") val athChangePercentage: Double? = null,
    @SerialName("atl") val allTimeLow: Double? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
    val sparkline_in_7d: SparklineData? = null
)

@Serializable
data class SparklineData(
    val price: List<Double>? = null
)

@Serializable
data class CoinGeckoChart(
    val prices: List<List<Double>>, // [[timestamp_ms, price], ...]
    val market_caps: List<List<Double>>? = null,
    val total_volumes: List<List<Double>>? = null
)

@Serializable
data class CoinGeckoSearchResult(
    val coins: List<CoinGeckoSearchCoin> = emptyList()
)

@Serializable
data class CoinGeckoSearchCoin(
    val id: String,
    val name: String,
    val symbol: String,
    @SerialName("market_cap_rank") val marketCapRank: Int? = null,
    val thumb: String? = null
)

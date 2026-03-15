package com.fourdigital.marketintelligence.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AssetClass {
    EQUITY,
    INDEX,
    ETF,
    COMMODITY,
    CRYPTO,
    FOREX,
    BOND,
    UNKNOWN
}

@Serializable
enum class Exchange(val displayName: String, val timezone: String) {
    XETRA("Xetra", "Europe/Berlin"),
    FRANKFURT("Frankfurt", "Europe/Berlin"),
    B3("B3 - Bovespa", "America/Sao_Paulo"),
    NYSE("NYSE", "America/New_York"),
    NASDAQ("NASDAQ", "America/New_York"),
    CME("CME", "America/Chicago"),
    COMEX("COMEX", "America/New_York"),
    NYMEX("NYMEX", "America/New_York"),
    LSE("LSE", "Europe/London"),
    TSE("TSE", "Asia/Tokyo"),
    HKEX("HKEX", "Asia/Hong_Kong"),
    EURONEXT("Euronext", "Europe/Paris"),
    ASX("ASX", "Australia/Sydney"),
    SSE("SSE", "Asia/Shanghai"),
    BSE("BSE", "Asia/Kolkata"),
    FOREX_GLOBAL("Forex (24/5)", "UTC"),
    CRYPTO_GLOBAL("Crypto (24/7)", "UTC"),
    UNKNOWN("Unknown", "UTC")
}

@Serializable
data class Asset(
    val symbol: String,
    val name: String,
    val assetClass: AssetClass,
    val exchange: Exchange,
    val currency: String,
    val isin: String? = null,
    val providerSymbol: String = symbol,
    val metadata: Map<String, String> = emptyMap()
) {
    val displayId: String get() = "$symbol (${exchange.displayName})"
}

package com.fourdigital.marketintelligence.domain.provider

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.Asset

/**
 * Provider for searching symbols and asset metadata.
 */
interface SymbolSearchProvider {

    val providerName: String

    suspend fun searchSymbols(query: String, assetClassFilter: String? = null): DataResult<List<Asset>>

    suspend fun getAssetDetails(symbol: String): DataResult<Asset>
}

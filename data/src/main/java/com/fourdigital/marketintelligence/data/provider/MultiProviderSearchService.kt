package com.fourdigital.marketintelligence.data.provider

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.Asset
import com.fourdigital.marketintelligence.domain.provider.SymbolSearchProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates search results from Finnhub + CoinGecko + DemoAssets local registry.
 * Returns de-duplicated results sorted by relevance.
 */
@Singleton
class MultiProviderSearchService @Inject constructor(
    private val finnhubProvider: FinnhubProvider,
    private val coinGeckoProvider: CoinGeckoProvider,
    private val demoAssetsSearchProvider: DemoAssetsSearchProvider
) {
    suspend fun search(query: String): List<Asset> = supervisorScope {
        if (query.isBlank() || query.length < 2) return@supervisorScope emptyList()

        val localDeferred = async { demoAssetsSearchProvider.search(query) }
        val finnhubDeferred = async {
            when (val r = finnhubProvider.searchSymbols(query)) {
                is DataResult.Success -> r.data
                else -> emptyList()
            }
        }
        val cryptoDeferred = async {
            when (val r = coinGeckoProvider.searchCoins(query)) {
                is DataResult.Success -> r.data
                else -> emptyList()
            }
        }

        val local = try { localDeferred.await() } catch (_: Exception) { emptyList() }
        val finnhub = try { finnhubDeferred.await() } catch (_: Exception) { emptyList() }
        val crypto = try { cryptoDeferred.await() } catch (_: Exception) { emptyList() }

        // Local results first (known assets), then API results, de-duplicated by symbol
        val seen = mutableSetOf<String>()
        val merged = mutableListOf<Asset>()
        for (asset in local + crypto + finnhub) {
            if (seen.add(asset.symbol)) {
                merged.add(asset)
            }
        }
        merged
    }
}

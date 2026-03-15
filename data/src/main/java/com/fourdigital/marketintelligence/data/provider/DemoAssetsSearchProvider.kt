package com.fourdigital.marketintelligence.data.provider

import com.fourdigital.marketintelligence.data.mock.DemoAssets
import com.fourdigital.marketintelligence.domain.model.Asset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Searches the local DemoAssets registry for matching symbols/names.
 * No network call — instant results for all pre-configured assets.
 */
@Singleton
class DemoAssetsSearchProvider @Inject constructor() {

    fun search(query: String): List<Asset> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        return DemoAssets.allAssets.filter { asset ->
            asset.symbol.lowercase().contains(q) ||
                    asset.name.lowercase().contains(q)
        }.take(20)
    }
}

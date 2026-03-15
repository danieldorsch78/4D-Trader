package com.fourdigital.marketintelligence.data.repository

import com.fourdigital.marketintelligence.data.provider.MultiProviderSearchService
import com.fourdigital.marketintelligence.domain.model.Asset
import com.fourdigital.marketintelligence.domain.repository.SymbolSearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SymbolSearchRepositoryImpl @Inject constructor(
    private val searchService: MultiProviderSearchService
) : SymbolSearchRepository {

    override suspend fun search(query: String): List<Asset> =
        searchService.search(query)
}

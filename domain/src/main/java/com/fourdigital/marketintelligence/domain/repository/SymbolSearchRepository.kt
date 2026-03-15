package com.fourdigital.marketintelligence.domain.repository

import com.fourdigital.marketintelligence.domain.model.Asset

/**
 * Repository for searching symbols across all configured providers.
 */
interface SymbolSearchRepository {
    suspend fun search(query: String): List<Asset>
}

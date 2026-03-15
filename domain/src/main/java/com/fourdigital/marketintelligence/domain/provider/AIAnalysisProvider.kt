package com.fourdigital.marketintelligence.domain.provider

import com.fourdigital.marketintelligence.core.common.result.DataResult
import com.fourdigital.marketintelligence.domain.model.SignalAnalysis

/**
 * Provider abstraction for AI-assisted analysis.
 * Layer 1: Local rule-based engine
 * Layer 2: Statistical scoring engine
 * Layer 3: Optional remote LLM endpoint
 */
interface AIAnalysisProvider {

    val providerName: String
    val isLocalOnly: Boolean

    suspend fun analyzeAsset(
        symbol: String,
        context: AnalysisContext
    ): DataResult<SignalAnalysis>

    suspend fun analyzeMultiple(
        symbols: List<String>,
        context: AnalysisContext
    ): DataResult<List<SignalAnalysis>>
}

data class AnalysisContext(
    val riskProfile: String = "balanced",
    val includeCorrelation: Boolean = true,
    val includeVolatility: Boolean = true,
    val maxDataAgeMs: Long = 300_000,  // 5 minutes
    val locale: String = "en"
)

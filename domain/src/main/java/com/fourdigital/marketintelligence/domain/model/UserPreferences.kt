package com.fourdigital.marketintelligence.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class RiskProfile {
    CONSERVATIVE,
    BALANCED,
    AGGRESSIVE
}

@Serializable
enum class AIProviderMode {
    GITHUB,
    OPENAI,
    BOTH
}

@Serializable
data class UserPreferences(
    val theme: AppTheme = AppTheme.DARK,
    val defaultMarket: String = "DAX",
    val defaultWatchlistId: String = "dax_core",
    val refreshIntervalSeconds: Int = 30,
    val streamingEnabled: Boolean = true,
    val analyticsWindowShort: Int = 20,
    val analyticsWindowMedium: Int = 60,
    val analyticsWindowLong: Int = 120,
    val riskProfile: RiskProfile = RiskProfile.BALANCED,
    val notificationsEnabled: Boolean = true,
    val alertSoundEnabled: Boolean = true,
    val providerType: DataProviderType = DataProviderType.PRIMARY,
    val developerMode: Boolean = false,
    val showDebugPanel: Boolean = false,
    // API keys (stored separately in Room, but tracked here for UI)
    val finnhubKeyConfigured: Boolean = false,
    val brapiKeyConfigured: Boolean = false,
    val openAiKeyConfigured: Boolean = false,
    // GitHub
    val githubUsername: String? = null,
    // AI Configuration
    val aiProviderMode: AIProviderMode = AIProviderMode.BOTH,
    val selectedAIModel: String = "gpt-4o-mini",
    val aiAutoRefreshEnabled: Boolean = true,
    val aiAutoRefreshIntervalSeconds: Int = 30,
    val targetReturnPercent: Double = 10.0,
    // Prediction horizons
    val showWeeklyPredictions: Boolean = true,
    val showMonthlyPredictions: Boolean = true,
    val showQuarterlyPredictions: Boolean = true,
    val showYearlyPredictions: Boolean = true,
    // Signal center
    val maxSignalAssets: Int = 50,
    val signalAutoRefresh: Boolean = true,
    val signalRefreshIntervalSeconds: Int = 30,
    // Cross-asset
    val crossAssetAutoRefresh: Boolean = true,
    val crossAssetRefreshIntervalSeconds: Int = 20
)

@Serializable
enum class AppTheme {
    DARK,
    LIGHT,
    SYSTEM
}

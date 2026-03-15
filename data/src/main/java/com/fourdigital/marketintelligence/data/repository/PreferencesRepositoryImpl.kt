package com.fourdigital.marketintelligence.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.fourdigital.marketintelligence.domain.model.AppTheme
import com.fourdigital.marketintelligence.domain.model.AIProviderMode
import com.fourdigital.marketintelligence.domain.model.DataProviderType
import com.fourdigital.marketintelligence.domain.model.RiskProfile
import com.fourdigital.marketintelligence.domain.model.UserPreferences
import com.fourdigital.marketintelligence.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PreferencesRepository {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DEFAULT_MARKET = stringPreferencesKey("default_market")
        val DEFAULT_WATCHLIST_ID = stringPreferencesKey("default_watchlist_id")
        val REFRESH_INTERVAL = intPreferencesKey("refresh_interval_seconds")
        val STREAMING_ENABLED = booleanPreferencesKey("streaming_enabled")
        val ANALYTICS_SHORT = intPreferencesKey("analytics_window_short")
        val ANALYTICS_MEDIUM = intPreferencesKey("analytics_window_medium")
        val ANALYTICS_LONG = intPreferencesKey("analytics_window_long")
        val RISK_PROFILE = stringPreferencesKey("risk_profile")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ALERT_SOUND_ENABLED = booleanPreferencesKey("alert_sound_enabled")
        val PROVIDER_TYPE = stringPreferencesKey("provider_type")
        val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
        val SHOW_DEBUG_PANEL = booleanPreferencesKey("show_debug_panel")
        val FINNHUB_KEY_CONFIGURED = booleanPreferencesKey("finnhub_key_configured")
        val BRAPI_KEY_CONFIGURED = booleanPreferencesKey("brapi_key_configured")
        val OPENAI_KEY_CONFIGURED = booleanPreferencesKey("openai_key_configured")
        val GITHUB_USERNAME = stringPreferencesKey("github_username")
        val AI_PROVIDER_MODE = stringPreferencesKey("ai_provider_mode")
        val SELECTED_AI_MODEL = stringPreferencesKey("selected_ai_model")
        val AI_AUTO_REFRESH_ENABLED = booleanPreferencesKey("ai_auto_refresh_enabled")
        val AI_AUTO_REFRESH_INTERVAL = intPreferencesKey("ai_auto_refresh_interval_seconds")
        val TARGET_RETURN_PERCENT = doublePreferencesKey("target_return_percent")
        val SHOW_WEEKLY_PREDICTIONS = booleanPreferencesKey("show_weekly_predictions")
        val SHOW_MONTHLY_PREDICTIONS = booleanPreferencesKey("show_monthly_predictions")
        val SHOW_QUARTERLY_PREDICTIONS = booleanPreferencesKey("show_quarterly_predictions")
        val SHOW_YEARLY_PREDICTIONS = booleanPreferencesKey("show_yearly_predictions")
        val MAX_SIGNAL_ASSETS = intPreferencesKey("max_signal_assets")
        val SIGNAL_AUTO_REFRESH = booleanPreferencesKey("signal_auto_refresh")
        val SIGNAL_REFRESH_INTERVAL = intPreferencesKey("signal_refresh_interval_seconds")
        val CROSS_ASSET_AUTO_REFRESH = booleanPreferencesKey("cross_asset_auto_refresh")
        val CROSS_ASSET_REFRESH_INTERVAL = intPreferencesKey("cross_asset_refresh_interval_seconds")
    }

    override fun observePreferences(): Flow<UserPreferences> =
        context.dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs -> prefs.toUserPreferences() }

    override suspend fun getPreferences(): UserPreferences =
        context.dataStore.data.first().toUserPreferences()

    override suspend fun updatePreferences(preferences: UserPreferences) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME] = preferences.theme.name
            prefs[Keys.DEFAULT_MARKET] = preferences.defaultMarket
            prefs[Keys.DEFAULT_WATCHLIST_ID] = preferences.defaultWatchlistId
            prefs[Keys.REFRESH_INTERVAL] = preferences.refreshIntervalSeconds
            prefs[Keys.STREAMING_ENABLED] = preferences.streamingEnabled
            prefs[Keys.ANALYTICS_SHORT] = preferences.analyticsWindowShort
            prefs[Keys.ANALYTICS_MEDIUM] = preferences.analyticsWindowMedium
            prefs[Keys.ANALYTICS_LONG] = preferences.analyticsWindowLong
            prefs[Keys.RISK_PROFILE] = preferences.riskProfile.name
            prefs[Keys.NOTIFICATIONS_ENABLED] = preferences.notificationsEnabled
            prefs[Keys.ALERT_SOUND_ENABLED] = preferences.alertSoundEnabled
            prefs[Keys.PROVIDER_TYPE] = preferences.providerType.name
            prefs[Keys.DEVELOPER_MODE] = preferences.developerMode
            prefs[Keys.SHOW_DEBUG_PANEL] = preferences.showDebugPanel
            prefs[Keys.FINNHUB_KEY_CONFIGURED] = preferences.finnhubKeyConfigured
            prefs[Keys.BRAPI_KEY_CONFIGURED] = preferences.brapiKeyConfigured
            prefs[Keys.OPENAI_KEY_CONFIGURED] = preferences.openAiKeyConfigured
            preferences.githubUsername?.let { prefs[Keys.GITHUB_USERNAME] = it }
                ?: prefs.remove(Keys.GITHUB_USERNAME)
            prefs[Keys.AI_PROVIDER_MODE] = preferences.aiProviderMode.name
            prefs[Keys.SELECTED_AI_MODEL] = preferences.selectedAIModel
            prefs[Keys.AI_AUTO_REFRESH_ENABLED] = preferences.aiAutoRefreshEnabled
            prefs[Keys.AI_AUTO_REFRESH_INTERVAL] = preferences.aiAutoRefreshIntervalSeconds
            prefs[Keys.TARGET_RETURN_PERCENT] = preferences.targetReturnPercent
            prefs[Keys.SHOW_WEEKLY_PREDICTIONS] = preferences.showWeeklyPredictions
            prefs[Keys.SHOW_MONTHLY_PREDICTIONS] = preferences.showMonthlyPredictions
            prefs[Keys.SHOW_QUARTERLY_PREDICTIONS] = preferences.showQuarterlyPredictions
            prefs[Keys.SHOW_YEARLY_PREDICTIONS] = preferences.showYearlyPredictions
            prefs[Keys.MAX_SIGNAL_ASSETS] = preferences.maxSignalAssets
            prefs[Keys.SIGNAL_AUTO_REFRESH] = preferences.signalAutoRefresh
            prefs[Keys.SIGNAL_REFRESH_INTERVAL] = preferences.signalRefreshIntervalSeconds
            prefs[Keys.CROSS_ASSET_AUTO_REFRESH] = preferences.crossAssetAutoRefresh
            prefs[Keys.CROSS_ASSET_REFRESH_INTERVAL] = preferences.crossAssetRefreshIntervalSeconds
        }
    }

    override suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }

    private fun Preferences.toUserPreferences(): UserPreferences {
        val defaults = UserPreferences()
        return UserPreferences(
            theme = this[Keys.THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: defaults.theme,
            defaultMarket = this[Keys.DEFAULT_MARKET] ?: defaults.defaultMarket,
            defaultWatchlistId = this[Keys.DEFAULT_WATCHLIST_ID] ?: defaults.defaultWatchlistId,
            refreshIntervalSeconds = this[Keys.REFRESH_INTERVAL] ?: defaults.refreshIntervalSeconds,
            streamingEnabled = this[Keys.STREAMING_ENABLED] ?: defaults.streamingEnabled,
            analyticsWindowShort = this[Keys.ANALYTICS_SHORT] ?: defaults.analyticsWindowShort,
            analyticsWindowMedium = this[Keys.ANALYTICS_MEDIUM] ?: defaults.analyticsWindowMedium,
            analyticsWindowLong = this[Keys.ANALYTICS_LONG] ?: defaults.analyticsWindowLong,
            riskProfile = this[Keys.RISK_PROFILE]?.let { runCatching { RiskProfile.valueOf(it) }.getOrNull() } ?: defaults.riskProfile,
            notificationsEnabled = this[Keys.NOTIFICATIONS_ENABLED] ?: defaults.notificationsEnabled,
            alertSoundEnabled = this[Keys.ALERT_SOUND_ENABLED] ?: defaults.alertSoundEnabled,
            providerType = this[Keys.PROVIDER_TYPE]?.let { runCatching { DataProviderType.valueOf(it) }.getOrNull() } ?: defaults.providerType,
            developerMode = this[Keys.DEVELOPER_MODE] ?: defaults.developerMode,
            showDebugPanel = this[Keys.SHOW_DEBUG_PANEL] ?: defaults.showDebugPanel,
            finnhubKeyConfigured = this[Keys.FINNHUB_KEY_CONFIGURED] ?: defaults.finnhubKeyConfigured,
            brapiKeyConfigured = this[Keys.BRAPI_KEY_CONFIGURED] ?: defaults.brapiKeyConfigured,
            openAiKeyConfigured = this[Keys.OPENAI_KEY_CONFIGURED] ?: defaults.openAiKeyConfigured,
            githubUsername = this[Keys.GITHUB_USERNAME],
            aiProviderMode = this[Keys.AI_PROVIDER_MODE]?.let { runCatching { AIProviderMode.valueOf(it) }.getOrNull() } ?: defaults.aiProviderMode,
            selectedAIModel = this[Keys.SELECTED_AI_MODEL] ?: defaults.selectedAIModel,
            aiAutoRefreshEnabled = this[Keys.AI_AUTO_REFRESH_ENABLED] ?: defaults.aiAutoRefreshEnabled,
            aiAutoRefreshIntervalSeconds = this[Keys.AI_AUTO_REFRESH_INTERVAL] ?: defaults.aiAutoRefreshIntervalSeconds,
            targetReturnPercent = this[Keys.TARGET_RETURN_PERCENT] ?: defaults.targetReturnPercent,
            showWeeklyPredictions = this[Keys.SHOW_WEEKLY_PREDICTIONS] ?: defaults.showWeeklyPredictions,
            showMonthlyPredictions = this[Keys.SHOW_MONTHLY_PREDICTIONS] ?: defaults.showMonthlyPredictions,
            showQuarterlyPredictions = this[Keys.SHOW_QUARTERLY_PREDICTIONS] ?: defaults.showQuarterlyPredictions,
            showYearlyPredictions = this[Keys.SHOW_YEARLY_PREDICTIONS] ?: defaults.showYearlyPredictions,
            maxSignalAssets = this[Keys.MAX_SIGNAL_ASSETS] ?: defaults.maxSignalAssets,
            signalAutoRefresh = this[Keys.SIGNAL_AUTO_REFRESH] ?: defaults.signalAutoRefresh,
            signalRefreshIntervalSeconds = this[Keys.SIGNAL_REFRESH_INTERVAL] ?: defaults.signalRefreshIntervalSeconds,
            crossAssetAutoRefresh = this[Keys.CROSS_ASSET_AUTO_REFRESH] ?: defaults.crossAssetAutoRefresh,
            crossAssetRefreshIntervalSeconds = this[Keys.CROSS_ASSET_REFRESH_INTERVAL] ?: defaults.crossAssetRefreshIntervalSeconds
        )
    }
}

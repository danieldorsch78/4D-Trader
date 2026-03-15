package com.fourdigital.marketintelligence

import android.app.Application
import com.fourdigital.marketintelligence.data.provider.ApiKeyManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MarketIntelligenceApp : Application() {

    @Inject lateinit var apiKeyManager: ApiKeyManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initLogging()
        seedDefaultApiKeys()
    }

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(SecureReleaseTree())
        }
    }

    private fun seedDefaultApiKeys() {
        appScope.launch {
            try {
                // Only seed if no key is already configured (preserves user edits)
                if (BuildConfig.DEFAULT_FINNHUB_KEY.isNotBlank()) {
                    val existing = apiKeyManager.getKey(ApiKeyManager.FINNHUB)
                    if (existing.isNullOrBlank()) {
                        apiKeyManager.setKey(ApiKeyManager.FINNHUB, BuildConfig.DEFAULT_FINNHUB_KEY)
                        Timber.d("Seeded default Finnhub API key")
                    }
                }
                if (BuildConfig.DEFAULT_BRAPI_KEY.isNotBlank()) {
                    val existing = apiKeyManager.getKey(ApiKeyManager.BRAPI)
                    if (existing.isNullOrBlank()) {
                        apiKeyManager.setKey(ApiKeyManager.BRAPI, BuildConfig.DEFAULT_BRAPI_KEY)
                        Timber.d("Seeded default Brapi API key")
                    }
                }
                if (BuildConfig.DEFAULT_GITHUB_KEY.isNotBlank()) {
                    val existing = apiKeyManager.getKey(com.fourdigital.marketintelligence.data.provider.GitHubAIAnalyst.GITHUB_KEY)
                    if (existing.isNullOrBlank()) {
                        apiKeyManager.setKey(com.fourdigital.marketintelligence.data.provider.GitHubAIAnalyst.GITHUB_KEY, BuildConfig.DEFAULT_GITHUB_KEY)
                        Timber.d("Seeded default GitHub AI key")
                    }
                }
                if (BuildConfig.DEFAULT_OPENAI_KEY.isNotBlank()) {
                    val existing = apiKeyManager.getKey(ApiKeyManager.OPENAI)
                    if (existing.isNullOrBlank()) {
                        apiKeyManager.setKey(ApiKeyManager.OPENAI, BuildConfig.DEFAULT_OPENAI_KEY)
                        Timber.d("Seeded default OpenAI key")
                    }
                }
                if (BuildConfig.DEFAULT_TWELVEDATA_KEY.isNotBlank()) {
                    val existing = apiKeyManager.getKey(ApiKeyManager.TWELVE_DATA)
                    if (existing.isNullOrBlank()) {
                        apiKeyManager.setKey(ApiKeyManager.TWELVE_DATA, BuildConfig.DEFAULT_TWELVEDATA_KEY)
                        Timber.d("Seeded default TwelveData key")
                    }
                }
                if (BuildConfig.DEFAULT_MASSIVE_KEY.isNotBlank()) {
                    val existing = apiKeyManager.getKey(ApiKeyManager.MASSIVE)
                    if (existing.isNullOrBlank()) {
                        apiKeyManager.setKey(ApiKeyManager.MASSIVE, BuildConfig.DEFAULT_MASSIVE_KEY)
                        Timber.d("Seeded default Massive key")
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to seed default API keys")
            }
        }
    }

    /**
     * Release-safe logging tree that redacts sensitive info
     * and only logs warnings and errors.
     */
    private class SecureReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority < android.util.Log.WARN) return
            val redacted = message
                .replace(Regex("(api[_-]?key|token|secret|password)=[^&\\s]+", RegexOption.IGNORE_CASE), "$1=[REDACTED]")
            android.util.Log.println(priority, tag ?: "4DMI", redacted)
        }
    }
}

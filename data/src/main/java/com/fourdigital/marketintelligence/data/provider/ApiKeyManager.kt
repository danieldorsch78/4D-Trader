package com.fourdigital.marketintelligence.data.provider

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fourdigital.marketintelligence.core.database.dao.ApiKeyDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiKeyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyDao: ApiKeyDao
) {
    private val cache = mutableMapOf<String, String>()
    private val mutex = Mutex()

    private val secureStorage by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_api_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    suspend fun getKey(provider: String): String? = mutex.withLock {
        cache[provider]
            ?: secureStorage.getString(provider, null)?.also {
                cache[provider] = it
            }
            ?: apiKeyDao.get(provider)?.apiKey?.also {
                secureStorage.edit().putString(provider, it).apply()
                apiKeyDao.delete(provider)
                cache[provider] = it
            }
    }

    suspend fun setKey(provider: String, key: String) = mutex.withLock {
        cache[provider] = key
        secureStorage.edit().putString(provider, key).apply()
        apiKeyDao.delete(provider)
    }

    suspend fun deleteKey(provider: String) = mutex.withLock {
        cache.remove(provider)
        secureStorage.edit().remove(provider).apply()
        apiKeyDao.delete(provider)
    }

    companion object {
        const val FINNHUB = "finnhub"
        const val BRAPI = "brapi"
        const val OPENAI = "openai"
        const val TWELVE_DATA = "twelvedata"
        const val MASSIVE = "massive"
        const val GITHUB = "github"
    }
}

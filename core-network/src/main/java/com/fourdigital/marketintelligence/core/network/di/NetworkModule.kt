package com.fourdigital.marketintelligence.core.network.di

import com.fourdigital.marketintelligence.core.network.api.BrapiApi
import com.fourdigital.marketintelligence.core.network.api.CoinGeckoApi
import com.fourdigital.marketintelligence.core.network.api.FinnhubApi
import com.fourdigital.marketintelligence.core.network.api.GitHubModelsApi
import com.fourdigital.marketintelligence.core.network.api.OpenAIModelsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    @Provides
    @Singleton
    @Named("ai")
    fun provideAIOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideCoinGeckoApi(client: OkHttpClient): CoinGeckoApi {
        return Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(CoinGeckoApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFinnhubApi(client: OkHttpClient): FinnhubApi {
        return Retrofit.Builder()
            .baseUrl("https://finnhub.io/api/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FinnhubApi::class.java)
    }

    @Provides
    @Singleton
    fun provideBrapiApi(client: OkHttpClient): BrapiApi {
        return Retrofit.Builder()
            .baseUrl("https://brapi.dev/api/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BrapiApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGitHubModelsApi(@Named("ai") client: OkHttpClient): GitHubModelsApi {
        return Retrofit.Builder()
            .baseUrl("https://models.inference.ai.azure.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GitHubModelsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenAIModelsApi(@Named("ai") client: OkHttpClient): OpenAIModelsApi {
        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenAIModelsApi::class.java)
    }
}

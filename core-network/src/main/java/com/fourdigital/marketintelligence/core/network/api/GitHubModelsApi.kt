package com.fourdigital.marketintelligence.core.network.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET

/**
 * GitHub Models API — OpenAI-compatible chat completions endpoint.
 * Uses GitHub PAT for authentication.
 * Supports multiple models: gpt-4o, gpt-4o-mini, o3-mini, etc.
 * Base URL: https://models.inference.ai.azure.com/
 */
interface GitHubModelsApi {

    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") auth: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    @GET("models")
    suspend fun listModels(
        @Header("Authorization") auth: String
    ): ModelsListResponse
}

/**
 * OpenAI API (Chat Completions + models listing).
 * Base URL: https://api.openai.com/v1/
 */
interface OpenAIModelsApi {

    @POST("chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") auth: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse

    @GET("models")
    suspend fun listModels(
        @Header("Authorization") auth: String
    ): ModelsListResponse
}

@Serializable
data class ChatCompletionRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 2048
)

@Serializable
data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@Serializable
data class ChatCompletionResponse(
    val id: String = "",
    val choices: List<ChatChoice> = emptyList(),
    val model: String = "",
    val usage: ChatUsage? = null
)

@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class ChatUsage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)

@Serializable
data class ModelsListResponse(
    val data: List<ModelInfo> = emptyList()
)

@Serializable
data class ModelInfo(
    val id: String = "",
    val name: String = "",
    @SerialName("friendly_name") val friendlyName: String = "",
    val publisher: String = "",
    @SerialName("model_family") val modelFamily: String = "",
    @SerialName("model_version") val modelVersion: String = "",
    val description: String = "",
    val summary: String = ""
)

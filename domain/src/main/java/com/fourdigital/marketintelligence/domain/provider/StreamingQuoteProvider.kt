package com.fourdigital.marketintelligence.domain.provider

import com.fourdigital.marketintelligence.domain.model.Quote
import kotlinx.coroutines.flow.Flow

/**
 * Provider for real-time streaming quote updates via WebSocket or similar.
 */
interface StreamingQuoteProvider {

    val providerName: String

    fun streamQuotes(symbols: List<String>): Flow<Quote>

    suspend fun connect()

    suspend fun disconnect()

    fun isConnected(): Boolean

    fun connectionState(): Flow<StreamingConnectionState>
}

enum class StreamingConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

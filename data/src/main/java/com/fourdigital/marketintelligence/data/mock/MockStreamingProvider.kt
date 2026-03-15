package com.fourdigital.marketintelligence.data.mock

import com.fourdigital.marketintelligence.domain.model.Quote
import com.fourdigital.marketintelligence.domain.provider.StreamingConnectionState
import com.fourdigital.marketintelligence.domain.provider.StreamingQuoteProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockStreamingProvider @Inject constructor() : StreamingQuoteProvider {

    override val providerName: String = "MockStreaming"

    private val connectionState = MutableStateFlow(StreamingConnectionState.DISCONNECTED)
    private var connected = false

    override fun streamQuotes(symbols: List<String>): Flow<Quote> = flow {
        connectionState.value = StreamingConnectionState.CONNECTED
        connected = true
        while (connected) {
            symbols.forEach { symbol ->
                emit(MockQuoteGenerator.generateQuote(symbol))
            }
            delay(2_000) // Simulated tick interval
        }
    }

    override suspend fun connect() {
        connectionState.value = StreamingConnectionState.CONNECTING
        delay(300) // Simulate connection latency
        connectionState.value = StreamingConnectionState.CONNECTED
        connected = true
    }

    override suspend fun disconnect() {
        connected = false
        connectionState.value = StreamingConnectionState.DISCONNECTED
    }

    override fun isConnected(): Boolean = connected

    override fun connectionState(): Flow<StreamingConnectionState> = connectionState
}

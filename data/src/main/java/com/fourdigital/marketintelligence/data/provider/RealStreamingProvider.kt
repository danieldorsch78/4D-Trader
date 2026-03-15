package com.fourdigital.marketintelligence.data.provider

import com.fourdigital.marketintelligence.domain.model.Quote
import com.fourdigital.marketintelligence.domain.model.QuoteDataQuality
import com.fourdigital.marketintelligence.domain.provider.StreamingConnectionState
import com.fourdigital.marketintelligence.domain.provider.StreamingQuoteProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import okhttp3.*
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time streaming via Finnhub WebSocket.
 * Free tier supports real-time trade data for US stocks and crypto.
 * Falls back to polling if WebSocket unavailable.
 */
@Singleton
class RealStreamingProvider @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val keyManager: ApiKeyManager,
    private val realProvider: RealMarketDataProvider
) : StreamingQuoteProvider {

    override val providerName: String = "FinnhubWS"

    private val _connectionState = MutableStateFlow(StreamingConnectionState.DISCONNECTED)
    private var webSocket: WebSocket? = null
    private var connected = false
    private val json = Json { ignoreUnknownKeys = true }
    // Cache last-known quotes so streaming trades carry forward previousClose/open/high/low
    private val lastKnownQuotes = java.util.concurrent.ConcurrentHashMap<String, Quote>()

    override fun streamQuotes(symbols: List<String>): Flow<Quote> = channelFlow {
        // Pre-load last-known quotes so streaming trades have correct previousClose/open
        try {
            val seedResult = realProvider.getQuotes(symbols)
            if (seedResult is com.fourdigital.marketintelligence.core.common.result.DataResult.Success) {
                seedResult.data.forEach { q -> lastKnownQuotes[q.symbol] = q }
            }
        } catch (_: Exception) { }

        val token = keyManager.getKey(ApiKeyManager.FINNHUB)

        if (token != null) {
            // Try WebSocket streaming
            try {
                val request = Request.Builder()
                    .url("wss://ws.finnhub.io?token=$token")
                    .build()

                val listener = object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        _connectionState.value = StreamingConnectionState.CONNECTED
                        connected = true
                        symbols.forEach { symbol ->
                            val wsSymbol = mapToWsSymbol(symbol)
                            webSocket.send("""{"type":"subscribe","symbol":"$wsSymbol"}""")
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        try {
                            val element = json.parseToJsonElement(text)
                            val type = element.jsonObject["type"]?.jsonPrimitive?.content
                            if (type == "trade") {
                                val data = element.jsonObject["data"]?.jsonArray
                                data?.forEach { trade ->
                                    val obj = trade.jsonObject
                                    val wsSymbol = obj["s"]?.jsonPrimitive?.content ?: return@forEach
                                    val price = obj["p"]?.jsonPrimitive?.double ?: return@forEach
                                    val volume = obj["v"]?.jsonPrimitive?.long ?: 0L
                                    val ts = obj["t"]?.jsonPrimitive?.long ?: 0L

                                    val originalSymbol = reverseMapSymbol(wsSymbol)
                                    val cached = lastKnownQuotes[originalSymbol]
                                    val quote = Quote(
                                        symbol = originalSymbol,
                                        price = price,
                                        previousClose = cached?.previousClose ?: price,
                                        open = cached?.open ?: price,
                                        dayHigh = maxOf(price, cached?.dayHigh ?: price),
                                        dayLow = minOf(price, cached?.dayLow ?: price),
                                        volume = (cached?.volume ?: 0L) + volume,
                                        timestamp = if (ts > 0) kotlinx.datetime.Instant.fromEpochMilliseconds(ts) else Clock.System.now(),
                                        dataQuality = QuoteDataQuality.REALTIME_STREAMING,
                                        providerName = providerName
                                    )
                                    lastKnownQuotes[originalSymbol] = quote
                                    trySend(quote)
                                }
                            }
                        } catch (e: Exception) {
                            timber.log.Timber.d(e, "WS parse error")
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        _connectionState.value = StreamingConnectionState.ERROR
                        connected = false
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        _connectionState.value = StreamingConnectionState.DISCONNECTED
                        connected = false
                    }
                }

                _connectionState.value = StreamingConnectionState.CONNECTING
                webSocket = okHttpClient.newWebSocket(request, listener)

                // Keep channel open while connected
                while (isActive && connected) {
                    delay(1000)
                }
            } catch (_: Exception) {
                // Fall through to polling
            }
        }

        // Fallback: degrade to slower snapshots instead of pretending to be realtime.
        // Use a longer cadence to reduce rate-limit pressure on provider APIs.
        _connectionState.value = StreamingConnectionState.RECONNECTING
        connected = true
        while (isActive && connected) {
            try {
                val result = realProvider.getQuotes(symbols)
                if (result is com.fourdigital.marketintelligence.core.common.result.DataResult.Success) {
                    result.data.forEach { quote ->
                        send(
                            quote.copy(
                                dataQuality = QuoteDataQuality.CACHED,
                                providerName = "${quote.providerName} Snapshot"
                            )
                        )
                    }
                }
            } catch (_: Exception) { }
            delay(45_000)
        }
    }

    override suspend fun connect() {
        _connectionState.value = StreamingConnectionState.CONNECTING
        delay(100)
        _connectionState.value = StreamingConnectionState.CONNECTED
        connected = true
    }

    override suspend fun disconnect() {
        connected = false
        webSocket?.close(1000, "User disconnect")
        webSocket = null
        _connectionState.value = StreamingConnectionState.DISCONNECTED
    }

    override fun isConnected(): Boolean = connected

    override fun connectionState(): Flow<StreamingConnectionState> = _connectionState

    private fun mapToWsSymbol(symbol: String): String = when (symbol) {
        "BTC-USD" -> "BINANCE:BTCUSDT"
        "ETH-USD" -> "BINANCE:ETHUSDT"
        else -> symbol.replace(".DE", "").replace(".SA", "")
    }

    private fun reverseMapSymbol(wsSymbol: String): String = when (wsSymbol) {
        "BINANCE:BTCUSDT" -> "BTC-USD"
        "BINANCE:ETHUSDT" -> "ETH-USD"
        else -> wsSymbol
    }
}

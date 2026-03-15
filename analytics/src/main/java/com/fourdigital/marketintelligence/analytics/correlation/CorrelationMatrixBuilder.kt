package com.fourdigital.marketintelligence.analytics.correlation

import com.fourdigital.marketintelligence.domain.model.CorrelationSnapshot
import com.fourdigital.marketintelligence.domain.model.CorrelationWindow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Predefined correlation pairs for cross-market analysis.
 */
data class CorrelationPairDef(
    val symbolA: String,
    val symbolB: String,
    val label: String,
    val category: String
)

@Singleton
class CorrelationMatrixBuilder @Inject constructor(
    private val engine: CorrelationEngine
) {

    val predefinedPairs = listOf(
        CorrelationPairDef("^GDAXI", "GC=F", "DAX vs Gold", "Equity-Metal"),
        CorrelationPairDef("^GDAXI", "BTC-USD", "DAX vs Bitcoin", "Equity-Crypto"),
        CorrelationPairDef("^BVSP", "GC=F", "Ibovespa vs Gold", "Equity-Metal"),
        CorrelationPairDef("^BVSP", "CL=F", "Ibovespa vs Oil", "Equity-Commodity"),
        CorrelationPairDef("PETR4.SA", "CL=F", "Petrobras vs Oil", "Stock-Commodity"),
        CorrelationPairDef("VALE3.SA", "GC=F", "Vale vs Gold", "Stock-Metal"),
        CorrelationPairDef("BTC-USD", "GC=F", "Bitcoin vs Gold", "Crypto-Metal"),
        CorrelationPairDef("BTC-USD", "ETH-USD", "Bitcoin vs Ethereum", "Crypto"),
        CorrelationPairDef("^GDAXI", "^BVSP", "Germany vs Brazil", "Regional"),
        CorrelationPairDef("^GSPC", "^N225", "S&P 500 vs Nikkei", "Regional"),
        CorrelationPairDef("^GSPC", "^GDAXI", "S&P 500 vs DAX", "Regional"),
        CorrelationPairDef("EWG", "EWZ", "Germany ETF vs Brazil ETF", "Regional-ETF"),
        CorrelationPairDef("CL=F", "BZ=F", "WTI vs Brent", "Commodity"),
        CorrelationPairDef("GC=F", "SI=F", "Gold vs Silver", "Metal"),
        CorrelationPairDef("EUR/USD", "GBP/USD", "EUR/USD vs GBP/USD", "Forex"),
        CorrelationPairDef("EUR/USD", "GC=F", "EUR/USD vs Gold", "Forex-Metal"),
        CorrelationPairDef("USD/BRL", "^BVSP", "USD/BRL vs Ibovespa", "Forex-Equity"),
        CorrelationPairDef("BTC-USD", "^GSPC", "Bitcoin vs S&P 500", "Crypto-Equity"),
    )

    fun buildMatrix(
        priceData: Map<String, List<Double>>,
        window: CorrelationWindow
    ): List<CorrelationSnapshot> {
        return predefinedPairs.mapNotNull { pair ->
            val pricesA = priceData[pair.symbolA] ?: return@mapNotNull null
            val pricesB = priceData[pair.symbolB] ?: return@mapNotNull null
            if (pricesA.size < 10 || pricesB.size < 10) return@mapNotNull null

            engine.computeSnapshot(
                symbolA = pair.symbolA,
                symbolB = pair.symbolB,
                pricesA = pricesA,
                pricesB = pricesB,
                windowDays = window.days
            )
        }
    }
}

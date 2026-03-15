package com.fourdigital.marketintelligence.analytics.correlation

import com.fourdigital.marketintelligence.domain.model.*
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Core correlation computation engine.
 * Computes Pearson and Spearman rank correlations over configurable windows.
 * All computations are transparent and locally deterministic.
 */
@Singleton
class CorrelationEngine @Inject constructor() {

    /**
     * Compute Pearson correlation between two return series.
     */
    fun pearsonCorrelation(x: List<Double>, y: List<Double>): Double {
        require(x.size == y.size) { "Series must have equal length" }
        val n = x.size
        if (n < 3) return 0.0

        val meanX = x.average()
        val meanY = y.average()

        var sumXY = 0.0
        var sumX2 = 0.0
        var sumY2 = 0.0

        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            sumXY += dx * dy
            sumX2 += dx * dx
            sumY2 += dy * dy
        }

        val denominator = sqrt(sumX2 * sumY2)
        return if (denominator == 0.0) 0.0 else (sumXY / denominator).coerceIn(-1.0, 1.0)
    }

    /**
     * Compute Spearman rank correlation using rank transformation.
     */
    fun spearmanCorrelation(x: List<Double>, y: List<Double>): Double {
        require(x.size == y.size) { "Series must have equal length" }
        if (x.size < 3) return 0.0

        val rankedX = computeRanks(x)
        val rankedY = computeRanks(y)
        return pearsonCorrelation(rankedX, rankedY)
    }

    /**
     * Compute returns from a price series.
     */
    fun computeReturns(prices: List<Double>): List<Double> {
        if (prices.size < 2) return emptyList()
        return prices.zipWithNext { a, b ->
            if (a != 0.0) (b - a) / a else 0.0
        }
    }

    /**
     * Compute rolling correlation over a window.
     */
    fun rollingCorrelation(
        returnsA: List<Double>,
        returnsB: List<Double>,
        windowSize: Int
    ): List<Double> {
        require(returnsA.size == returnsB.size) { "Return series must have equal length" }
        if (returnsA.size < windowSize) return emptyList()

        return (windowSize..returnsA.size).map { end ->
            val start = end - windowSize
            val windowA = returnsA.subList(start, end)
            val windowB = returnsB.subList(start, end)
            pearsonCorrelation(windowA, windowB)
        }
    }

    /**
     * Compute a full CorrelationSnapshot for a pair of assets.
     */
    fun computeSnapshot(
        symbolA: String,
        symbolB: String,
        pricesA: List<Double>,
        pricesB: List<Double>,
        windowDays: Int
    ): CorrelationSnapshot {
        val returnsA = computeReturns(pricesA)
        val returnsB = computeReturns(pricesB)

        val size = minOf(returnsA.size, returnsB.size, windowDays)
        if (size < 10) {
            return CorrelationSnapshot(
                assetA = symbolA,
                assetB = symbolB,
                pearson = 0.0,
                spearman = 0.0,
                windowDays = windowDays,
                sampleSize = size,
                timestamp = Clock.System.now(),
                isStatisticallySignificant = false,
                stability = CorrelationStability.UNKNOWN
            )
        }

        val trimA = returnsA.takeLast(size)
        val trimB = returnsB.takeLast(size)

        val pearson = pearsonCorrelation(trimA, trimB)
        val spearman = spearmanCorrelation(trimA, trimB)

        val stability = assessStability(returnsA, returnsB, windowDays)

        return CorrelationSnapshot(
            assetA = symbolA,
            assetB = symbolB,
            pearson = pearson,
            spearman = spearman,
            windowDays = windowDays,
            sampleSize = size,
            timestamp = Clock.System.now(),
            isStatisticallySignificant = size >= 30,
            stability = stability
        )
    }

    /**
     * Assess whether a correlation is stable, weakening, or breaking.
     */
    private fun assessStability(
        returnsA: List<Double>,
        returnsB: List<Double>,
        windowDays: Int
    ): CorrelationStability {
        if (returnsA.size < windowDays * 2) return CorrelationStability.UNKNOWN

        val halfWindow = windowDays / 2
        val recentA = returnsA.takeLast(halfWindow)
        val recentB = returnsB.takeLast(halfWindow)
        val olderA = returnsA.dropLast(halfWindow).takeLast(halfWindow)
        val olderB = returnsB.dropLast(halfWindow).takeLast(halfWindow)

        val recentCorr = pearsonCorrelation(recentA, recentB)
        val olderCorr = pearsonCorrelation(olderA, olderB)
        val delta = recentCorr - olderCorr

        return when {
            kotlin.math.abs(delta) < 0.15 -> CorrelationStability.STABLE
            delta < -0.30 -> CorrelationStability.BREAKING
            delta < -0.15 -> CorrelationStability.WEAKENING
            delta > 0.15 -> CorrelationStability.REVERTING
            else -> CorrelationStability.STABLE
        }
    }

    /**
     * Assign ranks (average rank for ties).
     */
    private fun computeRanks(values: List<Double>): List<Double> {
        val indexed = values.mapIndexed { i, v -> Pair(i, v) }
        val sorted = indexed.sortedBy { it.second }
        val ranks = DoubleArray(values.size)

        var i = 0
        while (i < sorted.size) {
            var j = i
            while (j < sorted.size && sorted[j].second == sorted[i].second) j++
            val avgRank = (i + j - 1) / 2.0 + 1.0
            for (k in i until j) ranks[sorted[k].first] = avgRank
            i = j
        }
        return ranks.toList()
    }
}

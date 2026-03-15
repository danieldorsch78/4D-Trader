package com.fourdigital.marketintelligence.feature.marketoverview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdigital.marketintelligence.analytics.ai.*

private val Green = Color(0xFF00E676)
private val Red = Color(0xFFFF5252)
private val Blue = Color(0xFF448AFF)
private val Yellow = Color(0xFFFFD740)
private val Purple = Color(0xFFAB47BC)
private val BgBlack = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A2E)
private val Muted = Color(0xFF888888)

@Composable
fun AssetDetailScreen(
    onBack: () -> Unit,
    viewModel: AssetDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgBlack).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Blue, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(state.symbol, color = Blue, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.loadAssetData() }) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp),
                            color = Blue, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Blue)
                    }
                }
            }
        }

        state.error?.let { error ->
            item {
                Text("⚠ $error", color = Red, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Quote card
        state.quote?.let { q ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("> QUOTE DATA", color = Green, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Price
                        val chgColor = if (q.isPositive) Green else Red
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${"%.2f".format(q.price)}", color = Color.White,
                                fontSize = 28.sp, fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "${if (q.isPositive) "+" else ""}${"%.2f".format(q.change)} (${"%.2f".format(q.changePercent)}%)",
                                color = chgColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatBox("Open", "${"%.2f".format(q.open)}")
                            StatBox("Prev Close", "${"%.2f".format(q.previousClose)}")
                            StatBox("Day High", "${"%.2f".format(q.dayHigh)}")
                            StatBox("Day Low", "${"%.2f".format(q.dayLow)}")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatBox("Volume", formatVolume(q.volume))
                            q.weekHigh52?.let { StatBox("52W High", "${"%.2f".format(it)}") }
                            q.weekLow52?.let { StatBox("52W Low", "${"%.2f".format(it)}") }
                            q.marketCap?.let { StatBox("Mkt Cap", formatMarketCap(it)) }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Provider: ${q.providerName}", color = Muted, fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace)
                            Text("Quality: ${q.dataQuality.name}", color = Muted, fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace)
                            Text("Latency: ${q.latencyMs}ms", color = Muted, fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // AI Analysis
        state.analysis?.let { analysis ->
            item {
                val actionColor = when (analysis.action) {
                    AIAction.STRONG_BUY -> Green
                    AIAction.BUY -> Green.copy(alpha = 0.7f)
                    AIAction.HOLD -> Yellow
                    AIAction.SELL -> Red.copy(alpha = 0.7f)
                    AIAction.STRONG_SELL -> Red
                }

                Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("> AI ANALYSIS", color = Green, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(
                                analysis.action.name.replace("_", " "),
                                color = actionColor, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(actionColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatBox("AI Score", "${"%.0f".format(analysis.compositeScore * 100)}%", actionColor)
                            StatBox("Confidence", "${"%.0f".format(analysis.confidence)}%", Blue)
                            StatBox("Risk", analysis.riskLevel.name, when (analysis.riskLevel) {
                                RiskLevel.LOW -> Green; RiskLevel.MODERATE -> Yellow
                                RiskLevel.HIGH -> Red.copy(0.7f); RiskLevel.CRITICAL -> Red
                            })
                        }

                        // Timeframes
                        Spacer(modifier = Modifier.height(10.dp))
                        analysis.timeframes.forEach { tf ->
                            val tfColor = when (tf.direction) {
                                "UP" -> Green; "DOWN" -> Red; else -> Muted
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(tf.timeframe, color = Muted, fontSize = 11.sp,
                                    modifier = Modifier.weight(1f))
                                Text(
                                    "${if (tf.estimatedChange > 0) "▲" else if (tf.estimatedChange < 0) "▼" else "▬"} ${"%.1f".format(tf.estimatedChange)}%",
                                    color = tfColor, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Insights
                        if (analysis.insights.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("> INSIGHTS", color = Green, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            analysis.insights.forEach { insight ->
                                val impactColor = when (insight.impact) {
                                    InsightImpact.POSITIVE -> Green
                                    InsightImpact.NEGATIVE -> Red
                                    InsightImpact.WARNING -> Yellow
                                    InsightImpact.NEUTRAL -> Muted
                                }
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("● ", color = impactColor, fontSize = 10.sp)
                                    Text("${insight.title}: ${insight.detail}",
                                        color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                                }
                            }
                        }

                        // Key levels
                        if (analysis.keyLevels.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("> KEY LEVELS", color = Green, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            analysis.keyLevels.forEach { level ->
                                val levelColor = when (level.type) {
                                    "TARGET" -> Green; "STOP" -> Red
                                    "RESISTANCE", "ABOVE" -> Purple
                                    "SUPPORT", "BELOW" -> Blue; else -> Muted
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(level.label, color = levelColor, fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace)
                                    Text("${"%.2f".format(level.price)}", color = Color.White,
                                        fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Trade setup
                        analysis.prediction?.let { pred ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("> TRADE SETUP", color = Green, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatBox("Entry", "${"%.2f".format(pred.entryPrice)}", Blue)
                                StatBox("Target", "${"%.2f".format(pred.targetPrice)}", Green)
                                StatBox("Stop", "${"%.2f".format(pred.stopLoss)}", Red)
                                StatBox("R:R", "${"%.1f".format(pred.riskRewardRatio)}:1", Yellow)
                            }
                        }
                    }
                }
            }
        }

        // Technical signals
        if (state.signals.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("> TECHNICAL SIGNALS", color = Green, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))
                        state.signals.forEach { signal ->
                            Text("• $signal", color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }
            }
        }

        // Loading state
        if (state.isLoading && state.quote == null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        color = Blue, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Loading ${state.symbol}...", color = Muted,
                        fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        item {
            Text(
                text = "⚠ For informational purposes only. Not financial advice.",
                color = Muted.copy(alpha = 0.6f), fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace)
        Text(label, color = Muted, fontSize = 9.sp)
    }
}

private fun formatVolume(vol: Long): String {
    return when {
        vol >= 1_000_000_000 -> "${"%.1f".format(vol / 1_000_000_000.0)}B"
        vol >= 1_000_000 -> "${"%.1f".format(vol / 1_000_000.0)}M"
        vol >= 1_000 -> "${"%.1f".format(vol / 1_000.0)}K"
        else -> "$vol"
    }
}

private fun formatMarketCap(cap: Double): String {
    return when {
        cap >= 1_000_000_000_000 -> "${"%.1f".format(cap / 1_000_000_000_000.0)}T"
        cap >= 1_000_000_000 -> "${"%.1f".format(cap / 1_000_000_000.0)}B"
        cap >= 1_000_000 -> "${"%.1f".format(cap / 1_000_000.0)}M"
        else -> "${"%.0f".format(cap)}"
    }
}

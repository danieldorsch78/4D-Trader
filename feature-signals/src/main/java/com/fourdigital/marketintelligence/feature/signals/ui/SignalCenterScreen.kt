package com.fourdigital.marketintelligence.feature.signals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fourdigital.marketintelligence.domain.model.*

@Composable
fun SignalCenterScreen(
    viewModel: SignalCenterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SIGNAL CENTER",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI-powered market analysis • Explainable signals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Default.Refresh, "Refresh", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Analyzing markets...",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            val selectedSignal = uiState.selectedSignal
            if (selectedSignal != null) {
                SignalDetailView(
                    signal = selectedSignal,
                    showAdvanced = uiState.showAdvanced,
                    onToggleAdvanced = viewModel::toggleAdvanced,
                    onBack = { viewModel.selectSignal(null) }
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.signals, key = { it.symbol }) { signal ->
                        SignalCard(
                            signal = signal,
                            onClick = { viewModel.selectSignal(signal) }
                        )
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "⚠ Decision-support tool only. Not financial advice. Past performance ≠ future results.",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalCard(signal: SignalAnalysis, onClick: () -> Unit) {
    val biasColor = biasToColor(signal.directionalBias)
    val regimeColor = regimeToColor(signal.regime)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = signal.symbol,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Regime badge
                    Badge(regimeColor, signal.regime.name)
                    // Bias badge
                    Badge(biasColor, signal.directionalBias.name.replace("_", " "))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricColumn("Momentum", signal.momentumScore, momentumColor(signal.momentumScore))
                MetricColumn("Risk", signal.riskScore, riskColor(signal.riskScore))
                MetricColumn("Confidence", signal.confidenceScore.toDouble(), confidenceColor(signal.confidenceScore))
                MetricColumn("Volatility", null, null, signal.volatilityRegime.name)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Concise explanation
            Text(
                text = signal.conciseExplanation,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Data quality warnings
            if (signal.dataQualityWarnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠ ${signal.dataQualityWarnings.first()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFA726)
                )
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: Double?, color: Color?, text: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        if (value != null && color != null) {
            Text(
                text = "${"%.0f".format(value)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
        } else if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun Badge(color: Color, text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            ),
            color = color
        )
    }
}

@Composable
private fun SignalDetailView(
    signal: SignalAnalysis,
    showAdvanced: Boolean,
    onToggleAdvanced: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack) {
                Text("← Back to signals", fontFamily = FontFamily.Monospace)
            }
        }

        item {
            Text(
                text = signal.symbol,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Regime & Bias
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SectionLabel("MARKET REGIME")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Badge(regimeToColor(signal.regime), signal.regime.name)
                        Badge(biasToColor(signal.directionalBias), signal.directionalBias.name)
                        Badge(volatilityColor(signal.volatilityRegime), signal.volatilityRegime.name)
                    }
                }
            }
        }

        // Key Metrics
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SectionLabel("METRICS")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricColumn("Momentum", signal.momentumScore, momentumColor(signal.momentumScore))
                        MetricColumn("Risk", signal.riskScore, riskColor(signal.riskScore))
                        MetricColumn("Confidence", signal.confidenceScore.toDouble(), confidenceColor(signal.confidenceScore))
                    }
                }
            }
        }

        // Buy / Sell Zones
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SectionLabel("PRICE ZONES")
                    Spacer(Modifier.height(8.dp))
                    signal.buyZone?.let { zone ->
                        ZoneRow("BUY ZONE", zone, Color(0xFF4CAF50))
                    }
                    Spacer(Modifier.height(4.dp))
                    signal.sellZone?.let { zone ->
                        ZoneRow("SELL ZONE", zone, Color(0xFFEF5350))
                    }
                    signal.invalidation?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "INVALIDATION: ${"%.2f".format(it)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFFFF5722)
                        )
                    }
                }
            }
        }

        // Key Drivers
        if (signal.keyDrivers.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SectionLabel("KEY DRIVERS")
                        Spacer(Modifier.height(4.dp))
                        signal.keyDrivers.forEach { driver ->
                            Text(
                                text = "• $driver",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Feature Contributions
        if (signal.featureContributions.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        SectionLabel("FEATURE CONTRIBUTIONS")
                        Spacer(Modifier.height(4.dp))
                        signal.featureContributions.forEach { (feature, weight) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = feature,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "%+.2f".format(weight),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (weight > 0) Color(0xFF4CAF50) else if (weight < 0) Color(0xFFEF5350) else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Advanced / Concise toggle
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleAdvanced),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel(if (showAdvanced) "ADVANCED ANALYSIS" else "CONCISE ANALYSIS")
                        Icon(
                            if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (showAdvanced) signal.advancedExplanation else signal.conciseExplanation,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Disclaimer
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "⚠ This analysis is generated by rule-based algorithms using historical data. It is NOT financial advice. All signals are for educational and research purposes only. 4D Digital Solutions and Daniel Dorsch assume no liability for trading decisions.",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        ),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ZoneRow(label: String, zone: PriceZone, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label: ${zone.label}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = color
        )
        Text(
            text = "${"%.2f".format(zone.lower)} – ${"%.2f".format(zone.upper)}",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}

// ---- Color Helpers ----

private fun biasToColor(bias: DirectionalBias): Color = when (bias) {
    DirectionalBias.STRONG_BULLISH -> Color(0xFF4CAF50)
    DirectionalBias.BULLISH -> Color(0xFF8BC34A)
    DirectionalBias.NEUTRAL_BULLISH -> Color(0xFFCDDC39)
    DirectionalBias.NEUTRAL -> Color(0xFF9E9E9E)
    DirectionalBias.NEUTRAL_BEARISH -> Color(0xFFFFC107)
    DirectionalBias.BEARISH -> Color(0xFFFF9800)
    DirectionalBias.STRONG_BEARISH -> Color(0xFFEF5350)
    DirectionalBias.INDETERMINATE -> Color(0xFF757575)
}

private fun regimeToColor(regime: MarketRegime): Color = when (regime) {
    MarketRegime.RISK_ON -> Color(0xFF4CAF50)
    MarketRegime.RISK_OFF -> Color(0xFFEF5350)
    MarketRegime.TRANSITIONING -> Color(0xFFFFC107)
    MarketRegime.MIXED -> Color(0xFF9E9E9E)
    MarketRegime.UNKNOWN -> Color(0xFF757575)
}

private fun volatilityColor(vol: VolatilityRegime): Color = when (vol) {
    VolatilityRegime.LOW -> Color(0xFF4CAF50)
    VolatilityRegime.NORMAL -> Color(0xFF8BC34A)
    VolatilityRegime.ELEVATED -> Color(0xFFFFC107)
    VolatilityRegime.HIGH -> Color(0xFFFF9800)
    VolatilityRegime.EXTREME -> Color(0xFFEF5350)
}

private fun momentumColor(score: Double): Color = when {
    score > 30 -> Color(0xFF4CAF50)
    score > 0 -> Color(0xFF8BC34A)
    score > -30 -> Color(0xFFFFC107)
    else -> Color(0xFFEF5350)
}

private fun riskColor(score: Double): Color = when {
    score < 30 -> Color(0xFF4CAF50)
    score < 50 -> Color(0xFF8BC34A)
    score < 70 -> Color(0xFFFFC107)
    else -> Color(0xFFEF5350)
}

private fun confidenceColor(score: Int): Color = when {
    score >= 70 -> Color(0xFF4CAF50)
    score >= 50 -> Color(0xFF8BC34A)
    score >= 30 -> Color(0xFFFFC107)
    else -> Color(0xFFEF5350)
}

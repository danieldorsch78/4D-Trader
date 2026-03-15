package com.fourdigital.marketintelligence.feature.correlations.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fourdigital.marketintelligence.domain.model.CorrelationSnapshot
import com.fourdigital.marketintelligence.domain.model.CorrelationStability
import com.fourdigital.marketintelligence.domain.model.CorrelationWindow
import kotlin.math.absoluteValue

@Composable
fun CorrelationsScreen(
    viewModel: CorrelationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = "CROSS-ASSET CORRELATIONS",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Live correlation matrix across DAX, B3, Crypto & Commodities",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Window selector
        WindowSelector(
            selectedWindow = uiState.selectedWindow,
            onWindowSelected = viewModel::selectWindow
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.snapshots, key = { "${it.assetA}-${it.assetB}" }) { snapshot ->
                    CorrelationPairCard(snapshot)
                }
            }
        }
    }
}

@Composable
private fun WindowSelector(
    selectedWindow: CorrelationWindow,
    onWindowSelected: (CorrelationWindow) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CorrelationWindow.entries.forEach { window ->
            FilterChip(
                selected = window == selectedWindow,
                onClick = { onWindowSelected(window) },
                label = {
                    Text(
                        text = "${window.days}D",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            )
        }
    }
}

@Composable
private fun CorrelationPairCard(snapshot: CorrelationSnapshot) {
    val corrColor = correlationColor(snapshot.pearson)
    val stabilityColor = when (snapshot.stability) {
        CorrelationStability.STABLE -> Color(0xFF4CAF50)
        CorrelationStability.WEAKENING -> Color(0xFFFFC107)
        CorrelationStability.BREAKING -> Color(0xFFFF5722)
        CorrelationStability.REVERTING -> Color(0xFF2196F3)
        CorrelationStability.UNKNOWN -> Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = "${snapshot.assetA} ↔ ${snapshot.assetB}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Stability badge
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = stabilityColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = snapshot.stability.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = stabilityColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Correlation bar
            CorrelationBar(snapshot.pearson, corrColor)

            Spacer(modifier = Modifier.height(8.dp))

            val avgCorrelation = (snapshot.pearson + snapshot.spearman) / 2.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CorrelationStat("Pearson", snapshot.pearson, corrColor)
                CorrelationStat(
                    "Spearman",
                    snapshot.spearman,
                    correlationColor(snapshot.spearman)
                )
                CorrelationStat(
                    "Avg",
                    avgCorrelation,
                    correlationColor(avgCorrelation)
                )
            }
        }
    }
}

@Composable
private fun CorrelationBar(value: Double, color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
    ) {
        val midX = size.width / 2f
        // Background
        drawRect(Color(0xFF1A1A2E), size = size)
        // Center line
        drawLine(Color.Gray, Offset(midX, 0f), Offset(midX, size.height), strokeWidth = 1f)
        // Correlation bar
        val barWidth = (value.absoluteValue * midX).toFloat()
        val startX = if (value >= 0) midX else midX - barWidth
        drawRect(color.copy(alpha = 0.8f), Offset(startX, 2f), Size(barWidth, size.height - 4f))
    }
}

@Composable
private fun CorrelationStat(label: String, value: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = "%+.3f".format(value),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = color,
            textAlign = TextAlign.Center
        )
    }
}

private fun correlationColor(value: Double): Color {
    return when {
        value > 0.7 -> Color(0xFF4CAF50)   // strong positive
        value > 0.3 -> Color(0xFF8BC34A)   // moderate positive
        value > -0.3 -> Color(0xFF9E9E9E) // weak / neutral
        value > -0.7 -> Color(0xFFFFA726) // moderate negative
        else -> Color(0xFFEF5350)          // strong negative
    }
}

package com.fourdigital.marketintelligence.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fourdigital.marketintelligence.core.ui.theme.*

@Composable
fun TerminalCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = TerminalCardGray
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TerminalTextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
            content()
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun PriceChangeText(
    change: Double,
    changePercent: Double,
    modifier: Modifier = Modifier
) {
    val color = when {
        change > 0 -> GainGreen
        change < 0 -> LossRed
        else -> TerminalTextMuted
    }
    val arrow = when {
        change > 0 -> "▲"
        change < 0 -> "▼"
        else -> "–"
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$arrow ${"%.2f".format(change)} (${"%.2f".format(changePercent)}%)",
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
fun ConfidenceIndicator(
    confidence: Int,
    modifier: Modifier = Modifier
) {
    val color = when {
        confidence >= 70 -> ConfidenceHigh
        confidence >= 40 -> ConfidenceMedium
        else -> ConfidenceLow
    }
    val label = when {
        confidence >= 70 -> "HIGH"
        confidence >= 40 -> "MED"
        else -> "LOW"
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$confidence% $label",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun DataQualityBadge(
    quality: String,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (quality) {
        "REALTIME_STREAMING" -> "LIVE" to StatusStreaming
        "REALTIME_SNAPSHOT" -> "RT" to StatusOpen
        "DELAYED_15MIN", "DELAYED_30MIN" -> "DELAYED" to StatusDelayed
        "END_OF_DAY" -> "EOD" to TerminalTextMuted
        "CACHED" -> "CACHED" to NeutralAmber
        "STALE" -> "STALE" to LossRed
        else -> "UNK" to TerminalTextMuted
    }
    StatusBadge(text = text, color = color, modifier = modifier)
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TerminalTextPrimary
        )
        trailing?.invoke()
    }
}

@Composable
fun DisclaimerFooter(modifier: Modifier = Modifier) {
    Text(
        text = "Signals are informational only and do not constitute financial advice. " +
                "Past performance does not guarantee future results.",
        style = MaterialTheme.typography.labelSmall,
        color = TerminalTextMuted,
        modifier = modifier.padding(16.dp)
    )
}

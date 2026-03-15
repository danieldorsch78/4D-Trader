package com.fourdigital.marketintelligence.feature.worldclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fourdigital.marketintelligence.domain.model.MarketSession
import com.fourdigital.marketintelligence.domain.model.MarketStatus

@Composable
fun WorldClockScreen(
    viewModel: WorldClockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "WORLD MARKET CLOCK",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Global market sessions • Berlin | São Paulo | New York | UTC",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        // Clock grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ClockTile("BERLIN", "CET/CEST", uiState.berlinTime, uiState.berlinDate, Color(0xFF42A5F5))
                ClockTile("SÃO PAULO", "BRT", uiState.saoPauloTime, uiState.saoPauloDate, Color(0xFF66BB6A))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ClockTile("NEW YORK", "EST/EDT", uiState.newYorkTime, uiState.newYorkDate, Color(0xFFFFA726))
                ClockTile("UTC", "GMT", uiState.utcTime, uiState.utcDate, Color(0xFF9E9E9E))
            }
        }

        // Market Sessions
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "EXCHANGE SESSIONS",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(uiState.sessions, key = { it.exchange.name }) { session ->
            MarketSessionCard(session)
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Market hours are approximate. Holiday schedules may affect trading hours.",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun ClockTile(
    city: String,
    tzAbbr: String,
    time: String,
    date: String,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = city,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                ),
                color = accentColor
            )
            Text(
                text = tzAbbr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = time.ifEmpty { "--:--:--" },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = date.ifEmpty { "---" },
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun MarketSessionCard(session: MarketSession) {
    val statusColor = when (session.status) {
        MarketStatus.OPEN -> Color(0xFF4CAF50)
        MarketStatus.CLOSED -> Color(0xFFEF5350)
        MarketStatus.PRE_MARKET -> Color(0xFFFFC107)
        MarketStatus.POST_MARKET -> Color(0xFFFFA726)
        MarketStatus.HOLIDAY -> Color(0xFF9E9E9E)
        MarketStatus.WEEKEND -> Color(0xFF78909C)
        MarketStatus.UNKNOWN -> Color(0xFF757575)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.exchange.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${session.openTime} – ${session.closeTime} (${session.exchange.timezone})",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Status badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = statusColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = session.status.name.replace("_", " "),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = statusColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

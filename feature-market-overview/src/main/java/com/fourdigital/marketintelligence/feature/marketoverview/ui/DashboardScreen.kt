package com.fourdigital.marketintelligence.feature.marketoverview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdigital.marketintelligence.core.ui.components.*
import com.fourdigital.marketintelligence.core.ui.theme.*
import com.fourdigital.marketintelligence.domain.model.MarketRegime
import com.fourdigital.marketintelligence.domain.model.Quote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToWatchlist: () -> Unit = {},
    onNavigateToSignals: () -> Unit = {},
    onNavigateToCorrelations: () -> Unit = {},
    onNavigateToCrossAsset: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToGitHub: () -> Unit = {},
    onNavigateToAITrading: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onNavigateToWorldClock: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "4D Market Intelligence",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TerminalTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cross-Market Intelligence Cockpit",
                        style = MaterialTheme.typography.bodySmall,
                        color = TerminalTextMuted
                    )
                }
                Row {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, "Refresh", tint = AccentCyan)
                    }
                    IconButton(onClick = onNavigateToAITrading) {
                        Icon(Icons.Filled.Psychology, "AI Trading", tint = AccentCyan)
                    }
                    IconButton(onClick = onNavigateToGitHub) {
                        Icon(Icons.Filled.Code, "GitHub", tint = TerminalTextSecondary)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, "Settings", tint = TerminalTextSecondary)
                    }
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(Icons.Filled.Info, "About", tint = TerminalTextSecondary)
                    }
                }
            }
        }

        // Loading / Error state
        if (state.isLoading && state.quotes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading market data...", color = TerminalTextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        state.error?.let { error ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = LossRed.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = error,
                        color = LossRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Market Regime Card
        item {
            MarketRegimeCard(regime = state.marketRegime, onClick = onNavigateToCrossAsset)
        }

        // Quick Stats Row
        item {
            QuickStatsRow(quotes = state.quotes)
        }

        // Key Markets Ticker
        item {
            SectionHeader(title = "Key Markets")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                val keySymbols = listOf("BTC-USD", "^GSPC", "^GDAXI", "EUR/USD", "GC=F", "^BVSP", "CL=F", "ETH-USD")
                items(keySymbols) { symbol ->
                    val quote = state.quotes[symbol]
                    if (quote != null) {
                        CompactQuoteCard(quote = quote)
                    }
                }
            }
        }

        // AI Trading prominent card
        item {
            Spacer(modifier = Modifier.height(8.dp))
            NavCard(
                title = "AI Trading Intelligence",
                subtitle = "Real-time AI analysis + News + Predictions",
                icon = Icons.Filled.Psychology,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToAITrading
            )
        }

        // Navigation Cards
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = "Intelligence Modules")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NavCard(
                    title = "Watchlists",
                    subtitle = "${state.watchlists.size} lists",
                    icon = Icons.Filled.Visibility,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToWatchlist
                )
                NavCard(
                    title = "Signals",
                    subtitle = "AI Analysis",
                    icon = Icons.Filled.Insights,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSignals
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NavCard(
                    title = "Correlations",
                    subtitle = "Cross-asset matrix",
                    icon = Icons.Filled.GridView,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCorrelations
                )
                NavCard(
                    title = "Cross-Asset",
                    subtitle = "Risk regime",
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToCrossAsset
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NavCard(
                    title = "Alerts",
                    subtitle = "Price targets",
                    icon = Icons.Filled.NotificationsActive,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToAlerts
                )
                NavCard(
                    title = "World Clock",
                    subtitle = "Market sessions",
                    icon = Icons.Filled.Language,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToWorldClock
                )
            }
        }

        item {
            NavCard(
                title = "GitHub Intelligence",
                subtitle = "Repos, issues, AI models & code search",
                icon = Icons.Filled.Code,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToGitHub
            )
        }

        // Last refresh
        item {
            if (state.lastRefresh.isNotEmpty()) {
                Text(
                    text = "Last refresh: ${state.lastRefresh}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TerminalTextMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item {
            DisclaimerFooter()
        }
    }
}

@Composable
private fun MarketRegimeCard(regime: MarketRegime, onClick: () -> Unit) {
    val (label, color, description) = when (regime) {
        MarketRegime.RISK_ON -> Triple("RISK-ON", GainGreen, "Equities & risk assets trending higher")
        MarketRegime.RISK_OFF -> Triple("RISK-OFF", LossRed, "Defensive assets outperforming")
        MarketRegime.MIXED -> Triple("MIXED", NeutralAmber, "Conflicting signals across asset classes")
        MarketRegime.TRANSITIONING -> Triple("TRANSITIONING", AccentCyan, "Regime shift in progress")
        MarketRegime.UNKNOWN -> Triple("ASSESSING", TerminalTextMuted, "Gathering market data...")
    }

    TerminalCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Market Regime",
                    style = MaterialTheme.typography.labelMedium,
                    color = TerminalTextSecondary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TerminalTextMuted
                )
            }
        }
    }
}

@Composable
private fun QuickStatsRow(quotes: Map<String, Quote>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val spx = quotes["^GSPC"]
            val dax = quotes["^GDAXI"]
            val ibov = quotes["^BVSP"]

            QuickStatItem("S&P 500", spx?.changePercent, Modifier.weight(1f))
            QuickStatItem("DAX", dax?.changePercent, Modifier.weight(1f))
            QuickStatItem("IBOV", ibov?.changePercent, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val btc = quotes["BTC-USD"]
            val gold = quotes["GC=F"]
            val eur = quotes["EUR/USD"]

            QuickStatItem("BTC", btc?.changePercent, Modifier.weight(1f))
            QuickStatItem("GOLD", gold?.changePercent, Modifier.weight(1f))
            QuickStatItem("EUR/USD", eur?.changePercent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickStatItem(label: String, changePercent: Double?, modifier: Modifier) {
    val color = when {
        changePercent == null -> TerminalTextMuted
        changePercent > 0 -> GainGreen
        changePercent < 0 -> LossRed
        else -> TerminalTextMuted
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = TerminalMediumGray),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TerminalTextMuted)
            Text(
                text = changePercent?.let { "%+.2f%%".format(it) } ?: "—",
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CompactQuoteCard(quote: Quote) {
    val color = if (quote.isPositive) GainGreen else LossRed

    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalMediumGray),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(quote.symbol, style = MaterialTheme.typography.labelMedium, color = TerminalTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "%.2f".format(quote.price),
                style = MaterialTheme.typography.titleMedium,
                color = TerminalTextPrimary,
                fontWeight = FontWeight.Bold
            )
            PriceChangeText(change = quote.change, changePercent = quote.changePercent)
        }
    }
}

@Composable
private fun NavCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = TerminalCardGray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = TerminalTextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TerminalTextMuted)
        }
    }
}

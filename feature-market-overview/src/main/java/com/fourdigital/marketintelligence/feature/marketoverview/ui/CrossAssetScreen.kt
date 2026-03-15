package com.fourdigital.marketintelligence.feature.marketoverview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.fourdigital.marketintelligence.core.ui.theme.*

private val Green = Color(0xFF00E676)
private val Red = Color(0xFFFF5252)
private val Blue = Color(0xFF448AFF)
private val Yellow = Color(0xFFFFD740)
private val BgBlack = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A2E)
private val Muted = Color(0xFF888888)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossAssetScreen(onBack: () -> Unit = {}, viewModel: CrossAssetViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val regime = viewModel.getRiskRegime()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cross-Asset Overview", fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgBlack,
                    titleContentColor = Green,
                    navigationIconContentColor = Blue
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadQuotes() }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Blue)
                    }
                }
            )
        },
        containerColor = BgBlack
    ) { padding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Timestamp
        item {
            if (state.lastRefresh.isNotEmpty()) {
                Text("Last update: ${state.lastRefresh}", color = Muted,
                    fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        state.error?.let { error ->
            item {
                Text("⚠ $error", color = Red, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Risk Regime Card
        item {
            val regimeColor = when (regime) {
                "RISK ON" -> Green
                "RISK OFF" -> Red
                else -> Yellow
            }
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("> MARKET REGIME", color = Green, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(regimeColor))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(regime, color = regimeColor, fontSize = 22.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        when (regime) {
                            "RISK ON" -> "Equities leading, safe havens underperforming — bullish flows"
                            "RISK OFF" -> "Safe havens rallying, equities under pressure — defensive positioning"
                            else -> "Mixed signals across asset classes — no clear directional bias"
                        },
                        color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Asset group cards
        viewModel.assetGroups.forEach { group ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("> ${group.name.uppercase()}", color = Blue, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))

                        group.symbols.forEach { symbol ->
                            val quote = state.quotes[symbol]
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(symbol.removePrefix("^").take(10), color = Color.White,
                                    fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f))
                                if (quote != null) {
                                    Text("${"%.2f".format(quote.price)}", color = Color.White,
                                        fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
                                    val chg = quote.changePercent ?: 0.0
                                    val chgColor = when {
                                        chg > 0 -> Green; chg < 0 -> Red; else -> Muted
                                    }
                                    Text(
                                        "${if (chg > 0) "+" else ""}${"%.2f".format(chg)}%",
                                        color = chgColor, fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(75.dp), textAlign = TextAlign.End
                                    )
                                } else if (state.isLoading) {
                                    Text("...", color = Muted, fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace)
                                } else {
                                    Text("N/A", color = Muted, fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Group summary bar
                        val groupQuotes = group.symbols.mapNotNull { state.quotes[it] }
                        if (groupQuotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val avgChange = groupQuotes.mapNotNull { it.changePercent }.average()
                            val avgColor = when {
                                avgChange > 0.1 -> Green
                                avgChange < -0.1 -> Red
                                else -> Muted
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(avgColor.copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Avg Change", color = Muted, fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace)
                                Text("${"%.2f".format(avgChange)}%", color = avgColor,
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // Cross-asset correlations hint
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("> INTERMARKET SIGNALS", color = Green, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))

                    val sp = state.quotes["^GSPC"]
                    val dax = state.quotes["^GDAXI"]
                    val gold = state.quotes["GLD"]
                    val btc = state.quotes["bitcoin"]

                    val signals = mutableListOf<Pair<String, Color>>()

                    if (sp != null && dax != null) {
                        val spChg = sp.changePercent ?: 0.0
                        val daxChg = dax.changePercent ?: 0.0
                        if (spChg > 0 && daxChg > 0) signals.add("US & EU equities aligned bullish" to Green)
                        else if (spChg < 0 && daxChg < 0) signals.add("US & EU equities aligned bearish" to Red)
                        else signals.add("US/EU equities diverging — watch for convergence" to Yellow)
                    }

                    if (gold != null && sp != null) {
                        val goldChg = gold.changePercent ?: 0.0
                        val spChg = sp.changePercent ?: 0.0
                        if (goldChg > 0 && spChg < 0) signals.add("Gold up, equities down — flight to safety" to Red)
                        else if (goldChg < 0 && spChg > 0) signals.add("Gold down, equities up — risk appetite strong" to Green)
                    }

                    if (btc != null && sp != null) {
                        val btcChg = btc.changePercent ?: 0.0
                        val spChg = sp.changePercent ?: 0.0
                        if (btcChg > 0 && spChg > 0) signals.add("BTC & equities correlated — risk-on mode" to Green)
                        else if (btcChg > 2 && spChg < 0) signals.add("BTC decoupling from equities — watch for macro shift" to Yellow)
                    }

                    if (signals.isEmpty()) {
                        Text("Waiting for quote data...", color = Muted, fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace)
                    } else {
                        signals.forEach { (text, color) ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("● ", color = color, fontSize = 11.sp)
                                Text(text, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "⚠ Cross-asset analysis is for informational purposes only.",
                color = Muted.copy(alpha = 0.6f), fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
    } // Scaffold
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TerminalBlack,
                    titleContentColor = AccentBlue,
                    navigationIconContentColor = AccentBlue
                )
            )
        },
        containerColor = TerminalBlack
    ) { padding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "4D Market Intelligence",
                style = MaterialTheme.typography.headlineMedium,
                color = TerminalTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = AccentBlue
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Developed by Daniel Dorsch",
                style = MaterialTheme.typography.titleMedium,
                color = TerminalTextPrimary
            )
            Text(
                text = "4D Digital Solutions",
                style = MaterialTheme.typography.bodyMedium,
                color = AccentCyan
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Cross-market intelligence cockpit for disciplined\n" +
                        "analysis of German, Brazilian, and global markets.",
                style = MaterialTheme.typography.bodySmall,
                color = TerminalTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Divider(color = TerminalBorderGray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This application provides market intelligence for informational\n" +
                        "purposes only. It does not constitute financial advice.\n" +
                        "All signals are probabilistic and should be used as part\n" +
                        "of a comprehensive analysis framework.",
                style = MaterialTheme.typography.labelSmall,
                color = TerminalTextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
    } // Scaffold
}

package com.fourdigital.marketintelligence.feature.marketoverview.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdigital.marketintelligence.analytics.ai.*
import com.fourdigital.marketintelligence.analytics.prediction.PredictionEngine
import com.fourdigital.marketintelligence.core.network.api.FinnhubNews

private val Green = Color(0xFF00E676)
private val Red = Color(0xFFFF5252)
private val Blue = Color(0xFF448AFF)
private val Yellow = Color(0xFFFFD740)
private val Purple = Color(0xFFAB47BC)
private val BgBlack = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A2E)
private val Muted = Color(0xFF888888)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITradingScreen(onBack: () -> Unit = {}, viewModel: AITradingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tabs = listOf("AI Analysis", "News Feed", "Predictions", "AI Agent")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Trading Intelligence", fontFamily = FontFamily.Monospace, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgBlack,
                    titleContentColor = Green,
                    navigationIconContentColor = Blue
                ),
                actions = {
                    IconButton(onClick = { viewModel.runFullAnalysis() }) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp),
                                color = Blue, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, "Refresh", tint = Blue)
                        }
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
        // Last update timestamp
        item {
            if (state.lastRefresh.isNotEmpty()) {
                Text("Last update: ${state.lastRefresh}", color = Muted,
                    fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Error
        state.error?.let { error ->
            item {
                Text("⚠ $error", color = Red, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Watchlist scope selector
        item {
            if (state.watchlists.isNotEmpty()) {
                Text("> WATCHLIST SCOPE", color = Green, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.watchlists, key = { it.id }) { wl ->
                        val selected = wl.id == state.selectedWatchlistId
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setSelectedWatchlist(wl.id) },
                            label = {
                                Text(
                                    "${wl.name} (${wl.items.size})",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Blue.copy(alpha = 0.2f),
                                selectedLabelColor = Blue,
                                containerColor = CardBg,
                                labelColor = Muted
                            )
                        )
                    }
                }
            }
        }

        // Tab selector
        item {
            ScrollableTabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = CardBg,
                contentColor = Blue,
                edgePadding = 0.dp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = state.selectedTab == idx,
                        onClick = { viewModel.setTab(idx) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (idx == 3 && state.aiAgentAvailable) {
                                    Box(modifier = Modifier.size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Green))
                                }
                                Text(title, color = if (state.selectedTab == idx) Blue else Muted,
                                    fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    )
                }
            }
        }

        // Detail view if selected
        val selectedAnalysis = state.selectedAnalysis
        if (selectedAnalysis != null) {
            item { AnalysisDetailView(selectedAnalysis, state, viewModel) { viewModel.selectAnalysis(null) } }
        } else {
            when (state.selectedTab) {
                0 -> {
                    // Market summary
                    if (state.analyses.isNotEmpty()) {
                        item { MarketSummaryCard(state.analyses) }
                    }

                    // AI analysis cards
                    if (state.analyses.isEmpty() && !state.isLoading) {
                        item {
                            Text("  No analysis data. Add symbols to a watchlist and tap refresh.",
                                color = Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }

                    items(state.analyses, key = { it.symbol }) { analysis ->
                        AIAnalysisCard(analysis) { viewModel.selectAnalysis(analysis) }
                    }
                }
                1 -> {
                    // News feed
                    if (state.newsItems.isEmpty()) {
                        item {
                            Text("  Configure Finnhub API key in Settings for real-time news.",
                                color = Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }
                    items(state.newsItems, key = { it.id }) { news ->
                        NewsCard(news)
                    }
                }
                2 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.requestAIPredictions() },
                                enabled = !state.isLoadingPredictions,
                                colors = ButtonDefaults.buttonColors(containerColor = Blue.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (state.isLoadingPredictions) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Blue,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generating...", color = Blue, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                } else {
                                    Text("Generate AI Predictions", color = Blue, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    if (state.aiPredictions.isNotBlank()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("> AI TIMEFRAME PREDICTIONS (WEEK / MONTH / QUARTER / YEAR)",
                                        color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        state.aiPredictions,
                                        color = Color.White.copy(alpha = 0.92f),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Predictions summary
                    val actionables = state.analyses.filter { it.confidence > 40 }
                    if (actionables.isEmpty()) {
                        item {
                            Text("  No high-confidence predictions yet.",
                                color = Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }

                    // Strong signals first
                    val strongBuys = actionables.filter { it.action == AIAction.STRONG_BUY }
                    val strongSells = actionables.filter { it.action == AIAction.STRONG_SELL }
                    val buys = actionables.filter { it.action == AIAction.BUY }
                    val sells = actionables.filter { it.action == AIAction.SELL }
                    val holds = actionables.filter { it.action == AIAction.HOLD }

                    if (strongBuys.isNotEmpty()) {
                        item { SectionLabel("STRONG BUY SIGNALS", Green) }
                        items(strongBuys, key = { "sb_${it.symbol}" }) { PredictionCard(it) }
                    }
                    if (buys.isNotEmpty()) {
                        item { SectionLabel("BUY SIGNALS", Green.copy(alpha = 0.7f)) }
                        items(buys, key = { "b_${it.symbol}" }) { PredictionCard(it) }
                    }
                    if (holds.isNotEmpty()) {
                        item { SectionLabel("HOLD", Muted) }
                        items(holds, key = { "h_${it.symbol}" }) { PredictionCard(it) }
                    }
                    if (sells.isNotEmpty()) {
                        item { SectionLabel("SELL SIGNALS", Red.copy(alpha = 0.7f)) }
                        items(sells, key = { "s_${it.symbol}" }) { PredictionCard(it) }
                    }
                    if (strongSells.isNotEmpty()) {
                        item { SectionLabel("STRONG SELL SIGNALS", Red) }
                        items(strongSells, key = { "ss_${it.symbol}" }) { PredictionCard(it) }
                    }
                }
                3 -> {
                    // AI Agent tab
                    item { AIAgentTab(state, viewModel) }
                }
            }
        }

        // Disclaimer
        item {
            Text(
                text = "⚠ AI analysis is for informational purposes only. Not financial advice. Always do your own research.",
                color = Muted.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
    } // Scaffold
}

@Composable
private fun MarketSummaryCard(analyses: List<AIAnalysisResult>) {
    val bulls = analyses.count { it.compositeScore > 0.1 }
    val bears = analyses.count { it.compositeScore < -0.1 }
    val neutral = analyses.size - bulls - bears
    val avgScore = analyses.map { it.compositeScore }.average()

    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("> MARKET OVERVIEW", color = Green, fontSize = 12.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryChip("Bullish", bulls, Green)
                SummaryChip("Neutral", neutral, Muted)
                SummaryChip("Bearish", bears, Red)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sentiment bar
            val bullPct = if (analyses.isNotEmpty()) bulls.toFloat() / analyses.size else 0f
            val bearPct = if (analyses.isNotEmpty()) bears.toFloat() / analyses.size else 0f
            Row(
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            ) {
                if (bullPct > 0) Box(modifier = Modifier.weight(bullPct).fillMaxHeight().background(Green))
                if (1 - bullPct - bearPct > 0) Box(modifier = Modifier.weight((1 - bullPct - bearPct).coerceAtLeast(0.01f)).fillMaxHeight().background(Muted.copy(alpha = 0.3f)))
                if (bearPct > 0) Box(modifier = Modifier.weight(bearPct).fillMaxHeight().background(Red))
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "AI Composite: ${"%.2f".format(avgScore)} | ${analyses.size} assets analyzed",
                color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun SummaryChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun AIAnalysisCard(analysis: AIAnalysisResult, onClick: () -> Unit) {
    val actionColor = when (analysis.action) {
        AIAction.STRONG_BUY -> Green
        AIAction.BUY -> Green.copy(alpha = 0.7f)
        AIAction.HOLD -> Yellow
        AIAction.SELL -> Red.copy(alpha = 0.7f)
        AIAction.STRONG_SELL -> Red
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(analysis.symbol, color = Blue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(analysis.assetClass.name, color = Muted, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${"%.2f".format(analysis.currentPrice)}", color = Color.White,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        analysis.action.name.replace("_", " "),
                        color = actionColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(actionColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Score bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("AI Score: ${"%.0f".format(analysis.compositeScore * 100)}%",
                    color = actionColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("Confidence: ${"%.0f".format(analysis.confidence)}%",
                    color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                RiskBadge(analysis.riskLevel)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Timeframe previews
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(analysis.timeframes) { tf ->
                    val tfColor = when (tf.direction) {
                        "UP" -> Green
                        "DOWN" -> Red
                        else -> Muted
                    }
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tfColor.copy(alpha = 0.08f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(tf.timeframe.substringBefore(" ("), color = Muted, fontSize = 9.sp)
                        Text(
                            "${if (tf.estimatedChange > 0) "+" else ""}${"%.1f".format(tf.estimatedChange)}%",
                            color = tfColor, fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Key insight preview
            analysis.insights.firstOrNull()?.let { insight ->
                Text(
                    "→ ${insight.title}",
                    color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun RiskBadge(risk: RiskLevel) {
    val (color, label) = when (risk) {
        RiskLevel.LOW -> Green to "LOW"
        RiskLevel.MODERATE -> Yellow to "MED"
        RiskLevel.HIGH -> Red.copy(alpha = 0.7f) to "HIGH"
        RiskLevel.CRITICAL -> Red to "CRIT"
    }
    Text(
        text = "RISK:$label",
        color = color,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

@Composable
private fun AnalysisDetailView(analysis: AIAnalysisResult, state: AITradingState, viewModel: AITradingViewModel, onBack: () -> Unit) {
    val actionColor = when (analysis.action) {
        AIAction.STRONG_BUY -> Green
        AIAction.BUY -> Green.copy(alpha = 0.7f)
        AIAction.HOLD -> Yellow
        AIAction.SELL -> Red.copy(alpha = 0.7f)
        AIAction.STRONG_SELL -> Red
    }

    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Blue, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(analysis.symbol, color = Blue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    analysis.action.name.replace("_", " "),
                    color = actionColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(actionColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price & Score
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MetricBox("Price", "${"%.2f".format(analysis.currentPrice)}", Color.White)
                MetricBox("AI Score", "${"%.0f".format(analysis.compositeScore * 100)}%", actionColor)
                MetricBox("Confidence", "${"%.0f".format(analysis.confidence)}%", Blue)
                MetricBox("Risk", analysis.riskLevel.name, when (analysis.riskLevel) {
                    RiskLevel.LOW -> Green; RiskLevel.MODERATE -> Yellow
                    RiskLevel.HIGH -> Red.copy(0.7f); RiskLevel.CRITICAL -> Red
                })
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timeframe forecasts
            Text("> TIMEFRAME FORECASTS", color = Green, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
            analysis.timeframes.forEach { tf ->
                val tfColor = when (tf.direction) {
                    "UP" -> Green; "DOWN" -> Red; else -> Muted
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(tf.timeframe, color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text(
                        "${if (tf.estimatedChange > 0) "▲" else if (tf.estimatedChange < 0) "▼" else "▬"} ${"%.1f".format(tf.estimatedChange)}%",
                        color = tfColor, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("${tf.confidence}%", color = Muted, fontSize = 10.sp,
                        modifier = Modifier.width(35.dp))
                }
                Text("  ${tf.keyFactor}", color = Muted.copy(alpha = 0.7f), fontSize = 9.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Insights
            Text("> AI INSIGHTS", color = Green, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
            analysis.insights.forEach { insight ->
                val impactColor = when (insight.impact) {
                    InsightImpact.POSITIVE -> Green
                    InsightImpact.NEGATIVE -> Red
                    InsightImpact.WARNING -> Yellow
                    InsightImpact.NEUTRAL -> Muted
                }
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("● ", color = impactColor, fontSize = 10.sp)
                    Column {
                        Text(insight.title, color = Color.White, fontSize = 11.sp)
                        Text(insight.detail, color = Muted, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Key levels
            if (analysis.keyLevels.isNotEmpty()) {
                Text("> KEY PRICE LEVELS", color = Green, fontSize = 11.sp,
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
                        Text(level.label, color = levelColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("${"%.2f".format(level.price)}", color = Color.White, fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Prediction details
            analysis.prediction?.let { pred ->
                Spacer(modifier = Modifier.height(12.dp))
                Text("> TRADE SETUP", color = Green, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MetricBox("Entry", "${"%.2f".format(pred.entryPrice)}", Blue)
                    MetricBox("Target", "${"%.2f".format(pred.targetPrice)}", Green)
                    MetricBox("Stop", "${"%.2f".format(pred.stopLoss)}", Red)
                    MetricBox("R:R", "${"%.1f".format(pred.riskRewardRatio)}:1", Yellow)
                }

                Spacer(modifier = Modifier.height(6.dp))
                pred.reasoning.forEach { reason ->
                    Text("  • $reason", color = Muted, fontSize = 10.sp)
                }
            }

            // Sentiment
            if (analysis.newsCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("> NEWS SENTIMENT", color = Green, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Text(analysis.sentiment.label, color = when {
                    analysis.sentiment.score > 0.2 -> Green
                    analysis.sentiment.score < -0.2 -> Red
                    else -> Muted
                }, fontSize = 11.sp)
            }

            // AI Deep Analysis (GitHub Models)
            if (state.aiAgentAvailable) {
                val currentModelName = state.availableModels.find { it.id == state.selectedAIModel }?.displayName ?: state.selectedAIModel
                Spacer(modifier = Modifier.height(12.dp))
                Text("> AI DEEP ANALYSIS ($currentModelName)", color = Purple, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))

                if (state.aiAssetAnalysis.isBlank() && !state.isLoadingAIAsset) {
                    Button(
                        onClick = { viewModel.requestAIAssetAnalysis(analysis) },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Purple,
                            modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run AI Deep Analysis", color = Purple, fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                } else if (state.isLoadingAIAsset) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp),
                            color = Purple, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing with $currentModelName...", color = Muted,
                            fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    Text(state.aiAssetAnalysis, color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun AIAgentTab(state: AITradingState, viewModel: AITradingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Status card
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (state.aiAgentAvailable) Green else Red))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (state.aiAgentAvailable) "> AI AGENT: CONNECTED" else "> AI AGENT: OFFLINE",
                        color = if (state.aiAgentAvailable) Green else Red,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val modelName = state.availableModels.find { it.id == state.selectedAIModel }?.displayName ?: state.selectedAIModel
                val providerLabel = when (state.aiProviderMode) {
                    com.fourdigital.marketintelligence.domain.model.AIProviderMode.GITHUB -> "GitHub"
                    com.fourdigital.marketintelligence.domain.model.AIProviderMode.OPENAI -> "OpenAI"
                    com.fourdigital.marketintelligence.domain.model.AIProviderMode.BOTH -> "GitHub + OpenAI"
                }
                Text(
                    if (state.aiAgentAvailable)
                        "$modelName via $providerLabel | Real-time market analysis"
                    else
                        "Add GitHub or OpenAI key in Settings to enable AI analysis",
                    color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace
                )
            }
        }

        // Model Selector
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("> SELECT AI MODEL", color = Green, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))

                state.availableModels.forEach { model ->
                    val isSelected = model.id == state.selectedAIModel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Blue.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { viewModel.selectAIModel(model.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(18.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) Blue else Muted.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(modifier = Modifier.size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(model.displayName, color = if (isSelected) Blue else Color.White,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(model.description, color = Muted, fontSize = 10.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        if (state.aiAgentAvailable) {
            // Market Outlook button
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("> MARKET OUTLOOK", color = Green, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("AI-generated overview combining technical signals, news sentiment, and cross-asset analysis.",
                        color = Muted, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.aiMarketOutlook.isBlank() && !state.isLoadingAIOutlook) {
                        Button(
                            onClick = { viewModel.requestAIMarketOutlook() },
                            colors = ButtonDefaults.buttonColors(containerColor = Blue.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Blue,
                                modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Market Outlook", color = Blue, fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                    } else if (state.isLoadingAIOutlook) {
                        val outlookModel = state.availableModels.find { it.id == state.selectedAIModel }?.displayName ?: state.selectedAIModel
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp),
                                color = Blue, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Analyzing markets with $outlookModel...", color = Muted,
                                fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Text(state.aiMarketOutlook, color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp, lineHeight = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.requestAIMarketOutlook() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Blue,
                                modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh", color = Blue, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Quick asset analysis
            if (state.analyses.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("> QUICK AI ANALYSIS", color = Green, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap any asset for AI deep-dive analysis", color = Muted, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.analyses.take(10)) { analysis ->
                                val actionColor = when (analysis.action) {
                                    AIAction.STRONG_BUY -> Green
                                    AIAction.BUY -> Green.copy(alpha = 0.7f)
                                    AIAction.HOLD -> Yellow
                                    AIAction.SELL -> Red.copy(alpha = 0.7f)
                                    AIAction.STRONG_SELL -> Red
                                }
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(actionColor.copy(alpha = 0.08f))
                                        .clickable { viewModel.selectAnalysis(analysis) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(analysis.symbol, color = Blue, fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold)
                                    Text(analysis.action.name.replace("_", " "), color = actionColor,
                                        fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            // Chat interface
            Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("> ASK AI ANALYST", color = Green, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ask any market question — AI has access to your watchlist data and news",
                        color = Muted, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Chat history
                    state.aiChatMessages.forEach { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = if (msg.role == "user") Arrangement.End else Arrangement.Start
                        ) {
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .clip(RoundedCornerShape(
                                        topStart = 12.dp, topEnd = 12.dp,
                                        bottomStart = if (msg.role == "user") 12.dp else 2.dp,
                                        bottomEnd = if (msg.role == "user") 2.dp else 12.dp
                                    ))
                                    .background(
                                        if (msg.role == "user") Blue.copy(alpha = 0.15f)
                                        else Purple.copy(alpha = 0.1f)
                                    )
                                    .padding(10.dp)
                            ) {
                                if (msg.role == "assistant") {
                                    Text("AI Analyst", color = Purple, fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(msg.content, color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                    }

                    if (state.isLoadingChat) {
                        Row(modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp),
                                color = Purple, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Thinking...", color = Muted, fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input field
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.aiChatInput,
                            onValueChange = { viewModel.updateChatInput(it) },
                            placeholder = { Text("Ask about markets, stocks, strategy...",
                                fontSize = 11.sp, color = Muted) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue,
                                unfocusedBorderColor = Muted.copy(alpha = 0.3f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Blue
                            ),
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.sendChatMessage() },
                            enabled = state.aiChatInput.isNotBlank() && !state.isLoadingChat,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (state.aiChatInput.isNotBlank()) Blue.copy(alpha = 0.2f) else Color.Transparent)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = if (state.aiChatInput.isNotBlank()) Blue else Muted,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(label, color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun NewsCard(news: FinnhubNews) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                news.headline,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (news.summary.isNotBlank()) {
                Text(
                    news.summary,
                    color = Muted,
                    fontSize = 11.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(news.source, color = Blue, fontSize = 10.sp)
                if (news.related.isNotBlank()) {
                    Text(news.related, color = Purple, fontSize = 10.sp)
                }
                val timeAgo = formatTimeAgo(news.datetime)
                Text(timeAgo, color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun PredictionCard(analysis: AIAnalysisResult) {
    val actionColor = when (analysis.action) {
        AIAction.STRONG_BUY -> Green
        AIAction.BUY -> Green.copy(alpha = 0.7f)
        AIAction.HOLD -> Yellow
        AIAction.SELL -> Red.copy(alpha = 0.7f)
        AIAction.STRONG_SELL -> Red
    }

    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(analysis.symbol, color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${"%.2f".format(analysis.currentPrice)}",
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Prediction line with targets
            analysis.prediction?.let { pred ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Entry: ${"%.2f".format(pred.entryPrice)}", color = Muted, fontSize = 10.sp)
                    Text("→ Target: ${"%.2f".format(pred.targetPrice)}", color = Green, fontSize = 10.sp)
                    Text("SL: ${"%.2f".format(pred.stopLoss)}", color = Red, fontSize = 10.sp)
                    Text("R:R ${"%.1f".format(pred.riskRewardRatio)}:1", color = Yellow, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timeframe forecasts inline
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                analysis.timeframes.forEach { tf ->
                    val c = when (tf.direction) { "UP" -> Green; "DOWN" -> Red; else -> Muted }
                    Text(
                        "${tf.timeframe.take(5)}: ${"%.1f".format(tf.estimatedChange)}%",
                        color = c, fontSize = 9.sp, fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("AI: ${"%.0f".format(analysis.compositeScore * 100)}%",
                    color = actionColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Conf: ${"%.0f".format(analysis.confidence)}%",
                    color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        "> $text",
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 8.dp)
    )
}

private fun formatTimeAgo(epochSeconds: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - epochSeconds
    return when {
        diff < 60 -> "just now"
        diff < 3600 -> "${diff / 60}m ago"
        diff < 86400 -> "${diff / 3600}h ago"
        else -> "${diff / 86400}d ago"
    }
}

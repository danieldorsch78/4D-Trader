package com.fourdigital.marketintelligence.feature.watchlist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdigital.marketintelligence.core.ui.components.*
import com.fourdigital.marketintelligence.core.ui.theme.*
import com.fourdigital.marketintelligence.domain.model.Asset
import com.fourdigital.marketintelligence.domain.model.Quote
import com.fourdigital.marketintelligence.domain.model.WatchlistItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel = hiltViewModel(),
    onAssetClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showNewWatchlistDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = TerminalBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = AccentBlue,
                contentColor = TerminalBlack
            ) {
                Icon(Icons.Default.Add, "Add symbol")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(TerminalBlack),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Watchlists",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TerminalTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = { showNewWatchlistDialog = true }) {
                            Icon(Icons.Default.Add, "New watchlist", tint = GainGreen)
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Filled.Refresh, "Refresh", tint = AccentBlue)
                        }
                    }
                }
            }

            // Watchlist selector tabs
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(state.watchlists) { wl ->
                        val selected = wl.id == state.selectedWatchlistId
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.selectWatchlist(wl.id) },
                            label = {
                                Text(
                                    wl.name,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                                selectedLabelColor = AccentBlue,
                                containerColor = TerminalCardGray,
                                labelColor = TerminalTextSecondary
                            ),
                            trailingIcon = if (selected && state.watchlists.size > 1) {
                                {
                                    IconButton(
                                        onClick = { viewModel.deleteWatchlist(wl.id) },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete watchlist",
                                            modifier = Modifier.size(12.dp),
                                            tint = TerminalTextMuted
                                        )
                                    }
                                }
                            } else null
                        )
                    }
                }
            }

            // Error
            if (state.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LossRed.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = state.error ?: "",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = LossRed
                        )
                    }
                }
            }

            // Loading
            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(32.dp))
                    }
                }
            }

            // Watchlist items
            val selectedWl = state.selectedWatchlist
            if (selectedWl != null) {
                items(selectedWl.items, key = { it.symbol }) { item ->
                    SwipeToDismissBox(
                        state = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.removeSymbol(item.symbol)
                                    true
                                } else false
                            }
                        ),
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LossRed.copy(alpha = 0.3f))
                                    .padding(end = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(Icons.Default.Delete, "Delete", tint = LossRed)
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true
                    ) {
                        WatchlistItemRow(
                            item = item,
                            quote = state.quotes[item.symbol],
                            onClick = { onAssetClick(item.symbol) }
                        )
                    }
                }
            }

            if (selectedWl != null && selectedWl.items.isEmpty()) {
                item {
                    Text(
                        text = "No assets in this watchlist.\nTap + to add symbols.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TerminalTextMuted,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }

            item { DisclaimerFooter() }
        }
    }

    // Add symbol dialog
    if (state.showAddDialog) {
        AddSymbolDialog(
            searchQuery = state.searchQuery,
            searchResults = state.searchResults,
            isSearching = state.isSearching,
            onQueryChanged = { viewModel.onSearchQueryChanged(it) },
            onDismiss = { viewModel.dismissAddDialog() },
            onAdd = { viewModel.addSymbol(it) }
        )
    }

    // New watchlist dialog
    if (showNewWatchlistDialog) {
        NewWatchlistDialog(
            onDismiss = { showNewWatchlistDialog = false },
            onCreate = {
                viewModel.createWatchlist(it)
                showNewWatchlistDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSymbolDialog(
    searchQuery: String,
    searchResults: List<Asset>,
    isSearching: Boolean,
    onQueryChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.7f),
        containerColor = TerminalCardGray,
        title = {
            Text("Search & Add Symbol", color = TerminalTextPrimary, fontFamily = FontFamily.Monospace)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChanged,
                    label = { Text("Search stocks, crypto, ETFs…", fontFamily = FontFamily.Monospace) },
                    placeholder = { Text("e.g. Apple, BTC, SAP, iShares…", fontFamily = FontFamily.Monospace) },
                    leadingIcon = { Icon(Icons.Default.Search, "Search", tint = AccentBlue) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                when {
                    isSearching -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(24.dp))
                        }
                    }
                    searchQuery.length < 2 -> {
                        Text(
                            text = "Type at least 2 characters to search across all markets",
                            style = MaterialTheme.typography.bodySmall,
                            color = TerminalTextMuted,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    searchResults.isEmpty() -> {
                        Text(
                            text = "No results found for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = TerminalTextMuted,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(searchResults, key = { it.symbol }) { asset ->
                                SearchResultRow(
                                    asset = asset,
                                    onAdd = { onAdd(asset.symbol) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", fontFamily = FontFamily.Monospace, color = TerminalTextMuted)
            }
        }
    )
}

@Composable
private fun SearchResultRow(
    asset: Asset,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd),
        colors = CardDefaults.cardColors(containerColor = TerminalBlack.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = asset.symbol,
                        style = MaterialTheme.typography.titleSmall,
                        color = TerminalTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(
                        text = asset.assetClass.name,
                        color = when (asset.assetClass.name) {
                            "CRYPTO" -> CryptoOrange
                            "ETF" -> AccentBlue
                            "COMMODITY" -> GoldYellow
                            else -> GainGreen
                        }
                    )
                }
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = TerminalTextMuted,
                    maxLines = 1
                )
            }
            IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, "Add", tint = GainGreen, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun NewWatchlistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TerminalCardGray,
        title = {
            Text("New Watchlist", color = TerminalTextPrimary, fontFamily = FontFamily.Monospace)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Watchlist Name", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                enabled = name.isNotBlank()
            ) {
                Text("CREATE", fontFamily = FontFamily.Monospace, color = GainGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontFamily = FontFamily.Monospace, color = TerminalTextMuted)
            }
        }
    )
}

@Composable
private fun WatchlistItemRow(
    item: WatchlistItem,
    quote: Quote?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = TerminalCardGray),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Symbol & Name
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.symbol.replace(".DE", "").replace(".SA", ""),
                        style = MaterialTheme.typography.titleSmall,
                        color = TerminalTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(
                        text = item.asset.exchange.displayName,
                        color = AccentBlue
                    )
                }
                Text(
                    text = item.asset.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = TerminalTextMuted
                )
            }

            // Right: Price & Change
            if (quote != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatPrice(quote.price, item.asset.currency),
                        style = MaterialTheme.typography.titleSmall,
                        color = TerminalTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    PriceChangeText(change = quote.change, changePercent = quote.changePercent)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DataQualityBadge(quality = quote.dataQuality.name)
                        val ageLabel = formatQuoteAge(quote)
                        if (ageLabel != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = ageLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = LossRed
                            )
                        }
                        if (quote.volume > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatVolume(quote.volume),
                                style = MaterialTheme.typography.labelSmall,
                                color = TerminalTextMuted
                            )
                        }
                    }
                }
            } else {
                Text("—", style = MaterialTheme.typography.bodyMedium, color = TerminalTextMuted)
            }
        }
    }
}

private fun formatPrice(price: Double, currency: String): String {
    val symbol = when (currency) {
        "EUR" -> "€"
        "USD" -> "$"
        "BRL" -> "R$"
        "GBP" -> "£"
        else -> currency
    }
    return "$symbol${"%.2f".format(price)}"
}

private fun formatVolume(volume: Long): String = when {
    volume >= 1_000_000 -> "${"%.1f".format(volume / 1_000_000.0)}M"
    volume >= 1_000 -> "${"%.1f".format(volume / 1_000.0)}K"
    else -> volume.toString()
}

private fun formatQuoteAge(quote: Quote): String? {
    val ageMs = System.currentTimeMillis() - quote.timestamp.toEpochMilliseconds()
    if (ageMs < 5 * 60 * 1000) return null

    val minutes = ageMs / 60_000
    return when {
        minutes < 60 -> "${minutes}m old"
        else -> "${minutes / 60}h old"
    }
}

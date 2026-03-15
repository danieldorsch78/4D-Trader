package com.fourdigital.marketintelligence.feature.alerts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fourdigital.marketintelligence.domain.model.AlertEvent
import com.fourdigital.marketintelligence.domain.model.AlertRule
import com.fourdigital.marketintelligence.domain.model.AlertType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    onBack: () -> Unit = {},
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alerts", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showCreateDialog,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, "Create Alert")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
        ) {
            // Tab selector
            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Text(
                            "RULES (${uiState.rules.size})",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Text(
                            "HISTORY (${uiState.events.size})",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            when (uiState.selectedTab) {
                0 -> AlertRulesList(
                    rules = uiState.rules,
                    onToggle = viewModel::toggleRule,
                    onDelete = viewModel::deleteRule
                )
                1 -> AlertEventsList(
                    events = uiState.events,
                    onDismiss = viewModel::dismissEvent
                )
            }
        }
    }

    // Create alert dialog
    if (uiState.showCreateDialog) {
        CreateAlertDialog(
            onDismiss = viewModel::dismissCreateDialog,
            onCreate = viewModel::createAlert
        )
    }
}

@Composable
private fun AlertRulesList(
    rules: List<AlertRule>,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    if (rules.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "No alert rules configured",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap + to create your first alert",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rules, key = { it.id }) { rule ->
                AlertRuleCard(rule, onToggle, onDelete)
            }
        }
    }
}

@Composable
private fun AlertRuleCard(
    rule: AlertRule,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val typeColor = alertTypeColor(rule.type)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (rule.isEnabled) 0.5f else 0.2f
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rule.symbol,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (rule.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = rule.type.name.replace("_", " "),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            ),
                            color = typeColor
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${rule.message} (threshold: ${"%.2f".format(rule.threshold)})",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (rule.triggerCount > 0) {
                    Text(
                        text = "Triggered ${rule.triggerCount}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            Switch(
                checked = rule.isEnabled,
                onCheckedChange = { onToggle(rule.id) }
            )

            IconButton(onClick = { onDelete(rule.id) }) {
                Icon(
                    Icons.Default.Delete,
                    "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun AlertEventsList(
    events: List<AlertEvent>,
    onDismiss: (String) -> Unit
) {
    if (events.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No alert events yet",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(events, key = { it.id }) { event ->
                AlertEventCard(event, onDismiss)
            }
        }
    }
}

@Composable
private fun AlertEventCard(event: AlertEvent, onDismiss: (String) -> Unit) {
    val typeColor = alertTypeColor(event.type)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = if (event.isDismissed) 0.15f else 0.4f
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.symbol,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = event.type.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = typeColor
                    )
                }
                Text(
                    text = "${event.message} — Value: ${"%.2f".format(event.triggeredValue)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            if (!event.isDismissed) {
                IconButton(onClick = { onDismiss(event.id) }) {
                    Icon(Icons.Default.Close, "Dismiss", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun CreateAlertDialog(
    onDismiss: () -> Unit,
    onCreate: (symbol: String, type: AlertType, threshold: Double, message: String) -> Unit
) {
    var symbol by remember { mutableStateOf("DAX") }
    var selectedType by remember { mutableStateOf(AlertType.PRICE_ABOVE) }
    var threshold by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "CREATE ALERT",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("Symbol") },
                    singleLine = true
                )

                Text("Alert Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(AlertType.PRICE_ABOVE, AlertType.PRICE_BELOW, AlertType.PERCENT_MOVE).forEach { type ->
                        FilterChip(
                            selected = type == selectedType,
                            onClick = { selectedType = type },
                            label = {
                                Text(
                                    type.name.replace("_", " "),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = { Text("Threshold") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val thresholdVal = threshold.toDoubleOrNull() ?: return@TextButton
                    onCreate(symbol, selectedType, thresholdVal, message.ifEmpty { "${selectedType.name} $threshold" })
                }
            ) {
                Text("CREATE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", fontFamily = FontFamily.Monospace)
            }
        }
    )
}

private fun alertTypeColor(type: AlertType): Color = when (type) {
    AlertType.PRICE_ABOVE -> Color(0xFF4CAF50)
    AlertType.PRICE_BELOW -> Color(0xFFEF5350)
    AlertType.PERCENT_MOVE -> Color(0xFFFFC107)
    AlertType.CORRELATION_THRESHOLD -> Color(0xFF42A5F5)
    AlertType.VOLATILITY_THRESHOLD -> Color(0xFFFFA726)
    AlertType.SIGNAL_CONFIDENCE -> Color(0xFF7E57C2)
    AlertType.MARKET_OPEN -> Color(0xFF66BB6A)
    AlertType.MARKET_CLOSE -> Color(0xFFFF7043)
    AlertType.DIVERGENCE_DETECTED -> Color(0xFF26C6DA)
}

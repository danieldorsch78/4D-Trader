package com.fourdigital.marketintelligence.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.fourdigital.marketintelligence.domain.model.AppTheme
import com.fourdigital.marketintelligence.domain.model.RiskProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsState()
    val apiKeys by viewModel.apiKeyState.collectAsState()
    val diagnostics by viewModel.diagnostics.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontFamily = FontFamily.Monospace) },
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
        }
    ) { padding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Configure your 4D Market Intelligence experience",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        // --- Appearance ---
        item { SectionHeader("APPEARANCE") }

        item {
            SettingSegmentedButton(
                label = "Theme",
                options = AppTheme.entries.map { it.name },
                selectedIndex = AppTheme.entries.indexOf(prefs.theme),
                onSelect = { viewModel.updateTheme(AppTheme.entries[it]) }
            )
        }

        // --- Market Defaults ---
        item { SectionHeader("MARKET DEFAULTS") }

        item {
            SettingSegmentedButton(
                label = "Default Market",
                options = listOf("DAX", "B3", "Commodities", "Crypto"),
                selectedIndex = listOf("DAX", "B3", "Commodities", "Crypto").indexOf(prefs.defaultMarket).coerceAtLeast(0),
                onSelect = { viewModel.updateDefaultMarket(listOf("DAX", "B3", "Commodities", "Crypto")[it]) }
            )
        }

        // --- Data & Refresh ---
        item { SectionHeader("DATA & REFRESH") }

        item {
            SettingSlider(
                label = "Refresh Interval",
                value = prefs.refreshIntervalSeconds.toFloat(),
                valueRange = 5f..120f,
                steps = 22,
                format = { "${it.toInt()}s" },
                onValueChange = { viewModel.updateRefreshInterval(it.toInt()) }
            )
        }

        item {
            SettingSwitch(
                label = "Streaming Quotes",
                description = "Live streaming when available",
                checked = prefs.streamingEnabled,
                onCheckedChange = viewModel::updateStreaming
            )
        }

        // --- API Keys ---
        item { SectionHeader("API CONFIGURATION") }

        item {
            ApiKeyField(
                label = "Finnhub",
                description = "Stocks, DAX, indices — finnhub.io",
                value = apiKeys.finnhubKey,
                onSave = viewModel::saveFinnhubKey
            )
        }

        item {
            ApiKeyField(
                label = "Brapi",
                description = "Bovespa / B3 stocks — brapi.dev",
                value = apiKeys.brapiKey,
                onSave = viewModel::saveBrapiKey
            )
        }

        item {
            ApiKeyField(
                label = "TwelveData API Key",
                description = "Optional market data — twelvedata.com",
                value = apiKeys.twelveDataKey,
                onSave = viewModel::saveTwelveDataKey
            )
        }

        item {
            ApiKeyField(
                label = "Massive/Polygon API Key",
                description = "Optional market data — massive.com / polygon.io",
                value = apiKeys.massiveKey,
                onSave = viewModel::saveMassiveKey
            )
        }

        item {
            ApiKeyField(
                label = "GitHub Models PAT",
                description = "AI analysis via GitHub Models (editable)",
                value = apiKeys.githubToken,
                onSave = viewModel::saveGitHubToken
            )
        }

        item {
            ApiKeyField(
                label = "OpenAI API Key",
                description = "AI analysis via OpenAI (editable)",
                value = apiKeys.openAiKey,
                onSave = viewModel::saveOpenAIKey
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = "CoinGecko (Bitcoin, crypto) — No API key needed",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // --- API Diagnostics ---
        item { SectionHeader("API DIAGNOSTICS") }

        item {
            Button(
                onClick = { viewModel.testAllApis() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("TEST ALL CONNECTIONS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        item { ApiStatusCard("CoinGecko", "Free — no key needed", diagnostics.coinGecko) { viewModel.testCoinGecko() } }
        item { ApiStatusCard("Finnhub", "Stocks, indices, WebSocket", diagnostics.finnhub) { viewModel.testFinnhub() } }
        item { ApiStatusCard("Brapi", "Bovespa / B3", diagnostics.brapi) { viewModel.testBrapi() } }
        item { ApiStatusCard("GitHub Models", "AI analysis engine", diagnostics.githubModels) { viewModel.testGitHubModels() } }
        item { ApiStatusCard("OpenAI", "AI analysis engine", diagnostics.openAI) { viewModel.testOpenAI() } }

        // --- AI ENGINE ---
        item { SectionHeader("AI ENGINE") }

        item {
            SettingSegmentedButton(
                label = "AI Provider Mode",
                options = listOf("BOTH", "GITHUB", "OPENAI"),
                selectedIndex = listOf("BOTH", "GITHUB", "OPENAI").indexOf(prefs.aiProviderMode.name).coerceAtLeast(0),
                onSelect = {
                    val mode = when (it) {
                        1 -> com.fourdigital.marketintelligence.domain.model.AIProviderMode.GITHUB
                        2 -> com.fourdigital.marketintelligence.domain.model.AIProviderMode.OPENAI
                        else -> com.fourdigital.marketintelligence.domain.model.AIProviderMode.BOTH
                    }
                    viewModel.updateAIProviderMode(mode)
                }
            )
        }

        item {
            val models = viewModel.availableAIModels
            val selectedModel = models.firstOrNull { it.id == prefs.selectedAIModel } ?: models.first()
            var showModelDialog by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "AI Model",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = selectedModel.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showModelDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedModel.displayName,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            if (showModelDialog) {
                AlertDialog(
                    onDismissRequest = { showModelDialog = false },
                    title = { Text("Select AI Model", fontFamily = FontFamily.Monospace) },
                    text = {
                        Column {
                            models.forEach { model ->
                                val isSelected = model.id == selectedModel.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateSelectedAIModel(model.id)
                                            showModelDialog = false
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateSelectedAIModel(model.id)
                                            showModelDialog = false
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = model.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                        Text(
                                            text = model.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showModelDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }

        item {
            SettingSwitch(
                label = "AI Auto Refresh",
                description = "Automatically refresh AI outlook/predictions based on selected watchlist",
                checked = prefs.aiAutoRefreshEnabled,
                onCheckedChange = viewModel::updateAIAutoRefresh
            )
        }

        item {
            SettingSlider(
                label = "AI Refresh Interval",
                value = prefs.aiAutoRefreshIntervalSeconds.toFloat(),
                valueRange = 15f..300f,
                steps = 18,
                format = { "${it.toInt()}s" },
                onValueChange = { viewModel.updateAIRefreshInterval(it.toInt()) }
            )
        }

        item {
            SettingSlider(
                label = "Target Return",
                value = prefs.targetReturnPercent.toFloat(),
                valueRange = 1f..100f,
                steps = 19,
                format = { "${it.toInt()}%" },
                onValueChange = { viewModel.updateTargetReturn(it.toDouble()) }
            )
        }

        // --- Analytics ---
        item { SectionHeader("ANALYTICS WINDOWS") }

        item {
            SettingSlider(
                label = "Short Window",
                value = prefs.analyticsWindowShort.toFloat(),
                valueRange = 5f..50f,
                steps = 8,
                format = { "${it.toInt()} bars" },
                onValueChange = {
                    viewModel.updateAnalyticsWindows(
                        it.toInt(), prefs.analyticsWindowMedium, prefs.analyticsWindowLong
                    )
                }
            )
        }

        item {
            SettingSlider(
                label = "Medium Window",
                value = prefs.analyticsWindowMedium.toFloat(),
                valueRange = 20f..120f,
                steps = 9,
                format = { "${it.toInt()} bars" },
                onValueChange = {
                    viewModel.updateAnalyticsWindows(
                        prefs.analyticsWindowShort, it.toInt(), prefs.analyticsWindowLong
                    )
                }
            )
        }

        item {
            SettingSlider(
                label = "Long Window",
                value = prefs.analyticsWindowLong.toFloat(),
                valueRange = 60f..250f,
                steps = 18,
                format = { "${it.toInt()} bars" },
                onValueChange = {
                    viewModel.updateAnalyticsWindows(
                        prefs.analyticsWindowShort, prefs.analyticsWindowMedium, it.toInt()
                    )
                }
            )
        }

        // --- Risk ---
        item { SectionHeader("RISK PROFILE") }

        item {
            SettingSegmentedButton(
                label = "Risk Tolerance",
                options = RiskProfile.entries.map { it.name },
                selectedIndex = RiskProfile.entries.indexOf(prefs.riskProfile),
                onSelect = { viewModel.updateRiskProfile(RiskProfile.entries[it]) }
            )
        }

        // --- Notifications ---
        item { SectionHeader("NOTIFICATIONS") }

        item {
            SettingSwitch(
                label = "Push Notifications",
                description = "Alert notifications on price targets",
                checked = prefs.notificationsEnabled,
                onCheckedChange = viewModel::updateNotifications
            )
        }

        item {
            SettingSwitch(
                label = "Alert Sound",
                description = "Play sound on alert trigger",
                checked = prefs.alertSoundEnabled,
                onCheckedChange = viewModel::updateAlertSound
            )
        }

        // --- Developer ---
        item { SectionHeader("DEVELOPER") }

        item {
            SettingSwitch(
                label = "Developer Mode",
                description = "Show debug info and raw data",
                checked = prefs.developerMode,
                onCheckedChange = viewModel::updateDeveloperMode
            )
        }

        if (prefs.developerMode) {
            item {
                SettingSwitch(
                    label = "Debug Panel",
                    description = "Show overlay performance panel",
                    checked = prefs.showDebugPanel,
                    onCheckedChange = viewModel::updateShowDebugPanel
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Provider: ${prefs.providerType.name}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            "Windows: S=${prefs.analyticsWindowShort} M=${prefs.analyticsWindowMedium} L=${prefs.analyticsWindowLong}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
    } // Scaffold
}

// ---- Reusable Setting Components ----

@Composable
private fun SectionHeader(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        ),
        color = MaterialTheme.colorScheme.primary
    )
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    )
}

@Composable
private fun SettingSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    format: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = format(value),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps
            )
        }
    }
}

@Composable
private fun SettingSegmentedButton(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(options.size) { index ->
                    FilterChip(
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        label = {
                            Text(
                                text = options[index],
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    description: String,
    value: String,
    onSave: (String) -> Unit
) {
    var text by remember(value) { mutableStateOf(value) }
    var visible by remember { mutableStateOf(false) }
    val isConfigured = value.isNotBlank()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isConfigured) "CONFIGURED" else "NOT SET",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = if (isConfigured) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Paste your API key here", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                visualTransformation = if (visible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility"
                        )
                    }
                },
                singleLine = true
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSave(text) },
                    enabled = text != value,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save", fontFamily = FontFamily.Monospace)
                }
                if (isConfigured) {
                    OutlinedButton(
                        onClick = {
                            text = ""
                            onSave("")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear", fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApiStatusCard(
    name: String,
    description: String,
    result: ApiTestResult,
    onTest: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                OutlinedButton(onClick = onTest, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Text("Test", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
            if (result.status != ApiTestStatus.IDLE) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (result.status) {
                        ApiTestStatus.TESTING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Testing...",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        ApiTestStatus.SUCCESS -> {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                result.message,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        ApiTestStatus.ERROR -> {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                result.message,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

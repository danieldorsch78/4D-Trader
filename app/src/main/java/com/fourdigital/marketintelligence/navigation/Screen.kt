package com.fourdigital.marketintelligence.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Outlined.Dashboard, Icons.Filled.Dashboard)
    data object Watchlists : Screen("watchlists", "Watchlist", Icons.Outlined.Visibility, Icons.Filled.Visibility)
    data object Correlations : Screen("correlations", "Correlations", Icons.Outlined.GridView, Icons.Filled.GridView)
    data object Signals : Screen("signals", "Signals", Icons.Outlined.Insights, Icons.Filled.Insights)
    data object WorldClock : Screen("world_clock", "Markets", Icons.Outlined.Schedule, Icons.Filled.Schedule)
    data object Alerts : Screen("alerts", "Alerts", Icons.Outlined.NotificationsNone, Icons.Filled.Notifications)
    data object CrossAsset : Screen("cross_asset", "Cross-Asset", Icons.AutoMirrored.Outlined.CompareArrows, Icons.AutoMirrored.Filled.CompareArrows)
    data object Settings : Screen("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
    data object About : Screen("about", "About", Icons.Outlined.Info, Icons.Filled.Info)
    data object GitHubAgent : Screen("github_agent", "GitHub", Icons.Outlined.Code, Icons.Filled.Code)
    data object AITrading : Screen("ai_trading", "AI Trading", Icons.Outlined.Psychology, Icons.Filled.Psychology)
    data object AssetDetail : Screen("asset_detail/{symbol}", "Asset Detail", Icons.AutoMirrored.Outlined.ShowChart, Icons.AutoMirrored.Filled.ShowChart) {
        fun createRoute(symbol: String) = "asset_detail/$symbol"
    }

    companion object {
        val bottomNavItems = listOf(Dashboard, Watchlists, Correlations, Signals, WorldClock)
    }
}

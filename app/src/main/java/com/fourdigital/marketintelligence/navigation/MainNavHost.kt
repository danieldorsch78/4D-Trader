package com.fourdigital.marketintelligence.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.fourdigital.marketintelligence.core.ui.theme.TerminalBlack
import com.fourdigital.marketintelligence.core.ui.theme.TerminalCardGray
import com.fourdigital.marketintelligence.core.ui.theme.TerminalTextMuted
import com.fourdigital.marketintelligence.core.ui.theme.AccentBlue
import com.fourdigital.marketintelligence.feature.watchlist.ui.WatchlistScreen
import com.fourdigital.marketintelligence.feature.correlations.ui.CorrelationsScreen
import com.fourdigital.marketintelligence.feature.signals.ui.SignalCenterScreen
import com.fourdigital.marketintelligence.feature.worldclock.ui.WorldClockScreen
import com.fourdigital.marketintelligence.feature.alerts.ui.AlertsScreen
import com.fourdigital.marketintelligence.feature.settings.ui.SettingsScreen
import com.fourdigital.marketintelligence.feature.marketoverview.ui.DashboardScreen
import com.fourdigital.marketintelligence.feature.marketoverview.ui.CrossAssetScreen
import com.fourdigital.marketintelligence.feature.marketoverview.ui.AboutScreen
import com.fourdigital.marketintelligence.feature.githubsync.ui.GitHubScreen
import com.fourdigital.marketintelligence.feature.marketoverview.ui.AITradingScreen
import com.fourdigital.marketintelligence.feature.marketoverview.ui.AssetDetailScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = Screen.bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        containerColor = TerminalBlack,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = TerminalCardGray,
                    tonalElevation = 0.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AccentBlue,
                                selectedTextColor = AccentBlue,
                                unselectedIconColor = TerminalTextMuted,
                                unselectedTextColor = TerminalTextMuted,
                                indicatorColor = AccentBlue.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToWatchlist = { navController.navigate(Screen.Watchlists.route) },
                    onNavigateToSignals = { navController.navigate(Screen.Signals.route) },
                    onNavigateToCorrelations = { navController.navigate(Screen.Correlations.route) },
                    onNavigateToCrossAsset = { navController.navigate(Screen.CrossAsset.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) },
                    onNavigateToGitHub = { navController.navigate(Screen.GitHubAgent.route) },
                    onNavigateToAITrading = { navController.navigate(Screen.AITrading.route) },
                    onNavigateToAlerts = { navController.navigate(Screen.Alerts.route) },
                    onNavigateToWorldClock = { navController.navigate(Screen.WorldClock.route) }
                )
            }
            composable(Screen.Watchlists.route) {
                WatchlistScreen(
                    onAssetClick = { symbol ->
                        navController.navigate(Screen.AssetDetail.createRoute(symbol))
                    }
                )
            }
            composable(Screen.Correlations.route) { CorrelationsScreen() }
            composable(Screen.Signals.route) { SignalCenterScreen() }
            composable(Screen.WorldClock.route) { WorldClockScreen() }
            composable(Screen.Alerts.route) { AlertsScreen() }
            composable(Screen.CrossAsset.route) { CrossAssetScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.About.route) { AboutScreen() }
            composable(Screen.GitHubAgent.route) { GitHubScreen() }
            composable(Screen.AITrading.route) { AITradingScreen() }
            composable(
                route = Screen.AssetDetail.route,
                arguments = listOf(androidx.navigation.navArgument("symbol") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val symbol = backStackEntry.arguments?.getString("symbol") ?: return@composable
                AssetDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

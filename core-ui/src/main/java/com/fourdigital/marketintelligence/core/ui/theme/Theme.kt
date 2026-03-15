package com.fourdigital.marketintelligence.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlueDark,
    onPrimaryContainer = Color.White,
    secondary = AccentCyan,
    onSecondary = Color.Black,
    secondaryContainer = AccentTeal,
    onSecondaryContainer = Color.White,
    tertiary = NeutralAmber,
    onTertiary = Color.Black,
    background = TerminalBlack,
    onBackground = TerminalTextPrimary,
    surface = TerminalDarkGray,
    onSurface = TerminalTextPrimary,
    surfaceVariant = TerminalCardGray,
    onSurfaceVariant = TerminalTextSecondary,
    outline = TerminalBorderGray,
    outlineVariant = TerminalBorderGray,
    error = LossRed,
    onError = Color.White,
    errorContainer = LossRedMuted,
    onErrorContainer = Color.White,
    inverseSurface = LightSurface,
    inverseOnSurface = LightTextPrimary,
    inversePrimary = AccentBlueDark,
    surfaceTint = Color.Transparent,
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlueDark,
    onPrimary = Color.White,
    primaryContainer = AccentBlue,
    onPrimaryContainer = Color.White,
    secondary = AccentTeal,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCardBg,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFCCCCD5),
    error = LossRed,
    onError = Color.White,
)

@Composable
fun MarketIntelligenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MarketTypography,
        content = content
    )
}

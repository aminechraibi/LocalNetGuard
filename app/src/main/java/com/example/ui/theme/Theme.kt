package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PolishBlueDark,
    onPrimary = PolishOnBlueContainer,
    primaryContainer = PolishBlue,
    onPrimaryContainer = PolishBlueContainer,
    secondary = PolishEmeraldDark,
    onSecondary = PolishOnEmeraldContainer,
    secondaryContainer = PolishEmerald,
    onSecondaryContainer = PolishEmeraldContainer,
    tertiary = AccentAmber,
    background = PolishBackgroundDark,
    surface = PolishSurfaceDark,
    onBackground = PolishOnSurfaceDark,
    onSurface = PolishOnSurfaceDark,
    surfaceVariant = PolishSurfaceVariantDark,
    onSurfaceVariant = PolishOnSurfaceVariantDark,
    error = AccentRed
)

private val LightColorScheme = lightColorScheme(
    primary = PolishBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PolishBlueContainer,
    onPrimaryContainer = PolishOnBlueContainer,
    secondary = PolishEmerald,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = PolishEmeraldContainer,
    onSecondaryContainer = PolishOnEmeraldContainer,
    tertiary = AccentAmber,
    background = PolishBackgroundLight,
    surface = PolishSurfaceLight,
    onBackground = PolishOnSurfaceLight,
    onSurface = PolishOnSurfaceLight,
    surfaceVariant = PolishSurfaceVariantLight,
    onSurfaceVariant = PolishOnSurfaceVariantLight,
    error = AccentRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

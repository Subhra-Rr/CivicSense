package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = CivicBlueLight,
    onPrimary = CivicNavyDark,
    primaryContainer = CivicNavyMedium,
    onPrimaryContainer = CivicBlueLight,
    secondary = CivicTeal,
    onSecondary = TextWhite,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CivicNavyDark,
    onPrimary = TextWhite,
    primaryContainer = CivicBlueLight,
    onPrimaryContainer = CivicBlueDark,
    secondary = CivicBlue,
    onSecondary = TextWhite,
    background = CanvasBackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceCardLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardSubtle,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent civic branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

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
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = DarkNavyPrimaryContainer,
    onPrimaryContainer = DarkNavyOnPrimaryContainer,
    secondary = DarkNavySecondary,
    onSecondary = Color.White,
    secondaryContainer = DarkNavySecondaryContainer,
    onSecondaryContainer = DarkNavyOnSecondaryContainer,
    background = DarkNavyBackground,
    onBackground = DarkNavyOnSecondaryContainer,
    surface = DarkNavySurface,
    onSurface = DarkNavyOnSecondaryContainer,
    surfaceVariant = DarkNavySurfaceVariant,
    onSurfaceVariant = DarkNavySecondary,
    outline = DarkNavyOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = HighDensityPrimary,
    onPrimary = Color.White,
    primaryContainer = HighDensityPrimaryContainer,
    onPrimaryContainer = HighDensityOnPrimaryContainer,
    secondary = HighDensitySecondary,
    onSecondary = Color.White,
    secondaryContainer = HighDensitySecondaryContainer,
    onSecondaryContainer = HighDensityOnSecondaryContainer,
    background = HighDensityBackground,
    onBackground = HighDensityOnSecondaryContainer,
    surface = HighDensitySurface,
    onSurface = HighDensityOnSecondaryContainer,
    surfaceVariant = HighDensitySecondaryContainer,
    onSurfaceVariant = HighDensitySecondary,
    outline = HighDensityOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

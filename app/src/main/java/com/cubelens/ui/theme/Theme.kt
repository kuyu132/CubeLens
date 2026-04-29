package com.cubelens.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
  primary = md_theme_light_primary,
  onPrimary = md_theme_light_onPrimary,
  primaryContainer = md_theme_light_primaryContainer,
  onPrimaryContainer = md_theme_light_onPrimaryContainer,
  secondary = md_theme_light_secondary,
  onSecondary = md_theme_light_onSecondary,
  background = md_theme_light_background,
  onBackground = md_theme_light_onBackground,
  surface = md_theme_light_surface,
  onSurface = md_theme_light_onSurface,
)

private val DarkColors = darkColorScheme(
  primary = md_theme_dark_primary,
  onPrimary = md_theme_dark_onPrimary,
  primaryContainer = md_theme_dark_primaryContainer,
  onPrimaryContainer = md_theme_dark_onPrimaryContainer,
  secondary = md_theme_dark_secondary,
  onSecondary = md_theme_dark_onSecondary,
  background = md_theme_dark_background,
  onBackground = md_theme_dark_onBackground,
  surface = md_theme_dark_surface,
  onSurface = md_theme_dark_onSurface,
)

@Composable
fun CubeLensTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colors = if (darkTheme) DarkColors else LightColors
  MaterialTheme(
    colorScheme = colors,
    typography = Typography,
    content = content,
  )
}


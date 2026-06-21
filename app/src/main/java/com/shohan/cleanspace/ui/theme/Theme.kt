package com.shohan.cleanspace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.shohan.cleanspace.data.models.ThemeMode

private val LightColors = lightColorScheme(
    primary = BluePrimary,
    secondary = TealAccent,
    background = LightBackground,
    surface = LightSurface,
    error = RedAccent
)

private val DarkColors = darkColorScheme(
    primary = BlueLight,
    secondary = TealAccent,
    background = DarkBackground,
    surface = DarkSurface,
    error = RedAccent
)

@Composable
fun CleanSpaceTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = CleanSpaceTypography,
        content = content
    )
}

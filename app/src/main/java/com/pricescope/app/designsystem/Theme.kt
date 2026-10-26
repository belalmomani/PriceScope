package com.pricescope.app.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pricescope.app.core.AppSettings

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3DA5FF),
    onPrimary = Color(0xFF06122B),
    background = Color(0xFF0B1530),
    onBackground = Color(0xFFF2F5FF),
    surface = Color(0xFF13204A),
    onSurface = Color(0xFFF2F5FF),
    surfaceVariant = Color(0xFF1B2B5E),
    onSurfaceVariant = Color(0xFFA9B5DB),
    outline = Color(0xFF3A4C8F),
    secondary = Color(0xFF19C6A6),
    tertiary = Color(0xFFFFB24D),
    error = Color(0xFFFF8A8A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1D6FD1),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFF4F7FF),
    onBackground = Color(0xFF0B1530),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B1530),
    surfaceVariant = Color(0xFFE6ECFA),
    onSurfaceVariant = Color(0xFF4A5A8A),
    outline = Color(0xFFB9C5E6),
    secondary = Color(0xFF0B7D69),
    tertiary = Color(0xFFA85F00),
    error = Color(0xFFB3261E)
)

object Dimens {
    val ScreenPadding = 16.dp
    val CardRadius = 16.dp
    val ControlRadius = 12.dp
    val MinTouch = 48.dp
    val RowHeight = 52.dp
    val Gap = 12.dp
}

@Composable
fun PriceScopeTheme(themeMode: String, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        AppSettings.THEME_LIGHT -> false
        AppSettings.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}

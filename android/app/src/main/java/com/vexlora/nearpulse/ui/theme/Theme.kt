package com.vexlora.nearpulse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = CyberBgDark,
    secondary = NeonViolet,
    onSecondary = TextPrimary,
    tertiary = SosRed,
    background = CyberBgDark,
    surface = CyberSurfaceDark,
    surfaceVariant = CyberSurfaceElevated,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = CyberBorder
)

@Composable
fun VexloraNearPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

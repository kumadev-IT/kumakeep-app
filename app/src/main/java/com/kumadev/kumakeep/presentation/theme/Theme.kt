package com.kumadev.kumakeep.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KumaKeepColorScheme = darkColorScheme(
    primary = AccentOrange,
    secondary = AccentGreen,
    background = BackgroundDeep,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariant,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
)

@Composable
fun KumaKeepTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KumaKeepColorScheme,
        typography = Typography,
        content = content
    )
}
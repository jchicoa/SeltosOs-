package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SeltosDarkColorScheme = darkColorScheme(
    primary = PemexGreen,
    onPrimary = ObsidianBg,
    primaryContainer = CarbonElevated,
    onPrimaryContainer = PemexGreen,
    secondary = PemexRed,
    onSecondary = TelemetryWhite,
    secondaryContainer = CarbonSurface,
    onSecondaryContainer = TelemetryWhite,
    tertiary = PemexGold,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TelemetryWhite,
    surface = CarbonSurface,
    onSurface = TelemetryWhite,
    surfaceVariant = CarbonElevated,
    onSurfaceVariant = SlateLabel,
    outline = CarbonBorder,
    outlineVariant = CarbonBorderGlow,
    error = PemexRed,
    onError = TelemetryWhite
)

private val SeltosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SeltosMateTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SeltosDarkColorScheme,
        typography = SeltosTypography,
        shapes = SeltosShapes,
        content = content
    )
}

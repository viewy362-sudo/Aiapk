package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DexShieldDarkScheme = darkColorScheme(
    primary = EmeraldNeon,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = CyberCyan,
    onSecondary = CyberCyanOnSecondary,
    secondaryContainer = CyberCyanContainer,
    onSecondaryContainer = CyberCyanOnContainer,
    tertiary = AmberWarn,
    onTertiary = ObsidianBg,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    error = CoralAlert,
    onError = ObsidianBg,
    errorContainer = CoralContainer,
    onErrorContainer = CoralOnContainer,
    background = ObsidianBg,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianCardElevated,
    outline = ObsidianOutline
)

private val DexShieldLightScheme = lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = LightSurface,
    primaryContainer = EmeraldLightContainer,
    onPrimaryContainer = EmeraldLightOnContainer,
    secondary = CyanDeep,
    onSecondary = LightSurface,
    secondaryContainer = CyanLightContainer,
    onSecondaryContainer = CyanLightOnContainer,
    tertiary = AmberWarn,
    onTertiary = TextPrimaryLight,
    tertiaryContainer = AmberOnContainer,
    onTertiaryContainer = AmberContainer,
    error = CoralAlert,
    onError = LightSurface,
    errorContainer = CoralOnContainer,
    onErrorContainer = CoralContainer,
    background = LightSlateBg,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightCard,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightCard,
    surfaceContainerHigh = LightCard,
    outline = LightOutline
)

val DexShieldShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DexShieldTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DexShieldDarkScheme else DexShieldLightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = DexShieldShapes,
        content = content
    )
}

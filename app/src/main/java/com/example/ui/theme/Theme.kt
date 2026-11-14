package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BloodyDarkScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = CrimsonOnPrimary,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = CrimsonOnContainer,
    secondary = MoltenAmber,
    onSecondary = MoltenOnSecondary,
    secondaryContainer = MoltenContainer,
    onSecondaryContainer = MoltenOnContainer,
    tertiary = CyanTelemetry,
    onTertiary = CarbonBg,
    tertiaryContainer = EmeraldVerifiedContainer,
    onTertiaryContainer = Color(0xFFA7F3D0),
    error = CoralError,
    onError = CarbonBg,
    errorContainer = CoralErrorContainer,
    onErrorContainer = CoralErrorOnContainer,
    background = CarbonBg,
    onBackground = TextPrimaryDark,
    surface = CarbonSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CarbonCard,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = CarbonSurface,
    surfaceContainer = CarbonCard,
    surfaceContainerHigh = CarbonCardElevated,
    outline = CarbonOutline
)

private val BloodyLightScheme = lightColorScheme(
    primary = CrimsonDeep,
    onPrimary = LightForgeSurface,
    primaryContainer = CrimsonLightContainer,
    onPrimaryContainer = CrimsonLightOnContainer,
    secondary = AmberDeep,
    onSecondary = LightForgeSurface,
    secondaryContainer = AmberLightContainer,
    onSecondaryContainer = AmberLightOnContainer,
    tertiary = CyanTelemetry,
    onTertiary = TextPrimaryLight,
    tertiaryContainer = EmeraldVerifiedContainer,
    onTertiaryContainer = LightForgeSurface,
    error = CoralError,
    onError = LightForgeSurface,
    errorContainer = CoralErrorOnContainer,
    onErrorContainer = CoralErrorContainer,
    background = LightForgeBg,
    onBackground = TextPrimaryLight,
    surface = LightForgeSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightForgeCard,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = LightForgeSurface,
    surfaceContainer = LightForgeCard,
    surfaceContainerHigh = LightForgeCard,
    outline = LightForgeOutline
)

val BloodyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DexShieldTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BloodyDarkScheme else BloodyLightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = BloodyShapes,
        content = content
    )
}

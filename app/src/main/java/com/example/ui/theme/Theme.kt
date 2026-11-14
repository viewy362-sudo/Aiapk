package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val BloodyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DexShieldTheme(
    themePreset: AppThemePreset = AppThemePreset.CRIMSON_FORGE,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = themePreset.colorScheme(),
        typography = AppTypography,
        shapes = themePreset.shapes(),
        content = content
    )
}

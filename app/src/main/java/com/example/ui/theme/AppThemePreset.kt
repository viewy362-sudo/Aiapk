package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppThemePreset(
    val displayName: String,
    val subtitle: String,
    val badge: String,
    val previewPrimary: Color,
    val previewSecondary: Color,
    val previewSurface: Color,
    val isDark: Boolean
) {
    CRIMSON_FORGE(
        displayName = "Crimson Forge",
        subtitle = "Carbon black with blood-crimson & molten amber",
        badge = "SIGNATURE",
        previewPrimary = Color(0xFFFF2E4C),
        previewSecondary = Color(0xFFFFB020),
        previewSurface = Color(0xFF09090D),
        isDark = true
    ),
    CYBER_MATRIX(
        displayName = "Cyber Matrix",
        subtitle = "Tactical pitch terminal with phosphor emerald & cyan",
        badge = "STEALTH",
        previewPrimary = Color(0xFF00F59B),
        previewSecondary = Color(0xFF00E5FF),
        previewSurface = Color(0xFF050C09),
        isDark = true
    ),
    OBSIDIAN_GOLD(
        displayName = "Obsidian Gold",
        subtitle = "Velvet midnight obsidian with 24K amber gold & orchid",
        badge = "VIP LUXURY",
        previewPrimary = Color(0xFFF59E0B),
        previewSecondary = Color(0xFFE879F9),
        previewSurface = Color(0xFF0B0A10),
        isDark = true
    ),
    STEALTH_COBALT(
        displayName = "Stealth Cobalt",
        subtitle = "Aerospace navy deck with electric cobalt & radar coral",
        badge = "AERO",
        previewPrimary = Color(0xFF38BDF8),
        previewSecondary = Color(0xFFFB7185),
        previewSurface = Color(0xFF070B14),
        isDark = true
    ),
    ARCTIC_TACTICAL(
        displayName = "Arctic Tactical",
        subtitle = "Crisp daylight lab slate with crimson & deep teal",
        badge = "DAYLIGHT",
        previewPrimary = Color(0xFFD90429),
        previewSecondary = Color(0xFF0F766E),
        previewSurface = Color(0xFFF4F6FA),
        isDark = false
    );

    fun colorScheme(): ColorScheme = when (this) {
        CRIMSON_FORGE -> darkColorScheme(
            primary = Color(0xFFFF2E4C),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF4D0D1A),
            onPrimaryContainer = Color(0xFFFFD6DD),
            secondary = Color(0xFFFFB020),
            onSecondary = Color(0xFF291800),
            secondaryContainer = Color(0xFF472B00),
            onSecondaryContainer = Color(0xFFFFE5B4),
            tertiary = Color(0xFF38BDF8),
            onTertiary = Color(0xFF09090D),
            tertiaryContainer = Color(0xFF064E3B),
            onTertiaryContainer = Color(0xFFA7F3D0),
            error = Color(0xFFFB7185),
            onError = Color(0xFF09090D),
            errorContainer = Color(0xFF541022),
            onErrorContainer = Color(0xFFFFD9DF),
            background = Color(0xFF09090D),
            onBackground = Color(0xFFF8FAFC),
            surface = Color(0xFF111218),
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF171923),
            onSurfaceVariant = Color(0xFF94A3B8),
            surfaceContainerLow = Color(0xFF111218),
            surfaceContainer = Color(0xFF171923),
            surfaceContainerHigh = Color(0xFF202331),
            outline = Color(0xFF2F3347)
        )

        CYBER_MATRIX -> darkColorScheme(
            primary = Color(0xFF00F59B),
            onPrimary = Color(0xFF022014),
            primaryContainer = Color(0xFF064E35),
            onPrimaryContainer = Color(0xFFB4FFE2),
            secondary = Color(0xFF00E5FF),
            onSecondary = Color(0xFF00242A),
            secondaryContainer = Color(0xFF083D47),
            onSecondaryContainer = Color(0xFFB3F8FF),
            tertiary = Color(0xFFA855F7),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFF064E3B),
            onTertiaryContainer = Color(0xFFA7F3D0),
            error = Color(0xFFFF5370),
            onError = Color(0xFF050C09),
            errorContainer = Color(0xFF4B1120),
            onErrorContainer = Color(0xFFFFD6DF),
            background = Color(0xFF050C09),
            onBackground = Color(0xFFE6FFF5),
            surface = Color(0xFF0B1511),
            onSurface = Color(0xFFE6FFF5),
            surfaceVariant = Color(0xFF111F1A),
            onSurfaceVariant = Color(0xFF8FB8A8),
            surfaceContainerLow = Color(0xFF0B1511),
            surfaceContainer = Color(0xFF111F1A),
            surfaceContainerHigh = Color(0xFF182C25),
            outline = Color(0xFF234237)
        )

        OBSIDIAN_GOLD -> darkColorScheme(
            primary = Color(0xFFF59E0B),
            onPrimary = Color(0xFF261600),
            primaryContainer = Color(0xFF4C3005),
            onPrimaryContainer = Color(0xFFFFE7BA),
            secondary = Color(0xFFE879F9),
            onSecondary = Color(0xFF33063B),
            secondaryContainer = Color(0xFF4A1356),
            onSecondaryContainer = Color(0xFFFBD5FF),
            tertiary = Color(0xFF2DD4BF),
            onTertiary = Color(0xFF04221F),
            tertiaryContainer = Color(0xFF0D443D),
            onTertiaryContainer = Color(0xFFB5FFF4),
            error = Color(0xFFFB7185),
            onError = Color(0xFF0B0A10),
            errorContainer = Color(0xFF4E1122),
            onErrorContainer = Color(0xFFFFD8DF),
            background = Color(0xFF0B0A10),
            onBackground = Color(0xFFFAF5FF),
            surface = Color(0xFF13111C),
            onSurface = Color(0xFFFAF5FF),
            surfaceVariant = Color(0xFF1B1828),
            onSurfaceVariant = Color(0xFFA7A0BF),
            surfaceContainerLow = Color(0xFF13111C),
            surfaceContainer = Color(0xFF1B1828),
            surfaceContainerHigh = Color(0xFF252136),
            outline = Color(0xFF36304E)
        )

        STEALTH_COBALT -> darkColorScheme(
            primary = Color(0xFF38BDF8),
            onPrimary = Color(0xFF032030),
            primaryContainer = Color(0xFF0B3B59),
            onPrimaryContainer = Color(0xFFC4EDFF),
            secondary = Color(0xFFFB7185),
            onSecondary = Color(0xFF360510),
            secondaryContainer = Color(0xFF561224),
            onSecondaryContainer = Color(0xFFFFD6DF),
            tertiary = Color(0xFF34D399),
            onTertiary = Color(0xFF022417),
            tertiaryContainer = Color(0xFF084730),
            onTertiaryContainer = Color(0xFFB3FFE0),
            error = Color(0xFFFF6B8B),
            onError = Color(0xFF070B14),
            errorContainer = Color(0xFF521223),
            onErrorContainer = Color(0xFFFFD6DF),
            background = Color(0xFF070B14),
            onBackground = Color(0xFFF0F6FF),
            surface = Color(0xFF0E1422),
            onSurface = Color(0xFFF0F6FF),
            surfaceVariant = Color(0xFF151E31),
            onSurfaceVariant = Color(0xFF94A6C7),
            surfaceContainerLow = Color(0xFF0E1422),
            surfaceContainer = Color(0xFF151E31),
            surfaceContainerHigh = Color(0xFF1E2942),
            outline = Color(0xFF2C3B5C)
        )

        ARCTIC_TACTICAL -> lightColorScheme(
            primary = Color(0xFFD90429),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFE4E8),
            onPrimaryContainer = Color(0xFF590010),
            secondary = Color(0xFF0F766E),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFCCFBF1),
            onSecondaryContainer = Color(0xFF042F2E),
            tertiary = Color(0xFF7C3AED),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFD1FAE5),
            onTertiaryContainer = Color(0xFF065F46),
            error = Color(0xFFBE123C),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFE4E6),
            onErrorContainer = Color(0xFF881337),
            background = Color(0xFFF4F6FA),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE9EEF6),
            onSurfaceVariant = Color(0xFF475569),
            surfaceContainerLow = Color(0xFFFFFFFF),
            surfaceContainer = Color(0xFFF8FAFC),
            surfaceContainerHigh = Color(0xFFEDF2F7),
            outline = Color(0xFFCBD5E1)
        )
    }

    fun shapes(): Shapes = when (this) {
        CYBER_MATRIX -> Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(10.dp),
            large = RoundedCornerShape(14.dp),
            extraLarge = RoundedCornerShape(18.dp)
        )
        OBSIDIAN_GOLD -> Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(22.dp),
            large = RoundedCornerShape(26.dp),
            extraLarge = RoundedCornerShape(32.dp)
        )
        STEALTH_COBALT -> Shapes(
            extraSmall = RoundedCornerShape(6.dp),
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(24.dp)
        )
        CRIMSON_FORGE, ARCTIC_TACTICAL -> Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp)
        )
    }

    companion object {
        fun fromNameOrDefault(raw: String?): AppThemePreset =
            entries.firstOrNull { it.name == raw } ?: CRIMSON_FORGE

        fun randomDifferentFrom(current: AppThemePreset): AppThemePreset {
            val candidates = entries.filter { it != current }
            return candidates.random()
        }
    }
}

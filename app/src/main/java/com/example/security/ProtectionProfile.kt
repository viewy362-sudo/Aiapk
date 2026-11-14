package com.example.security

import com.example.ui.theme.AppThemePreset

enum class WallProtectionTier(
    val title: String,
    val badge: String,
    val summary: String
) {
    WALL_V1_STANDARD(
        title = "Wall V1 • Standard Shield",
        badge = "BASE",
        summary = "Core anti-tamper, signature lock & crash guard"
    ),
    WALL_V2_VAULT(
        title = "Wall V2 • Deep Asset Vault",
        badge = "PRO",
        summary = "AES-256-GCM asset encryption + HttpCanary & proxy block"
    ),
    WALL_V3_JNI_STUB(
        title = "Wall V3 • Hardened Stub + JNI",
        badge = "PRO",
        summary = "Disguised DEX vault + C++ NDK/InMemoryDexClassLoader kit"
    ),
    WALL_V4_FORTRESS(
        title = "Wall V4 • Maximum Fortress",
        badge = "VIP",
        summary = "All 10 layers + resource shrink + v1/v2 signed APK"
    )
}

data class ProtectionProfile(
    val appName: String = "Bloody Team",
    val packageName: String = "com.bloodyteam.app",
    val expectedSignatureSha256: String = "",
    val dexDisguiseExtension: String = ".png",
    val themePresetName: String = AppThemePreset.CRIMSON_FORGE.name,
    val wallProtectionTier: String = WallProtectionTier.WALL_V4_FORTRESS.name,
    // 10 Core Security Layers
    val dexBytecodeGuard: Boolean = true,
    val assetAes256Encryption: Boolean = true,
    val antiTamperSignature: Boolean = true,
    val antiDebugFridaShield: Boolean = true,
    val stringMetadataObfuscation: Boolean = true,
    val zeroCrashExceptionGuard: Boolean = true,
    val rootMagiskDetection: Boolean = true,
    val emulatorCloneShield: Boolean = true,
    val sslPinningProxyBlock: Boolean = true,
    val screenTapjackingShield: Boolean = true,
    val nativeJniStubPacking: Boolean = true,
    // Resources Confusing & Shrinking
    val shrinkResources: Boolean = true,
    val shrinkDrawablesAndAnim: Boolean = true,
    val shrinkLayoutsAndXml: Boolean = true,
    val stripDebugMetadata: Boolean = true,
    // Configuration & Runtime Policy
    val detectHttpCanary: Boolean = true,
    val customSecurityToast: Boolean = true,
    val customToastMessage: String = "Secured by Bloody Team",
    val dialogCancelable: Boolean = false,
    val checkInternetRequired: Boolean = false,
    val loginValidateGate: Boolean = false,
    // APK Signing Output
    val signOutputApk: Boolean = true,
    val signerCommonName: String = "Bloody Team",
    val signerOrg: String = "Bloody Team Security"
) {
    val enabledLayersCount: Int
        get() = listOf(
            dexBytecodeGuard,
            assetAes256Encryption,
            antiTamperSignature,
            antiDebugFridaShield,
            stringMetadataObfuscation,
            zeroCrashExceptionGuard,
            rootMagiskDetection,
            emulatorCloneShield,
            sslPinningProxyBlock,
            screenTapjackingShield
        ).count { it }

    val resolvedThemePreset: AppThemePreset
        get() = AppThemePreset.fromNameOrDefault(themePresetName)

    val resolvedWallTier: WallProtectionTier
        get() = WallProtectionTier.entries.firstOrNull { it.name == wallProtectionTier }
            ?: WallProtectionTier.WALL_V4_FORTRESS
}

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
    val wallProtectionTier: String = WallProtectionTier.WALL_V1_STANDARD.name,
    // 10 Core Security Layers (all OFF initially until an APK/file is selected)
    val dexBytecodeGuard: Boolean = false,
    val assetAes256Encryption: Boolean = false,
    val antiTamperSignature: Boolean = false,
    val antiDebugFridaShield: Boolean = false,
    val stringMetadataObfuscation: Boolean = false,
    val zeroCrashExceptionGuard: Boolean = false,
    val rootMagiskDetection: Boolean = false,
    val emulatorCloneShield: Boolean = false,
    val sslPinningProxyBlock: Boolean = false,
    val screenTapjackingShield: Boolean = false,
    val nativeJniStubPacking: Boolean = false,
    // Resources Confusing & Shrinking
    val shrinkResources: Boolean = false,
    val shrinkDrawablesAndAnim: Boolean = false,
    val shrinkLayoutsAndXml: Boolean = false,
    val stripDebugMetadata: Boolean = false,
    // Configuration & Runtime Policy
    val detectHttpCanary: Boolean = false,
    val customSecurityToast: Boolean = false,
    val customToastMessage: String = "Secured by Bloody Team",
    val dialogCancelable: Boolean = false,
    val checkInternetRequired: Boolean = false,
    val loginValidateGate: Boolean = false,
    // APK Signing Output
    val signOutputApk: Boolean = false,
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

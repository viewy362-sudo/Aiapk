package com.example.security

import com.example.ui.theme.AppThemePreset

enum class WallProtectionTier(
    val title: String,
    val badge: String,
    val summary: String
) {
    WALL_V1_STANDARD(
        title = "Protection V1",
        badge = "FREE",
        summary = "Standard Shield • Anti-Tamper, Zero-Crash Guard & v1/v2 Sign"
    ),
    WALL_V2_VAULT(
        title = "Protection V2",
        badge = "VIP",
        summary = "Deep Asset Vault • AES-256-GCM + HttpCanary & Proxy Block"
    ),
    WALL_V3_JNI_STUB(
        title = "Protection V3",
        badge = "VIP",
        summary = "Hardened Stub + InMemoryDexClassLoader (Sketchware Safe)"
    ),
    WALL_V4_FORTRESS(
        title = "Protection V4",
        badge = "VIP",
        summary = "Maximum Fortress • All 10 Layers + Resource Rules + Signer"
    ),
    WALL_360_JIAGU(
        title = "Protection 360加固",
        badge = "VIP",
        summary = "360-Style Multi-Layer DEX Shell & Anti-Dump Guard"
    ),
    WALL_KIWI_SEC(
        title = "Protection 几维安全",
        badge = "VIP",
        summary = "Kiwi-Style Bytecode Guard + StringVault + JNI Kit"
    ),
    WALL_IJIAMI(
        title = "Protection 爱加密",
        badge = "VIP",
        summary = "iJiami-Style Anti-Hook + Frida/Xposed + Signature Lock"
    ),
    WALL_TENCENT_LEGU(
        title = "Protection 腾讯加固",
        badge = "VIP",
        summary = "Tencent Legu-Style Proxy Stub + Anti-Debug + Metadata Strip"
    ),
    WALL_BANGCLE(
        title = "Protection 梆梆加固",
        badge = "VIP",
        summary = "Bangcle-Style RAM-Only DEX Vault + Anti-Clone Shield"
    )
}

data class ProtectionProfile(
    val appName: String = "Bloody Team",
    val packageName: String = "com.bloodyteam.app",
    val expectedSignatureSha256: String = "",
    val dexDisguiseExtension: String = ".png",
    val themePresetName: String = AppThemePreset.CRIMSON_FORGE.name,
    val wallProtectionTier: String = "",
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
    // Resources Confusing & Shrinking (all 16 options + strip metadata)
    val shrinkResources: Boolean = false,
    val shrinkAnim: Boolean = false,
    val shrinkDrawablesAndAnim: Boolean = false,
    val shrinkColor: Boolean = false,
    val shrinkAnimator: Boolean = false,
    val shrinkLayoutsAndXml: Boolean = false,
    val shrinkRaw: Boolean = false,
    val shrinkFont: Boolean = false,
    val shrinkMenu: Boolean = false,
    val shrinkMipmap: Boolean = false,
    val shrinkInterpolator: Boolean = false,
    val shrinkNavigation: Boolean = false,
    val shrinkTransition: Boolean = false,
    val shrinkXml: Boolean = false,
    val shrinkResourceXmlV1: Boolean = false,
    val shrinkResourceXmlV2: Boolean = false,
    val stripDebugMetadata: Boolean = false,
    // Configuration & Runtime Policy
    val detectHttpCanary: Boolean = false,
    val customSecurityToast: Boolean = false,
    val customToastMessage: String = "Secured by Bloody Team",
    val dialogCancelable: Boolean = false,
    val checkInternetRequired: Boolean = false,
    val removeDetectVpnProxy: Boolean = false,
    val removeDetectHttpCanary: Boolean = false,
    val removeBundlingSoftware: Boolean = false,
    val removeUpdateAlertDialog: Boolean = false,
    val hideRoot: Boolean = false,
    // Advanced Settings
    val loginValidateGate: Boolean = false,
    val loginValidateV2: Boolean = false,
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

    val selectedResourceConfuseCount: Int
        get() = listOf(
            shrinkResources,
            shrinkAnim,
            shrinkDrawablesAndAnim,
            shrinkColor,
            shrinkAnimator,
            shrinkLayoutsAndXml,
            shrinkRaw,
            shrinkFont,
            shrinkMenu,
            shrinkMipmap,
            shrinkInterpolator,
            shrinkNavigation,
            shrinkTransition,
            shrinkXml,
            shrinkResourceXmlV1,
            shrinkResourceXmlV2
        ).count { it }

    val resolvedThemePreset: AppThemePreset
        get() = AppThemePreset.fromNameOrDefault(themePresetName)

    val resolvedWallTier: WallProtectionTier?
        get() = WallProtectionTier.entries.firstOrNull { it.name == wallProtectionTier }
}

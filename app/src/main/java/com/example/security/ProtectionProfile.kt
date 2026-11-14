package com.example.security

data class ProtectionProfile(
    val appName: String = "Bloody Team",
    val packageName: String = "com.bloodyteam.app",
    val expectedSignatureSha256: String = "",
    val dexDisguiseExtension: String = ".png",
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
    val nativeJniStubPacking: Boolean = true
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
}

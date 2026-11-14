package com.example.security

enum class WarningSeverity {
    CRITICAL,
    WARNING,
    INFO
}

data class CompatibilityWarning(
    val title: String,
    val detail: String,
    val severity: WarningSeverity,
    val actionLabel: String? = null,
    val fixTransform: ((ProtectionProfile) -> ProtectionProfile)? = null
)

data class AutoProfileResult(
    val recommendedProfile: ProtectionProfile,
    val appCategoryTitle: String,
    val appliedReasons: List<String>,
    val warnings: List<CompatibilityWarning>
)

/**
 * Smart Compatibility & Auto-Selection Engine.
 * Inspects the loaded APK or Project ZIP to automatically checkmark crash-safe protections,
 * while letting users freely customize options and immediately warning if a toggle creates a crash risk.
 */
object ApkCompatibilityAnalyzer {

    fun recommendProfile(
        report: ApkInspectionReport,
        current: ProtectionProfile
    ): AutoProfileResult {
        val reasons = mutableListOf<String>()
        val isApk = report.isApkArchive
        val pkg = report.detectedPackageName.ifBlank { current.packageName }
        val perms = report.usesPermissions.map { it.lowercase() }

        val hasInternet = perms.any { it.endsWith(".internet") }
        val isVpnApp = perms.any { it.endsWith("bind_vpn_service") }
        val alreadyPacked = report.existingPackerName != null
        val isHeavyEngine = report.detectedFramework.contains("Flutter") ||
            report.detectedFramework.contains("Unity") ||
            report.detectedFramework.contains("React Native")
        val hasAssetsOrData = report.assetEntriesCount > 0 || report.dataFileEntriesCount > 0
        val hasDex = report.dexReports.isNotEmpty()
        val dexSafe = report.dexReports.all { it.withinDalvik65kLimit }

        val recommended = if (!isApk) {
            reasons.add("Project/Data ZIP detected: set ZIP bundle mode with AES-256-GCM asset & kit encryption")
            current.copy(
                packageName = pkg,
                signOutputApk = false,
                wallProtectionTier = WallProtectionTier.WALL_V3_JNI_STUB.name,
                dexBytecodeGuard = hasDex,
                nativeJniStubPacking = hasDex,
                assetAes256Encryption = hasAssetsOrData,
                antiTamperSignature = true,
                antiDebugFridaShield = true,
                stringMetadataObfuscation = true,
                zeroCrashExceptionGuard = true,
                rootMagiskDetection = true,
                emulatorCloneShield = false,
                sslPinningProxyBlock = true,
                screenTapjackingShield = true,
                shrinkResources = true,
                shrinkDrawablesAndAnim = true,
                shrinkLayoutsAndXml = true,
                stripDebugMetadata = true,
                detectHttpCanary = true,
                checkInternetRequired = false,
                loginValidateGate = false
            )
        } else if (alreadyPacked) {
            reasons.add("Already packed (${report.existingPackerName}): disabled double Stub DEX swap to prevent boot crash")
            reasons.add("Enabled Signature Lock, Zero-Crash Guard & v1/v2 APK Re-Signing")
            current.copy(
                packageName = pkg,
                signOutputApk = true,
                wallProtectionTier = WallProtectionTier.WALL_V1_STANDARD.name,
                dexBytecodeGuard = false,
                nativeJniStubPacking = false,
                assetAes256Encryption = false,
                antiTamperSignature = true,
                antiDebugFridaShield = true,
                stringMetadataObfuscation = true,
                zeroCrashExceptionGuard = true,
                rootMagiskDetection = false,
                emulatorCloneShield = false,
                sslPinningProxyBlock = false,
                screenTapjackingShield = true,
                shrinkResources = false,
                shrinkDrawablesAndAnim = false,
                shrinkLayoutsAndXml = false,
                stripDebugMetadata = report.debugMetadataCount > 0,
                detectHttpCanary = false,
                checkInternetRequired = false,
                loginValidateGate = false
            )
        } else if (isHeavyEngine) {
            reasons.add("${report.detectedFramework}: enabled crash-safe wrapper shields without overwriting native engine bootstrap")
            if (hasAssetsOrData) {
                reasons.add("Data/asset files exported safely to Developer Kit (.enc) so live APK assets never fail to read")
            }
            if (isVpnApp) {
                reasons.add("VPN permission detected: disabled Check VPN to prevent self-blocking")
            }
            current.copy(
                packageName = pkg,
                signOutputApk = true,
                wallProtectionTier = WallProtectionTier.WALL_V2_VAULT.name,
                dexBytecodeGuard = false,
                nativeJniStubPacking = false,
                assetAes256Encryption = hasAssetsOrData,
                antiTamperSignature = true,
                antiDebugFridaShield = true,
                stringMetadataObfuscation = true,
                zeroCrashExceptionGuard = true,
                rootMagiskDetection = true,
                emulatorCloneShield = false,
                sslPinningProxyBlock = hasInternet && !isVpnApp,
                screenTapjackingShield = true,
                shrinkResources = false,
                shrinkDrawablesAndAnim = false,
                shrinkLayoutsAndXml = false,
                stripDebugMetadata = true,
                detectHttpCanary = hasInternet && !isVpnApp,
                checkInternetRequired = false,
                loginValidateGate = false
            )
        } else {
            // Standard Java/Kotlin/Compose/MultiDEX Android APK
            val enableStub = hasDex && dexSafe
            if (enableStub) {
                reasons.add("${report.detectedFramework}: auto-selected InMemoryDexClassLoader Stub DEX + AES-256-GCM packing")
            } else {
                reasons.add("DEX missing or >65K limit: kept original DEX and enabled wrapper shields")
            }
            if (hasAssetsOrData) {
                reasons.add("Detected ${report.assetEntriesCount} asset(s) & ${report.dataFileEntriesCount} data file(s): mirrored safely to encrypted Kit without breaking APK reads")
            }
            if (isVpnApp) {
                reasons.add("VPN service permission found: Check VPN auto-disabled to prevent self-lockout")
            } else if (hasInternet) {
                reasons.add("INTERNET permission found: enabled HttpCanary & Proxy/VPN shields")
            } else {
                reasons.add("Offline app (no INTERNET permission): kept Check Internet & VPN filters OFF")
            }
            reasons.add("Emulator block left OFF by default so protected APK opens cleanly on both emulators & real phones")

            current.copy(
                packageName = pkg,
                signOutputApk = true,
                wallProtectionTier = if (enableStub) {
                    WallProtectionTier.WALL_V3_JNI_STUB.name
                } else {
                    WallProtectionTier.WALL_V2_VAULT.name
                },
                dexBytecodeGuard = enableStub,
                nativeJniStubPacking = enableStub,
                assetAes256Encryption = hasAssetsOrData,
                antiTamperSignature = true,
                antiDebugFridaShield = true,
                stringMetadataObfuscation = true,
                zeroCrashExceptionGuard = true,
                rootMagiskDetection = true,
                emulatorCloneShield = false,
                sslPinningProxyBlock = hasInternet && !isVpnApp,
                screenTapjackingShield = true,
                shrinkResources = false,
                shrinkDrawablesAndAnim = false,
                shrinkLayoutsAndXml = false,
                stripDebugMetadata = true,
                detectHttpCanary = hasInternet && !isVpnApp,
                checkInternetRequired = false,
                loginValidateGate = false
            )
        }

        return AutoProfileResult(
            recommendedProfile = recommended,
            appCategoryTitle = report.detectedFramework,
            appliedReasons = reasons,
            warnings = analyzeWarnings(report, recommended)
        )
    }

    fun analyzeWarnings(
        report: ApkInspectionReport?,
        profile: ProtectionProfile
    ): List<CompatibilityWarning> {
        val warnings = mutableListOf<CompatibilityWarning>()

        if (report != null) {
            val isApk = report.isApkArchive
            val perms = report.usesPermissions.map { it.lowercase() }
            val hasInternet = perms.any { it.endsWith(".internet") }
            val isVpnApp = perms.any { it.endsWith("bind_vpn_service") }
            val isHeavyEngine = report.detectedFramework.contains("Flutter") ||
                report.detectedFramework.contains("Unity") ||
                report.detectedFramework.contains("React Native")

            if (report.existingPackerName != null && (profile.dexBytecodeGuard || profile.nativeJniStubPacking)) {
                warnings += CompatibilityWarning(
                    title = "Double-Packer Boot Crash Risk",
                    detail = "Target APK already uses ${report.existingPackerName}. Injecting a second DEX stub can crash on launch.",
                    severity = WarningSeverity.CRITICAL,
                    actionLabel = "Disable Stub Pack"
                ) { p ->
                    p.copy(dexBytecodeGuard = false, nativeJniStubPacking = false)
                }
            }

            if (isApk && isHeavyEngine && profile.dexBytecodeGuard && profile.nativeJniStubPacking) {
                warnings += CompatibilityWarning(
                    title = "${report.detectedFramework} Crash Risk",
                    detail = "Swapping classes.dex with a runtime stub on Flutter/Unity/RN apps can break native bootstrap. Safer to keep Stub OFF unless tested.",
                    severity = WarningSeverity.CRITICAL,
                    actionLabel = "Use Safe Wrapper"
                ) { p ->
                    p.copy(dexBytecodeGuard = false, nativeJniStubPacking = false)
                }
            }

            if (isApk && isVpnApp && profile.sslPinningProxyBlock) {
                warnings += CompatibilityWarning(
                    title = "VPN App Self-Block Risk",
                    detail = "Target APK uses BIND_VPN_SERVICE. Enabling 'Check VPN' will block the app itself when its VPN tunnel starts.",
                    severity = WarningSeverity.CRITICAL,
                    actionLabel = "Turn Off Check VPN"
                ) { p ->
                    p.copy(sslPinningProxyBlock = false)
                }
            }

            if (isApk && report.hasManifest && !hasInternet && (profile.checkInternetRequired || profile.sslPinningProxyBlock)) {
                warnings += CompatibilityWarning(
                    title = "Offline App Policy Conflict",
                    detail = "No INTERNET permission in AndroidManifest.xml. 'Check Internet' or VPN checks can lock users out of an offline app.",
                    severity = WarningSeverity.WARNING,
                    actionLabel = "Disable Net Checks"
                ) { p ->
                    p.copy(checkInternetRequired = false, sslPinningProxyBlock = false, detectHttpCanary = false)
                }
            }

            if (report.dexReports.any { !it.withinDalvik65kLimit } && profile.dexBytecodeGuard) {
                warnings += CompatibilityWarning(
                    title = "DEX 65K Method Limit Warning",
                    detail = "A DEX file exceeds 65,535 method IDs. Verify multidex output on target devices.",
                    severity = WarningSeverity.WARNING
                )
            }

            if (isApk && profile.assetAes256Encryption && (report.assetEntriesCount > 0 || report.dataFileEntriesCount > 0)) {
                warnings += CompatibilityWarning(
                    title = "Data File Encryption Protected Safely",
                    detail = "Inside signed .apk, assets/ data files remain readable so the app won't crash; encrypted .enc copies + AssetVaultHelper.java are saved inside _bloody_kit.zip.",
                    severity = WarningSeverity.INFO
                )
            }
        }

        if (!profile.zeroCrashExceptionGuard) {
            warnings += CompatibilityWarning(
                title = "Zero-Crash Guard Disabled",
                detail = "Turning off Zero-Crash Guard leaves background thread exceptions unhandled.",
                severity = WarningSeverity.WARNING,
                actionLabel = "Enable Guard"
            ) { p ->
                p.copy(zeroCrashExceptionGuard = true)
            }
        }

        if (profile.emulatorCloneShield) {
            warnings += CompatibilityWarning(
                title = "Emulator Shield Active",
                detail = "App will show a security block dialog if opened inside an Android Emulator (Genymotion, QEMU, BlueStacks, or AI Studio Emulator).",
                severity = WarningSeverity.INFO,
                actionLabel = "Allow Emulators"
            ) { p ->
                p.copy(emulatorCloneShield = false)
            }
        }

        return warnings
    }
}

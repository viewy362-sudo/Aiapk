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
 * Inspects the loaded APK (including Sketchware / Sketchware Pro APKs) or Project ZIP
 * to automatically checkmark crash-safe protections, while letting users freely customize options
 * and warning immediately if a toggle combination risks crashing.
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

        // Start from a clean base keeping user identity/theme settings
        val cleanBase = ProtectionProfile(
            appName = current.appName,
            packageName = pkg,
            expectedSignatureSha256 = current.expectedSignatureSha256,
            dexDisguiseExtension = current.dexDisguiseExtension,
            themePresetName = current.themePresetName,
            customToastMessage = current.customToastMessage,
            signerCommonName = current.signerCommonName,
            signerOrg = current.signerOrg
        )

        val recommended = if (!isApk) {
            reasons.add("Project/Data ZIP detected: enabled AES-256-GCM asset & kit encryption + ProGuard shrink rules")
            cleanBase.copy(
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
                sslPinningProxyBlock = true,
                screenTapjackingShield = true,
                shrinkResources = true,
                shrinkDrawablesAndAnim = true,
                shrinkLayoutsAndXml = true,
                stripDebugMetadata = true,
                detectHttpCanary = true,
                removeBundlingSoftware = true
            )
        } else if (alreadyPacked) {
            reasons.add("Already packed (${report.existingPackerName}): disabled double Stub DEX swap to prevent boot crash")
            reasons.add("Enabled Signature Lock, Zero-Crash Guard & v1/v2 APK Re-Signing")
            cleanBase.copy(
                signOutputApk = true,
                wallProtectionTier = WallProtectionTier.WALL_V1_STANDARD.name,
                antiTamperSignature = true,
                antiDebugFridaShield = true,
                stringMetadataObfuscation = true,
                zeroCrashExceptionGuard = true,
                screenTapjackingShield = true,
                stripDebugMetadata = report.debugMetadataCount > 0
            )
        } else if (isHeavyEngine) {
            reasons.add("${report.detectedFramework}: enabled crash-safe wrapper shields without overwriting native engine bootstrap")
            if (hasAssetsOrData) {
                reasons.add("Data/asset files exported safely to Developer Kit (.enc) so live APK assets never fail to read")
            }
            if (isVpnApp) {
                reasons.add("VPN permission detected: disabled Check VPN to prevent self-blocking")
            }
            cleanBase.copy(
                signOutputApk = true,
                wallProtectionTier = WallProtectionTier.WALL_V2_VAULT.name,
                assetAes256Encryption = hasAssetsOrData,
                antiTamperSignature = true,
                antiDebugFridaShield = true,
                stringMetadataObfuscation = true,
                zeroCrashExceptionGuard = true,
                rootMagiskDetection = true,
                sslPinningProxyBlock = hasInternet && !isVpnApp,
                screenTapjackingShield = true,
                stripDebugMetadata = true,
                detectHttpCanary = hasInternet && !isVpnApp,
                removeBundlingSoftware = true
            )
        } else {
            val enableStub = hasDex && dexSafe
            if (report.isSketchwareApp) {
                reasons.add("Sketchware APK detected: configured minSdk 21+ safe Stub & kept resources.arsc IDs intact (prevents Resources.NotFoundException)")
            } else if (enableStub) {
                reasons.add("${report.detectedFramework}: auto-selected InMemoryDexClassLoader Stub DEX + AES-256-GCM packing")
            } else {
                reasons.add("DEX missing or >65K limit: kept original DEX and enabled wrapper shields")
            }
            if (hasAssetsOrData) {
                reasons.add("Detected ${report.assetEntriesCount} asset(s) & ${report.dataFileEntriesCount} data file(s): safe .enc vault in Kit without breaking APK reads")
            }
            if (isVpnApp) {
                reasons.add("VPN service permission found: Check VPN auto-disabled to prevent self-lockout")
            } else if (hasInternet) {
                reasons.add("INTERNET permission found: enabled HttpCanary & Proxy/VPN shields")
            } else {
                reasons.add("Offline app (no INTERNET permission): kept Check Internet & VPN filters OFF")
            }
            reasons.add("Emulator block, Login Validate & Resource XML shrinking kept OFF by default for 100% crash-free launch")

            cleanBase.copy(
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
                sslPinningProxyBlock = hasInternet && !isVpnApp,
                screenTapjackingShield = true,
                stripDebugMetadata = true,
                detectHttpCanary = hasInternet && !isVpnApp,
                removeBundlingSoftware = true
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
                    detail = "Target APK uses BIND_VPN_SERVICE. Enabling 'Detect VPN Proxy' will block the app itself when its VPN tunnel starts.",
                    severity = WarningSeverity.CRITICAL,
                    actionLabel = "Turn Off VPN Block"
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

            if (isApk && profile.selectedResourceConfuseCount > 0) {
                warnings += CompatibilityWarning(
                    title = if (report.isSketchwareApp) "Sketchware Resource Safety Active" else "Resource Shrinking Safety Mode",
                    detail = "Selected ${profile.selectedResourceConfuseCount} Resource Confusing rule(s) are safely exported into _bloody_kit.zip. Live APK resources.arsc IDs remain intact so getIdentifier() never crashes.",
                    severity = WarningSeverity.INFO
                )
            }
        }

        // Contradictory toggle checks
        if (profile.rootMagiskDetection && profile.hideRoot) {
            warnings += CompatibilityWarning(
                title = "Check Root vs Hide Root Conflict",
                detail = "Both 'Check Root' and 'Hide Root' are ON. At runtime Hide Root will suppress root blocking so rooted devices are allowed.",
                severity = WarningSeverity.WARNING,
                actionLabel = "Keep Hide Root"
            ) { p ->
                p.copy(rootMagiskDetection = false, hideRoot = true)
            }
        }

        if (profile.sslPinningProxyBlock && profile.removeDetectVpnProxy) {
            warnings += CompatibilityWarning(
                title = "Detect VPN vs Remove Detect VPN Conflict",
                detail = "Both 'Detect VPN Proxy' and 'Remove Detect Vpn Proxy' are ON. Remove flag disables VPN blocking.",
                severity = WarningSeverity.WARNING,
                actionLabel = "Keep Remove VPN"
            ) { p ->
                p.copy(sslPinningProxyBlock = false, removeDetectVpnProxy = true)
            }
        }

        if (profile.detectHttpCanary && profile.removeDetectHttpCanary) {
            warnings += CompatibilityWarning(
                title = "HttpCanary Detect vs Remove Conflict",
                detail = "Both 'Detect HttpCanary' and 'Remove Detect HttpCanary' are ON. Remove flag disables sniffer blocking.",
                severity = WarningSeverity.WARNING,
                actionLabel = "Keep Remove Canary"
            ) { p ->
                p.copy(detectHttpCanary = false, removeDetectHttpCanary = true)
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

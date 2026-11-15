package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundFxManager
import com.example.data.ProtectionPreferences
import com.example.security.ApkCompatibilityAnalyzer
import com.example.security.ApkInspectionReport
import com.example.security.AppProtectorPipeline
import com.example.security.CompatibilityWarning
import com.example.security.CryptoVaultEngine
import com.example.security.DexApkInspector
import com.example.security.DexPackerEngine
import com.example.security.GuardedFault
import com.example.security.ObfuscatedStringResult
import com.example.security.ProtectedBuildArtifact
import com.example.security.ProtectionProfile
import com.example.security.RuntimeIntegrityShield
import com.example.security.RuntimeShieldReport
import com.example.security.SketchwareKitGenerator
import com.example.security.StringObfuscatorEngine
import com.example.security.VaultPayloadResult
import com.example.security.WallProtectionTier
import com.example.security.ZeroCrashGuard
import com.example.ui.theme.AppThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class VaultBinaryMode { ENCRYPT, DECRYPT }

data class BinaryVaultResult(
    val sourceFileName: String,
    val outputBytes: ByteArray,
    val suggestedOutputName: String,
    val mode: VaultBinaryMode,
    val entropyBits: Double,
    val summary: String
)

data class DexRamTestResult(
    val originalSize: Int,
    val packedSize: Int,
    val disguisedName: String,
    val entropyBits: Double,
    val roundTripVerified: Boolean,
    val sha256Short: String
)

data class ShieldUiState(
    val isAuditing: Boolean = false,
    val runtimeReport: RuntimeShieldReport? = null,
    val apkReport: ApkInspectionReport? = null,
    val selectedInputUri: Uri? = null,
    val selectedInputName: String = "",
    val selectedInputInspection: ApkInspectionReport? = null,
    val detectedAppCategory: String = "",
    val autoProfileSummary: List<String> = emptyList(),
    val compatibilityWarnings: List<CompatibilityWarning> = emptyList(),
    val protectorPassphrase: String = "BloodyTeam#2026!Key",
    val protectorSecretKey: String = "sk_live_bloody_team_token_2026",
    val isProtecting: Boolean = false,
    val protectionSteps: List<String> = emptyList(),
    val protectedOutput: ProtectedBuildArtifact? = null,
    val protectorError: String? = null,
    val dexRamTest: DexRamTestResult? = null,
    val plainVaultInput: String = "{\n  \"team\": \"Bloody Team\",\n  \"apiEndpoint\": \"https://api.example.invalid/v2\",\n  \"antiTamper\": true\n}",
    val vaultPassphrase: String = "BloodyTeam#2026!Vault",
    val vaultCipherText: String = "",
    val vaultResult: VaultPayloadResult? = null,
    val vaultDecryptedPreview: String = "",
    val vaultStatusMessage: String = "",
    val vaultErrorMessage: String? = null,
    val pendingBinaryVault: BinaryVaultResult? = null,
    val obfuscatorInput: String = "sk_live_bloody_team_sample_key",
    val obfuscatorMethodName: String = "getApiToken",
    val obfuscationResult: ObfuscatedStringResult? = null,
    val statusToast: String? = null
)

class DexShieldViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = ProtectionPreferences(application.applicationContext)

    val profile: StateFlow<ProtectionProfile> = prefs.profileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProtectionProfile()
    )

    val faultLog: StateFlow<List<GuardedFault>> = ZeroCrashGuard.faultLog
    val isGuardActive: StateFlow<Boolean> = ZeroCrashGuard.isGuardActive
    val mainThreadLatencyMs: StateFlow<Long> = ZeroCrashGuard.mainThreadLatencyMs
    val soundEnabled: StateFlow<Boolean> = SoundFxManager.soundEnabled

    private val _uiState = MutableStateFlow(ShieldUiState())
    val uiState: StateFlow<ShieldUiState> = _uiState.asStateFlow()

    init {
        runSelfAudit(playSound = false)
        obfuscateString(playSound = false)
        encryptVaultText(playSound = false)
        runDexRamPackingDemo(playSound = false)
    }

    fun toggleSoundFx() {
        SoundFxManager.toggleSound()
    }

    fun runSelfAudit(playSound: Boolean = true) {
        if (playSound) SoundFxManager.playClick()
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            _uiState.update { it.copy(isAuditing = true) }
            ZeroCrashGuard.probeMainThreadLatency()
            val ctx = getApplication<Application>().applicationContext
            val runtime = withContext(Dispatchers.IO) { RuntimeIntegrityShield.runLiveAudit(ctx) }
            val apk = withContext(Dispatchers.IO) { DexApkInspector.inspectSelfApk(ctx) }
            _uiState.update {
                it.copy(
                    isAuditing = false,
                    runtimeReport = runtime,
                    apkReport = apk
                )
            }
            if (playSound) SoundFxManager.playSuccessChime()
        }
    }

    fun inspectPickedArchive(uri: Uri) {
        SoundFxManager.playClick()
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            _uiState.update { it.copy(isAuditing = true) }
            val ctx = getApplication<Application>().applicationContext
            val displayName = resolveDisplayName(uri) ?: "selected_archive.apk"
            val report = withContext(Dispatchers.IO) {
                DexApkInspector.inspectUri(ctx, uri, displayName)
            }
            val autoResult = ApkCompatibilityAnalyzer.recommendProfile(report, profile.value)
            prefs.saveProfile(autoResult.recommendedProfile)
            _uiState.update {
                it.copy(
                    isAuditing = false,
                    apkReport = report,
                    selectedInputUri = uri,
                    selectedInputName = displayName,
                    selectedInputInspection = report,
                    detectedAppCategory = autoResult.appCategoryTitle,
                    autoProfileSummary = autoResult.appliedReasons,
                    compatibilityWarnings = autoResult.warnings,
                    protectedOutput = null,
                    protectorError = null,
                    statusToast = "Auto-selected ${autoResult.recommendedProfile.enabledLayersCount}/10 layers for $displayName"
                )
            }
            SoundFxManager.playSuccessChime()
        }
    }

    fun selectInputArchive(uri: Uri) {
        SoundFxManager.playClick()
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            val ctx = getApplication<Application>().applicationContext
            val displayName = resolveDisplayName(uri) ?: "input_app.apk"
            _uiState.update { it.copy(isAuditing = true, protectorError = null) }
            val report = withContext(Dispatchers.IO) {
                DexApkInspector.inspectUri(ctx, uri, displayName)
            }
            val autoResult = ApkCompatibilityAnalyzer.recommendProfile(report, profile.value)
            prefs.saveProfile(autoResult.recommendedProfile)
            _uiState.update {
                it.copy(
                    isAuditing = false,
                    selectedInputUri = uri,
                    selectedInputName = displayName,
                    selectedInputInspection = report,
                    detectedAppCategory = autoResult.appCategoryTitle,
                    autoProfileSummary = autoResult.appliedReasons,
                    compatibilityWarnings = autoResult.warnings,
                    protectedOutput = null,
                    protectionSteps = emptyList(),
                    statusToast = "Auto-checked ${autoResult.recommendedProfile.enabledLayersCount}/10 safe shields for ${autoResult.appCategoryTitle}"
                )
            }
            SoundFxManager.playSuccessChime()
        }
    }

    fun loadSelfApkAsInput() {
        SoundFxManager.playClick()
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            val ctx = getApplication<Application>().applicationContext
            _uiState.update { it.copy(isAuditing = true, protectorError = null) }
            val selfFile = File(ctx.applicationInfo.sourceDir)
            val cachedCopy = withContext(Dispatchers.IO) {
                val target = File(ctx.cacheDir, "BloodyTeam_base.apk")
                selfFile.copyTo(target, overwrite = true)
                target
            }
            val uri = Uri.fromFile(cachedCopy)
            val report = withContext(Dispatchers.IO) {
                DexApkInspector.inspectUri(ctx, uri, "BloodyTeam_base.apk")
            }
            val autoResult = ApkCompatibilityAnalyzer.recommendProfile(report, profile.value)
            prefs.saveProfile(autoResult.recommendedProfile)
            _uiState.update {
                it.copy(
                    isAuditing = false,
                    selectedInputUri = uri,
                    selectedInputName = "BloodyTeam_base.apk",
                    selectedInputInspection = report,
                    detectedAppCategory = autoResult.appCategoryTitle,
                    autoProfileSummary = autoResult.appliedReasons,
                    compatibilityWarnings = autoResult.warnings,
                    protectedOutput = null,
                    protectionSteps = emptyList(),
                    statusToast = "Loaded APK & auto-selected ${autoResult.recommendedProfile.enabledLayersCount}/10 shields"
                )
            }
            SoundFxManager.playSuccessChime()
        }
    }

    fun reapplySmartAutoProfile() {
        val report = _uiState.value.selectedInputInspection ?: return
        SoundFxManager.playSuccessChime()
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            val autoResult = ApkCompatibilityAnalyzer.recommendProfile(report, profile.value)
            prefs.saveProfile(autoResult.recommendedProfile)
            _uiState.update {
                it.copy(
                    detectedAppCategory = autoResult.appCategoryTitle,
                    autoProfileSummary = autoResult.appliedReasons,
                    compatibilityWarnings = autoResult.warnings,
                    statusToast = "Re-applied Smart Auto profile (${autoResult.recommendedProfile.enabledLayersCount}/10)"
                )
            }
        }
    }

    fun applyWarningFix(fixTransform: (ProtectionProfile) -> ProtectionProfile) {
        SoundFxManager.playToggle(true)
        updateProfile(fixTransform)
        _uiState.update { it.copy(statusToast = "Applied safe crash-free setting") }
    }

    fun updateProtectorInputs(passphrase: String, secretKey: String) {
        _uiState.update {
            it.copy(
                protectorPassphrase = passphrase,
                protectorSecretKey = secretKey,
                protectorError = null
            )
        }
    }

    fun selectAllSecurityLayers(enabled: Boolean) {
        SoundFxManager.playToggle(enabled)
        updateProfile {
            it.copy(
                dexBytecodeGuard = enabled,
                assetAes256Encryption = enabled,
                antiTamperSignature = enabled,
                antiDebugFridaShield = enabled,
                stringMetadataObfuscation = enabled,
                zeroCrashExceptionGuard = enabled,
                rootMagiskDetection = enabled,
                emulatorCloneShield = enabled,
                sslPinningProxyBlock = enabled,
                screenTapjackingShield = enabled,
                nativeJniStubPacking = enabled
            )
        }
    }

    fun runProtectionPipeline() {
        SoundFxManager.playClick()
        val state = _uiState.value
        val inputUri = state.selectedInputUri
        if (inputUri == null) {
            _uiState.update { it.copy(protectorError = "Select an .apk or .zip application first") }
            return
        }
        if (state.protectorPassphrase.isBlank()) {
            _uiState.update { it.copy(protectorError = "Enter an AES-256 master passphrase") }
            return
        }
        val activeProfile = profile.value
        val hasAnyProtection = activeProfile.enabledLayersCount > 0 ||
            activeProfile.signOutputApk ||
            activeProfile.selectedResourceConfuseCount > 0 ||
            activeProfile.stripDebugMetadata ||
            activeProfile.loginValidateGate ||
            activeProfile.loginValidateV2 ||
            activeProfile.customSecurityToast ||
            activeProfile.detectHttpCanary ||
            activeProfile.checkInternetRequired ||
            activeProfile.removeBundlingSoftware ||
            activeProfile.removeUpdateAlertDialog
        if (!hasAnyProtection) {
            _uiState.update { it.copy(protectorError = "Enable at least 1 security protection layer or tier") }
            return
        }

        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            _uiState.update {
                it.copy(
                    isProtecting = true,
                    protectorError = null,
                    protectedOutput = null,
                    protectionSteps = listOf("Initializing Bloody Team Forge Engine...")
                )
            }
            try {
                val ctx = getApplication<Application>().applicationContext
                val result = withContext(Dispatchers.IO) {
                    AppProtectorPipeline.protectArchive(
                        context = ctx,
                        inputUri = inputUri,
                        inputFileName = state.selectedInputName,
                        profile = activeProfile,
                        passphrase = state.protectorPassphrase,
                        secretToHide = state.protectorSecretKey,
                        onStep = { step ->
                            _uiState.update { cur ->
                                cur.copy(protectionSteps = cur.protectionSteps + step)
                            }
                        }
                    )
                }
                _uiState.update {
                    it.copy(
                        isProtecting = false,
                        protectedOutput = result,
                        statusToast = "Forged ${result.suggestedPrimaryName}"
                    )
                }
                SoundFxManager.playSuccessChime()
            } catch (t: Throwable) {
                ZeroCrashGuard.recordFault("AppProtectorPipeline", Thread.currentThread().name, t)
                _uiState.update {
                    it.copy(
                        isProtecting = false,
                        protectorError = "Protection failed: ${t.message ?: "Invalid archive"}"
                    )
                }
            }
        }
    }

    fun runDexRamPackingDemo(playSound: Boolean = true) {
        if (playSound) SoundFxManager.playClick()
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            val res = withContext(Dispatchers.Default) {
                val sampleDex = ByteArray(2048)
                "dex\n038\u0000".toByteArray(Charsets.US_ASCII).copyInto(sampleDex, 0)
                val buf = ByteBuffer.wrap(sampleDex).order(ByteOrder.LITTLE_ENDIAN)
                buf.putInt(32, sampleDex.size)
                buf.putInt(56, 420)
                buf.putInt(88, 1850)
                for (i in 112 until sampleDex.size) {
                    sampleDex[i] = ((i * 19 + 7) and 0xFF).toByte()
                }
                val pass = _uiState.value.protectorPassphrase.ifBlank { "BloodyTeam#2026!Key" }
                val ext = profile.value.dexDisguiseExtension
                val packed = DexPackerEngine.packDexEntry("classes.dex", sampleDex, pass, ext, 0)
                val unpacked = DexPackerEngine.unpackDexEntry(packed.packedBytes, pass, 0)
                DexRamTestResult(
                    originalSize = packed.originalSizeBytes,
                    packedSize = packed.packedSizeBytes,
                    disguisedName = packed.disguisedAssetName,
                    entropyBits = packed.entropyBitsPerByte,
                    roundTripVerified = unpacked.contentEquals(sampleDex),
                    sha256Short = packed.originalSha256Short
                )
            }
            _uiState.update { it.copy(dexRamTest = res) }
            if (playSound) SoundFxManager.playSuccessChime()
        }
    }

    fun saveProtectedArtifactToUri(destUri: Uri) {
        SoundFxManager.playClick()
        val artifact = _uiState.value.protectedOutput ?: return
        saveFileToUri(artifact.primaryOutputFile, artifact.suggestedPrimaryName, destUri)
    }

    fun saveCompanionKitToUri(destUri: Uri) {
        SoundFxManager.playClick()
        val artifact = _uiState.value.protectedOutput ?: return
        val kitFile = artifact.companionKitFile ?: return
        val kitName = artifact.suggestedKitName ?: "bloody_kit.zip"
        saveFileToUri(kitFile, kitName, destUri)
    }

    private fun saveFileToUri(sourceFile: File, displayName: String, destUri: Uri) {
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            try {
                val ctx = getApplication<Application>().applicationContext
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(destUri)?.use { out ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                        out.flush()
                    } ?: throw IllegalStateException("Cannot open destination URI")
                }
                _uiState.update {
                    it.copy(statusToast = "Saved $displayName")
                }
                SoundFxManager.playSuccessChime()
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(protectorError = "Save failed: ${t.message}")
                }
            }
        }
    }

    fun selectThemePreset(preset: AppThemePreset) {
        SoundFxManager.playToggle(true)
        updateProfile { it.copy(themePresetName = preset.name) }
        _uiState.update { it.copy(statusToast = "Theme: ${preset.displayName}") }
    }

    fun randomizeThemePreset() {
        SoundFxManager.playSuccessChime()
        val next = AppThemePreset.randomDifferentFrom(profile.value.resolvedThemePreset)
        updateProfile { it.copy(themePresetName = next.name) }
        _uiState.update { it.copy(statusToast = "Random Design: ${next.displayName}") }
    }

    fun applyWallTier(tier: WallProtectionTier) {
        SoundFxManager.playToggle(true)
        updateProfile { p ->
            when (tier) {
                WallProtectionTier.WALL_V1_STANDARD -> p.copy(
                    wallProtectionTier = tier.name,
                    antiTamperSignature = true,
                    zeroCrashExceptionGuard = true,
                    dexBytecodeGuard = true,
                    assetAes256Encryption = false,
                    nativeJniStubPacking = false,
                    signOutputApk = true
                )
                WallProtectionTier.WALL_V2_VAULT -> p.copy(
                    wallProtectionTier = tier.name,
                    antiTamperSignature = true,
                    zeroCrashExceptionGuard = true,
                    dexBytecodeGuard = true,
                    assetAes256Encryption = true,
                    stringMetadataObfuscation = true,
                    detectHttpCanary = true,
                    sslPinningProxyBlock = true,
                    nativeJniStubPacking = false,
                    signOutputApk = true
                )
                WallProtectionTier.WALL_V3_JNI_STUB -> p.copy(
                    wallProtectionTier = tier.name,
                    antiTamperSignature = true,
                    zeroCrashExceptionGuard = true,
                    dexBytecodeGuard = true,
                    assetAes256Encryption = true,
                    antiDebugFridaShield = true,
                    stringMetadataObfuscation = true,
                    rootMagiskDetection = true,
                    removeBundlingSoftware = true,
                    nativeJniStubPacking = true,
                    signOutputApk = true
                )
                WallProtectionTier.WALL_V4_FORTRESS -> p.copy(
                    wallProtectionTier = tier.name,
                    dexBytecodeGuard = true,
                    assetAes256Encryption = true,
                    antiTamperSignature = true,
                    antiDebugFridaShield = true,
                    stringMetadataObfuscation = true,
                    zeroCrashExceptionGuard = true,
                    rootMagiskDetection = true,
                    emulatorCloneShield = true,
                    sslPinningProxyBlock = true,
                    screenTapjackingShield = true,
                    nativeJniStubPacking = true,
                    shrinkResources = true,
                    shrinkAnim = true,
                    shrinkDrawablesAndAnim = true,
                    shrinkLayoutsAndXml = true,
                    stripDebugMetadata = true,
                    detectHttpCanary = true,
                    removeBundlingSoftware = true,
                    signOutputApk = true
                )
                WallProtectionTier.WALL_360_JIAGU -> p.copy(
                    wallProtectionTier = tier.name,
                    dexBytecodeGuard = true,
                    nativeJniStubPacking = true,
                    assetAes256Encryption = true,
                    antiTamperSignature = true,
                    antiDebugFridaShield = true,
                    zeroCrashExceptionGuard = true,
                    removeBundlingSoftware = true,
                    stripDebugMetadata = true,
                    signOutputApk = true
                )
                WallProtectionTier.WALL_KIWI_SEC -> p.copy(
                    wallProtectionTier = tier.name,
                    dexBytecodeGuard = true,
                    nativeJniStubPacking = true,
                    stringMetadataObfuscation = true,
                    antiTamperSignature = true,
                    antiDebugFridaShield = true,
                    zeroCrashExceptionGuard = true,
                    shrinkResources = true,
                    shrinkXml = true,
                    signOutputApk = true
                )
                WallProtectionTier.WALL_IJIAMI -> p.copy(
                    wallProtectionTier = tier.name,
                    dexBytecodeGuard = true,
                    nativeJniStubPacking = true,
                    antiDebugFridaShield = true,
                    antiTamperSignature = true,
                    rootMagiskDetection = true,
                    screenTapjackingShield = true,
                    zeroCrashExceptionGuard = true,
                    detectHttpCanary = true,
                    signOutputApk = true
                )
                WallProtectionTier.WALL_TENCENT_LEGU -> p.copy(
                    wallProtectionTier = tier.name,
                    dexBytecodeGuard = true,
                    nativeJniStubPacking = true,
                    assetAes256Encryption = true,
                    antiTamperSignature = true,
                    antiDebugFridaShield = true,
                    zeroCrashExceptionGuard = true,
                    sslPinningProxyBlock = true,
                    stripDebugMetadata = true,
                    signOutputApk = true
                )
                WallProtectionTier.WALL_BANGCLE -> p.copy(
                    wallProtectionTier = tier.name,
                    dexBytecodeGuard = true,
                    nativeJniStubPacking = true,
                    assetAes256Encryption = true,
                    antiTamperSignature = true,
                    stringMetadataObfuscation = true,
                    zeroCrashExceptionGuard = true,
                    removeBundlingSoftware = true,
                    screenTapjackingShield = true,
                    signOutputApk = true
                )
            }
        }
    }

    fun updateVaultInputs(plainText: String, passphrase: String) {
        _uiState.update {
            it.copy(
                plainVaultInput = plainText,
                vaultPassphrase = passphrase,
                vaultErrorMessage = null
            )
        }
    }

    fun updateVaultCipherInput(cipherBase64: String) {
        _uiState.update { it.copy(vaultCipherText = cipherBase64, vaultErrorMessage = null) }
    }

    fun encryptVaultText(playSound: Boolean = true) {
        if (playSound) SoundFxManager.playClick()
        val state = _uiState.value
        if (state.plainVaultInput.isEmpty() || state.vaultPassphrase.isEmpty()) {
            _uiState.update { it.copy(vaultErrorMessage = "Enter payload text and passphrase") }
            return
        }
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            try {
                val res = withContext(Dispatchers.Default) {
                    CryptoVaultEngine.encryptText(state.plainVaultInput, state.vaultPassphrase)
                }
                _uiState.update {
                    it.copy(
                        vaultResult = res,
                        vaultCipherText = res.base64Envelope,
                        vaultDecryptedPreview = "",
                        vaultErrorMessage = null,
                        vaultStatusMessage = "Encrypted ${res.originalBytes}B -> ${res.encryptedBytes}B (AES-256-GCM)"
                    )
                }
                if (playSound) SoundFxManager.playSuccessChime()
            } catch (t: Throwable) {
                _uiState.update { it.copy(vaultErrorMessage = t.message ?: "Encryption failed") }
            }
        }
    }

    fun decryptVaultText() {
        SoundFxManager.playClick()
        val state = _uiState.value
        if (state.vaultCipherText.isBlank() || state.vaultPassphrase.isEmpty()) {
            _uiState.update { it.copy(vaultErrorMessage = "Paste Base64 DXS1 ciphertext and passphrase") }
            return
        }
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            try {
                val plain = withContext(Dispatchers.Default) {
                    CryptoVaultEngine.decryptTextFromBase64(state.vaultCipherText, state.vaultPassphrase)
                }
                _uiState.update {
                    it.copy(
                        vaultDecryptedPreview = plain,
                        vaultErrorMessage = null,
                        vaultStatusMessage = "Authenticated & decrypted ${plain.toByteArray().size}B cleanly"
                    )
                }
                SoundFxManager.playSuccessChime()
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        vaultDecryptedPreview = "",
                        vaultErrorMessage = "Authentication failed: wrong passphrase or tampered GCM tag"
                    )
                }
            }
        }
    }

    fun processBinaryFile(uri: Uri, mode: VaultBinaryMode) {
        SoundFxManager.playClick()
        val passphrase = _uiState.value.vaultPassphrase
        if (passphrase.isEmpty()) {
            _uiState.update { it.copy(vaultErrorMessage = "Enter a passphrase before file vault operation") }
            return
        }
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            try {
                val ctx = getApplication<Application>().applicationContext
                val fileName = resolveDisplayName(uri) ?: "asset.bin"
                val rawBytes = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("Cannot read selected file")
                }
                val result = withContext(Dispatchers.Default) {
                    if (mode == VaultBinaryMode.ENCRYPT) {
                        val enc = CryptoVaultEngine.encryptBytes(rawBytes, passphrase)
                        BinaryVaultResult(
                            sourceFileName = fileName,
                            outputBytes = enc.cipherBytes,
                            suggestedOutputName = "$fileName.enc",
                            mode = VaultBinaryMode.ENCRYPT,
                            entropyBits = enc.entropyBitsPerByte,
                            summary = "Encrypted $fileName (${rawBytes.size}B -> ${enc.cipherBytes.size}B)"
                        )
                    } else {
                        val dec = CryptoVaultEngine.decryptBytes(rawBytes, passphrase)
                        val cleanName = if (fileName.endsWith(".enc")) fileName.removeSuffix(".enc") else "decrypted_$fileName"
                        BinaryVaultResult(
                            sourceFileName = fileName,
                            outputBytes = dec,
                            suggestedOutputName = cleanName,
                            mode = VaultBinaryMode.DECRYPT,
                            entropyBits = CryptoVaultEngine.shannonEntropy(dec),
                            summary = "Decrypted $fileName (${rawBytes.size}B -> ${dec.size}B)"
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        pendingBinaryVault = result,
                        vaultErrorMessage = null,
                        vaultStatusMessage = result.summary
                    )
                }
                SoundFxManager.playSuccessChime()
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(vaultErrorMessage = "File vault error: ${t.message ?: "Invalid DXS1 or key"}")
                }
            }
        }
    }

    fun saveBinaryVaultOutput(uri: Uri) {
        SoundFxManager.playClick()
        val pending = _uiState.value.pendingBinaryVault ?: return
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            try {
                val ctx = getApplication<Application>().applicationContext
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(pending.outputBytes)
                        out.flush()
                    } ?: throw IllegalStateException("Cannot open destination file")
                }
                _uiState.update {
                    it.copy(
                        statusToast = "Saved ${pending.suggestedOutputName}",
                        vaultStatusMessage = "Saved ${pending.outputBytes.size}B to ${pending.suggestedOutputName}"
                    )
                }
                SoundFxManager.playSuccessChime()
            } catch (t: Throwable) {
                _uiState.update { it.copy(vaultErrorMessage = "Save failed: ${t.message}") }
            }
        }
    }

    fun updateObfuscatorInput(secret: String, methodName: String) {
        _uiState.update { it.copy(obfuscatorInput = secret, obfuscatorMethodName = methodName) }
    }

    fun obfuscateString(playSound: Boolean = true) {
        if (playSound) SoundFxManager.playClick()
        val state = _uiState.value
        if (state.obfuscatorInput.isEmpty()) return
        val result = StringObfuscatorEngine.obfuscate(state.obfuscatorInput, state.obfuscatorMethodName)
        _uiState.update { it.copy(obfuscationResult = result) }
        if (playSound) SoundFxManager.playSuccessChime()
    }

    fun triggerCrashGuardSimulation() {
        SoundFxManager.playClick()
        viewModelScope.launch(Dispatchers.IO + ZeroCrashGuard.coroutineExceptionHandler) {
            ZeroCrashGuard.simulateGuardedWorkerFault()
            ZeroCrashGuard.probeMainThreadLatency()
            _uiState.update {
                it.copy(statusToast = "Fault intercepted safely — zero crash, zero ANR")
            }
            SoundFxManager.playSuccessChime()
        }
    }

    fun clearFaultLogs() {
        SoundFxManager.playClick()
        ZeroCrashGuard.clearLogs()
    }

    fun updateProfile(transform: (ProtectionProfile) -> ProtectionProfile) {
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            val next = transform(profile.value)
            prefs.saveProfile(next)
            _uiState.update { cur ->
                cur.copy(
                    compatibilityWarnings = ApkCompatibilityAnalyzer.analyzeWarnings(
                        report = cur.selectedInputInspection,
                        profile = next
                    )
                )
            }
        }
    }

    fun adoptCurrentDeviceCert() {
        SoundFxManager.playClick()
        val cert = _uiState.value.runtimeReport?.signingCertSha256 ?: return
        if (cert != "UNAVAILABLE") {
            updateProfile { it.copy(expectedSignatureSha256 = cert) }
            _uiState.update { it.copy(statusToast = "Applied device signing SHA-256") }
        }
    }

    fun buildCombinedExportBundle(): String {
        val p = profile.value
        return buildString {
            appendLine(SketchwareKitGenerator.generateSketchwareJavaGuard(p))
            appendLine()
            appendLine(SketchwareKitGenerator.generateAssetVaultHelperJava(p))
            appendLine()
            appendLine(SketchwareKitGenerator.generateSslPinningJava(p))
            appendLine()
            appendLine(DexPackerEngine.generateStubApplicationJava(p))
            appendLine()
            appendLine(DexPackerEngine.generateNativeJniCpp(p))
            appendLine()
            appendLine(SketchwareKitGenerator.generateProguardRules(p))
        }
    }

    fun clearStatusToast() {
        _uiState.update { it.copy(statusToast = null) }
    }

    private fun resolveDisplayName(uri: Uri): String? {
        val ctx = getApplication<Application>().applicationContext
        return ZeroCrashGuard.runGuarded("resolveDisplayName", null) {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }
    }
}

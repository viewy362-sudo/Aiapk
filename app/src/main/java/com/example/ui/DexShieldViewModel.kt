package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProtectionPreferences
import com.example.security.ApkInspectionReport
import com.example.security.AppProtectorPipeline
import com.example.security.CryptoVaultEngine
import com.example.security.DexApkInspector
import com.example.security.GuardedFault
import com.example.security.ObfuscatedStringResult
import com.example.security.ProtectedBuildArtifact
import com.example.security.ProtectionProfile
import com.example.security.RuntimeIntegrityShield
import com.example.security.RuntimeShieldReport
import com.example.security.SketchwareKitGenerator
import com.example.security.StringObfuscatorEngine
import com.example.security.VaultPayloadResult
import com.example.security.ZeroCrashGuard
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

enum class VaultBinaryMode { ENCRYPT, DECRYPT }

data class BinaryVaultResult(
    val sourceFileName: String,
    val outputBytes: ByteArray,
    val suggestedOutputName: String,
    val mode: VaultBinaryMode,
    val entropyBits: Double,
    val summary: String
)

data class ShieldUiState(
    val isAuditing: Boolean = false,
    val runtimeReport: RuntimeShieldReport? = null,
    val apkReport: ApkInspectionReport? = null,
    val selectedInputUri: Uri? = null,
    val selectedInputName: String = "",
    val selectedInputInspection: ApkInspectionReport? = null,
    val protectorPassphrase: String = "DexShield#2026!Vault",
    val protectorSecretKey: String = "sk_live_protected_token_2026",
    val isProtecting: Boolean = false,
    val protectionSteps: List<String> = emptyList(),
    val protectedOutput: ProtectedBuildArtifact? = null,
    val protectorError: String? = null,
    val plainVaultInput: String = "{\n  \"team\": \"BLOODY TEAM\",\n  \"apiEndpoint\": \"https://api.example.invalid/v2\",\n  \"antiTamper\": true\n}",
    val vaultPassphrase: String = "DexShield#2026!Key",
    val vaultCipherText: String = "",
    val vaultResult: VaultPayloadResult? = null,
    val vaultDecryptedPreview: String = "",
    val vaultStatusMessage: String = "",
    val vaultErrorMessage: String? = null,
    val pendingBinaryVault: BinaryVaultResult? = null,
    val obfuscatorInput: String = "sk_live_dexshield_sample_secret_key",
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

    private val _uiState = MutableStateFlow(ShieldUiState())
    val uiState: StateFlow<ShieldUiState> = _uiState.asStateFlow()

    init {
        runSelfAudit()
        obfuscateString()
        encryptVaultText()
    }

    fun runSelfAudit() {
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
        }
    }

    fun inspectPickedArchive(uri: Uri) {
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            _uiState.update { it.copy(isAuditing = true) }
            val ctx = getApplication<Application>().applicationContext
            val displayName = resolveDisplayName(uri) ?: "selected_archive.apk"
            val report = withContext(Dispatchers.IO) {
                DexApkInspector.inspectUri(ctx, uri, displayName)
            }
            _uiState.update {
                it.copy(
                    isAuditing = false,
                    apkReport = report,
                    selectedInputUri = uri,
                    selectedInputName = displayName,
                    selectedInputInspection = report,
                    protectedOutput = null,
                    protectorError = null,
                    statusToast = "Loaded $displayName (${report.dexReports.size} DEX)"
                )
            }
        }
    }

    fun selectInputArchive(uri: Uri) {
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            val ctx = getApplication<Application>().applicationContext
            val displayName = resolveDisplayName(uri) ?: "input_app.apk"
            _uiState.update { it.copy(isAuditing = true, protectorError = null) }
            val report = withContext(Dispatchers.IO) {
                DexApkInspector.inspectUri(ctx, uri, displayName)
            }
            _uiState.update {
                it.copy(
                    isAuditing = false,
                    selectedInputUri = uri,
                    selectedInputName = displayName,
                    selectedInputInspection = report,
                    protectedOutput = null,
                    protectionSteps = emptyList(),
                    statusToast = "Selected $displayName"
                )
            }
        }
    }

    fun loadSelfApkAsInput() {
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            val ctx = getApplication<Application>().applicationContext
            _uiState.update { it.copy(isAuditing = true, protectorError = null) }
            val selfFile = File(ctx.applicationInfo.sourceDir)
            val cachedCopy = withContext(Dispatchers.IO) {
                val target = File(ctx.cacheDir, "DexShield_base.apk")
                selfFile.copyTo(target, overwrite = true)
                target
            }
            val uri = Uri.fromFile(cachedCopy)
            val report = withContext(Dispatchers.IO) {
                DexApkInspector.inspectUri(ctx, uri, "DexShield_base.apk")
            }
            _uiState.update {
                it.copy(
                    isAuditing = false,
                    selectedInputUri = uri,
                    selectedInputName = "DexShield_base.apk",
                    selectedInputInspection = report,
                    protectedOutput = null,
                    protectionSteps = emptyList(),
                    statusToast = "Loaded current APK as input"
                )
            }
        }
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
                screenTapjackingShield = enabled
            )
        }
    }

    fun runProtectionPipeline() {
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
        if (activeProfile.enabledLayersCount == 0) {
            _uiState.update { it.copy(protectorError = "Enable at least 1 security protection layer") }
            return
        }

        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            _uiState.update {
                it.copy(
                    isProtecting = true,
                    protectorError = null,
                    protectedOutput = null,
                    protectionSteps = listOf("Initializing DexShield Protection Engine...")
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
                        statusToast = "Protected output ready: ${result.suggestedOutputName}"
                    )
                }
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

    fun saveProtectedArtifactToUri(destUri: Uri) {
        val artifact = _uiState.value.protectedOutput ?: return
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            try {
                val ctx = getApplication<Application>().applicationContext
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(destUri)?.use { out ->
                        artifact.outputCacheFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                        out.flush()
                    } ?: throw IllegalStateException("Cannot open destination URI")
                }
                _uiState.update {
                    it.copy(statusToast = "Saved ${artifact.suggestedOutputName}")
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(protectorError = "Save failed: ${t.message}")
                }
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

    fun encryptVaultText() {
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
            } catch (t: Throwable) {
                _uiState.update { it.copy(vaultErrorMessage = t.message ?: "Encryption failed") }
            }
        }
    }

    fun decryptVaultText() {
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
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(vaultErrorMessage = "File vault error: ${t.message ?: "Invalid DXS1 or key"}")
                }
            }
        }
    }

    fun saveBinaryVaultOutput(uri: Uri) {
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
            } catch (t: Throwable) {
                _uiState.update { it.copy(vaultErrorMessage = "Save failed: ${t.message}") }
            }
        }
    }

    fun updateObfuscatorInput(secret: String, methodName: String) {
        _uiState.update { it.copy(obfuscatorInput = secret, obfuscatorMethodName = methodName) }
    }

    fun obfuscateString() {
        val state = _uiState.value
        if (state.obfuscatorInput.isEmpty()) return
        val result = StringObfuscatorEngine.obfuscate(state.obfuscatorInput, state.obfuscatorMethodName)
        _uiState.update { it.copy(obfuscationResult = result) }
    }

    fun triggerCrashGuardSimulation() {
        viewModelScope.launch(Dispatchers.IO + ZeroCrashGuard.coroutineExceptionHandler) {
            ZeroCrashGuard.simulateGuardedWorkerFault()
            ZeroCrashGuard.probeMainThreadLatency()
            _uiState.update {
                it.copy(statusToast = "Fault intercepted safely — zero crash, zero ANR")
            }
        }
    }

    fun clearFaultLogs() {
        ZeroCrashGuard.clearLogs()
    }

    fun updateProfile(transform: (ProtectionProfile) -> ProtectionProfile) {
        viewModelScope.launch(ZeroCrashGuard.coroutineExceptionHandler) {
            prefs.saveProfile(transform(profile.value))
        }
    }

    fun adoptCurrentDeviceCert() {
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

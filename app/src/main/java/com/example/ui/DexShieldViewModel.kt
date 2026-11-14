package com.example.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProtectionPreferences
import com.example.security.ApkInspectionReport
import com.example.security.CryptoVaultEngine
import com.example.security.DexApkInspector
import com.example.security.GuardedFault
import com.example.security.ObfuscatedStringResult
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
                    statusToast = "Inspected $displayName (${report.dexReports.size} DEX)"
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

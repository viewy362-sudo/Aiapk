package com.example.security

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ProtectedBuildArtifact(
    val inputFileName: String,
    val isApkInput: Boolean,
    val primaryOutputFile: File,
    val suggestedPrimaryName: String,
    val primaryMimeType: String,
    val primarySizeBytes: Long,
    val companionKitFile: File?,
    val suggestedKitName: String?,
    val signedVerification: ApkSignatureVerificationResult?,
    val stubDexInjected: Boolean,
    val manifestPatched: Boolean,
    val detectedPackageName: String,
    val encryptedAssetsCount: Int,
    val packedDexCount: Int,
    val disguisedDexNames: List<String>,
    val strippedMetadataCount: Int,
    val injectedSecurityFilesCount: Int,
    val dexCount: Int,
    val primaryDexSha256Short: String,
    val logSteps: List<String>
)

/**
 * End-to-End Application Protection & Signing Pipeline (Bloody Team).
 * - For APK inputs with Stub Packing enabled:
 *     1) Inspects binary AXML AndroidManifest.xml & patches <application android:name> to BloodyStubApplication.
 *     2) Encrypts original classes*.dex into assets/bloody_wall/tex_atlas_N.<ext> (AES-256-GCM + XOR).
 *     3) Injects compiled pre-built bloody_stub.dex as classes.dex (active Runtime Guards + InMemoryDexClassLoader).
 *     4) Strips debug metadata, 4B/4KB ZipAligns entries, and signs with JAR v1 + APK Signature Scheme v2.
 * - Also exports companion Developer Security Kit .zip with Java/JNI/ProGuard files.
 */
object AppProtectorPipeline {

    fun protectArchive(
        context: Context,
        inputUri: Uri,
        inputFileName: String,
        profile: ProtectionProfile,
        passphrase: String,
        secretToHide: String,
        onStep: (String) -> Unit
    ): ProtectedBuildArtifact {
        require(passphrase.isNotBlank()) { "Master encryption passphrase is required" }

        val logs = mutableListOf<String>()
        fun log(msg: String) {
            logs.add(msg)
            onStep(msg)
        }

        val isApk = inputFileName.lowercase().endsWith(".apk")
        val baseName = inputFileName.substringBeforeLast(".").ifBlank { "app" }
        val kitOutFile = File(context.cacheDir, "bloody_protected_output.zip")
        val signedApkOutFile = File(context.cacheDir, "bloody_signed_output.apk")

        log("1/8 Inspecting input archive: $inputFileName (${profile.resolvedWallTier.title})")
        val inspection = DexApkInspector.inspectUri(context, inputUri, inputFileName)
        val primaryDexSha = inspection.dexReports.firstOrNull()?.sha256Short.orEmpty()
        log("   Detected ${inspection.dexReports.size} DEX file(s), ${inspection.assetEntriesCount} asset(s), ${inspection.totalEntries} entries.")

        var encryptedAssets = 0
        var packedDexCount = 0
        var strippedMetadataCount = 0
        val disguisedDexNames = mutableListOf<String>()
        var injectedFiles = 0

        var detectedPkgName = profile.packageName
        var origAppClassName = ""
        var rawManifestBytes: ByteArray? = null
        val apkNonDexEntries = mutableListOf<ApkEntryData>()
        val apkOriginalDexEntries = mutableListOf<ApkEntryData>()
        val packedDexForApkAssets = mutableListOf<Pair<String, ByteArray>>()

        val seenKitNames = HashSet<String>()
        log("2/8 Encrypting payloads & applying resource/metadata hardening...")
        ZipOutputStream(FileOutputStream(kitOutFile).buffered()).use { kitZos ->
            fun safeWriteKitEntry(name: String, bytes: ByteArray) {
                val clean = name.trimStart('/')
                if (clean.isNotBlank() && seenKitNames.add(clean)) {
                    writeZipEntry(kitZos, clean, bytes)
                }
            }
            context.contentResolver.openInputStream(inputUri)?.use { rawIn ->
                ZipInputStream(rawIn.buffered()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory) {
                            val data = zis.readBytes()
                            val lower = name.lowercase()
                            val isOldSignature = lower.startsWith("meta-inf/") && (
                                lower == "meta-inf/manifest.mf" ||
                                    lower.endsWith(".sf") ||
                                    lower.endsWith(".rsa") ||
                                    lower.endsWith(".dsa") ||
                                    lower.endsWith(".ec")
                                )
                            val isUnneededDebugMeta = profile.stripDebugMetadata && (
                                lower == "debugprobeskt.bin" ||
                                    lower == "kotlin-tooling-metadata.json" ||
                                    (lower.startsWith("meta-inf/") && lower.endsWith(".version")) ||
                                    lower.startsWith("meta-inf/com/android/build/")
                                )
                            val isDex = name.matches(Regex("^classes\\d*\\.dex$"))
                            val isAsset = (name.startsWith("assets/") || name.contains("/assets/")) &&
                                !name.endsWith(".enc")

                            when {
                                isOldSignature -> strippedMetadataCount++
                                isUnneededDebugMeta -> strippedMetadataCount++
                                else -> {
                                    if (isApk && name == "AndroidManifest.xml") {
                                        rawManifestBytes = data
                                        val manifestInfo = AxmlManifestPatcher.inspectBinaryManifest(data)
                                        if (manifestInfo.isValidBinaryAxml) {
                                            if (manifestInfo.packageName.isNotBlank()) {
                                                detectedPkgName = manifestInfo.packageName
                                            }
                                            origAppClassName = manifestInfo.originalApplicationClass
                                        }
                                    } else if (isApk && isDex) {
                                        apkOriginalDexEntries.add(ApkEntryData(name = name, bytes = data, forceStored = false))
                                    } else if (isApk && profile.signOutputApk) {
                                        val mustStore = lower == "resources.arsc" ||
                                            (lower.startsWith("lib/") && lower.endsWith(".so")) ||
                                            entry.method == ZipEntry.STORED
                                        apkNonDexEntries.add(ApkEntryData(name = name, bytes = data, forceStored = mustStore))
                                    }

                                    if (isDex && profile.dexBytecodeGuard && data.isNotEmpty()) {
                                        val packed = DexPackerEngine.packDexEntry(
                                            dexEntryName = name,
                                            dexBytes = data,
                                            passphrase = passphrase,
                                            disguiseExtension = profile.dexDisguiseExtension,
                                            index = packedDexCount
                                        )
                                        safeWriteKitEntry("disguised_dex_assets/${packed.disguisedAssetName}", packed.packedBytes)
                                        packedDexForApkAssets.add("assets/bloody_wall/${packed.disguisedAssetName}" to packed.packedBytes)
                                        disguisedDexNames.add(packed.disguisedAssetName)
                                        packedDexCount++
                                    } else if (profile.assetAes256Encryption && isAsset && data.isNotEmpty()) {
                                        val enc = CryptoVaultEngine.encryptBytes(data, passphrase)
                                        val targetPath = if (isApk) {
                                            "protected_assets/${name.removePrefix("assets/")}.enc"
                                        } else {
                                            "$name.enc"
                                        }
                                        safeWriteKitEntry(targetPath, enc.cipherBytes)
                                        encryptedAssets++
                                    } else if (!isApk) {
                                        safeWriteKitEntry(name, data)
                                    }
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: throw IllegalStateException("Unable to read input archive")

            val effectiveProfile = profile.copy(packageName = detectedPkgName)
            val pkgPath = detectedPkgName.replace('.', '/')

            log("   Packed $packedDexCount DEX -> $disguisedDexNames; stripped $strippedMetadataCount meta file(s).")

            val securityPrefix = if (isApk) {
                "security_patch/$pkgPath/security"
            } else {
                "bloody_security/java/$pkgPath/security"
            }

            log("3/8 Generating 10-layer Java protection suite (${effectiveProfile.enabledLayersCount}/10)...")
            val guardJava = SketchwareKitGenerator.generateSketchwareJavaGuard(effectiveProfile, primaryDexSha)
            safeWriteKitEntry("$securityPrefix/DexShieldGuard.java", guardJava.toByteArray(Charsets.UTF_8))
            injectedFiles++

            if (effectiveProfile.assetAes256Encryption) {
                val assetJava = SketchwareKitGenerator.generateAssetVaultHelperJava(effectiveProfile)
                safeWriteKitEntry("$securityPrefix/AssetVaultHelper.java", assetJava.toByteArray(Charsets.UTF_8))
                injectedFiles++
            }

            if (effectiveProfile.sslPinningProxyBlock || effectiveProfile.detectHttpCanary) {
                val sslJava = SketchwareKitGenerator.generateSslPinningJava(effectiveProfile)
                safeWriteKitEntry("$securityPrefix/SslPinningGuard.java", sslJava.toByteArray(Charsets.UTF_8))
                injectedFiles++
            }

            if (effectiveProfile.nativeJniStubPacking) {
                log("4/8 Generating NDK C++ JNI decryptor & InMemoryDexClassLoader Stub...")
                val cpp = DexPackerEngine.generateNativeJniCpp(effectiveProfile)
                val cmake = DexPackerEngine.generateCMakeLists()
                val stubJava = DexPackerEngine.generateStubApplicationJava(effectiveProfile)
                val manifestPatch = DexPackerEngine.generateManifestStubPatch(effectiveProfile)
                safeWriteKitEntry("native_jni/dex_shield_jni.cpp", cpp.toByteArray(Charsets.UTF_8))
                safeWriteKitEntry("native_jni/CMakeLists.txt", cmake.toByteArray(Charsets.UTF_8))
                safeWriteKitEntry("stub/$pkgPath/stub/DexStubApplication.java", stubJava.toByteArray(Charsets.UTF_8))
                safeWriteKitEntry("stub/AndroidManifest_stub_patch.xml", manifestPatch.toByteArray(Charsets.UTF_8))
                injectedFiles += 4
            } else {
                log("4/8 Native JNI & Stub generation skipped.")
            }

            if (effectiveProfile.stringMetadataObfuscation && secretToHide.isNotEmpty()) {
                log("5/8 Obfuscating API keys into StringVault.java (zero plaintext)...")
                val obf = StringObfuscatorEngine.obfuscate(secretToHide, "getProtectedSecret")
                val vaultClass = buildString {
                    appendLine("package ${effectiveProfile.packageName}.security;")
                    appendLine()
                    appendLine("public final class StringVault {")
                    appendLine("    private StringVault() {}")
                    appendLine(obf.javaDecoderCode.prependIndent("    "))
                    appendLine("}")
                }
                safeWriteKitEntry("$securityPrefix/StringVault.java", vaultClass.toByteArray(Charsets.UTF_8))
                injectedFiles++
            } else {
                log("5/8 StringVault generation skipped.")
            }

            log("6/8 Writing R8/ProGuard & Policy Manifest...")
            val proguard = SketchwareKitGenerator.generateProguardRules(effectiveProfile)
            safeWriteKitEntry("bloody_security/proguard-rules.pro", proguard.toByteArray(Charsets.UTF_8))
            injectedFiles++

            val policyJson = buildPolicyJson(
                profile = effectiveProfile,
                inputFileName = inputFileName,
                origAppClassName = origAppClassName,
                packedDexCount = packedDexCount,
                disguisedDexNames = disguisedDexNames,
                encryptedAssets = encryptedAssets,
                strippedMetadataCount = strippedMetadataCount,
                dexReports = inspection.dexReports
            )
            safeWriteKitEntry("bloody_security/dex_packing_manifest.json", policyJson.toByteArray(Charsets.UTF_8))
            injectedFiles++
        }

        // If input is an APK and signing is enabled, build a real ZipAligned & v1/v2 signed .apk
        var signatureResult: ApkSignatureVerificationResult? = null
        var stubDexInjected = false
        var manifestPatched = false

        if (isApk && profile.signOutputApk) {
            log("7/8 Injecting Compiled Runtime Stub DEX + Patching Binary AXML Manifest...")
            val identity = ApkSignerEngine.loadOrCreateSigningIdentity(
                context = context,
                commonName = profile.signerCommonName,
                organization = profile.signerOrg
            )
            val updatedProfile = profile.copy(
                packageName = detectedPkgName,
                expectedSignatureSha256 = identity.certSha256Colon
            )

            val stubDexBytes = runCatching {
                context.assets.open("bloody_stub.dex").use { it.readBytes() }
            }.getOrNull()

            val shouldReplaceDexWithStub = updatedProfile.dexBytecodeGuard &&
                updatedProfile.nativeJniStubPacking &&
                packedDexForApkAssets.isNotEmpty() &&
                stubDexBytes != null &&
                stubDexBytes.isNotEmpty() &&
                rawManifestBytes != null &&
                AxmlManifestPatcher.inspectBinaryManifest(rawManifestBytes!!).isValidBinaryAxml

            val finalEntries = apkNonDexEntries.filterNot {
                it.name.startsWith("assets/bloody_wall/")
            }.toMutableList()

            if (shouldReplaceDexWithStub) {
                val manifestBytes = rawManifestBytes!!
                val patched = AxmlManifestPatcher.patchApplicationStub(manifestBytes)
                finalEntries.add(ApkEntryData("AndroidManifest.xml", patched.patchedBytes))
                finalEntries.add(ApkEntryData("classes.dex", stubDexBytes!!))
                finalEntries.add(ApkEntryData("assets/bloody_wall/stub_seed.bin", sealPassphraseForStub(passphrase, detectedPkgName)))
                stubDexInjected = true
                manifestPatched = true
                log("   Injected compiled BloodyStubApplication classes.dex (${stubDexBytes.size / 1024} KB) & patched binary AndroidManifest.xml (origApp=${origAppClassName.ifBlank { "default" }}).")
            } else {
                rawManifestBytes?.let { finalEntries.add(ApkEntryData("AndroidManifest.xml", it)) }
                finalEntries.addAll(apkOriginalDexEntries)
                log("   Preserved original classes*.dex & manifest (Stub DEX swap disabled or plain ZIP).")
            }

            val embeddedPolicy = buildPolicyJson(
                profile = updatedProfile,
                inputFileName = inputFileName,
                origAppClassName = origAppClassName,
                packedDexCount = packedDexCount,
                disguisedDexNames = disguisedDexNames,
                encryptedAssets = encryptedAssets,
                strippedMetadataCount = strippedMetadataCount,
                dexReports = inspection.dexReports
            ).toByteArray(Charsets.UTF_8)

            finalEntries.add(ApkEntryData("assets/bloody_wall/protection_policy.json", embeddedPolicy))
            if (secretToHide.isNotEmpty()) {
                val encSecret = CryptoVaultEngine.encryptBytes(secretToHide.toByteArray(Charsets.UTF_8), passphrase)
                finalEntries.add(ApkEntryData("assets/bloody_wall/vault_config.enc", encSecret.cipherBytes))
            }
            for ((assetPath, packedBytes) in packedDexForApkAssets) {
                finalEntries.add(ApkEntryData(assetPath, packedBytes))
            }

            signatureResult = ApkSignerEngine.buildZipAlignedAndSignedApk(
                entries = finalEntries,
                identity = identity,
                outputFile = signedApkOutFile
            )
            log("8/8 ZipAligned & Signed APK verified! v1=${signatureResult.hasV1JarSignature}, v2=${signatureResult.hasV2SigningBlock}, RSA=${signatureResult.v2RsaVerified}")
            log("    Cert SHA-256: ${signatureResult.certSha256Colon}")
        } else {
            log("7/8 APK v1/v2 signing skipped (ZIP mode or signing disabled).")
            log("8/8 Protected bundle forged (${kitOutFile.length()} bytes). Ready to save!")
        }

        val useSignedApkAsPrimary = isApk && profile.signOutputApk && signedApkOutFile.exists()
        val primaryFile = if (useSignedApkAsPrimary) signedApkOutFile else kitOutFile
        val primaryName = if (useSignedApkAsPrimary) {
            "${baseName}_bloody_signed.apk"
        } else {
            "${baseName}_bloody_protected.zip"
        }
        val primaryMime = if (useSignedApkAsPrimary) {
            "application/vnd.android.package-archive"
        } else {
            "application/zip"
        }

        return ProtectedBuildArtifact(
            inputFileName = inputFileName,
            isApkInput = isApk,
            primaryOutputFile = primaryFile,
            suggestedPrimaryName = primaryName,
            primaryMimeType = primaryMime,
            primarySizeBytes = primaryFile.length(),
            companionKitFile = if (useSignedApkAsPrimary) kitOutFile else null,
            suggestedKitName = if (useSignedApkAsPrimary) "${baseName}_bloody_kit.zip" else null,
            signedVerification = signatureResult,
            stubDexInjected = stubDexInjected,
            manifestPatched = manifestPatched,
            detectedPackageName = detectedPkgName,
            encryptedAssetsCount = encryptedAssets,
            packedDexCount = packedDexCount,
            disguisedDexNames = disguisedDexNames,
            strippedMetadataCount = strippedMetadataCount,
            injectedSecurityFilesCount = injectedFiles,
            dexCount = inspection.dexReports.size,
            primaryDexSha256Short = primaryDexSha,
            logSteps = logs
        )
    }

    fun sealPassphraseForStub(passphrase: String, packageName: String): ByteArray {
        val raw = passphrase.toByteArray(Charsets.UTF_8)
        val mask = MessageDigest.getInstance("SHA-256")
            .digest("BT_STUB_2026|$packageName".toByteArray(Charsets.UTF_8))
        return ByteArray(raw.size) { i ->
            ((raw[i].toInt() and 0xFF) xor (mask[i % mask.size].toInt() and 0xFF)).toByte()
        }
    }

    private fun buildPolicyJson(
        profile: ProtectionProfile,
        inputFileName: String,
        origAppClassName: String,
        packedDexCount: Int,
        disguisedDexNames: List<String>,
        encryptedAssets: Int,
        strippedMetadataCount: Int,
        dexReports: List<DexFileReport>
    ): String = JSONObject().apply {
        put("engine", "Bloody Team Forge 2.0")
        put("appName", profile.appName)
        put("packageName", profile.packageName)
        put("origApplication", origAppClassName)
        put("wallTier", profile.resolvedWallTier.name)
        put("themePreset", profile.resolvedThemePreset.name)
        put("inputFile", inputFileName)
        put("enabledLayers", profile.enabledLayersCount)
        put("expectedSignatureSha256", profile.expectedSignatureSha256)
        put("dexBytecodeGuard", profile.dexBytecodeGuard)
        put("dexDisguiseExtension", profile.dexDisguiseExtension)
        put("antiTamperSignature", profile.antiTamperSignature)
        put("antiDebugFridaShield", profile.antiDebugFridaShield)
        put("rootMagiskDetection", profile.rootMagiskDetection)
        put("emulatorCloneShield", profile.emulatorCloneShield)
        put("sslPinningProxyBlock", profile.sslPinningProxyBlock)
        put("zeroCrashExceptionGuard", profile.zeroCrashExceptionGuard)
        put("screenTapjackingShield", profile.screenTapjackingShield)
        put("detectHttpCanary", profile.detectHttpCanary)
        put("customSecurityToast", profile.customSecurityToast)
        put("customToastMessage", profile.customToastMessage)
        put("dialogCancelable", profile.dialogCancelable)
        put("checkInternetRequired", profile.checkInternetRequired)
        put("loginValidateGate", profile.loginValidateGate)
        put("packedDexCount", packedDexCount)
        put("disguisedDexAssets", JSONArray(disguisedDexNames))
        put("encryptedAssets", encryptedAssets)
        put("strippedMetadataCount", strippedMetadataCount)
        put("dexReports", JSONArray().apply {
            dexReports.forEach { d ->
                put(JSONObject().apply {
                    put("entry", d.entryName)
                    put("version", d.dexVersion)
                    put("methods", d.methodIdsCount)
                    put("within65kLimit", d.withinDalvik65kLimit)
                    put("sha256Short", d.sha256Short)
                })
            }
        })
    }.toString(2)

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, bytes: ByteArray) {
        val cleanName = entryName.trimStart('/')
        zos.putNextEntry(ZipEntry(cleanName))
        zos.write(bytes)
        zos.closeEntry()
    }
}

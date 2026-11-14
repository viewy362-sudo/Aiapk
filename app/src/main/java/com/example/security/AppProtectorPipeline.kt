package com.example.security

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
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
 * - For APK inputs: injects Bloody Wall policy + disguised DEX vault + encrypted secret config,
 *   strips selected debug metadata, 4B/4KB ZipAligns entries, and applies JAR v1 + APK Signature
 *   Scheme v2 signing to produce a signed .apk alongside the companion JNI/Stub Security Kit .zip.
 * - For ZIP inputs: encrypts assets, packs any classes.dex payloads, and injects the full Java/JNI/ProGuard kit.
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
        val pkgPath = profile.packageName.replace('.', '/')

        val apkEntriesForSigning = mutableListOf<ApkEntryData>()
        val packedDexForApkAssets = mutableListOf<Pair<String, ByteArray>>()

        log("2/8 Encrypting payloads & applying resource/metadata hardening...")
        ZipOutputStream(FileOutputStream(kitOutFile).buffered()).use { kitZos ->
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
                                isOldSignature -> {
                                    strippedMetadataCount++
                                }
                                isUnneededDebugMeta -> {
                                    strippedMetadataCount++
                                }
                                else -> {
                                    if (isApk && profile.signOutputApk) {
                                        val mustStore = lower == "resources.arsc" ||
                                            (lower.startsWith("lib/") && lower.endsWith(".so")) ||
                                            entry.method == ZipEntry.STORED
                                        apkEntriesForSigning.add(
                                            ApkEntryData(
                                                name = name,
                                                bytes = data,
                                                forceStored = mustStore
                                            )
                                        )
                                    }

                                    if (isDex && profile.dexBytecodeGuard && data.isNotEmpty()) {
                                        val packed = DexPackerEngine.packDexEntry(
                                            dexEntryName = name,
                                            dexBytes = data,
                                            passphrase = passphrase,
                                            disguiseExtension = profile.dexDisguiseExtension,
                                            index = packedDexCount
                                        )
                                        writeZipEntry(kitZos, "disguised_dex_assets/${packed.disguisedAssetName}", packed.packedBytes)
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
                                        writeZipEntry(kitZos, targetPath, enc.cipherBytes)
                                        encryptedAssets++
                                    } else if (!isApk) {
                                        writeZipEntry(kitZos, name, data)
                                    }
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: throw IllegalStateException("Unable to read input archive")

            log("   Packed $packedDexCount DEX -> $disguisedDexNames; stripped $strippedMetadataCount meta file(s).")

            val securityPrefix = if (isApk) {
                "security_patch/$pkgPath/security"
            } else {
                "bloody_security/java/$pkgPath/security"
            }

            log("3/8 Generating 10-layer Java protection suite (${profile.enabledLayersCount}/10)...")
            val guardJava = SketchwareKitGenerator.generateSketchwareJavaGuard(profile, primaryDexSha)
            writeZipEntry(kitZos, "$securityPrefix/DexShieldGuard.java", guardJava.toByteArray(Charsets.UTF_8))
            injectedFiles++

            if (profile.assetAes256Encryption) {
                val assetJava = SketchwareKitGenerator.generateAssetVaultHelperJava(profile)
                writeZipEntry(kitZos, "$securityPrefix/AssetVaultHelper.java", assetJava.toByteArray(Charsets.UTF_8))
                injectedFiles++
            }

            if (profile.sslPinningProxyBlock || profile.detectHttpCanary) {
                val sslJava = SketchwareKitGenerator.generateSslPinningJava(profile)
                writeZipEntry(kitZos, "$securityPrefix/SslPinningGuard.java", sslJava.toByteArray(Charsets.UTF_8))
                injectedFiles++
            }

            if (profile.nativeJniStubPacking) {
                log("4/8 Generating NDK C++ JNI decryptor & InMemoryDexClassLoader Stub...")
                val cpp = DexPackerEngine.generateNativeJniCpp(profile)
                val cmake = DexPackerEngine.generateCMakeLists()
                val stubJava = DexPackerEngine.generateStubApplicationJava(profile)
                val manifestPatch = DexPackerEngine.generateManifestStubPatch(profile)
                writeZipEntry(kitZos, "native_jni/dex_shield_jni.cpp", cpp.toByteArray(Charsets.UTF_8))
                writeZipEntry(kitZos, "native_jni/CMakeLists.txt", cmake.toByteArray(Charsets.UTF_8))
                writeZipEntry(kitZos, "stub/$pkgPath/stub/DexStubApplication.java", stubJava.toByteArray(Charsets.UTF_8))
                writeZipEntry(kitZos, "stub/AndroidManifest_stub_patch.xml", manifestPatch.toByteArray(Charsets.UTF_8))
                injectedFiles += 4
            } else {
                log("4/8 Native JNI & Stub generation skipped.")
            }

            if (profile.stringMetadataObfuscation && secretToHide.isNotEmpty()) {
                log("5/8 Obfuscating API keys into StringVault.java (zero plaintext)...")
                val obf = StringObfuscatorEngine.obfuscate(secretToHide, "getProtectedSecret")
                val vaultClass = buildString {
                    appendLine("package ${profile.packageName}.security;")
                    appendLine()
                    appendLine("public final class StringVault {")
                    appendLine("    private StringVault() {}")
                    appendLine(obf.javaDecoderCode.prependIndent("    "))
                    appendLine("}")
                }
                writeZipEntry(kitZos, "$securityPrefix/StringVault.java", vaultClass.toByteArray(Charsets.UTF_8))
                injectedFiles++
            } else {
                log("5/8 StringVault generation skipped.")
            }

            log("6/8 Writing R8/ProGuard & Resource Confusing manifest...")
            val proguard = SketchwareKitGenerator.generateProguardRules(profile)
            writeZipEntry(kitZos, "bloody_security/proguard-rules.pro", proguard.toByteArray(Charsets.UTF_8))
            injectedFiles++

            val policyJson = buildPolicyJson(
                profile = profile,
                inputFileName = inputFileName,
                packedDexCount = packedDexCount,
                disguisedDexNames = disguisedDexNames,
                encryptedAssets = encryptedAssets,
                strippedMetadataCount = strippedMetadataCount,
                dexReports = inspection.dexReports
            )
            writeZipEntry(kitZos, "bloody_security/dex_packing_manifest.json", policyJson.toByteArray(Charsets.UTF_8))
            injectedFiles++
        }

        // If input is an APK and signing is enabled, build a real ZipAligned & v1/v2 signed .apk
        var signatureResult: ApkSignatureVerificationResult? = null
        if (isApk && profile.signOutputApk) {
            log("7/8 Injecting Bloody Wall vault & RSA-2048 signing certificate (${profile.signerCommonName})...")
            val identity = ApkSignerEngine.loadOrCreateSigningIdentity(
                context = context,
                commonName = profile.signerCommonName,
                organization = profile.signerOrg
            )
            val updatedProfile = profile.copy(expectedSignatureSha256 = identity.certSha256Colon)
            val embeddedPolicy = buildPolicyJson(
                profile = updatedProfile,
                inputFileName = inputFileName,
                packedDexCount = packedDexCount,
                disguisedDexNames = disguisedDexNames,
                encryptedAssets = encryptedAssets,
                strippedMetadataCount = strippedMetadataCount,
                dexReports = inspection.dexReports
            ).toByteArray(Charsets.UTF_8)

            // Replace or append Bloody Wall embedded policy + disguised DEX vaults + encrypted secret config
            val finalEntries = apkEntriesForSigning.filterNot {
                it.name.startsWith("assets/bloody_wall/")
            }.toMutableList()

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

    private fun buildPolicyJson(
        profile: ProtectionProfile,
        inputFileName: String,
        packedDexCount: Int,
        disguisedDexNames: List<String>,
        encryptedAssets: Int,
        strippedMetadataCount: Int,
        dexReports: List<DexFileReport>
    ): String = JSONObject().apply {
        put("engine", "Bloody Team Forge 2.0")
        put("appName", profile.appName)
        put("packageName", profile.packageName)
        put("wallTier", profile.resolvedWallTier.name)
        put("themePreset", profile.resolvedThemePreset.name)
        put("inputFile", inputFileName)
        put("enabledLayers", profile.enabledLayersCount)
        put("expectedSignatureSha256", profile.expectedSignatureSha256)
        put("packedDexCount", packedDexCount)
        put("disguisedDexAssets", JSONArray(disguisedDexNames))
        put("encryptedAssets", encryptedAssets)
        put("strippedMetadataCount", strippedMetadataCount)
        put("resourceHardening", JSONObject().apply {
            put("shrinkResources", profile.shrinkResources)
            put("shrinkDrawablesAndAnim", profile.shrinkDrawablesAndAnim)
            put("shrinkLayoutsAndXml", profile.shrinkLayoutsAndXml)
            put("stripDebugMetadata", profile.stripDebugMetadata)
        })
        put("runtimeConfig", JSONObject().apply {
            put("detectHttpCanary", profile.detectHttpCanary)
            put("customSecurityToast", profile.customSecurityToast)
            put("customToastMessage", profile.customToastMessage)
            put("dialogCancelable", profile.dialogCancelable)
            put("checkInternetRequired", profile.checkInternetRequired)
            put("loginValidateGate", profile.loginValidateGate)
        })
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

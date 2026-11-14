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
    val outputCacheFile: File,
    val suggestedOutputName: String,
    val outputSizeBytes: Long,
    val encryptedAssetsCount: Int,
    val packedDexCount: Int,
    val disguisedDexNames: List<String>,
    val preservedEntriesCount: Int,
    val injectedSecurityFilesCount: Int,
    val dexCount: Int,
    val primaryDexSha256Short: String,
    val logSteps: List<String>
)

/**
 * Real End-to-End Application Protection Pipeline (Bloody Team).
 * Phase A Build-Time:
 *  - Parses input .apk or .zip
 *  - Encrypts & disguises classes*.dex into assets/ (e.g. .png / .dat / .bin)
 *  - Encrypts assets/ files with AES-256-GCM (DXS1)
 *  - Generates C++ JNI Native Decryptor, InMemoryDexClassLoader Stub, Manifest Patch,
 *    10-layer Java guards, StringVault, and hardened R8/ProGuard rules.
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
        val suggestedOut = if (isApk) "${baseName}_bloody_protected.zip" else "${baseName}_protected.zip"
        val outFile = File(context.cacheDir, "bloody_protected_output.zip")

        log("1/7 Inspecting archive: $inputFileName")
        val inspection = DexApkInspector.inspectUri(context, inputUri, inputFileName)
        val primaryDexSha = inspection.dexReports.firstOrNull()?.sha256Short.orEmpty()
        log("   Found ${inspection.dexReports.size} DEX file(s), ${inspection.assetEntriesCount} asset(s).")

        var encryptedAssets = 0
        var packedDexCount = 0
        val disguisedDexNames = mutableListOf<String>()
        var preservedEntries = 0
        var injectedFiles = 0

        val pkgPath = profile.packageName.replace('.', '/')

        ZipOutputStream(FileOutputStream(outFile).buffered()).use { zos ->
            log("2/7 Encrypting assets & packing classes*.dex (${profile.dexDisguiseExtension} disguise)...")
            context.contentResolver.openInputStream(inputUri)?.use { rawIn ->
                ZipInputStream(rawIn.buffered()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory) {
                            val data = zis.readBytes()
                            val isDex = name.matches(Regex("^classes\\d*\\.dex$"))
                            val isAsset = (name.startsWith("assets/") || name.contains("/assets/")) &&
                                !name.endsWith(".enc")

                            when {
                                isDex && profile.dexBytecodeGuard && data.isNotEmpty() -> {
                                    val packed = DexPackerEngine.packDexEntry(
                                        dexEntryName = name,
                                        dexBytes = data,
                                        passphrase = passphrase,
                                        disguiseExtension = profile.dexDisguiseExtension,
                                        index = packedDexCount
                                    )
                                    val targetPath = "disguised_dex_assets/${packed.disguisedAssetName}"
                                    writeZipEntry(zos, targetPath, packed.packedBytes)
                                    disguisedDexNames.add(packed.disguisedAssetName)
                                    packedDexCount++
                                }
                                profile.assetAes256Encryption && isAsset && data.isNotEmpty() -> {
                                    val enc = CryptoVaultEngine.encryptBytes(data, passphrase)
                                    val targetPath = if (isApk) {
                                        "protected_assets/${name.removePrefix("assets/")}.enc"
                                    } else {
                                        "$name.enc"
                                    }
                                    writeZipEntry(zos, targetPath, enc.cipherBytes)
                                    encryptedAssets++
                                }
                                !isApk -> {
                                    writeZipEntry(zos, name, data)
                                    preservedEntries++
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: throw IllegalStateException("Unable to read input archive")

            log("   Packed $packedDexCount DEX file(s) -> $disguisedDexNames")
            log("   Encrypted $encryptedAssets asset(s) using AES-256-GCM.")

            val securityPrefix = if (isApk) {
                "security_patch/$pkgPath/security"
            } else {
                "bloody_security/java/$pkgPath/security"
            }

            log("3/7 Generating 10-layer Java protection suite (${profile.enabledLayersCount}/10)...")
            val guardJava = SketchwareKitGenerator.generateSketchwareJavaGuard(profile, primaryDexSha)
            writeZipEntry(zos, "$securityPrefix/DexShieldGuard.java", guardJava.toByteArray(Charsets.UTF_8))
            injectedFiles++

            if (profile.assetAes256Encryption) {
                val assetJava = SketchwareKitGenerator.generateAssetVaultHelperJava(profile)
                writeZipEntry(zos, "$securityPrefix/AssetVaultHelper.java", assetJava.toByteArray(Charsets.UTF_8))
                injectedFiles++
            }

            if (profile.sslPinningProxyBlock) {
                val sslJava = SketchwareKitGenerator.generateSslPinningJava(profile)
                writeZipEntry(zos, "$securityPrefix/SslPinningGuard.java", sslJava.toByteArray(Charsets.UTF_8))
                injectedFiles++
            }

            if (profile.nativeJniStubPacking) {
                log("4/7 Generating NDK C++ JNI decryptor & InMemoryDexClassLoader Stub...")
                val cpp = DexPackerEngine.generateNativeJniCpp(profile)
                val cmake = DexPackerEngine.generateCMakeLists()
                val stubJava = DexPackerEngine.generateStubApplicationJava(profile)
                val manifestPatch = DexPackerEngine.generateManifestStubPatch(profile)
                writeZipEntry(zos, "native_jni/dex_shield_jni.cpp", cpp.toByteArray(Charsets.UTF_8))
                writeZipEntry(zos, "native_jni/CMakeLists.txt", cmake.toByteArray(Charsets.UTF_8))
                writeZipEntry(zos, "stub/$pkgPath/stub/DexStubApplication.java", stubJava.toByteArray(Charsets.UTF_8))
                writeZipEntry(zos, "stub/AndroidManifest_stub_patch.xml", manifestPatch.toByteArray(Charsets.UTF_8))
                injectedFiles += 4
            } else {
                log("4/7 Native JNI & Stub generation skipped.")
            }

            if (profile.stringMetadataObfuscation && secretToHide.isNotEmpty()) {
                log("5/7 Obfuscating API keys into StringVault.java (zero plaintext)...")
                val obf = StringObfuscatorEngine.obfuscate(secretToHide, "getProtectedSecret")
                val vaultClass = buildString {
                    appendLine("package ${profile.packageName}.security;")
                    appendLine()
                    appendLine("public final class StringVault {")
                    appendLine("    private StringVault() {}")
                    appendLine(obf.javaDecoderCode.prependIndent("    "))
                    appendLine("}")
                }
                writeZipEntry(zos, "$securityPrefix/StringVault.java", vaultClass.toByteArray(Charsets.UTF_8))
                injectedFiles++
            } else {
                log("5/7 StringVault generation skipped.")
            }

            log("6/7 Writing R8/ProGuard hardening rules & packing manifest...")
            val proguard = SketchwareKitGenerator.generateProguardRules(profile)
            writeZipEntry(zos, "bloody_security/proguard-rules.pro", proguard.toByteArray(Charsets.UTF_8))
            injectedFiles++

            val manifestJson = JSONObject().apply {
                put("appName", profile.appName)
                put("packageName", profile.packageName)
                put("inputFile", inputFileName)
                put("enabledLayers", profile.enabledLayersCount)
                put("packedDexCount", packedDexCount)
                put("disguisedDexAssets", JSONArray(disguisedDexNames))
                put("encryptedAssets", encryptedAssets)
                put("dexReports", JSONArray().apply {
                    inspection.dexReports.forEach { d ->
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
            writeZipEntry(zos, "bloody_security/dex_packing_manifest.json", manifestJson.toByteArray(Charsets.UTF_8))
            injectedFiles++
        }

        log("7/7 Protected package forged (${outFile.length()} bytes). Ready to save!")

        return ProtectedBuildArtifact(
            inputFileName = inputFileName,
            isApkInput = isApk,
            outputCacheFile = outFile,
            suggestedOutputName = suggestedOut,
            outputSizeBytes = outFile.length(),
            encryptedAssetsCount = encryptedAssets,
            packedDexCount = packedDexCount,
            disguisedDexNames = disguisedDexNames,
            preservedEntriesCount = preservedEntries,
            injectedSecurityFilesCount = injectedFiles,
            dexCount = inspection.dexReports.size,
            primaryDexSha256Short = primaryDexSha,
            logSteps = logs
        )
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, bytes: ByteArray) {
        val cleanName = entryName.trimStart('/')
        zos.putNextEntry(ZipEntry(cleanName))
        zos.write(bytes)
        zos.closeEntry()
    }
}

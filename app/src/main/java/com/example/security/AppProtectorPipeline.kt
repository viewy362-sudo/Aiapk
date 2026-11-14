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
    val preservedEntriesCount: Int,
    val injectedSecurityFilesCount: Int,
    val dexCount: Int,
    val primaryDexSha256Short: String,
    val logSteps: List<String>
)

/**
 * Real End-to-End Application Protection Pipeline.
 * Reads a user-selected .apk or .zip archive, inspects DEX/assets, encrypts assets
 * with AES-256-GCM (DXS1), obfuscates secrets, injects customized 10-layer Java/R8
 * security modules, and writes a real protected output .zip archive ready for export.
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
        val suggestedOut = if (isApk) "${baseName}_shielded_bundle.zip" else "${baseName}_protected.zip"
        val outFile = File(context.cacheDir, "dexshield_protected_output.zip")

        log("1/6 Analyzing input archive: $inputFileName")
        val inspection = DexApkInspector.inspectUri(context, inputUri, inputFileName)
        val primaryDexSha = inspection.dexReports.firstOrNull()?.sha256Short.orEmpty()
        log("   Found ${inspection.dexReports.size} DEX file(s), ${inspection.assetEntriesCount} asset(s).")

        var encryptedAssets = 0
        var preservedEntries = 0
        var injectedFiles = 0

        val pkgPath = profile.packageName.replace('.', '/')

        ZipOutputStream(FileOutputStream(outFile).buffered()).use { zos ->
            log("2/6 Processing archive entries & encrypting assets (AES-256-GCM)...")
            context.contentResolver.openInputStream(inputUri)?.use { rawIn ->
                ZipInputStream(rawIn.buffered()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory) {
                            val data = zis.readBytes()
                            val isAsset = (name.startsWith("assets/") || name.contains("/assets/")) &&
                                !name.endsWith(".enc")

                            if (profile.assetAes256Encryption && isAsset && data.isNotEmpty()) {
                                val enc = CryptoVaultEngine.encryptBytes(data, passphrase)
                                val targetPath = if (isApk) {
                                    "protected_assets/${name.removePrefix("assets/")}.enc"
                                } else {
                                    "$name.enc"
                                }
                                writeZipEntry(zos, targetPath, enc.cipherBytes)
                                encryptedAssets++
                            } else if (!isApk) {
                                // Keep original project source/resource files intact in project zip
                                writeZipEntry(zos, name, data)
                                preservedEntries++
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: throw IllegalStateException("Unable to read input archive")

            log("   Encrypted $encryptedAssets asset(s) using DXS1 AES-256-GCM envelope.")

            val securityPrefix = if (isApk) {
                "security_patch/$pkgPath/security"
            } else {
                "dexshield_security/java/$pkgPath/security"
            }

            log("3/6 Injecting 10-layer Java protection classes (${profile.enabledLayersCount}/10 active)...")
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

            if (profile.stringMetadataObfuscation && secretToHide.isNotEmpty()) {
                log("4/6 Obfuscating sensitive keys into zero-plaintext StringVault.java...")
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
                log("4/6 String obfuscation skipped or no secret provided.")
            }

            log("5/6 Writing hardened R8/ProGuard rules & integrity manifest...")
            val proguard = SketchwareKitGenerator.generateProguardRules(profile)
            writeZipEntry(zos, "dexshield_security/proguard-rules.pro", proguard.toByteArray(Charsets.UTF_8))
            injectedFiles++

            val manifestJson = JSONObject().apply {
                put("appName", profile.appName)
                put("packageName", profile.packageName)
                put("inputFile", inputFileName)
                put("enabledLayers", profile.enabledLayersCount)
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
            writeZipEntry(zos, "dexshield_security/dexshield_integrity.json", manifestJson.toByteArray(Charsets.UTF_8))
            injectedFiles++

            val guide = buildString {
                appendLine("=== DexShield Protection Output Summary ===")
                appendLine("Target App: ${profile.appName} (${profile.packageName})")
                appendLine("Enabled Security Layers: ${profile.enabledLayersCount} / 10")
                appendLine("Encrypted Assets (AES-256-GCM): $encryptedAssets")
                appendLine("Primary DEX SHA-256 Lock: ${primaryDexSha.ifEmpty { "N/A (Source Project)" }}")
                appendLine()
                appendLine("Integration:")
                appendLine("1. Copy files under $securityPrefix/ into your Sketchware / Android src folder.")
                appendLine("2. Call DexShieldGuard.activateAll(this); inside MainActivity.onCreate().")
                appendLine("3. Use AssetVaultHelper.decryptAsset(context, \"file.enc\", passphrase) for encrypted assets.")
                appendLine("4. Merge dexshield_security/proguard-rules.pro into your release build.")
            }
            writeZipEntry(zos, "dexshield_security/README_INTEGRATION.txt", guide.toByteArray(Charsets.UTF_8))
            injectedFiles++
        }

        log("6/6 Protected package built (${outFile.length()} bytes). Ready to save!")

        return ProtectedBuildArtifact(
            inputFileName = inputFileName,
            isApkInput = isApk,
            outputCacheFile = outFile,
            suggestedOutputName = suggestedOut,
            outputSizeBytes = outFile.length(),
            encryptedAssetsCount = encryptedAssets,
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

package com.example.security

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class DexFileReport(
    val entryName: String,
    val dexVersion: String,
    val fileSizeBytes: Long,
    val stringIdsCount: Int,
    val typeIdsCount: Int,
    val methodIdsCount: Int,
    val classDefsCount: Int,
    val sha256Short: String,
    val withinDalvik65kLimit: Boolean
)

data class ApkInspectionReport(
    val sourceLabel: String,
    val totalArchiveSizeBytes: Long,
    val totalEntries: Int,
    val dexReports: List<DexFileReport>,
    val assetEntriesCount: Int,
    val encryptedAssetCount: Int,
    val nativeLibArchitectures: List<String>,
    val hasManifest: Boolean,
    val hasSignatureBlock: Boolean,
    val detectedPackageName: String = "",
    val detectedAppClass: String = "",
    val appComponentFactory: String = "",
    val usesPermissions: List<String> = emptyList(),
    val isApkArchive: Boolean = false,
    val isSketchwareApp: Boolean = false,
    val detectedFramework: String = "Android APK",
    val existingPackerName: String? = null,
    val dataFileEntriesCount: Int = 0,
    val debugMetadataCount: Int = 0,
    val summaryMessage: String
)

/**
 * Layer 1: Real DEX Bytecode & Archive Integrity Inspector.
 * Parses APK/Zip archives (both self-APK and user-picked SAF APK/Zip files),
 * reads little-endian DEX header fields, detects Sketchware / frameworks / packers / permissions / data files,
 * validates magic bytes, and checks the Dalvik 65,535 method_ids_size limit.
 */
object DexApkInspector {
    private const val DALVIK_METHOD_LIMIT = 65_535
    private val SKETCHWARE_MARKERS = listOf(
        "SketchApplication".toByteArray(Charsets.US_ASCII),
        "SketchwareUtil".toByteArray(Charsets.US_ASCII),
        "com/besome/sketch".toByteArray(Charsets.US_ASCII),
        "SketchLogger".toByteArray(Charsets.US_ASCII)
    )

    fun inspectSelfApk(context: Context): ApkInspectionReport {
        return ZeroCrashGuard.runGuarded(
            source = "DexApkInspector.inspectSelfApk",
            fallback = emptyFallback("Running APK")
        ) {
            val sourceDir = context.applicationInfo.sourceDir
            val file = java.io.File(sourceDir)
            file.inputStream().use { input ->
                inspectStream(
                    inputStream = input,
                    sourceLabel = "Self APK (${context.packageName})",
                    totalSizeBytes = file.length()
                )
            }
        }
    }

    fun inspectUri(context: Context, uri: Uri, displayName: String): ApkInspectionReport {
        return ZeroCrashGuard.runGuarded(
            source = "DexApkInspector.inspectUri",
            fallback = emptyFallback(displayName)
        ) {
            val size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            context.contentResolver.openInputStream(uri)?.use { input ->
                inspectStream(
                    inputStream = input,
                    sourceLabel = displayName,
                    totalSizeBytes = size.coerceAtLeast(0L)
                )
            } ?: emptyFallback(displayName, "Unable to open document stream")
        }
    }

    private fun inspectStream(
        inputStream: InputStream,
        sourceLabel: String,
        totalSizeBytes: Long
    ): ApkInspectionReport {
        val dexReports = mutableListOf<DexFileReport>()
        val nativeAbis = linkedSetOf<String>()
        var totalEntries = 0
        var assetCount = 0
        var encryptedAssetCount = 0
        var dataFileCount = 0
        var debugMetadataCount = 0
        var hasManifest = false
        var hasSignature = false
        var detectedPkg = ""
        var detectedApp = ""
        var compFactory = ""
        var usesPermissions = emptyList<String>()

        var isFlutter = false
        var isUnity = false
        var isReactNative = false
        var isSketchware = false
        var detectedPacker: String? = null

        ZipInputStream(inputStream.buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                totalEntries++
                val name = entry.name
                val lower = name.lowercase()
                if (!entry.isDirectory) {
                    when {
                        name.matches(Regex("^classes\\d*\\.dex$")) -> {
                            val bytes = zis.readBytes()
                            dexReports.add(parseDexBytes(name, bytes))
                            if (!isSketchware && containsAnyAsciiMarker(bytes, SKETCHWARE_MARKERS)) {
                                isSketchware = true
                            }
                        }
                        name == "AndroidManifest.xml" -> {
                            hasManifest = true
                            val mBytes = zis.readBytes()
                            val info = AxmlManifestPatcher.inspectBinaryManifest(mBytes)
                            if (info.isValidBinaryAxml) {
                                detectedPkg = info.packageName
                                detectedApp = info.originalApplicationClass
                                compFactory = info.appComponentFactory
                                usesPermissions = info.usesPermissions
                                if (detectedApp == AxmlManifestPatcher.STUB_APPLICATION_CLASS) {
                                    detectedPacker = "Bloody Team Stub"
                                } else if (detectedApp.endsWith(".SketchApplication") ||
                                    detectedApp.contains("besome.sketch")
                                ) {
                                    isSketchware = true
                                }
                            }
                        }
                        lower.startsWith("meta-inf/") &&
                            (lower.endsWith(".rsa") || lower.endsWith(".dsa") || lower.endsWith(".ec")) -> {
                            hasSignature = true
                        }
                    }

                    if (lower.startsWith("assets/") || lower.contains("/assets/")) {
                        assetCount++
                        if (lower.endsWith(".enc") || lower.endsWith(".dxs") || lower.endsWith(".vault")) {
                            encryptedAssetCount++
                        }
                        if (lower.startsWith("assets/bloody_wall/")) {
                            detectedPacker = detectedPacker ?: "Bloody Team Stub"
                        }
                        if (lower.startsWith("assets/flutter_assets/")) {
                            isFlutter = true
                        }
                        if (lower == "assets/index.android.bundle") {
                            isReactNative = true
                        }
                        if (lower.contains("sketchware")) {
                            isSketchware = true
                        }
                    }

                    if (lower.endsWith(".json") || lower.endsWith(".db") || lower.endsWith(".sqlite") ||
                        lower.endsWith(".dat") || lower.endsWith(".bin") || lower.endsWith(".lua") ||
                        lower.endsWith(".bundle") || lower.endsWith(".txt")
                    ) {
                        dataFileCount++
                    }

                    if (lower == "debugprobeskt.bin" ||
                        lower == "kotlin-tooling-metadata.json" ||
                        (lower.startsWith("meta-inf/") && lower.endsWith(".version")) ||
                        lower.startsWith("meta-inf/com/android/build/")
                    ) {
                        debugMetadataCount++
                    }

                    if (lower.startsWith("lib/")) {
                        val parts = name.split("/")
                        if (parts.size >= 2 && parts[1].isNotEmpty()) {
                            nativeAbis.add(parts[1])
                        }
                        val soName = parts.lastOrNull()?.lowercase().orEmpty()
                        when {
                            soName.contains("flutter") || soName == "libapp.so" -> isFlutter = true
                            soName.contains("unity") || soName.contains("il2cpp") -> isUnity = true
                            soName.contains("reactnative") -> isReactNative = true
                            soName.contains("jiagu") -> detectedPacker = "360 Jiagu"
                            soName.contains("tersafe") || soName.contains("txapp") || soName.contains("shella") -> {
                                detectedPacker = "Tencent Legu / Shell"
                            }
                            soName.contains("dexhelper") || soName.contains("secneo") -> {
                                detectedPacker = "Bangcle / SecNeo"
                            }
                            soName.contains("pairipcore") -> detectedPacker = "Google Play PairIP"
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val isApk = sourceLabel.lowercase().endsWith(".apk") || (hasManifest && dexReports.isNotEmpty())
        val framework = when {
            detectedPacker != null -> "Protected / Packed ($detectedPacker)"
            isFlutter -> "Flutter Engine APK"
            isUnity -> "Unity IL2CPP Game APK"
            isReactNative -> "React Native APK"
            isSketchware && dexReports.size > 1 -> "Sketchware MultiDEX APK (${dexReports.size} DEX)"
            isSketchware -> "Sketchware / Sketchware Pro APK"
            !isApk && hasManifest -> "Android Source Project ZIP"
            !isApk -> "Data / Asset Archive"
            dexReports.size > 1 -> "MultiDEX Android APK (${dexReports.size} DEX)"
            else -> "Standard Java/Kotlin APK"
        }

        val allWithinLimit = dexReports.all { it.withinDalvik65kLimit }
        val summary = when {
            dexReports.isEmpty() -> "Archive scanned ($totalEntries entries, $dataFileCount data files)."
            !allWithinLimit -> "Warning: DEX method index exceeds 65,535 Dalvik limit!"
            detectedPacker != null -> "Detected existing packer ($detectedPacker) • ${dexReports.size} DEX."
            else -> "$framework • ${dexReports.size} DEX verified (65K limit safe)."
        }

        return ApkInspectionReport(
            sourceLabel = sourceLabel,
            totalArchiveSizeBytes = totalSizeBytes,
            totalEntries = totalEntries,
            dexReports = dexReports.sortedBy { it.entryName },
            assetEntriesCount = assetCount,
            encryptedAssetCount = encryptedAssetCount,
            nativeLibArchitectures = nativeAbis.toList(),
            hasManifest = hasManifest,
            hasSignatureBlock = hasSignature,
            detectedPackageName = detectedPkg,
            detectedAppClass = detectedApp,
            appComponentFactory = compFactory,
            usesPermissions = usesPermissions,
            isApkArchive = isApk,
            isSketchwareApp = isSketchware,
            detectedFramework = framework,
            existingPackerName = detectedPacker,
            dataFileEntriesCount = dataFileCount,
            debugMetadataCount = debugMetadataCount,
            summaryMessage = summary
        )
    }

    private fun containsAnyAsciiMarker(haystack: ByteArray, markers: List<ByteArray>): Boolean {
        for (needle in markers) {
            if (indexOfBytes(haystack, needle) >= 0) return true
        }
        return false
    }

    private fun indexOfBytes(haystack: ByteArray, needle: ByteArray): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        val first = needle[0]
        val maxStart = haystack.size - needle.size
        var i = 0
        while (i <= maxStart) {
            if (haystack[i] != first) {
                while (++i <= maxStart && haystack[i] != first) {}
            }
            if (i <= maxStart) {
                var j = 1
                while (j < needle.size && haystack[i + j] == needle[j]) j++
                if (j == needle.size) return i
            }
            i++
        }
        return -1
    }

    fun parseDexBytes(entryName: String, bytes: ByteArray): DexFileReport {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val shaShort = digest.take(8).joinToString("") { "%02X".format(it) }

        if (bytes.size < 112) {
            return DexFileReport(
                entryName = entryName,
                dexVersion = "Invalid (<112B)",
                fileSizeBytes = bytes.size.toLong(),
                stringIdsCount = 0,
                typeIdsCount = 0,
                methodIdsCount = 0,
                classDefsCount = 0,
                sha256Short = shaShort,
                withinDalvik65kLimit = false
            )
        }

        val magicStr = String(bytes, 0, 4, Charsets.US_ASCII)
        val versionStr = String(bytes, 4, 3, Charsets.US_ASCII)
        val isDex = magicStr == "dex\n" && bytes[7] == 0.toByte()

        val buf = ByteBuffer.wrap(bytes, 0, 112).order(ByteOrder.LITTLE_ENDIAN)
        val stringIdsSize = buf.getInt(56)
        val typeIdsSize = buf.getInt(64)
        val methodIdsSize = buf.getInt(88)
        val classDefsSize = buf.getInt(96)

        return DexFileReport(
            entryName = entryName,
            dexVersion = if (isDex) "DEX $versionStr" else "Encrypted/Obfuscated",
            fileSizeBytes = bytes.size.toLong(),
            stringIdsCount = stringIdsSize.coerceAtLeast(0),
            typeIdsCount = typeIdsSize.coerceAtLeast(0),
            methodIdsCount = methodIdsSize.coerceAtLeast(0),
            classDefsCount = classDefsSize.coerceAtLeast(0),
            sha256Short = shaShort,
            withinDalvik65kLimit = methodIdsSize in 1..DALVIK_METHOD_LIMIT
        )
    }

    private fun emptyFallback(label: String, reason: String = "Unable to inspect archive") =
        ApkInspectionReport(
            sourceLabel = label,
            totalArchiveSizeBytes = 0L,
            totalEntries = 0,
            dexReports = emptyList(),
            assetEntriesCount = 0,
            encryptedAssetCount = 0,
            nativeLibArchitectures = emptyList(),
            hasManifest = false,
            hasSignatureBlock = false,
            summaryMessage = reason
        )
}

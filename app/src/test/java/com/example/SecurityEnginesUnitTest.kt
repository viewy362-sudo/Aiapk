package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.security.ApkSignerEngine
import com.example.security.AppProtectorPipeline
import com.example.security.AxmlManifestPatcher
import com.example.security.CryptoVaultEngine
import com.example.security.DexApkInspector
import com.example.security.DexPackerEngine
import com.example.security.ProtectionProfile
import com.example.security.SketchwareKitGenerator
import com.example.security.StringObfuscatorEngine
import com.example.security.ZeroCrashGuard
import com.example.ui.theme.AppThemePreset
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecurityEnginesUnitTest {

    @Test
    fun aes256GcmRoundTripAndTamperDetection() {
        val secret = "DexShield-BLOODY-TEAM-Payload-2026"
        val pass = "StrongMasterPass#99"
        val encrypted = CryptoVaultEngine.encryptBytes(secret.toByteArray(Charsets.UTF_8), pass)
        assertTrue(encrypted.entropyBitsPerByte > 3.5)

        val decrypted = CryptoVaultEngine.decryptBytes(encrypted.cipherBytes, pass)
        assertEquals(secret, String(decrypted, Charsets.UTF_8))

        val tampered = encrypted.cipherBytes.copyOf()
        tampered[tampered.lastIndex] = (tampered[tampered.lastIndex].toInt() xor 0xFF).toByte()
        var rejected = false
        try {
            CryptoVaultEngine.decryptBytes(tampered, pass)
        } catch (_: Throwable) {
            rejected = true
        }
        assertTrue("GCM authentication must reject tampered bytes", rejected)
    }

    @Test
    fun dexHeaderParserChecksDalvikMethodLimit() {
        val fakeDex = ByteArray(112)
        "dex\n035\u0000".toByteArray(Charsets.US_ASCII).copyInto(fakeDex, 0)
        val buf = ByteBuffer.wrap(fakeDex).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(56, 1200)
        buf.putInt(64, 340)
        buf.putInt(88, 12450)
        buf.putInt(96, 180)

        val safeReport = DexApkInspector.parseDexBytes("classes.dex", fakeDex)
        assertEquals("DEX 035", safeReport.dexVersion)
        assertEquals(12450, safeReport.methodIdsCount)
        assertTrue(safeReport.withinDalvik65kLimit)

        buf.putInt(88, 70_000)
        val overflowReport = DexApkInspector.parseDexBytes("classes.dex", fakeDex)
        assertFalse(overflowReport.withinDalvik65kLimit)
    }

    @Test
    fun stringObfuscatorProducesLosslessDecoderAndNoPlaintext() {
        val secret = "AIzaSyBLOODY_TEAM_TEST_TOKEN_9876"
        val result = StringObfuscatorEngine.obfuscate(secret, "getSecretToken")
        assertEquals(secret, result.verifiedRoundTrip)
        assertFalse(result.javaDecoderCode.contains(secret))
        assertFalse(result.kotlinDecoderCode.contains(secret))
        assertTrue(result.javaDecoderCode.contains("public static String getSecretToken()"))
    }

    @Test
    fun initialProtectionProfileStartsAllOff() {
        val fresh = ProtectionProfile()
        assertEquals(0, fresh.enabledLayersCount)
        assertEquals(0, fresh.selectedResourceConfuseCount)
        assertEquals("", fresh.wallProtectionTier)
        assertEquals(null, fresh.resolvedWallTier)
        assertFalse(fresh.dexBytecodeGuard)
        assertFalse(fresh.assetAes256Encryption)
        assertFalse(fresh.nativeJniStubPacking)
        assertFalse(fresh.signOutputApk)
        assertFalse(fresh.shrinkResources)
        assertFalse(fresh.shrinkAnim)
        assertFalse(fresh.shrinkResourceXmlV1)
        assertFalse(fresh.shrinkResourceXmlV2)
        assertFalse(fresh.removeDetectVpnProxy)
        assertFalse(fresh.removeDetectHttpCanary)
        assertFalse(fresh.removeBundlingSoftware)
        assertFalse(fresh.removeUpdateAlertDialog)
        assertFalse(fresh.hideRoot)
        assertFalse(fresh.loginValidateGate)
        assertFalse(fresh.loginValidateV2)
    }

    @Test
    fun allNineWallProtectionTiersAvailable() {
        assertEquals(9, com.example.security.WallProtectionTier.entries.size)
        val titles = com.example.security.WallProtectionTier.entries.map { it.title }
        assertTrue(titles.contains("Protection V1"))
        assertTrue(titles.contains("Protection 360加固"))
        assertTrue(titles.contains("Protection 几维安全"))
        assertTrue(titles.contains("Protection 爱加密"))
        assertTrue(titles.contains("Protection 腾讯加固"))
        assertTrue(titles.contains("Protection 梆梆加固"))
    }

    @Test
    fun sketchwareKitGeneratorIncludesAll10Layers() {
        val profile = ProtectionProfile(
            appName = "BLOODY TEAM",
            packageName = "com.bloodyteam.app",
            expectedSignatureSha256 = "AA:BB:CC",
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
            shrinkResources = true
        )
        val java = SketchwareKitGenerator.generateSketchwareJavaGuard(profile, "DEAD1234")
        val ssl = SketchwareKitGenerator.generateSslPinningJava(profile)
        val proguard = SketchwareKitGenerator.generateProguardRules(profile)
        assertTrue(java.contains("package com.bloodyteam.app.security;"))
        assertTrue(java.contains("AA:BB:CC"))
        assertTrue(java.contains("DEAD1234"))
        assertTrue(java.contains("FLAG_SECURE"))
        assertTrue(java.contains("isRootedOrMagisk"))
        assertTrue(ssl.contains("SslPinningGuard"))
        assertTrue(proguard.contains("-repackageclasses"))
    }

    @Test
    fun protectorPipelineEncryptsAssetsAndInjectsSecuritySuite() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inputZip = File(context.cacheDir, "sample_project.zip")
        val assetText = """{"secret":"hello_bloody_team"}"""
        ZipOutputStream(FileOutputStream(inputZip)).use { zos ->
            zos.putNextEntry(ZipEntry("assets/config.json"))
            zos.write(assetText.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("app/src/main/MainActivity.java"))
            zos.write("class MainActivity {}".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val profile = ProtectionProfile(
            appName = "BLOODY TEAM",
            packageName = "com.bloodyteam.app",
            dexBytecodeGuard = true,
            assetAes256Encryption = true,
            antiTamperSignature = true,
            stringMetadataObfuscation = true,
            zeroCrashExceptionGuard = true,
            sslPinningProxyBlock = true,
            nativeJniStubPacking = true
        )
        val artifact = AppProtectorPipeline.protectArchive(
            context = context,
            inputUri = Uri.fromFile(inputZip),
            inputFileName = "sample_project.zip",
            profile = profile,
            passphrase = "TestPass#2026",
            secretToHide = "my_secret_api_key_99",
            onStep = {}
        )

        assertEquals(1, artifact.encryptedAssetsCount)
        assertTrue(artifact.primaryOutputFile.exists())

        ZipFile(artifact.primaryOutputFile).use { outZip ->
            val encEntry = outZip.getEntry("assets/config.json.enc")
            assertTrue("Encrypted asset must exist", encEntry != null)
            val encBytes = outZip.getInputStream(encEntry).readBytes()
            val decrypted = String(CryptoVaultEngine.decryptBytes(encBytes, "TestPass#2026"), Charsets.UTF_8)
            assertEquals(assetText, decrypted)

            assertTrue(outZip.getEntry("bloody_security/java/com/bloodyteam/app/security/DexShieldGuard.java") != null)
            assertTrue(outZip.getEntry("bloody_security/java/com/bloodyteam/app/security/AssetVaultHelper.java") != null)
            assertTrue(outZip.getEntry("bloody_security/java/com/bloodyteam/app/security/SslPinningGuard.java") != null)
            assertTrue(outZip.getEntry("bloody_security/java/com/bloodyteam/app/security/StringVault.java") != null)
            assertTrue(outZip.getEntry("bloody_security/proguard-rules.pro") != null)
            assertTrue(outZip.getEntry("bloody_security/dex_packing_manifest.json") != null)
            assertTrue(outZip.getEntry("native_jni/dex_shield_jni.cpp") != null)
            assertTrue(outZip.getEntry("native_jni/CMakeLists.txt") != null)
            assertTrue(outZip.getEntry("stub/com/bloodyteam/app/stub/DexStubApplication.java") != null)
        }
    }

    @Test
    fun apkProtectorAndSignerProducesValidV1AndV2SignedApk() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inputApk = File(context.cacheDir, "sample_input.apk")
        val fakeDex = ByteArray(512).apply {
            "dex\n038\u0000".toByteArray(Charsets.US_ASCII).copyInto(this, 0)
            for (i in 8 until size) this[i] = (i * 7 and 0xFF).toByte()
        }
        val fakeArsc = ByteArray(128) { 0x41 }
        val fakeSo = ByteArray(256) { 0x7F }

        ZipOutputStream(FileOutputStream(inputApk)).use { zos ->
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write("<manifest/>".toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write(fakeDex)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("resources.arsc"))
            zos.write(fakeArsc)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("lib/arm64-v8a/libdemo.so"))
            zos.write(fakeSo)
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("DebugProbesKt.bin"))
            zos.write(byteArrayOf(1, 2, 3, 4))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("META-INF/OLD.RSA"))
            zos.write(byteArrayOf(9, 8, 7))
            zos.closeEntry()
        }

        val profile = ProtectionProfile(
            appName = "Bloody Team",
            packageName = "com.bloodyteam.demo",
            dexBytecodeGuard = true,
            assetAes256Encryption = true,
            antiTamperSignature = true,
            stringMetadataObfuscation = true,
            zeroCrashExceptionGuard = true,
            nativeJniStubPacking = true,
            signOutputApk = true,
            stripDebugMetadata = true
        )

        val artifact = AppProtectorPipeline.protectArchive(
            context = context,
            inputUri = Uri.fromFile(inputApk),
            inputFileName = "sample_input.apk",
            profile = profile,
            passphrase = "SignMaster#2026",
            secretToHide = "api_secret_token_abc",
            onStep = {}
        )

        assertTrue(artifact.isApkInput)
        assertTrue(artifact.suggestedPrimaryName.endsWith("_bloody_signed.apk"))
        assertTrue(artifact.primaryOutputFile.exists())
        assertTrue(artifact.companionKitFile?.exists() == true)

        val verify = ApkSignerEngine.verifyApkSignatures(artifact.primaryOutputFile)
        assertTrue("APK v1 JAR signature missing", verify.hasV1JarSignature)
        assertTrue("APK v2 Signing Block missing", verify.hasV2SigningBlock)
        assertTrue("APK v2 RSA-2048 signature verification failed", verify.v2RsaVerified)

        ZipFile(artifact.primaryOutputFile).use { apkZip ->
            assertTrue(apkZip.getEntry("META-INF/OLD.RSA") == null)
            assertTrue(apkZip.getEntry("DebugProbesKt.bin") == null)
            assertEquals(ZipEntry.STORED, apkZip.getEntry("resources.arsc").method)
            assertEquals(ZipEntry.STORED, apkZip.getEntry("lib/arm64-v8a/libdemo.so").method)
            assertTrue(apkZip.getEntry("assets/bloody_wall/protection_policy.json") != null)
            assertTrue("Non-binary manifest safely preserves original classes.dex", apkZip.getEntry("classes.dex") != null)

            val encSecretBytes = apkZip.getInputStream(apkZip.getEntry("assets/bloody_wall/vault_config.enc")).readBytes()
            val decrypted = String(CryptoVaultEngine.decryptBytes(encSecretBytes, "SignMaster#2026"), Charsets.UTF_8)
            assertEquals("api_secret_token_abc", decrypted)
        }
        ZipFile(artifact.companionKitFile!!).use { kitZip ->
            assertTrue(kitZip.getEntry("disguised_dex_assets/tex_atlas_0.png") != null)
        }
    }

    @Test
    fun themePresetsAreDistinctAndRandomShuffleChangesPreset() {
        assertEquals(5, AppThemePreset.entries.size)
        val current = AppThemePreset.CRIMSON_FORGE
        val shuffled = AppThemePreset.randomDifferentFrom(current)
        assertNotEquals(current, shuffled)
    }

    @Test
    fun compiledStubDexAndBinaryAxmlPatchingEndToEnd() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val stubDexBytes = context.assets.open("bloody_stub.dex").use { it.readBytes() }
        val stubReport = DexApkInspector.parseDexBytes("classes.dex", stubDexBytes)
        assertEquals("DEX 035", stubReport.dexVersion)
        assertTrue(stubReport.withinDalvik65kLimit)

        // Locate real compiled binary AndroidManifest.xml from existing debug APK if present
        val debugApk = File("build/outputs/apk/debug/app-debug.apk").let { f ->
            if (f.exists()) f else File("app/build/outputs/apk/debug/app-debug.apk")
        }
        if (debugApk.exists()) {
            val realManifestBytes = ZipFile(debugApk).use { zf ->
                zf.getInputStream(zf.getEntry("AndroidManifest.xml")).readBytes()
            }
            val parsedOrig = AxmlManifestPatcher.inspectBinaryManifest(realManifestBytes)
            assertTrue(parsedOrig.isValidBinaryAxml)
            assertTrue(parsedOrig.packageName.isNotBlank())

            val patched = AxmlManifestPatcher.patchApplicationStub(realManifestBytes)
            val reParsed = AxmlManifestPatcher.inspectBinaryManifest(patched.patchedBytes)
            assertTrue(reParsed.isValidBinaryAxml)
            assertEquals(parsedOrig.packageName, reParsed.packageName)
            assertEquals(AxmlManifestPatcher.STUB_APPLICATION_CLASS, reParsed.originalApplicationClass)

            // Run full protection pipeline on real APK and verify Stub DEX + Encrypted DEX Vault + v1/v2 Signatures
            val profile = ProtectionProfile(
                appName = "Bloody Team",
                packageName = parsedOrig.packageName,
                signOutputApk = true,
                nativeJniStubPacking = true,
                dexBytecodeGuard = true
            )
            val artifact = AppProtectorPipeline.protectArchive(
                context = context,
                inputUri = Uri.fromFile(debugApk),
                inputFileName = "app-debug.apk",
                profile = profile,
                passphrase = "RealApkPass#2026",
                secretToHide = "live_token_123",
                onStep = {}
            )
            assertTrue(artifact.stubDexInjected)
            assertTrue(artifact.manifestPatched)
            assertEquals(parsedOrig.packageName, artifact.detectedPackageName)
            assertTrue(artifact.signedVerification?.v2RsaVerified == true)

            ZipFile(artifact.primaryOutputFile).use { outApk ->
                val outClassesDex = outApk.getInputStream(outApk.getEntry("classes.dex")).readBytes()
                assertArrayEquals(stubDexBytes, outClassesDex)

                val packedAsset = outApk.getInputStream(outApk.getEntry("assets/bloody_wall/tex_atlas_0.png")).readBytes()
                val unpackedDex = DexPackerEngine.unpackDexEntry(packedAsset, "RealApkPass#2026", 0)
                val origDex = ZipFile(debugApk).use { it.getInputStream(it.getEntry("classes.dex")).readBytes() }
                assertArrayEquals(origDex, unpackedDex)
            }

            // Run real Android SDK zipalign & apksigner verification on generated Stub APK
            val zipalignBin = File("/opt/android/sdk/build-tools/36.0.0/zipalign")
            val apksignerBin = File("/opt/android/sdk/build-tools/36.0.0/apksigner")
            if (zipalignBin.exists() && apksignerBin.exists()) {
                val alignProc = ProcessBuilder(zipalignBin.absolutePath, "-c", "4", artifact.primaryOutputFile.absolutePath)
                    .redirectErrorStream(true).start()
                assertEquals("SDK zipalign verification must succeed", 0, alignProc.waitFor())

                val signProc = ProcessBuilder(
                    apksignerBin.absolutePath, "verify", "--min-sdk-version", "21",
                    artifact.primaryOutputFile.absolutePath
                ).redirectErrorStream(true).start()
                assertEquals("SDK apksigner verification must succeed", 0, signProc.waitFor())
            }

            // Also verify Wrapper-Stub mode (when DEX encryption is OFF but runtime guards are ON)
            val wrapperProfile = ProtectionProfile(
                appName = "Bloody Team",
                packageName = parsedOrig.packageName,
                signOutputApk = true,
                dexBytecodeGuard = false,
                nativeJniStubPacking = false,
                screenTapjackingShield = true,
                loginValidateV2 = true,
                hideRoot = true,
                removeBundlingSoftware = true,
                shrinkAnim = true
            )
            val wrapperArtifact = AppProtectorPipeline.protectArchive(
                context = context,
                inputUri = Uri.fromFile(debugApk),
                inputFileName = "app-debug.apk",
                profile = wrapperProfile,
                passphrase = "RealApkPass#2026",
                secretToHide = "",
                onStep = {}
            )
            assertTrue(wrapperArtifact.stubDexInjected)
            assertTrue(wrapperArtifact.manifestPatched)
            assertTrue(wrapperArtifact.signedVerification?.v2RsaVerified == true)
            ZipFile(wrapperArtifact.primaryOutputFile).use { wrapApk ->
                val origDex = ZipFile(debugApk).use { it.getInputStream(it.getEntry("classes.dex")).readBytes() }
                assertArrayEquals(origDex, wrapApk.getInputStream(wrapApk.getEntry("classes.dex")).readBytes())
                val dexEntries = wrapApk.entries().toList().count { it.name.matches(Regex("^classes\\d*\\.dex$")) }
                assertTrue("Wrapper stub DEX must be appended as classesN.dex", dexEntries >= 2)
                assertArrayEquals(stubDexBytes, wrapApk.getInputStream(wrapApk.getEntry("classes$dexEntries.dex")).readBytes())

                val policyText = wrapApk.getInputStream(wrapApk.getEntry("assets/bloody_wall/protection_policy.json")).readBytes().toString(Charsets.UTF_8)
                assertTrue(policyText.contains("\"loginValidateV2\": true"))
                assertTrue(policyText.contains("\"hideRoot\": true"))
                assertTrue(policyText.contains("\"removeBundlingSoftware\": true"))
                assertTrue(policyText.contains("\"anim\""))
            }
            if (zipalignBin.exists() && apksignerBin.exists()) {
                val alignWrap = ProcessBuilder(zipalignBin.absolutePath, "-c", "4", wrapperArtifact.primaryOutputFile.absolutePath)
                    .redirectErrorStream(true).start()
                assertEquals("Wrapper APK SDK zipalign verification must succeed", 0, alignWrap.waitFor())

                val signWrap = ProcessBuilder(
                    apksignerBin.absolutePath, "verify", "--min-sdk-version", "21",
                    wrapperArtifact.primaryOutputFile.absolutePath
                ).redirectErrorStream(true).start()
                assertEquals("Wrapper APK SDK apksigner verification must succeed", 0, signWrap.waitFor())
            }
        }
    }

    @Test
    fun dexPackerDisguiseAndJniStubGeneration() {
        val fakeDex = ByteArray(512)
        "dex\n038\u0000".toByteArray(Charsets.US_ASCII).copyInto(fakeDex, 0)
        for (i in 8 until fakeDex.size) fakeDex[i] = (i and 0xFF).toByte()

        val expectedAssets = mapOf(
            ".png" to "tex_atlas_0.png",
            ".dat" to "font_metrics_0.dat",
            ".bin" to "res_cache_0.bin"
        )
        for ((ext, expectedName) in expectedAssets) {
            val packed = DexPackerEngine.packDexEntry(
                dexEntryName = "classes.dex",
                dexBytes = fakeDex,
                passphrase = "BloodyPass#99_বাংলা",
                disguiseExtension = ext,
                index = 0
            )
            assertEquals(expectedName, packed.disguisedAssetName)
            val unpacked = DexPackerEngine.unpackDexEntry(
                packedBytes = packed.packedBytes,
                passphrase = "BloodyPass#99_বাংলা",
                index = 0
            )
            assertArrayEquals(fakeDex, unpacked)
        }

        val profile = ProtectionProfile(appName = "Bloody Team", packageName = "com.bloodyteam.app")
        val cpp = DexPackerEngine.generateNativeJniCpp(profile)
        val stub = DexPackerEngine.generateStubApplicationJava(profile)
        assertTrue(cpp.contains("Java_com_bloodyteam_app_stub_DexStubApplication_nativeDecryptDexToRam"))
        assertTrue(stub.contains("InMemoryDexClassLoader"))
    }

    @Test
    fun smartCompatibilityAnalyzerAutoSelectsAndWarns() {
        val base = ProtectionProfile()

        // 1. Standard Android APK with INTERNET and assets
        val standardReport = com.example.security.ApkInspectionReport(
            sourceLabel = "app.apk",
            totalArchiveSizeBytes = 1024_000L,
            totalEntries = 50,
            dexReports = listOf(
                com.example.security.DexFileReport("classes.dex", "DEX 038", 50_000L, 100, 50, 1200, 80, "AABB1122", true)
            ),
            assetEntriesCount = 4,
            encryptedAssetCount = 0,
            nativeLibArchitectures = listOf("arm64-v8a"),
            hasManifest = true,
            hasSignatureBlock = true,
            detectedPackageName = "com.demo.standard",
            usesPermissions = listOf("android.permission.INTERNET"),
            isApkArchive = true,
            detectedFramework = "Standard Java/Kotlin APK",
            dataFileEntriesCount = 2,
            summaryMessage = "Standard Java/Kotlin APK verified"
        )
        val stdRes = com.example.security.ApkCompatibilityAnalyzer.recommendProfile(standardReport, base)
        assertTrue(stdRes.recommendedProfile.dexBytecodeGuard)
        assertTrue(stdRes.recommendedProfile.nativeJniStubPacking)
        assertTrue(stdRes.recommendedProfile.assetAes256Encryption)
        assertTrue(stdRes.recommendedProfile.zeroCrashExceptionGuard)
        assertFalse(stdRes.recommendedProfile.emulatorCloneShield)

        // 2. VPN APK must auto-disable Check VPN, and warn CRITICAL if user re-enables it
        val vpnReport = standardReport.copy(
            sourceLabel = "my_vpn.apk",
            usesPermissions = listOf("android.permission.INTERNET", "android.permission.BIND_VPN_SERVICE")
        )
        val vpnRes = com.example.security.ApkCompatibilityAnalyzer.recommendProfile(vpnReport, base)
        assertFalse("Check VPN must be auto-disabled for VPN apps", vpnRes.recommendedProfile.sslPinningProxyBlock)
        val vpnForcedWarnings = com.example.security.ApkCompatibilityAnalyzer.analyzeWarnings(
            vpnReport,
            vpnRes.recommendedProfile.copy(sslPinningProxyBlock = true)
        )
        assertTrue(vpnForcedWarnings.any { it.severity == com.example.security.WarningSeverity.CRITICAL })

        // 3. Flutter / Already Packed APKs must auto-disable Stub DEX replacement to prevent crash
        val flutterReport = standardReport.copy(
            sourceLabel = "flutter_app.apk",
            detectedFramework = "Flutter Engine APK"
        )
        val flRes = com.example.security.ApkCompatibilityAnalyzer.recommendProfile(flutterReport, base)
        assertFalse(flRes.recommendedProfile.nativeJniStubPacking)
        val flWarnings = com.example.security.ApkCompatibilityAnalyzer.analyzeWarnings(
            flutterReport,
            flRes.recommendedProfile.copy(dexBytecodeGuard = true, nativeJniStubPacking = true)
        )
        assertTrue(flWarnings.any { it.severity == com.example.security.WarningSeverity.CRITICAL && it.fixTransform != null })

        // 4. Sketchware APK auto-detection & conflicting config toggles warning
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sketchApk = File(context.cacheDir, "sketchware_sample.apk")
        val sketchDex = ByteArray(256).apply {
            "dex\n035\u0000".toByteArray(Charsets.US_ASCII).copyInto(this, 0)
            ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).putInt(88, 1200)
            val marker = "Lcom/besome/sketch/SketchApplication;".toByteArray(Charsets.US_ASCII)
            marker.copyInto(this, 112)
        }
        ZipOutputStream(FileOutputStream(sketchApk)).use { zos ->
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write(byteArrayOf(1, 2, 3, 4))
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write(sketchDex)
            zos.closeEntry()
        }
        val sketchReport = DexApkInspector.inspectUri(context, Uri.fromFile(sketchApk), "sketchware_sample.apk")
        assertTrue("Sketchware APK must be detected from DEX marker", sketchReport.isSketchwareApp)
        assertTrue(sketchReport.detectedFramework.contains("Sketchware"))

        val sketchAuto = com.example.security.ApkCompatibilityAnalyzer.recommendProfile(sketchReport, base)
        assertEquals(com.example.security.WallProtectionTier.WALL_V3_JNI_STUB, sketchAuto.recommendedProfile.resolvedWallTier)
        assertTrue(sketchAuto.recommendedProfile.removeBundlingSoftware)
        assertFalse(sketchAuto.recommendedProfile.shrinkResources)

        val conflictWarnings = com.example.security.ApkCompatibilityAnalyzer.analyzeWarnings(
            sketchReport,
            sketchAuto.recommendedProfile.copy(
                rootMagiskDetection = true,
                hideRoot = true,
                sslPinningProxyBlock = true,
                removeDetectVpnProxy = true,
                detectHttpCanary = true,
                removeDetectHttpCanary = true
            )
        )
        assertTrue(conflictWarnings.any { it.title.contains("Hide Root Conflict") })
        assertTrue(conflictWarnings.any { it.title.contains("Remove Detect VPN Conflict") })
        assertTrue(conflictWarnings.any { it.title.contains("HttpCanary Detect vs Remove Conflict") })
    }

    @Test
    fun zeroCrashGuardAbsorbsWorkerFaultWithoutCrashing() {
        ZeroCrashGuard.clearLogs()
        ZeroCrashGuard.install()
        ZeroCrashGuard.simulateGuardedWorkerFault()
        assertTrue(ZeroCrashGuard.isGuardActive.value)
        assertTrue(ZeroCrashGuard.faultLog.value.isNotEmpty())
        assertArrayEquals(
            byteArrayOf(1, 2, 3),
            ZeroCrashGuard.runGuarded("unitTest", byteArrayOf(1, 2, 3)) {
                error("boom")
            }
        )
    }

    @Test
    fun featureGuideCatalogAndLanguagesAreComplete() {
        val langs = com.example.i18n.AppLanguage.entries
        assertTrue(langs.size >= 6)
        assertTrue(langs.any { it.code == "en" })
        assertTrue(langs.any { it.code == "bn" })
        assertTrue(langs.any { it.code == "hi" })

        for (lang in langs) {
            val strings = com.example.i18n.AppStrings.get(lang)
            assertTrue(strings.guideScreenTitle.isNotBlank())
            assertTrue(strings.whatItDoesLabel.isNotBlank())
            assertTrue(strings.howItWorksLabel.isNotBlank())
            assertTrue(strings.safetyAndWhenLabel.isNotBlank())
        }

        val allSpecs = com.example.i18n.FeatureGuideCatalog.allFeatures
        assertTrue("Expected at least 50 documented features", allSpecs.size >= 50)
        val uniqueIds = allSpecs.map { it.id }.toSet()
        assertEquals("Feature IDs must be unique", allSpecs.size, uniqueIds.size)

        val base = ProtectionProfile()
        for (spec in allSpecs) {
            for (lang in langs) {
                val exp = spec.explain(lang)
                assertTrue("${spec.id} title missing for $lang", exp.title.isNotBlank())
                assertTrue("${spec.id} whatItDoes missing for $lang", exp.whatItDoes.isNotBlank())
                assertTrue("${spec.id} howItWorks missing for $lang", exp.howItWorks.isNotBlank())
                assertTrue("${spec.id} whenToUse missing for $lang", exp.whenToUse.isNotBlank())
            }
            val checker = spec.isChecked
            val toggler = spec.applyToggle
            if (checker != null && toggler != null) {
                val enabled = toggler(base, true)
                assertTrue("${spec.id} should enable", checker(enabled))
                val disabled = toggler(enabled, false)
                assertFalse("${spec.id} should disable", checker(disabled))
            }
        }
    }
}

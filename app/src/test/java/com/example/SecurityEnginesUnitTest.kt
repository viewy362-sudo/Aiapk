package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.security.AppProtectorPipeline
import com.example.security.CryptoVaultEngine
import com.example.security.DexApkInspector
import com.example.security.ProtectionProfile
import com.example.security.SketchwareKitGenerator
import com.example.security.StringObfuscatorEngine
import com.example.security.ZeroCrashGuard
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun sketchwareKitGeneratorIncludesAll10Layers() {
        val profile = ProtectionProfile(
            appName = "BLOODY TEAM",
            packageName = "com.bloodyteam.app",
            expectedSignatureSha256 = "AA:BB:CC"
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

        val profile = ProtectionProfile(appName = "BLOODY TEAM", packageName = "com.bloodyteam.app")
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
        assertTrue(artifact.outputCacheFile.exists())

        ZipFile(artifact.outputCacheFile).use { outZip ->
            val encEntry = outZip.getEntry("assets/config.json.enc")
            assertTrue("Encrypted asset must exist", encEntry != null)
            val encBytes = outZip.getInputStream(encEntry).readBytes()
            val decrypted = String(CryptoVaultEngine.decryptBytes(encBytes, "TestPass#2026"), Charsets.UTF_8)
            assertEquals(assetText, decrypted)

            assertTrue(outZip.getEntry("dexshield_security/java/com/bloodyteam/app/security/DexShieldGuard.java") != null)
            assertTrue(outZip.getEntry("dexshield_security/java/com/bloodyteam/app/security/AssetVaultHelper.java") != null)
            assertTrue(outZip.getEntry("dexshield_security/java/com/bloodyteam/app/security/SslPinningGuard.java") != null)
            assertTrue(outZip.getEntry("dexshield_security/java/com/bloodyteam/app/security/StringVault.java") != null)
            assertTrue(outZip.getEntry("dexshield_security/proguard-rules.pro") != null)
        }
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
}

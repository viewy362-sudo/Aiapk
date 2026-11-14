package com.example

import com.example.security.CryptoVaultEngine
import com.example.security.DexApkInspector
import com.example.security.SketchwareKitGenerator
import com.example.security.ProtectionProfile
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
import java.nio.ByteBuffer
import java.nio.ByteOrder

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

        // Tamper 1 byte in ciphertext and verify GCM rejects it
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
        buf.putInt(56, 1200) // string_ids_size
        buf.putInt(64, 340)  // type_ids_size
        buf.putInt(88, 12450) // method_ids_size
        buf.putInt(96, 180)  // class_defs_size

        val safeReport = DexApkInspector.parseDexBytes("classes.dex", fakeDex)
        assertEquals("DEX 035", safeReport.dexVersion)
        assertEquals(12450, safeReport.methodIdsCount)
        assertTrue(safeReport.withinDalvik65kLimit)

        // Exceed 65,535 method limit
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
    fun sketchwareKitGeneratorIncludesTargetIdentifiers() {
        val profile = ProtectionProfile(
            appName = "BLOODY TEAM",
            packageName = "com.bloodyteam.app",
            expectedSignatureSha256 = "AA:BB:CC"
        )
        val java = SketchwareKitGenerator.generateSketchwareJavaGuard(profile)
        val proguard = SketchwareKitGenerator.generateProguardRules(profile)
        assertTrue(java.contains("package com.bloodyteam.app.security;"))
        assertTrue(java.contains("AA:BB:CC"))
        assertTrue(proguard.contains("-repackageclasses"))
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

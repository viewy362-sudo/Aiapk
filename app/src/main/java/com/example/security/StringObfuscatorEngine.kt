package com.example.security

import kotlin.random.Random

data class ObfuscatedStringResult(
    val originalInput: String,
    val encodedBytes: IntArray,
    val keySeed: Int,
    val javaDecoderCode: String,
    val kotlinDecoderCode: String,
    val verifiedRoundTrip: String,
    val proguardDictionarySnippet: String
)

/**
 * Layer 5: Advanced String & Metadata Obfuscation Engine.
 * Eliminates plaintext string literals from classes.dex by transforming them into
 * rotating-XOR + bit-rotated integer arrays and generating zero-dependency
 * Sketchware Pro / Dalvik / ART decoder methods.
 */
object StringObfuscatorEngine {

    fun obfuscate(secret: String, methodName: String = "resolveVaultSecret"): ObfuscatedStringResult {
        val raw = secret.toByteArray(Charsets.UTF_8)
        val seed = (Random.nextInt(0x11, 0x7E) shl 8) or Random.nextInt(0x13, 0xFE)
        val encoded = IntArray(raw.size)

        for (i in raw.indices) {
            val mask = ((seed + i * 31) xor (i * 17)) and 0xFF
            val xored = (raw[i].toInt() and 0xFF) xor mask
            val rotated = ((xored shl 3) or (xored ushr 5)) and 0xFF
            encoded[i] = rotated
        }

        val verified = decodeInternal(encoded, seed)
        val safeMethod = methodName.replace(Regex("[^A-Za-z0-9_]"), "").ifEmpty { "resolveSecret" }
        val arrayLiteral = encoded.joinToString(", ")

        val javaCode = """
            |public static String $safeMethod() {
            |    final int[] data = new int[] { $arrayLiteral };
            |    final int seed = $seed;
            |    final byte[] out = new byte[data.length];
            |    for (int i = 0; i < data.length; i++) {
            |        int v = data[i] & 0xFF;
            |        int unrot = ((v >>> 3) | (v << 5)) & 0xFF;
            |        int mask = ((seed + i * 31) ^ (i * 17)) & 0xFF;
            |        out[i] = (byte) (unrot ^ mask);
            |    }
            |    return new String(out, java.nio.charset.StandardCharsets.UTF_8);
            |}
        """.trimMargin()

        val kotlinCode = """
            |fun $safeMethod(): String {
            |    val data = intArrayOf($arrayLiteral)
            |    val seed = $seed
            |    val out = ByteArray(data.size)
            |    for (i in data.indices) {
            |        val v = data[i] and 0xFF
            |        val unrot = ((v ushr 3) or (v shl 5)) and 0xFF
            |        val mask = ((seed + i * 31) xor (i * 17)) and 0xFF
            |        out[i] = (unrot xor mask).toByte()
            |    }
            |    return String(out, Charsets.UTF_8)
            |}
        """.trimMargin()

        val dict = """
            |# R8 / ProGuard Obfuscation Hardening
            |-repackageclasses 'o0'
            |-allowaccessmodification
            |-renamesourcefileattribute SourceFile
            |-obfuscationdictionary dexshield_dict.txt
            |-classobfuscationdictionary dexshield_dict.txt
            |-packageobfuscationdictionary dexshield_dict.txt
        """.trimMargin()

        return ObfuscatedStringResult(
            originalInput = secret,
            encodedBytes = encoded,
            keySeed = seed,
            javaDecoderCode = javaCode,
            kotlinDecoderCode = kotlinCode,
            verifiedRoundTrip = verified,
            proguardDictionarySnippet = dict
        )
    }

    private fun decodeInternal(encoded: IntArray, seed: Int): String {
        val out = ByteArray(encoded.size)
        for (i in encoded.indices) {
            val v = encoded[i] and 0xFF
            val unrot = ((v ushr 3) or (v shl 5)) and 0xFF
            val mask = ((seed + i * 31) xor (i * 17)) and 0xFF
            out[i] = (unrot xor mask).toByte()
        }
        return String(out, Charsets.UTF_8)
    }
}

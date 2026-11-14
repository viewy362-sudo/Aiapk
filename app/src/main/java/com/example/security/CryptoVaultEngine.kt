package com.example.security

import android.util.Base64
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ln

data class VaultPayloadResult(
    val cipherBytes: ByteArray,
    val base64Envelope: String,
    val hexPreview: String,
    val entropyBitsPerByte: Double,
    val originalBytes: Int,
    val encryptedBytes: Int
)

/**
 * Layer 2: Real AES-256-GCM Asset & Resource Encryption Engine.
 * Binary Envelope Format:
 *   [4 bytes: Magic "DXS1"] [16 bytes: Salt] [12 bytes: GCM IV] [Ciphertext + 16B Auth Tag]
 * Uses pure HmacSHA256-based PBKDF2 for 100% compatibility across API 24..36 (Dalvik & ART).
 */
object CryptoVaultEngine {
    private val MAGIC = byteArrayOf('D'.code.toByte(), 'X'.code.toByte(), 'S'.code.toByte(), '1'.code.toByte())
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val GCM_TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 16_384
    private const val KEY_BYTES = 32 // 256-bit AES key

    private val secureRandom = SecureRandom()

    fun encryptBytes(plainBytes: ByteArray, passphrase: String): VaultPayloadResult {
        require(passphrase.isNotEmpty()) { "Passphrase cannot be empty" }
        val salt = ByteArray(SALT_LEN).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { secureRandom.nextBytes(it) }

        val keyBytes = deriveKeyPbkdf2HmacSha256(passphrase.toByteArray(Charsets.UTF_8), salt, PBKDF2_ITERATIONS, KEY_BYTES)
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val encrypted = cipher.doFinal(plainBytes)

        val envelope = ByteBuffer.allocate(MAGIC.size + SALT_LEN + IV_LEN + encrypted.size)
            .put(MAGIC)
            .put(salt)
            .put(iv)
            .put(encrypted)
            .array()

        val b64 = Base64.encodeToString(envelope, Base64.NO_WRAP)
        return VaultPayloadResult(
            cipherBytes = envelope,
            base64Envelope = b64,
            hexPreview = toHexPreview(envelope),
            entropyBitsPerByte = shannonEntropy(envelope),
            originalBytes = plainBytes.size,
            encryptedBytes = envelope.size
        )
    }

    fun decryptBytes(envelope: ByteArray, passphrase: String): ByteArray {
        require(passphrase.isNotEmpty()) { "Passphrase cannot be empty" }
        val minHeader = MAGIC.size + SALT_LEN + IV_LEN + 16
        require(envelope.size >= minHeader) { "Invalid DXS1 vault payload (too short)" }
        for (i in MAGIC.indices) {
            require(envelope[i] == MAGIC[i]) { "Invalid DXS1 magic header" }
        }

        val saltStart = MAGIC.size
        val ivStart = saltStart + SALT_LEN
        val dataStart = ivStart + IV_LEN

        val salt = envelope.copyOfRange(saltStart, ivStart)
        val iv = envelope.copyOfRange(ivStart, dataStart)
        val cipherData = envelope.copyOfRange(dataStart, envelope.size)

        val keyBytes = deriveKeyPbkdf2HmacSha256(passphrase.toByteArray(Charsets.UTF_8), salt, PBKDF2_ITERATIONS, KEY_BYTES)
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherData)
    }

    fun encryptText(plainText: String, passphrase: String): VaultPayloadResult =
        encryptBytes(plainText.toByteArray(Charsets.UTF_8), passphrase)

    fun decryptTextFromBase64(base64Envelope: String, passphrase: String): String {
        val raw = Base64.decode(base64Envelope.trim(), Base64.DEFAULT)
        return String(decryptBytes(raw, passphrase), Charsets.UTF_8)
    }

    fun shannonEntropy(data: ByteArray): Double {
        if (data.isEmpty()) return 0.0
        val counts = IntArray(256)
        for (b in data) {
            counts[b.toInt() and 0xFF]++
        }
        val len = data.size.toDouble()
        var entropy = 0.0
        for (count in counts) {
            if (count > 0) {
                val p = count / len
                entropy -= p * (ln(p) / ln(2.0))
            }
        }
        return entropy
    }

    fun toHexPreview(data: ByteArray, maxBytes: Int = 24): String {
        val slice = if (data.size > maxBytes) data.copyOf(maxBytes) else data
        val hex = slice.joinToString(" ") { "%02X".format(it) }
        return if (data.size > maxBytes) "$hex ..." else hex
    }

    /**
     * Pure RFC 2898 PBKDF2-HMAC-SHA256 implementation using HmacSHA256.
     */
    private fun deriveKeyPbkdf2HmacSha256(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        keyLength: Int
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password, "HmacSHA256"))
        val macLen = mac.macLength
        val blocks = (keyLength + macLen - 1) / macLen
        val output = ByteArray(blocks * macLen)
        val blockIndex = ByteArray(4)

        for (block in 1..blocks) {
            blockIndex[0] = (block ushr 24).toByte()
            blockIndex[1] = (block ushr 16).toByte()
            blockIndex[2] = (block ushr 8).toByte()
            blockIndex[3] = block.toByte()

            mac.reset()
            mac.update(salt)
            mac.update(blockIndex)
            var u = mac.doFinal()
            val t = u.copyOf()

            for (iter in 2..iterations) {
                mac.reset()
                u = mac.doFinal(u)
                for (j in t.indices) {
                    t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
                }
            }
            System.arraycopy(t, 0, output, (block - 1) * macLen, macLen)
        }
        return output.copyOf(keyLength)
    }
}

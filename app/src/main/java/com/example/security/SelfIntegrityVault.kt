package com.example.security

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import java.security.MessageDigest

data class SelfIntegrityStatus(
    val isIntegrityValid: Boolean,
    val tamperReasons: List<String>,
    val linksVerified: Boolean,
    val packageVerified: Boolean,
    val runtimeHookFree: Boolean,
    val cloneFree: Boolean
)

/**
 * Runtime self-protection vault for Bloody Team Apk Shield.
 *
 * All sensitive URLs and identity strings are stored as rotating-XOR + bit-rotated byte arrays
 * paired with SHA-256 integrity digests so attackers cannot search or patch plaintext strings
 * in DEX/Smali bytecode without triggering a tamper lockdown.
 */
object SelfIntegrityVault {

    private const val SEED_TG_JOIN = 0x4B
    private val ENC_TG_JOIN = intArrayOf(
        201, 90, 107, 188, 213, 109, 242, 131, 42, 137, 164, 21, 52, 226, 225,
        203, 131, 118, 237, 191, 111, 201, 146, 2, 78, 95, 31, 87, 225, 112
    )
    private const val HASH_TG_JOIN =
        "076a574d418212a427d09cdde7ed8a8fe2855653c61d36e9f2c7bf1c10cc52df"

    private const val SEED_TG_CHAN = 0x73
    private val ENC_TG_CHAN = intArrayOf(
        138, 155, 172, 253, 23, 42, 179, 64, 107, 78, 101, 86, 242, 232, 171, 66,
        181, 28, 199, 222, 160, 243, 162, 69, 252, 15, 70, 97, 195, 242, 173
    )
    private const val HASH_TG_CHAN =
        "1a07f74ad676e4922debd5e860c428e1ba07fd8810997d5989d8c2d09f03db22"

    private const val SEED_YT_CHAN = 0x29
    private val ENC_YT_CHAN = intArrayOf(
        248, 73, 90, 171, 228, 124, 197, 146, 113, 146, 83, 140, 213, 191, 144,
        155, 162, 147, 148, 87, 127, 232, 193, 82, 27, 148, 149, 38, 39, 129,
        242, 155, 238, 199, 224, 137, 2, 60, 71, 118, 247, 2, 57, 211, 3, 140,
        150, 183, 200, 43, 178, 250, 244, 151, 175, 185, 40, 131
    )
    private const val HASH_YT_CHAN =
        "f3d6fde83e777d79105fd6580e51571e0cbf4e05970b485d9c77f7c0b1566b97"

    private const val SEED_PKG_ID = 0x91
    private val ENC_PKG_ID = intArrayOf(
        227, 84, 85, 28, 176, 161, 98, 139, 84, 13, 118, 23, 203, 202, 211,
        236, 101, 110, 119, 64, 218, 203, 142, 117, 62, 149, 136, 225, 59
    )
    private const val HASH_PKG_ID =
        "02c78f313b60d348ea24d9ba364082013c63b1efe87a96c9a76f07aba6b4424b"

    private const val SEED_BRAND = 0xD5
    private val ENC_BRAND = intArrayOf(
        204, 111, 102, 49, 187, 2, 222, 172, 247, 6, 113
    )
    private const val HASH_BRAND =
        "b54f1cbb74676231ad6cee6584a6e977c1d82234a7b9969db0dd981c2a666e81"

    private val TELEGRAM_CLIENT_PACKAGES = listOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.telegram.plus",
        "org.thunderdog.challegram"
    )

    private val CLONE_CONTAINER_MARKERS = listOf(
        "parallel",
        "virtualapp",
        "dualspace",
        "multiaccount",
        "cloneapp",
        "dkplugin",
        "excelliance"
    )

    internal fun decodeVaultBytes(encoded: IntArray, seed: Int): ByteArray {
        val out = ByteArray(encoded.size)
        for (i in encoded.indices) {
            val step = encoded[i] xor 0xA7
            val x = ((step ushr 3) or (step shl 5)) and 0xFF
            val k = (seed + i * 37 + (i xor 0x5A)) and 0xFF
            out[i] = (x xor k).toByte()
        }
        return out
    }

    internal fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    internal fun verifySlot(encoded: IntArray, seed: Int, expectedHash: String): Boolean {
        val decoded = decodeVaultBytes(encoded, seed)
        return MessageDigest.isEqual(
            sha256Hex(decoded).toByteArray(Charsets.US_ASCII),
            expectedHash.toByteArray(Charsets.US_ASCII)
        )
    }

    fun areAllLinksValid(): Boolean {
        return verifySlot(ENC_TG_JOIN, SEED_TG_JOIN, HASH_TG_JOIN) &&
            verifySlot(ENC_TG_CHAN, SEED_TG_CHAN, HASH_TG_CHAN) &&
            verifySlot(ENC_YT_CHAN, SEED_YT_CHAN, HASH_YT_CHAN) &&
            verifySlot(ENC_PKG_ID, SEED_PKG_ID, HASH_PKG_ID) &&
            verifySlot(ENC_BRAND, SEED_BRAND, HASH_BRAND)
    }

    fun getTelegramJoinUrl(): String {
        val raw = decodeVaultBytes(ENC_TG_JOIN, SEED_TG_JOIN)
        check(sha256Hex(raw) == HASH_TG_JOIN) { "Vault link integrity mismatch (TG_JOIN)" }
        return String(raw, Charsets.UTF_8)
    }

    fun getTelegramChannelUrl(): String {
        val raw = decodeVaultBytes(ENC_TG_CHAN, SEED_TG_CHAN)
        check(sha256Hex(raw) == HASH_TG_CHAN) { "Vault link integrity mismatch (TG_CHAN)" }
        return String(raw, Charsets.UTF_8)
    }

    fun getYoutubeUrl(): String {
        val raw = decodeVaultBytes(ENC_YT_CHAN, SEED_YT_CHAN)
        check(sha256Hex(raw) == HASH_YT_CHAN) { "Vault link integrity mismatch (YT_CHAN)" }
        return String(raw, Charsets.UTF_8)
    }

    fun getExpectedPackageId(): String {
        val raw = decodeVaultBytes(ENC_PKG_ID, SEED_PKG_ID)
        check(sha256Hex(raw) == HASH_PKG_ID) { "Vault identity mismatch (PKG_ID)" }
        return String(raw, Charsets.UTF_8)
    }

    fun getProtectedBrandTitle(): String {
        val raw = decodeVaultBytes(ENC_BRAND, SEED_BRAND)
        check(sha256Hex(raw) == HASH_BRAND) { "Vault brand mismatch (BRAND)" }
        return String(raw, Charsets.UTF_8)
    }

    /**
     * Verifies link integrity hashes, package ID, runtime hook absence, clone isolation,
     * and release signing presence without disrupting local unit tests or debug emulator runs.
     */
    fun verifyAppIntegrity(
        context: Context,
        runtimeReport: RuntimeShieldReport? = null,
        isRobolectricTestOverride: Boolean = false
    ): SelfIntegrityStatus {
        val reasons = mutableListOf<String>()

        val linksOk = ZeroCrashGuard.runGuarded("SelfIntegrity.links", false) {
            areAllLinksValid()
        }
        if (!linksOk) {
            reasons.add("Protected URL/Brand SHA-256 mismatch — tampered binary detected")
        }

        val expectedPkg = ZeroCrashGuard.runGuarded("SelfIntegrity.pkg", "") {
            getExpectedPackageId()
        }
        val actualPkg = context.packageName ?: ""
        val pkgOk = expectedPkg.isNotEmpty() && actualPkg == expectedPkg
        if (!pkgOk) {
            reasons.add("Package ID mismatch ($actualPkg)")
        }

        val filesPath = ZeroCrashGuard.runGuarded("SelfIntegrity.path", "") {
            context.filesDir?.absolutePath?.lowercase() ?: ""
        }
        val cloneOk = CLONE_CONTAINER_MARKERS.none { marker -> filesPath.contains(marker) }
        if (!cloneOk) {
            reasons.add("Cloned / Dual-Space container execution blocked")
        }

        val isRobolectric = isRobolectricTestOverride ||
            Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

        val live = runtimeReport ?: if (!isRobolectric) {
            RuntimeIntegrityShield.runLiveAudit(context)
        } else {
            null
        }

        val hooksOk = live?.hookShieldPassed ?: true
        if (!hooksOk) {
            reasons.add("Runtime hooking framework (Frida / Xposed / LSPosed) active")
        }

        if (live != null && !live.isDebuggableBuild && !live.antiTamperPassed) {
            reasons.add("Release signing certificate missing or stripped")
        }

        return SelfIntegrityStatus(
            isIntegrityValid = reasons.isEmpty(),
            tamperReasons = reasons,
            linksVerified = linksOk,
            packageVerified = pkgOk,
            runtimeHookFree = hooksOk,
            cloneFree = cloneOk
        )
    }

    /**
     * Opens a Telegram invite/channel or YouTube link reliably:
     * tries installed Telegram apps first for t.me links, then automatically falls back
     * to default handler / web browser so at least one always opens cleanly.
     */
    fun openExternalLinkSafely(context: Context, url: String): Boolean {
        val uri = Uri.parse(url)
        val isTelegramUrl = uri.host?.lowercase()?.let { it == "t.me" || it.endsWith(".t.me") } == true

        if (isTelegramUrl) {
            for (tgPackage in TELEGRAM_CLIENT_PACKAGES) {
                val targeted = Intent(Intent.ACTION_VIEW, uri).apply {
                    setPackage(tgPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val launched = try {
                    if (targeted.resolveActivity(context.packageManager) != null) {
                        context.startActivity(targeted)
                        true
                    } else {
                        false
                    }
                } catch (_: ActivityNotFoundException) {
                    false
                } catch (_: SecurityException) {
                    false
                } catch (_: Throwable) {
                    false
                }
                if (launched) return true
            }
        }

        val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val genericLaunched = try {
            context.startActivity(genericIntent)
            true
        } catch (_: Throwable) {
            false
        }
        if (genericLaunched) return true

        val fallback = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(fallback)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun isDebuggableApp(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}

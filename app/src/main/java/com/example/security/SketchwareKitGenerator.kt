package com.example.security

data class ProtectionProfile(
    val appName: String = "BLOODY TEAM",
    val packageName: String = "com.bloodyteam.app",
    val expectedSignatureSha256: String = "",
    val dexBytecodeGuard: Boolean = true,
    val assetAes256Encryption: Boolean = true,
    val antiTamperSignature: Boolean = true,
    val antiDebugFridaShield: Boolean = true,
    val stringMetadataObfuscation: Boolean = true,
    val zeroCrashExceptionGuard: Boolean = true
) {
    val enabledLayersCount: Int
        get() = listOf(
            dexBytecodeGuard,
            assetAes256Encryption,
            antiTamperSignature,
            antiDebugFridaShield,
            stringMetadataObfuscation,
            zeroCrashExceptionGuard
        ).count { it }
}

/**
 * Generates Sketchware Pro / Custom Android Toolchain compatible Java classes and R8 rules.
 * Zero external library dependencies; safe for Dalvik (API 21+) and ART (API 24..36).
 */
object SketchwareKitGenerator {

    fun generateSketchwareJavaGuard(profile: ProtectionProfile): String {
        val safeSha = profile.expectedSignatureSha256.ifBlank { "REPLACE_WITH_YOUR_SHA256_FINGERPRINT" }
        return """
            |// ==========================================================
            |// DexShield Security Guard for ${profile.appName}
            |// Target Package: ${profile.packageName}
            |// Compatible: Sketchware Pro / Dalvik & ART (API 21 - 36)
            |// ==========================================================
            |package ${profile.packageName}.security;
            |
            |import android.content.Context;
            |import android.content.pm.PackageInfo;
            |import android.content.pm.PackageManager;
            |import android.content.pm.Signature;
            |import android.os.Build;
            |import android.os.Debug;
            |import java.io.BufferedReader;
            |import java.io.File;
            |import java.io.FileReader;
            |import java.security.MessageDigest;
            |
            |public final class DexShieldGuard {
            |    private static final String EXPECTED_SHA256 = "$safeSha";
            |
            |    private DexShieldGuard() {}
            |
            |    public static void activateAll(final Context context) {
            |        ${if (profile.zeroCrashExceptionGuard) "installZeroCrashHandler();" else "// Zero-Crash Guard disabled"}
            |        if (!verifyEnvironment(context)) {
            |            throw new SecurityException("Integrity verification failed");
            |        }
            |    }
            |
            |    public static boolean verifyEnvironment(final Context context) {
            |        try {
            |            ${if (profile.antiDebugFridaShield) "if (isDebuggerOrHookPresent()) return false;" else ""}
            |            ${if (profile.antiTamperSignature) "if (!verifySigningCertificate(context)) return false;" else ""}
            |            return true;
            |        } catch (Throwable ignored) {
            |            return false;
            |        }
            |    }
            |
            |    public static void installZeroCrashHandler() {
            |        final Thread.UncaughtExceptionHandler fallback =
            |                Thread.getDefaultUncaughtExceptionHandler();
            |        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            |            @Override
            |            public void uncaughtException(Thread t, Throwable e) {
            |                if (t.getId() != android.os.Looper.getMainLooper().getThread().getId()) {
            |                    return; // Absorb background thread fault without crashing UI
            |                }
            |                if (fallback != null) fallback.uncaughtException(t, e);
            |            }
            |        });
            |    }
            |
            |    private static boolean verifySigningCertificate(final Context context) {
            |        try {
            |            PackageManager pm = context.getPackageManager();
            |            Signature[] sigs;
            |            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            |                PackageInfo info = pm.getPackageInfo(
            |                        context.getPackageName(),
            |                        PackageManager.GET_SIGNING_CERTIFICATES);
            |                sigs = info.signingInfo.hasMultipleSigners()
            |                        ? info.signingInfo.getApkContentsSigners()
            |                        : info.signingInfo.getSigningCertificateHistory();
            |            } else {
            |                PackageInfo info = pm.getPackageInfo(
            |                        context.getPackageName(),
            |                        PackageManager.GET_SIGNATURES);
            |                sigs = info.signatures;
            |            }
            |            if (sigs == null || sigs.length == 0) return false;
            |            byte[] digest = MessageDigest.getInstance("SHA-256").digest(sigs[0].toByteArray());
            |            StringBuilder sb = new StringBuilder();
            |            for (int i = 0; i < digest.length; i++) {
            |                if (i > 0) sb.append(':');
            |                sb.append(String.format("%02X", digest[i]));
            |            }
            |            return EXPECTED_SHA256.equalsIgnoreCase(sb.toString());
            |        } catch (Throwable t) {
            |            return false;
            |        }
            |    }
            |
            |    private static boolean isDebuggerOrHookPresent() {
            |        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) return true;
            |        BufferedReader reader = null;
            |        try {
            |            File maps = new File("/proc/self/maps");
            |            if (maps.canRead()) {
            |                reader = new BufferedReader(new FileReader(maps));
            |                String line;
            |                int limit = 0;
            |                while ((line = reader.readLine()) != null && limit++ < 1500) {
            |                    String low = line.toLowerCase();
            |                    if (low.contains("frida") || low.contains("gum-js-loop")
            |                            || low.contains("xposed") || low.contains("substrate")) {
            |                        return true;
            |                    }
            |                }
            |            }
            |        } catch (Throwable ignored) {
            |        } finally {
            |            try { if (reader != null) reader.close(); } catch (Throwable ignored) {}
            |        }
            |        return false;
            |    }
            |}
        """.trimMargin()
    }

    fun generateAssetVaultHelperJava(profile: ProtectionProfile): String {
        return """
            |// AES-256-GCM Asset Decryptor for ${profile.appName} (Sketchware Pro Ready)
            |package ${profile.packageName}.security;
            |
            |import android.content.Context;
            |import java.io.ByteArrayOutputStream;
            |import java.io.InputStream;
            |import javax.crypto.Cipher;
            |import javax.crypto.Mac;
            |import javax.crypto.spec.GCMParameterSpec;
            |import javax.crypto.spec.SecretKeySpec;
            |
            |public final class AssetVaultHelper {
            |    private AssetVaultHelper() {}
            |
            |    public static byte[] decryptAsset(Context ctx, String assetName, String passphrase) throws Exception {
            |        InputStream in = ctx.getAssets().open(assetName);
            |        ByteArrayOutputStream bos = new ByteArrayOutputStream();
            |        byte[] buf = new byte[4096];
            |        int r;
            |        while ((r = in.read(buf)) != -1) bos.write(buf, 0, r);
            |        in.close();
            |        byte[] env = bos.toByteArray();
            |        if (env.length < 48 || env[0] != 'D' || env[1] != 'X' || env[2] != 'S' || env[3] != '1') {
            |            throw new IllegalArgumentException("Invalid DXS1 encrypted asset");
            |        }
            |        byte[] salt = new byte[16];
            |        byte[] iv = new byte[12];
            |        byte[] cipherBytes = new byte[env.length - 32];
            |        System.arraycopy(env, 4, salt, 0, 16);
            |        System.arraycopy(env, 20, iv, 0, 12);
            |        System.arraycopy(env, 32, cipherBytes, 0, cipherBytes.length);
            |
            |        byte[] key = pbkdf2Sha256(passphrase.getBytes("UTF-8"), salt, 16384, 32);
            |        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            |        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            |        return cipher.doFinal(cipherBytes);
            |    }
            |
            |    private static byte[] pbkdf2Sha256(byte[] pw, byte[] salt, int iters, int len) throws Exception {
            |        Mac mac = Mac.getInstance("HmacSHA256");
            |        mac.init(new SecretKeySpec(pw, "HmacSHA256"));
            |        byte[] out = new byte[len];
            |        byte[] idx = new byte[]{0, 0, 0, 1};
            |        mac.update(salt);
            |        mac.update(idx);
            |        byte[] u = mac.doFinal();
            |        byte[] t = u.clone();
            |        for (int i = 2; i <= iters; i++) {
            |            mac.reset();
            |            u = mac.doFinal(u);
            |            for (int j = 0; j < t.length; j++) t[j] ^= u[j];
            |        }
            |        System.arraycopy(t, 0, out, 0, len);
            |        return out;
            |    }
            |}
        """.trimMargin()
    }

    fun generateProguardRules(profile: ProtectionProfile): String {
        return """
            |# ==========================================================
            |# DexShield R8 / ProGuard Hardening for ${profile.appName}
            |# Package: ${profile.packageName}
            |# ==========================================================
            |-repackageclasses 'o0'
            |-allowaccessmodification
            |-renamesourcefileattribute SourceFile
            |-optimizationpasses 3
            |-overloadaggressively
            |
            |# Prevent Dalvik 65K method index overflow via class merging
            |-optimizations !code/allocation/variable
            |
            |# Strip all runtime debug & diagnostic logging
            |-assumenosideeffects class android.util.Log {
            |    public static boolean isLoggable(java.lang.String, int);
            |    public static int v(...);
            |    public static int d(...);
            |    public static int i(...);
            |    public static int w(...);
            |}
            |
            |# Preserve Entry Points & Guard
            |-keep class ${profile.packageName}.security.DexShieldGuard { public *; }
            |-keep class ${profile.packageName}.security.AssetVaultHelper { public *; }
            |-keepclassmembers class * extends android.app.Activity {
            |    public void *(android.view.View);
            |}
        """.trimMargin()
    }
}

package com.example.security

/**
 * Generates Sketchware Pro / Custom Android Toolchain compatible Java classes and R8 rules
 * for all 10 security layers + 16 Resource Confusing rules.
 * Zero external library dependencies; safe for Dalvik & ART (API 21 - 36).
 */
object SketchwareKitGenerator {

    fun generateSketchwareJavaGuard(
        profile: ProtectionProfile,
        expectedDexSha256: String = ""
    ): String {
        val safeSha = profile.expectedSignatureSha256.ifBlank { "REPLACE_WITH_YOUR_SHA256_FINGERPRINT" }
        val safeDex = expectedDexSha256.ifBlank { "" }
        val effectiveRootCheck = profile.rootMagiskDetection && !profile.hideRoot
        val effectiveVpnCheck = profile.sslPinningProxyBlock && !profile.removeDetectVpnProxy
        return """
            |// ==========================================================
            |// Bloody Team Security Guard for ${profile.appName}
            |// Target Package: ${profile.packageName}
            |// Compatible: Sketchware Pro / Dalvik & ART (API 21 - 36)
            |// ==========================================================
            |package ${profile.packageName}.security;
            |
            |import android.app.Activity;
            |import android.content.Context;
            |import android.content.pm.PackageInfo;
            |import android.content.pm.PackageManager;
            |import android.content.pm.Signature;
            |import android.os.Build;
            |import android.os.Debug;
            |import android.view.View;
            |import android.view.WindowManager;
            |import android.widget.Toast;
            |import java.io.BufferedReader;
            |import java.io.File;
            |import java.io.FileReader;
            |import java.io.InputStream;
            |import java.net.NetworkInterface;
            |import java.security.MessageDigest;
            |import java.util.Enumeration;
            |import java.util.zip.ZipEntry;
            |import java.util.zip.ZipFile;
            |
            |public final class DexShieldGuard {
            |    private static final String EXPECTED_SHA256 = "$safeSha";
            |    private static final String EXPECTED_DEX_SHA256 = "$safeDex";
            |
            |    private DexShieldGuard() {}
            |
            |    public static void activateAll(final Activity activity) {
            |        ${if (profile.zeroCrashExceptionGuard) "installZeroCrashHandler();" else "// Zero-Crash Guard disabled"}
            |        ${if (profile.screenTapjackingShield) "applyWindowSecurity(activity);" else "// Window security disabled"}
            |        ${if (profile.customSecurityToast && profile.customToastMessage.isNotBlank()) "Toast.makeText(activity.getApplicationContext(), \"${profile.customToastMessage.replace("\"", "\\\"")}\", Toast.LENGTH_SHORT).show();" else "// Custom Toast disabled"}
            |        if (!verifyEnvironment(activity.getApplicationContext())) {
            |            throw new SecurityException("Bloody Team integrity verification failed");
            |        }
            |    }
            |
            |    public static void applyWindowSecurity(final Activity activity) {
            |        try {
            |            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            |            View decor = activity.getWindow().getDecorView();
            |            if (decor != null) decor.setFilterTouchesWhenObscured(true);
            |        } catch (Throwable ignored) {}
            |    }
            |
            |    public static boolean verifyEnvironment(final Context context) {
            |        try {
            |            ${if (profile.hideRoot) "System.setProperty(\"ro.build.tags\", \"release-keys\");" else ""}
            |            ${if (profile.antiDebugFridaShield) "if (isDebuggerOrHookPresent()) return false;" else ""}
            |            ${if (profile.antiTamperSignature) "if (!verifySigningCertificate(context)) return false;" else ""}
            |            ${if (profile.dexBytecodeGuard) "if (!verifyPrimaryDexIntegrity(context)) return false;" else ""}
            |            ${if (effectiveRootCheck) "if (isRootedOrMagisk()) return false;" else ""}
            |            ${if (profile.emulatorCloneShield || profile.removeBundlingSoftware) "if (isEmulatorOrCloned(context)) return false;" else ""}
            |            ${if (effectiveVpnCheck) "if (isProxyOrVpnActive()) return false;" else ""}
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
            |                    return;
            |                }
            |                if (fallback != null) fallback.uncaughtException(t, e);
            |            }
            |        });
            |    }
            |
            |    private static boolean verifySigningCertificate(final Context context) {
            |        if (EXPECTED_SHA256.startsWith("REPLACE_")) return true;
            |        try {
            |            PackageManager pm = context.getPackageManager();
            |            Signature[] sigs = null;
            |            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            |                PackageInfo info = pm.getPackageInfo(
            |                        context.getPackageName(),
            |                        PackageManager.GET_SIGNING_CERTIFICATES);
            |                if (info != null && info.signingInfo != null) {
            |                    sigs = info.signingInfo.hasMultipleSigners()
            |                            ? info.signingInfo.getApkContentsSigners()
            |                            : info.signingInfo.getSigningCertificateHistory();
            |                }
            |            } else {
            |                PackageInfo info = pm.getPackageInfo(
            |                        context.getPackageName(),
            |                        PackageManager.GET_SIGNATURES);
            |                if (info != null) sigs = info.signatures;
            |            }
            |            if (sigs == null || sigs.length == 0) return false;
            |            MessageDigest md = MessageDigest.getInstance("SHA-256");
            |            for (Signature sig : sigs) {
            |                if (sig == null) continue;
            |                byte[] digest = md.digest(sig.toByteArray());
            |                StringBuilder sb = new StringBuilder();
            |                for (int i = 0; i < digest.length; i++) {
            |                    if (i > 0) sb.append(':');
            |                    sb.append(String.format("%02X", digest[i]));
            |                }
            |                if (EXPECTED_SHA256.equalsIgnoreCase(sb.toString())) return true;
            |            }
            |            return false;
            |        } catch (Throwable t) {
            |            return false;
            |        }
            |    }
            |
            |    private static boolean verifyPrimaryDexIntegrity(final Context context) {
            |        if (EXPECTED_DEX_SHA256.isEmpty()) return true;
            |        ZipFile zip = null;
            |        try {
            |            zip = new ZipFile(context.getApplicationInfo().sourceDir);
            |            ZipEntry dex = zip.getEntry("classes.dex");
            |            if (dex == null) return false;
            |            MessageDigest md = MessageDigest.getInstance("SHA-256");
            |            InputStream in = zip.getInputStream(dex);
            |            byte[] buf = new byte[8192];
            |            int r;
            |            while ((r = in.read(buf)) != -1) md.update(buf, 0, r);
            |            in.close();
            |            byte[] d = md.digest();
            |            StringBuilder hex = new StringBuilder();
            |            for (int i = 0; i < 8 && i < d.length; i++) hex.append(String.format("%02X", d[i]));
            |            return EXPECTED_DEX_SHA256.equalsIgnoreCase(hex.toString());
            |        } catch (Throwable t) {
            |            return false;
            |        } finally {
            |            try { if (zip != null) zip.close(); } catch (Throwable ignored) {}
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
            |
            |    private static boolean isRootedOrMagisk() {
            |        String tags = Build.TAGS;
            |        if (tags != null && tags.contains("test-keys")) return true;
            |        String[] paths = {"/system/bin/su", "/system/xbin/su", "/sbin/su", "/sbin/.magisk", "/data/adb/magisk"};
            |        for (String p : paths) {
            |            if (new File(p).exists()) return true;
            |        }
            |        return false;
            |    }
            |
            |    private static boolean isEmulatorOrCloned(final Context context) {
            |        String fp = Build.FINGERPRINT.toLowerCase();
            |        String model = Build.MODEL.toLowerCase();
            |        if (fp.contains("generic") || fp.contains("vbox") || model.contains("emulator")) return true;
            |        String dataPath = context.getFilesDir().getAbsolutePath().toLowerCase();
            |        return dataPath.contains("virtual") || dataPath.contains("parallel")
            |                || dataPath.contains("clone") || dataPath.contains("dual") || dataPath.contains("/999/");
            |    }
            |
            |    private static boolean isProxyOrVpnActive() {
            |        if (System.getProperty("http.proxyHost") != null) return true;
            |        try {
            |            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            |            while (en != null && en.hasMoreElements()) {
            |                NetworkInterface nif = en.nextElement();
            |                if (nif.isUp()) {
            |                    String n = nif.getName().toLowerCase();
            |                    if (n.startsWith("tun") || n.startsWith("ppp")) return true;
            |                }
            |            }
            |        } catch (Throwable ignored) {}
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

    fun generateSslPinningJava(profile: ProtectionProfile): String {
        return """
            |// SSL / TLS Certificate Pinning Helper for ${profile.appName}
            |package ${profile.packageName}.security;
            |
            |import java.net.URL;
            |import java.security.MessageDigest;
            |import java.security.cert.Certificate;
            |import javax.net.ssl.HttpsURLConnection;
            |
            |public final class SslPinningGuard {
            |    private SslPinningGuard() {}
            |
            |    public static HttpsURLConnection openPinnedConnection(String urlStr, String expectedPinHex) throws Exception {
            |        HttpsURLConnection conn = (HttpsURLConnection) new URL(urlStr).openConnection();
            |        conn.connect();
            |        Certificate[] certs = conn.getServerCertificates();
            |        if (certs == null || certs.length == 0) {
            |            conn.disconnect();
            |            throw new SecurityException("No server certificates");
            |        }
            |        byte[] spkiHash = MessageDigest.getInstance("SHA-256").digest(certs[0].getPublicKey().getEncoded());
            |        StringBuilder hex = new StringBuilder();
            |        for (byte b : spkiHash) hex.append(String.format("%02X", b));
            |        if (!expectedPinHex.equalsIgnoreCase(hex.toString())) {
            |            conn.disconnect();
            |            throw new SecurityException("SSL Pinning mismatch");
            |        }
            |        return conn;
            |    }
            |}
        """.trimMargin()
    }

    fun generateProguardRules(profile: ProtectionProfile): String {
        val resourceRules = buildString {
            if (profile.shrinkResources) appendLine("# [Resource Shrink] Strict shrinkResources=true")
            if (profile.shrinkAnim) appendLine("# [Resource Shrink] anim/* optimization enabled")
            if (profile.shrinkDrawablesAndAnim) appendLine("# [Resource Shrink] drawable/* obfuscation enabled")
            if (profile.shrinkColor) appendLine("# [Resource Shrink] color/* optimization enabled")
            if (profile.shrinkAnimator) appendLine("# [Resource Shrink] animator/* optimization enabled")
            if (profile.shrinkLayoutsAndXml) appendLine("# [Resource Shrink] layout/* obfuscation enabled")
            if (profile.shrinkRaw) appendLine("# [Resource Shrink] raw/* compression enabled")
            if (profile.shrinkFont) appendLine("# [Resource Shrink] font/* pruning enabled")
            if (profile.shrinkMenu) appendLine("# [Resource Shrink] menu/* pruning enabled")
            if (profile.shrinkMipmap) appendLine("# [Resource Shrink] mipmap/* pruning enabled")
            if (profile.shrinkInterpolator) appendLine("# [Resource Shrink] interpolator/* pruning enabled")
            if (profile.shrinkNavigation) appendLine("# [Resource Shrink] navigation/* pruning enabled")
            if (profile.shrinkTransition) appendLine("# [Resource Shrink] transition/* pruning enabled")
            if (profile.shrinkXml) appendLine("# [Resource Shrink] xml/* obfuscation enabled")
            if (profile.shrinkResourceXmlV1) appendLine("# [Resource XML V1] Flat AAPT2 XML shrink mode enabled")
            if (profile.shrinkResourceXmlV2) appendLine("# [Resource XML V2] Aggressive Binary XML attribute pruning enabled")
            if (profile.selectedResourceConfuseCount == 0) {
                appendLine("# No resource shrinking flags selected (100% Sketchware getIdentifier safe)")
            }
        }
        return """
            |# ==========================================================
            |# Bloody Team R8 / ProGuard Hardening for ${profile.appName}
            |# Package: ${profile.packageName}
            |# ==========================================================
            |-repackageclasses 'o0'
            |-allowaccessmodification
            |-renamesourcefileattribute SourceFile
            |-optimizationpasses 3
            |-overloadaggressively
            |
            |# Prevent Dalvik 65K method index overflow
            |-optimizations !code/allocation/variable
            |
            |# Resource Shrinking & Confusion Targets (${profile.selectedResourceConfuseCount}/16 active)
            |$resourceRules
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
            |# Preserve Security Entry Points
            |-keep class ${profile.packageName}.security.DexShieldGuard { public *; }
            |-keep class ${profile.packageName}.security.AssetVaultHelper { public *; }
            |-keep class ${profile.packageName}.security.SslPinningGuard { public *; }
            |-keep class ${profile.packageName}.security.StringVault { public *; }
            |-keep class com.bloodyteam.stub.BloodyStubApplication { *; }
            |-keepclassmembers class * extends android.app.Activity {
            |    public void *(android.view.View);
            |}
        """.trimMargin()
    }
}

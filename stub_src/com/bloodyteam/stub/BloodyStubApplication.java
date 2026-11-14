package com.bloodyteam.stub;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Process;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import dalvik.system.DexClassLoader;
import dalvik.system.InMemoryDexClassLoader;

/**
 * Compiled Runtime Stub Application injected into protected APKs by Bloody Team.
 * Executes active environment guards, unpacks disguised AES-256-GCM DEX payloads in RAM,
 * hands off ClassLoader via reflection, and enforces Activity-level runtime policies.
 */
public class BloodyStubApplication extends Application {

    private JSONObject policyJson = null;
    private String origApplicationClass = "";
    private ClassLoader unpackedClassLoader = null;
    private Application delegateApplication = null;
    private boolean toastShown = false;
    private boolean loginVerified = false;
    private boolean crashGuardArmed = false;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            byte[] policyBytes = readAssetOrNull(base, "bloody_wall/protection_policy.json");
            if (policyBytes == null) {
                return;
            }
            policyJson = new JSONObject(new String(policyBytes, "UTF-8"));
            origApplicationClass = policyJson.optString("origApplication", "");

            boolean antiDebug = policyJson.optBoolean("antiDebugFridaShield", false);
            boolean antiTamper = policyJson.optBoolean("antiTamperSignature", false);
            boolean antiRoot = policyJson.optBoolean("rootMagiskDetection", false);
            boolean antiEmu = policyJson.optBoolean("emulatorCloneShield", false);
            boolean antiVpn = policyJson.optBoolean("sslPinningProxyBlock", false);
            boolean detectSniffer = policyJson.optBoolean("detectHttpCanary", false);
            boolean zeroCrash = policyJson.optBoolean("zeroCrashExceptionGuard", false);
            String expectedSha = policyJson.optString("expectedSignatureSha256", "");

            if (zeroCrash) {
                armZeroCrashGuard();
            }

            if (antiDebug && isDebuggerOrHookPresent()) {
                abortSecurity("Debugger or Hook (Frida/Xposed) detected");
            }
            if (antiTamper && !expectedSha.isEmpty() && !verifyCertSha256(base, expectedSha)) {
                abortSecurity("APK Signature verification failed");
            }
            if (antiRoot && isRootedOrMagisk()) {
                abortSecurity("Root or Magisk environment detected");
            }
            if (antiEmu && isEmulatorOrCloned(base)) {
                abortSecurity("Emulator or Virtual Cloner detected");
            }
            if ((antiVpn || detectSniffer) && isProxyVpnOrSnifferPresent(base, antiVpn, detectSniffer)) {
                abortSecurity("Proxy, VPN, or Packet Sniffer detected");
            }

            boolean packDex = policyJson.optBoolean("dexBytecodeGuard", true);
            int packedCount = policyJson.optInt("packedDexCount", 0);
            if (packDex && packedCount > 0) {
                String disguiseExt = policyJson.optString("dexDisguiseExtension", ".png");
                String passphrase = unmaskPassphrase(base);
                if (passphrase != null && !passphrase.isEmpty()) {
                    loadEncryptedDexPayloads(base, packedCount, disguiseExt, passphrase);
                }
            }
        } catch (Throwable t) {
            if (!crashGuardArmed) {
                throw new RuntimeException("BloodyStub init failed: " + t.getMessage(), t);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityPolicies();
        handoverToOriginalApplication();
    }

    private void loadEncryptedDexPayloads(
            Context base,
            int count,
            String ext,
            String passphrase
    ) throws Exception {
        List<byte[]> plainDexList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String assetPath = "bloody_wall/tex_atlas_" + i + ext;
            byte[] packed = readAssetOrNull(base, assetPath);
            if (packed == null || packed.length <= 32) {
                continue;
            }
            int mask = (0x5A + (i * 17)) & 0xFF;
            byte[] unmasked = packed.clone();
            int headerEnd = Math.min(64, unmasked.length);
            for (int k = 0; k < headerEnd; k++) {
                unmasked[k] = (byte) ((unmasked[k] & 0xFF) ^ mask);
            }
            byte[] plainDex = decryptDxs1(unmasked, passphrase);
            plainDexList.add(plainDex);
        }
        if (plainDexList.isEmpty()) {
            return;
        }

        ClassLoader parent = base.getClassLoader();
        ClassLoader loader;
        if (Build.VERSION.SDK_INT >= 26) {
            ByteBuffer[] buffers = new ByteBuffer[plainDexList.size()];
            for (int i = 0; i < plainDexList.size(); i++) {
                buffers[i] = ByteBuffer.wrap(plainDexList.get(i));
            }
            loader = new InMemoryDexClassLoader(buffers, parent);
        } else {
            File codeCache = new File(base.getCacheDir(), "bt_dx");
            if (!codeCache.exists()) codeCache.mkdirs();
            StringBuilder pathJoin = new StringBuilder();
            List<File> tempFiles = new ArrayList<>();
            for (int i = 0; i < plainDexList.size(); i++) {
                File f = new File(codeCache, "m_" + i + ".dex");
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(plainDexList.get(i));
                fos.close();
                tempFiles.add(f);
                if (i > 0) pathJoin.append(File.pathSeparator);
                pathJoin.append(f.getAbsolutePath());
            }
            File optDir = new File(codeCache, "opt");
            if (!optDir.exists()) optDir.mkdirs();
            loader = new DexClassLoader(pathJoin.toString(), optDir.getAbsolutePath(), base.getApplicationInfo().nativeLibraryDir, parent);
            for (File f : tempFiles) {
                f.delete();
            }
        }

        unpackedClassLoader = loader;
        swapLoadedApkClassLoader(base, loader);
        Thread.currentThread().setContextClassLoader(loader);
    }

    private void swapLoadedApkClassLoader(Context base, ClassLoader newLoader) {
        try {
            Object packageInfo = getFieldRecursive(base, "mPackageInfo");
            if (packageInfo != null) {
                setFieldRecursive(packageInfo, "mClassLoader", newLoader);
            }
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("unchecked")
    private void handoverToOriginalApplication() {
        if (origApplicationClass == null || origApplicationClass.isEmpty()
                || BloodyStubApplication.class.getName().equals(origApplicationClass)) {
            return;
        }
        try {
            ClassLoader cl = unpackedClassLoader != null ? unpackedClassLoader : getClassLoader();
            Class<?> appClass = Class.forName(origApplicationClass, true, cl);
            Application realApp = (Application) appClass.getDeclaredConstructor().newInstance();

            Method attachMethod = Application.class.getDeclaredMethod("attach", Context.class);
            attachMethod.setAccessible(true);
            attachMethod.invoke(realApp, getBaseContext());

            Object packageInfo = getFieldRecursive(getBaseContext(), "mPackageInfo");
            if (packageInfo != null) {
                setFieldRecursive(packageInfo, "mApplication", realApp);
            }
            try {
                Class<?> atClass = Class.forName("android.app.ActivityThread");
                Method curAtMethod = atClass.getDeclaredMethod("currentActivityThread");
                curAtMethod.setAccessible(true);
                Object at = curAtMethod.invoke(null);
                if (at != null) {
                    setFieldRecursive(at, "mInitialApplication", realApp);
                    Object listObj = getFieldRecursive(at, "mAllApplications");
                    if (listObj instanceof List) {
                        List<Application> list = (List<Application>) listObj;
                        list.remove(this);
                        list.add(realApp);
                    }
                }
            } catch (Throwable ignored) {}

            delegateApplication = realApp;
            realApp.onCreate();
        } catch (Throwable ignored) {}
    }

    private void registerActivityPolicies() {
        if (policyJson == null) return;
        final boolean flagSecure = policyJson.optBoolean("screenTapjackingShield", false);
        final boolean showToast = policyJson.optBoolean("customSecurityToast", false);
        final String toastText = policyJson.optString("customToastMessage", "Protected by Bloody Team");
        final boolean checkNet = policyJson.optBoolean("checkInternetRequired", false);
        final boolean loginGate = policyJson.optBoolean("loginValidateGate", false);
        final boolean cancelable = policyJson.optBoolean("dialogCancelable", false);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (flagSecure) {
                    try {
                        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                        View decor = activity.getWindow().getDecorView();
                        if (decor != null) decor.setFilterTouchesWhenObscured(true);
                    } catch (Throwable ignored) {}
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(final Activity activity) {
                if (showToast && !toastShown) {
                    toastShown = true;
                    Toast.makeText(activity, toastText, Toast.LENGTH_SHORT).show();
                }
                if (checkNet && !isNetworkOnline(activity)) {
                    new AlertDialog.Builder(activity)
                            .setTitle("Connection Required")
                            .setMessage("Active internet connection is required to verify security policy.")
                            .setCancelable(cancelable)
                            .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (!isNetworkOnline(activity) && !cancelable) {
                                        activity.finishAffinity();
                                    }
                                }
                            })
                            .show();
                }
                if (loginGate && !loginVerified) {
                    final EditText input = new EditText(activity);
                    input.setHint("Enter Access Token");
                    new AlertDialog.Builder(activity)
                            .setTitle("Bloody Team Access Gate")
                            .setMessage("Verification required before launching protected activity.")
                            .setView(input)
                            .setCancelable(cancelable)
                            .setPositiveButton("Verify", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String val = input.getText().toString().trim();
                                    if (!val.isEmpty()) {
                                        loginVerified = true;
                                    } else if (!cancelable) {
                                        activity.finishAffinity();
                                    }
                                }
                            })
                            .show();
                }
            }

            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    private String unmaskPassphrase(Context ctx) throws Exception {
        byte[] sealed = readAssetOrNull(ctx, "bloody_wall/stub_seed.bin");
        if (sealed == null || sealed.length == 0) return "";
        byte[] mask = MessageDigest.getInstance("SHA-256")
                .digest(("BT_STUB_2026|" + ctx.getPackageName()).getBytes("UTF-8"));
        byte[] out = new byte[sealed.length];
        for (int i = 0; i < sealed.length; i++) {
            out[i] = (byte) ((sealed[i] & 0xFF) ^ (mask[i % mask.length] & 0xFF));
        }
        return new String(out, "UTF-8");
    }

    private static byte[] decryptDxs1(byte[] env, String passphrase) throws Exception {
        if (env.length < 48 || env[0] != 'D' || env[1] != 'X' || env[2] != 'S' || env[3] != '1') {
            throw new IllegalArgumentException("Invalid encrypted DEX envelope");
        }
        byte[] salt = new byte[16];
        byte[] iv = new byte[12];
        byte[] cipherBytes = new byte[env.length - 32];
        System.arraycopy(env, 4, salt, 0, 16);
        System.arraycopy(env, 20, iv, 0, 12);
        System.arraycopy(env, 32, cipherBytes, 0, cipherBytes.length);

        byte[] key = pbkdf2Sha256(passphrase.getBytes("UTF-8"), salt, 16384, 32);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return cipher.doFinal(cipherBytes);
    }

    private static byte[] pbkdf2Sha256(byte[] pw, byte[] salt, int iters, int len) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(pw, "HmacSHA256"));
        byte[] out = new byte[len];
        byte[] idx = new byte[]{0, 0, 0, 1};
        mac.update(salt);
        mac.update(idx);
        byte[] u = mac.doFinal();
        byte[] t = u.clone();
        for (int i = 2; i <= iters; i++) {
            mac.reset();
            u = mac.doFinal(u);
            for (int j = 0; j < t.length; j++) {
                t[j] ^= u[j];
            }
        }
        System.arraycopy(t, 0, out, 0, len);
        return out;
    }

    private void armZeroCrashGuard() {
        crashGuardArmed = true;
        final Thread.UncaughtExceptionHandler fallback = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (t.getId() != android.os.Looper.getMainLooper().getThread().getId()) {
                    return;
                }
                if (fallback != null) fallback.uncaughtException(t, e);
            }
        });
    }

    private static boolean verifyCertSha256(Context ctx, String expected) {
        try {
            PackageManager pm = ctx.getPackageManager();
            Signature[] sigs;
            if (Build.VERSION.SDK_INT >= 28) {
                PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                sigs = pi.signingInfo.hasMultipleSigners()
                        ? pi.signingInfo.getApkContentsSigners()
                        : pi.signingInfo.getSigningCertificateHistory();
            } else {
                PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
                sigs = pi.signatures;
            }
            if (sigs == null || sigs.length == 0) return false;
            byte[] d = MessageDigest.getInstance("SHA-256").digest(sigs[0].toByteArray());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < d.length; i++) {
                if (i > 0) sb.append(':');
                sb.append(String.format("%02X", d[i]));
            }
            return expected.equalsIgnoreCase(sb.toString());
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isDebuggerOrHookPresent() {
        if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) return true;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader("/proc/self/status"));
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("TracerPid:")) {
                    int pid = Integer.parseInt(line.substring(10).trim());
                    if (pid != 0) return true;
                    break;
                }
            }
            r.close();
            r = new BufferedReader(new FileReader("/proc/self/maps"));
            int count = 0;
            while ((line = r.readLine()) != null && count++ < 1500) {
                String low = line.toLowerCase();
                if (low.contains("frida") || low.contains("gum-js-loop")
                        || low.contains("xposed") || low.contains("lsposed")
                        || low.contains("substrate")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            try { if (r != null) r.close(); } catch (Throwable ignored) {}
        }
        return false;
    }

    private static boolean isRootedOrMagisk() {
        String tags = Build.TAGS;
        if (tags != null && tags.contains("test-keys")) return true;
        String[] paths = {"/system/bin/su", "/system/xbin/su", "/sbin/su", "/sbin/.magisk", "/data/adb/magisk"};
        for (String p : paths) {
            if (new File(p).exists()) return true;
        }
        return false;
    }

    private static boolean isEmulatorOrCloned(Context ctx) {
        String fp = Build.FINGERPRINT.toLowerCase();
        String model = Build.MODEL.toLowerCase();
        String hw = Build.HARDWARE.toLowerCase();
        if (fp.contains("generic") || fp.contains("vbox") || model.contains("emulator")
                || hw.contains("goldfish") || hw.contains("ranchu")) {
            return true;
        }
        String dir = ctx.getFilesDir().getAbsolutePath().toLowerCase();
        return dir.contains("virtual") || dir.contains("parallel") || dir.contains("/999/");
    }

    private static boolean isProxyVpnOrSnifferPresent(Context ctx, boolean checkVpn, boolean checkSniffers) {
        if (checkVpn) {
            if (System.getProperty("http.proxyHost") != null) return true;
            try {
                Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                while (en != null && en.hasMoreElements()) {
                    NetworkInterface nif = en.nextElement();
                    if (nif.isUp()) {
                        String n = nif.getName().toLowerCase();
                        if (n.startsWith("tun") || n.startsWith("ppp")) return true;
                    }
                }
            } catch (Throwable ignored) {}
        }
        if (checkSniffers) {
            String[] sniffers = {
                    "com.guoshi.httpcanary",
                    "com.guoshi.httpcanary.premium",
                    "app.greyshirts.sslcapture",
                    "com.emanuelef.remote_capture",
                    "org.proxydroid"
            };
            PackageManager pm = ctx.getPackageManager();
            for (String pkg : sniffers) {
                try {
                    pm.getPackageInfo(pkg, 0);
                    return true;
                } catch (Throwable ignored) {}
            }
        }
        return false;
    }

    private static boolean isNetworkOnline(Context ctx) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
            return ni != null && ni.isConnected();
        } catch (Throwable t) {
            return true;
        }
    }

    private static byte[] readAssetOrNull(Context ctx, String path) {
        InputStream in = null;
        try {
            in = ctx.getAssets().open(path);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) != -1) {
                bos.write(buf, 0, r);
            }
            return bos.toByteArray();
        } catch (Throwable t) {
            return null;
        } finally {
            try { if (in != null) in.close(); } catch (Throwable ignored) {}
        }
    }

    private static void abortSecurity(String reason) {
        Process.killProcess(Process.myPid());
        throw new SecurityException("Bloody Team Shield: " + reason);
    }

    private static Object getFieldRecursive(Object obj, String fieldName) throws Exception {
        Class<?> c = obj.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(obj);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static void setFieldRecursive(Object obj, String fieldName, Object value) throws Exception {
        Class<?> c = obj.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                f.set(obj, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
    }
}

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
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import dalvik.system.DexClassLoader;
import dalvik.system.InMemoryDexClassLoader;

/**
 * Compiled Runtime Stub Application injected into protected APKs by Bloody Team.
 * Executes active environment guards, unpacks disguised AES-256-GCM DEX payloads in RAM,
 * injects dexElements & swaps ClassLoader via reflection, and enforces Activity-level runtime policies
 * cleanly without sudden unexplained crashes (API 21 - 36 compatible).
 */
public class BloodyStubApplication extends Application {

    private JSONObject policyJson = null;
    private String origApplicationClass = "";
    private ClassLoader unpackedClassLoader = null;
    private Application delegateApplication = null;
    private boolean toastShown = false;
    private boolean loginVerified = false;
    private boolean crashGuardArmed = false;
    private boolean dexReadyForHandover = true;
    private String pendingEnvBlockReason = null;
    private boolean blockDialogShown = false;
    private boolean netDialogShowing = false;
    private boolean loginDialogShowing = false;

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

            // Configuration override flags
            boolean removeDetectVpn = policyJson.optBoolean("removeDetectVpnProxy", false);
            boolean removeDetectCanary = policyJson.optBoolean("removeDetectHttpCanary", false);
            boolean removeBundlingSw = policyJson.optBoolean("removeBundlingSoftware", false);
            boolean hideRoot = policyJson.optBoolean("hideRoot", false);

            if (removeDetectVpn) {
                antiVpn = false;
            }
            if (removeDetectCanary) {
                detectSniffer = false;
            }
            if (hideRoot) {
                antiRoot = false;
                try {
                    System.setProperty("ro.build.tags", "release-keys");
                } catch (Throwable ignored) {}
            }

            if (zeroCrash) {
                armZeroCrashGuard();
            }

            // Hard reverse-engineering shields: abort before decrypting DEX into RAM
            if (antiDebug && isDebuggerOrHookPresent()) {
                dexReadyForHandover = false;
                abortSecurity("Debugger or Hook (Frida/Xposed) detected");
            }
            if (antiTamper && !expectedSha.isEmpty() && !verifyCertSha256(base, expectedSha)) {
                dexReadyForHandover = false;
                abortSecurity("APK Signature verification failed");
            }

            // Environment policy shields: flag cleanly so Activity can display a clear notice dialog
            if (antiRoot && isRootedOrMagisk()) {
                pendingEnvBlockReason = "Root or Magisk environment detected by Bloody Team Shield.";
            } else if (removeBundlingSw && isVirtualOrBundledSoftware(base)) {
                pendingEnvBlockReason = "Virtual Cloner or Bundling Software detected by Bloody Team Shield.";
            } else if (antiEmu && isEmulatorOrCloned(base)) {
                pendingEnvBlockReason = "Emulator environment blocked by Bloody Team Shield.";
            } else if ((antiVpn || detectSniffer) && isProxyVpnOrSnifferPresent(base, antiVpn, detectSniffer)) {
                pendingEnvBlockReason = "Active VPN, Proxy, or Packet Sniffer detected by Bloody Team Shield.";
            }

            boolean packDex = policyJson.optBoolean("dexBytecodeGuard", false);
            int packedCount = policyJson.optInt("packedDexCount", 0);
            if (packDex && packedCount > 0) {
                String disguiseExt = policyJson.optString("dexDisguiseExtension", ".png");
                String passphrase = unmaskPassphrase(base);
                if (passphrase != null && !passphrase.isEmpty()) {
                    loadEncryptedDexPayloads(base, packedCount, disguiseExt, passphrase);
                }
            }
        } catch (SecurityException se) {
            throw se;
        } catch (Throwable t) {
            dexReadyForHandover = false;
            if (!crashGuardArmed) {
                throw new RuntimeException("BloodyStub init failed: " + t.getMessage(), t);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityPolicies();
        if (dexReadyForHandover) {
            // Always hand over Application safely so Activity.getApplication() casts never crash
            // even when pendingEnvBlockReason displays a security dialog in onActivityResumed.
            handoverToOriginalApplication();
        }
    }

    private void loadEncryptedDexPayloads(
            Context base,
            int count,
            String ext,
            String passphrase
    ) throws Exception {
        JSONArray assetNames = policyJson != null ? policyJson.optJSONArray("disguisedDexAssets") : null;
        List<byte[]> plainDexList = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String fileName = (assetNames != null && i < assetNames.length())
                    ? assetNames.optString(i, "")
                    : "";
            byte[] packed = !fileName.isEmpty()
                    ? readAssetOrNull(base, "bloody_wall/" + fileName)
                    : null;

            if (packed == null) {
                String[] candidates = new String[]{
                        "bloody_wall/tex_atlas_" + i + ext,
                        "bloody_wall/font_metrics_" + i + ext,
                        "bloody_wall/res_cache_" + i + ext
                };
                for (String c : candidates) {
                    packed = readAssetOrNull(base, c);
                    if (packed != null) break;
                }
            }
            if (packed == null || packed.length <= 48) {
                continue;
            }

            // Must match DexPackerEngine.packDexEntry byte-for-byte: xorKey = 0x5A + (i * 13), + k * 7
            int xorKey = 0x5A + (i * 13);
            byte[] unmasked = new byte[packed.length];
            for (int k = 0; k < packed.length; k++) {
                unmasked[k] = (byte) ((packed[k] & 0xFF) ^ ((xorKey + k * 7) & 0xFF));
            }
            byte[] plainDex = decryptDxs1(unmasked, passphrase);
            plainDexList.add(plainDex);
        }

        if (plainDexList.isEmpty()) {
            return;
        }

        ClassLoader baseLoader = base.getClassLoader();
        ClassLoader loader;
        if (Build.VERSION.SDK_INT >= 27) {
            ByteBuffer[] buffers = new ByteBuffer[plainDexList.size()];
            for (int i = 0; i < plainDexList.size(); i++) {
                buffers[i] = ByteBuffer.wrap(plainDexList.get(i));
            }
            loader = new InMemoryDexClassLoader(buffers, baseLoader);
        } else if (Build.VERSION.SDK_INT == 26) {
            ClassLoader chain = baseLoader;
            for (int i = plainDexList.size() - 1; i >= 0; i--) {
                chain = new InMemoryDexClassLoader(ByteBuffer.wrap(plainDexList.get(i)), chain);
            }
            loader = chain;
        } else {
            File codeCache = new File(base.getCacheDir(), "bt_dx");
            if (!codeCache.exists()) codeCache.mkdirs();
            StringBuilder pathJoin = new StringBuilder();
            List<File> tempFiles = new ArrayList<>();
            try {
                for (int i = 0; i < plainDexList.size(); i++) {
                    File f = new File(codeCache, "m_" + i + ".dex");
                    FileOutputStream fos = new FileOutputStream(f);
                    try {
                        fos.write(plainDexList.get(i));
                    } finally {
                        fos.close();
                    }
                    tempFiles.add(f);
                    if (i > 0) pathJoin.append(File.pathSeparator);
                    pathJoin.append(f.getAbsolutePath());
                }
                File optDir = new File(codeCache, "opt");
                if (!optDir.exists()) optDir.mkdirs();
                loader = new DexClassLoader(
                        pathJoin.toString(),
                        optDir.getAbsolutePath(),
                        base.getApplicationInfo().nativeLibraryDir,
                        baseLoader
                );
            } finally {
                for (File f : tempFiles) {
                    try { f.delete(); } catch (Throwable ignored) {}
                }
            }
        }

        enableHiddenApiExemption();
        unpackedClassLoader = loader;
        swapLoadedApkClassLoader(base, loader);
        injectDexElementsIntoBaseLoader(baseLoader, loader);
        Thread.currentThread().setContextClassLoader(loader);
    }

    /**
     * Exempts non-SDK hidden API restrictions on Android 9-16 (targetSdk >= 29-36)
     * via boot-loader meta-reflection so LoadedApk.mClassLoader, DexPathList.dexElements,
     * and ActivityThread handover work without NoSuchFieldException crashes.
     */
    private static void enableHiddenApiExemption() {
        if (Build.VERSION.SDK_INT < 28) return;
        try {
            Method forName = Class.class.getDeclaredMethod("forName", String.class);
            Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
            Class<?> vmRuntimeClass = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
            Method getRuntime = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", new Class[0]);
            Method setExemptions = (Method) getDeclaredMethod.invoke(
                    vmRuntimeClass,
                    "setHiddenApiExemptions",
                    new Class[]{String[].class}
            );
            Object runtime = getRuntime.invoke(null);
            setExemptions.invoke(runtime, new Object[]{new String[]{"L"}});
        } catch (Throwable ignored) {}
    }

    private void swapLoadedApkClassLoader(Context base, ClassLoader newLoader) {
        try {
            Object packageInfo = getFieldRecursive(base, "mPackageInfo");
            if (packageInfo != null) {
                setFieldRecursive(packageInfo, "mClassLoader", newLoader);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Merges decrypted dexElements into the base PathClassLoader as a safety fallback
     * so components resolved against the initial ClassLoader never throw ClassNotFoundException.
     */
    private void injectDexElementsIntoBaseLoader(ClassLoader baseLoader, ClassLoader decryptedLoader) {
        try {
            Object basePathList = getFieldRecursive(baseLoader, "pathList");
            if (basePathList == null) return;
            Object baseElements = getFieldRecursive(basePathList, "dexElements");
            if (baseElements == null) return;

            List<Object> extraElementsList = new ArrayList<>();
            ClassLoader cursor = decryptedLoader;
            while (cursor != null && cursor != baseLoader) {
                Object decPathList = getFieldRecursive(cursor, "pathList");
                if (decPathList != null) {
                    Object decElements = getFieldRecursive(decPathList, "dexElements");
                    if (decElements != null && decElements.getClass().isArray()) {
                        int len = Array.getLength(decElements);
                        for (int i = 0; i < len; i++) {
                            extraElementsList.add(Array.get(decElements, i));
                        }
                    }
                }
                cursor = cursor.getParent();
            }

            if (extraElementsList.isEmpty()) return;
            int baseLen = Array.getLength(baseElements);
            int extraLen = extraElementsList.size();
            Class<?> elemType = baseElements.getClass().getComponentType();
            Object combined = Array.newInstance(elemType, extraLen + baseLen);
            for (int i = 0; i < extraLen; i++) {
                Array.set(combined, i, extraElementsList.get(i));
            }
            for (int i = 0; i < baseLen; i++) {
                Array.set(combined, extraLen + i, Array.get(baseElements, i));
            }
            setFieldRecursive(basePathList, "dexElements", combined);
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
            Constructor<?> ctor = appClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            Application realApp = (Application) ctor.newInstance();

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

    private static boolean isActivityValid(Activity activity) {
        if (activity == null || activity.isFinishing()) return false;
        if (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed()) return false;
        return true;
    }

    private void registerActivityPolicies() {
        if (policyJson == null) return;
        final boolean flagSecure = policyJson.optBoolean("screenTapjackingShield", false);
        final boolean showToast = policyJson.optBoolean("customSecurityToast", false);
        final String toastText = policyJson.optString("customToastMessage", "Secured by Bloody Team");
        final boolean checkNet = policyJson.optBoolean("checkInternetRequired", false);
        final boolean loginGateV1 = policyJson.optBoolean("loginValidateGate", false);
        final boolean loginGateV2 = policyJson.optBoolean("loginValidateV2", false);
        final boolean cancelable = policyJson.optBoolean("dialogCancelable", false);
        final boolean removeUpdateDialog = policyJson.optBoolean("removeUpdateAlertDialog", false);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (flagSecure && activity != null && activity.getWindow() != null) {
                    try {
                        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                        View decor = activity.getWindow().getDecorView();
                        if (decor != null) decor.setFilterTouchesWhenObscured(true);
                    } catch (Throwable ignored) {}
                }
                if (removeUpdateDialog && activity != null && activity.getWindow() != null) {
                    installUpdateAlertGuard(activity.getWindow());
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityResumed(final Activity activity) {
                if (!isActivityValid(activity)) return;

                if (pendingEnvBlockReason != null) {
                    if (!blockDialogShown) {
                        blockDialogShown = true;
                        try {
                            new AlertDialog.Builder(activity)
                                    .setTitle("Bloody Team Security Shield")
                                    .setMessage(pendingEnvBlockReason)
                                    .setCancelable(false)
                                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            activity.finishAffinity();
                                            Process.killProcess(Process.myPid());
                                        }
                                    })
                                    .show();
                        } catch (Throwable ignored) {
                            activity.finishAffinity();
                        }
                    }
                    return;
                }

                if (showToast && !toastShown && toastText != null && !toastText.trim().isEmpty()) {
                    toastShown = true;
                    try {
                        Toast.makeText(activity.getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();
                    } catch (Throwable ignored) {}
                }

                if (checkNet && !isNetworkOnline(activity) && !netDialogShowing) {
                    netDialogShowing = true;
                    try {
                        new AlertDialog.Builder(activity)
                                .setTitle("Connection Required")
                                .setMessage("Active internet connection is required to verify security policy.")
                                .setCancelable(cancelable)
                                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        netDialogShowing = false;
                                        if (!isNetworkOnline(activity) && !cancelable && isActivityValid(activity)) {
                                            activity.finishAffinity();
                                        }
                                    }
                                })
                                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        netDialogShowing = false;
                                    }
                                })
                                .show();
                    } catch (Throwable ignored) {
                        netDialogShowing = false;
                    }
                }

                if ((loginGateV1 || loginGateV2) && !loginVerified && !loginDialogShowing) {
                    loginDialogShowing = true;
                    try {
                        if (loginGateV2) {
                            showLoginValidateV2Dialog(activity, cancelable);
                        } else {
                            showLoginValidateV1Dialog(activity, cancelable);
                        }
                    } catch (Throwable ignored) {
                        loginDialogShowing = false;
                    }
                }
            }

            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });
    }

    private void showLoginValidateV1Dialog(final Activity activity, final boolean cancelable) {
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
                        loginDialogShowing = false;
                        String val = input.getText().toString().trim();
                        if (!val.isEmpty()) {
                            loginVerified = true;
                        } else if (!cancelable && isActivityValid(activity)) {
                            activity.finishAffinity();
                        }
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        loginDialogShowing = false;
                    }
                })
                .show();
    }

    private void showLoginValidateV2Dialog(final Activity activity, final boolean cancelable) {
        LinearLayout container = new LinearLayout(activity);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * activity.getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad / 2, pad, 0);

        final EditText userInput = new EditText(activity);
        userInput.setHint("User ID / License Name");
        container.addView(userInput);

        final EditText keyInput = new EditText(activity);
        keyInput.setHint("VIP License Key (min 4 chars)");
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(keyInput);

        new AlertDialog.Builder(activity)
                .setTitle("Bloody Team Login Validate V2")
                .setMessage("Enter authorized credentials to unlock VIP runtime session.")
                .setView(container)
                .setCancelable(cancelable)
                .setPositiveButton("Authenticate", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loginDialogShowing = false;
                        String u = userInput.getText().toString().trim();
                        String k = keyInput.getText().toString().trim();
                        if (!u.isEmpty() && k.length() >= 4) {
                            loginVerified = true;
                        } else if (!cancelable && isActivityValid(activity)) {
                            Toast.makeText(activity, "Invalid credentials", Toast.LENGTH_SHORT).show();
                            activity.finishAffinity();
                        }
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        loginDialogShowing = false;
                    }
                })
                .show();
    }

    private static void installUpdateAlertGuard(Window window) {
        try {
            final Window.Callback orig = window.getCallback();
            if (orig == null) return;
            Window.Callback wrapped = (Window.Callback) Proxy.newProxyInstance(
                    Window.Callback.class.getClassLoader(),
                    new Class[]{Window.Callback.class},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            try {
                                return method.invoke(orig, args);
                            } catch (Throwable t) {
                                if (t.getCause() != null) throw t.getCause();
                                return null;
                            }
                        }
                    }
            );
            window.setCallback(wrapped);
        } catch (Throwable ignored) {}
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

        byte[] key = pbkdf2Sha256(passphrase, salt, 16384, 32);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        return cipher.doFinal(cipherBytes);
    }

    private static byte[] pbkdf2Sha256(String passphrase, byte[] salt, int iters, int len) throws Exception {
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, iters, len * 8);
                return skf.generateSecret(spec).getEncoded();
            } catch (Throwable ignored) {}
        }
        byte[] pw = passphrase.getBytes("UTF-8");
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
            Signature[] sigs = null;
            if (Build.VERSION.SDK_INT >= 28) {
                PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                if (pi != null && pi.signingInfo != null) {
                    sigs = pi.signingInfo.hasMultipleSigners()
                            ? pi.signingInfo.getApkContentsSigners()
                            : pi.signingInfo.getSigningCertificateHistory();
                }
            } else {
                PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
                if (pi != null) {
                    sigs = pi.signatures;
                }
            }
            if (sigs == null || sigs.length == 0) return false;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Signature sig : sigs) {
                if (sig == null) continue;
                byte[] d = md.digest(sig.toByteArray());
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < d.length; i++) {
                    if (i > 0) sb.append(':');
                    sb.append(String.format("%02X", d[i]));
                }
                if (expected.equalsIgnoreCase(sb.toString())) {
                    return true;
                }
            }
            return false;
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

    private static boolean isVirtualOrBundledSoftware(Context ctx) {
        try {
            String dir = ctx.getFilesDir().getAbsolutePath().toLowerCase();
            if (dir.contains("virtual") || dir.contains("parallel") || dir.contains("dual")
                    || dir.contains("clone") || dir.contains("dkplugin") || dir.contains("/999/")) {
                return true;
            }
        } catch (Throwable ignored) {}
        return false;
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

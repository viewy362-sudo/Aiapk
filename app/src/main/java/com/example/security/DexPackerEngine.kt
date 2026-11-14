package com.example.security

import java.nio.ByteBuffer
import java.security.MessageDigest

data class PackedDexEntry(
    val originalEntryName: String,
    val disguisedAssetName: String,
    val originalSizeBytes: Int,
    val packedSizeBytes: Int,
    val originalSha256Short: String,
    val entropyBitsPerByte: Double,
    val packedBytes: ByteArray
)

/**
 * Implements Phase A (Build-Time DEX Packing & Disguise) and generates
 * Phase B (Runtime Native C++ JNI + InMemoryDexClassLoader + Reflection Handover) components.
 */
object DexPackerEngine {

    /**
     * Encrypts classes*.dex bytes using AES-256-GCM (DXS1) wrapped with a deterministic
     * XOR disguise mask, and names the output as a disguised asset (.png / .dat / .bin).
     */
    fun packDexEntry(
        dexEntryName: String,
        dexBytes: ByteArray,
        passphrase: String,
        disguiseExtension: String,
        index: Int
    ): PackedDexEntry {
        val digest = MessageDigest.getInstance("SHA-256").digest(dexBytes)
        val shaShort = digest.take(8).joinToString("") { "%02X".format(it) }

        val aesEnvelope = CryptoVaultEngine.encryptBytes(dexBytes, passphrase).cipherBytes
        // Apply secondary fast XOR disguise layer so DXS1 magic is hidden inside disguised .png/.dat/.bin
        val xorKey = 0x5A + (index * 13)
        val disguisedBytes = ByteArray(aesEnvelope.size)
        for (i in aesEnvelope.indices) {
            disguisedBytes[i] = (aesEnvelope[i].toInt() xor ((xorKey + i * 7) and 0xFF)).toByte()
        }

        val cleanExt = if (disguiseExtension.startsWith(".")) disguiseExtension else ".$disguiseExtension"
        val disguisedName = when (cleanExt.lowercase()) {
            ".png" -> "tex_atlas_$index.png"
            ".dat" -> "font_metrics_$index.dat"
            else -> "res_cache_$index$cleanExt"
        }

        return PackedDexEntry(
            originalEntryName = dexEntryName,
            disguisedAssetName = disguisedName,
            originalSizeBytes = dexBytes.size,
            packedSizeBytes = disguisedBytes.size,
            originalSha256Short = shaShort,
            entropyBitsPerByte = CryptoVaultEngine.shannonEntropy(disguisedBytes),
            packedBytes = disguisedBytes
        )
    }

    fun unpackDexEntry(
        packedBytes: ByteArray,
        passphrase: String,
        index: Int
    ): ByteArray {
        val xorKey = 0x5A + (index * 13)
        val aesEnvelope = ByteArray(packedBytes.size)
        for (i in packedBytes.indices) {
            aesEnvelope[i] = (packedBytes[i].toInt() xor ((xorKey + i * 7) and 0xFF)).toByte()
        }
        return CryptoVaultEngine.decryptBytes(aesEnvelope, passphrase)
    }

    fun generateNativeJniCpp(profile: ProtectionProfile): String {
        val jniPkg = profile.packageName.replace('.', '_')
        return """
            |// ====================================================================
            |// Bloody Team Native DEX Shield (Android NDK C++ / JNI)
            |// Anti-Debug, Anti-Frida, and In-Memory RAM Decryption (No Disk Write)
            |// ====================================================================
            |#include <jni.h>
            |#include <android/asset_manager.h>
            |#include <android/asset_manager_jni.h>
            |#include <android/log.h>
            |#include <cstdio>
            |#include <cstdlib>
            |#include <cstring>
            |#include <string>
            |#include <vector>
            |
            |#define LOG_TAG "BloodyShieldJNI"
            |
            |static bool is_tracer_or_hook_active() {
            |    // 1. Check TracerPid in /proc/self/status
            |    FILE* status = fopen("/proc/self/status", "r");
            |    if (status) {
            |        char line[256];
            |        while (fgets(line, sizeof(line), status)) {
            |            if (strncmp(line, "TracerPid:", 10) == 0) {
            |                int pid = atoi(line + 10);
            |                if (pid != 0) {
            |                    fclose(status);
            |                    return true;
            |                }
            |            }
            |        }
            |        fclose(status);
            |    }
            |    // 2. Scan /proc/self/maps for Frida / Xposed / Substrate
            |    FILE* maps = fopen("/proc/self/maps", "r");
            |    if (maps) {
            |        char line[512];
            |        int count = 0;
            |        while (fgets(line, sizeof(line), maps) && count++ < 1500) {
            |            if (strstr(line, "frida") || strstr(line, "gum-js-loop") ||
            |                strstr(line, "XposedBridge") || strstr(line, "substrate")) {
            |                fclose(maps);
            |                return true;
            |            }
            |        }
            |        fclose(maps);
            |    }
            |    return false;
            |}
            |
            |extern "C"
            |JNIEXPORT jobject JNICALL
            |Java_${jniPkg}_stub_DexStubApplication_nativeDecryptDexToRam(
            |        JNIEnv* env,
            |        jobject /* thiz */,
            |        jobject javaAssetManager,
            |        jstring jAssetName,
            |        jint dexIndex) {
            |    if (is_tracer_or_hook_active()) {
            |        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Debugger or hook detected; aborting RAM decrypt");
            |        return nullptr;
            |    }
            |
            |    AAssetManager* mgr = AAssetManager_fromJava(env, javaAssetManager);
            |    const char* assetName = env->GetStringUTFChars(jAssetName, nullptr);
            |    AAsset* asset = AAssetManager_open(mgr, assetName, AASSET_MODE_BUFFER);
            |    env->ReleaseStringUTFChars(jAssetName, assetName);
            |    if (!asset) return nullptr;
            |
            |    off_t len = AAsset_getLength(asset);
            |    uint8_t* ramBuffer = static_cast<uint8_t*>(malloc(static_cast<size_t>(len)));
            |    if (!ramBuffer) {
            |        AAsset_close(asset);
            |        return nullptr;
            |    }
            |    AAsset_read(asset, ramBuffer, static_cast<size_t>(len));
            |    AAsset_close(asset);
            |
            |    // Reverse fast XOR disguise layer directly in RAM
            |    const int xorKey = 0x5A + (dexIndex * 13);
            |    for (off_t i = 0; i < len; i++) {
            |        ramBuffer[i] ^= static_cast<uint8_t>((xorKey + i * 7) & 0xFF);
            |    }
            |
            |    // Return DirectByteBuffer backed by RAM (caller passes to AssetVaultHelper / InMemoryDexClassLoader)
            |    return env->NewDirectByteBuffer(ramBuffer, static_cast<jlong>(len));
            |}
        """.trimMargin()
    }

    fun generateCMakeLists(): String {
        return """
            |cmake_minimum_required(VERSION 3.22.1)
            |project("bloody_dex_shield")
            |
            |add_library(bloody_dex_shield SHARED
            |    dex_shield_jni.cpp
            |)
            |
            |find_library(log-lib log)
            |find_library(android-lib android)
            |
            |target_link_libraries(bloody_dex_shield
            |    ${'$'}{android-lib}
            |    ${'$'}{log-lib}
            |)
        """.trimMargin()
    }

    fun generateStubApplicationJava(profile: ProtectionProfile): String {
        val sampleAsset = "tex_atlas_0${profile.dexDisguiseExtension}"
        return """
            |// ====================================================================
            |// Bloody Team Phase B Runtime Stub Loader
            |// Uses InMemoryDexClassLoader (API 26+) + LoadedApk Reflection Handover
            |// ====================================================================
            |package ${profile.packageName}.stub;
            |
            |import android.app.Application;
            |import android.content.Context;
            |import android.content.res.AssetManager;
            |import android.os.Build;
            |import dalvik.system.InMemoryDexClassLoader;
            |import java.lang.reflect.Field;
            |import java.nio.ByteBuffer;
            |import ${profile.packageName}.security.AssetVaultHelper;
            |import ${profile.packageName}.security.DexShieldGuard;
            |
            |public class DexStubApplication extends Application {
            |    static {
            |        try {
            |            System.loadLibrary("bloody_dex_shield");
            |        } catch (Throwable ignored) {}
            |    }
            |
            |    private Application realApplication;
            |
            |    private native ByteBuffer nativeDecryptDexToRam(
            |            AssetManager assetManager,
            |            String disguisedAssetName,
            |            int dexIndex);
            |
            |    @Override
            |    protected void attachBaseContext(Context base) {
            |        super.attachBaseContext(base);
            |        try {
            |            DexShieldGuard.installZeroCrashHandler();
            |            if (!DexShieldGuard.verifyEnvironment(base)) {
            |                throw new SecurityException("Runtime integrity check failed");
            |            }
            |            loadEncryptedDexFromRam(base, "$sampleAsset", 0);
            |        } catch (Throwable t) {
            |            throw new RuntimeException("DexStub bootstrap failed", t);
            |        }
            |    }
            |
            |    @Override
            |    public void onCreate() {
            |        super.onCreate();
            |        if (realApplication != null) {
            |            realApplication.onCreate();
            |        }
            |    }
            |
            |    private void loadEncryptedDexFromRam(Context base, String assetName, int idx) throws Exception {
            |        ByteBuffer disguised = nativeDecryptDexToRam(base.getAssets(), assetName, idx);
            |        if (disguised == null) return;
            |
            |        byte[] raw = new byte[disguised.remaining()];
            |        disguised.get(raw);
            |        byte[] plainDex = AssetVaultHelper.decryptBytes(raw, "REPLACE_WITH_MASTER_PASSPHRASE");
            |
            |        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            |            ByteBuffer dexBuffer = ByteBuffer.wrap(plainDex);
            |            InMemoryDexClassLoader memLoader = new InMemoryDexClassLoader(dexBuffer, getClassLoader());
            |            swapLoadedApkClassLoader(base, memLoader);
            |        }
            |    }
            |
            |    private void swapLoadedApkClassLoader(Context base, ClassLoader newLoader) throws Exception {
            |        Object packageInfo = getFieldValue(base, "mPackageInfo");
            |        if (packageInfo != null) {
            |            setFieldValue(packageInfo, "mClassLoader", newLoader);
            |        }
            |    }
            |
            |    private static Object getFieldValue(Object target, String name) throws Exception {
            |        Class<?> cls = target.getClass();
            |        while (cls != null) {
            |            try {
            |                Field f = cls.getDeclaredField(name);
            |                f.setAccessible(true);
            |                return f.get(target);
            |            } catch (NoSuchFieldException e) {
            |                cls = cls.getSuperclass();
            |            }
            |        }
            |        return null;
            |    }
            |
            |    private static void setFieldValue(Object target, String name, Object value) throws Exception {
            |        Field f = target.getClass().getDeclaredField(name);
            |        f.setAccessible(true);
            |        f.set(target, value);
            |    }
            |}
        """.trimMargin()
    }

    fun generateManifestStubPatch(profile: ProtectionProfile): String {
        return """
            |<!-- Merge into AndroidManifest.xml to activate Bloody Team DEX Stub -->
            |<application
            |    android:name="${profile.packageName}.stub.DexStubApplication"
            |    android:allowBackup="false"
            |    android:extractNativeLibs="true">
            |    <meta-data
            |        android:name="bloody_team.real_application"
            |        android:value="${profile.packageName}.MainApplication" />
            |</application>
        """.trimMargin()
    }
}

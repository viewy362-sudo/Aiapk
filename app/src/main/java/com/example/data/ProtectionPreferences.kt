package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.security.ProtectionProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.protectionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dexshield_profile"
)

class ProtectionPreferences(private val context: Context) {

    private object Keys {
        val APP_NAME = stringPreferencesKey("target_app_name")
        val PACKAGE_NAME = stringPreferencesKey("target_package_name")
        val EXPECTED_SHA = stringPreferencesKey("expected_sha256")
        val DEX_DISGUISE = stringPreferencesKey("dex_disguise_ext")
        val LAYER_DEX = booleanPreferencesKey("layer_dex")
        val LAYER_ASSET = booleanPreferencesKey("layer_asset")
        val LAYER_TAMPER = booleanPreferencesKey("layer_tamper")
        val LAYER_ANTIDEBUG = booleanPreferencesKey("layer_antidebug")
        val LAYER_OBFUSCATE = booleanPreferencesKey("layer_obfuscate")
        val LAYER_ZEROCRASH = booleanPreferencesKey("layer_zerocrash")
        val LAYER_ROOT = booleanPreferencesKey("layer_root_magisk")
        val LAYER_EMULATOR = booleanPreferencesKey("layer_emulator_clone")
        val LAYER_SSL_PROXY = booleanPreferencesKey("layer_ssl_proxy")
        val LAYER_SCREEN_SECURE = booleanPreferencesKey("layer_screen_secure")
        val NATIVE_JNI_STUB = booleanPreferencesKey("native_jni_stub")
    }

    val profileFlow: Flow<ProtectionProfile> = context.protectionDataStore.data.map { prefs ->
        ProtectionProfile(
            appName = prefs[Keys.APP_NAME] ?: "Bloody Team",
            packageName = prefs[Keys.PACKAGE_NAME] ?: "com.bloodyteam.app",
            expectedSignatureSha256 = prefs[Keys.EXPECTED_SHA] ?: "",
            dexDisguiseExtension = prefs[Keys.DEX_DISGUISE] ?: ".png",
            dexBytecodeGuard = prefs[Keys.LAYER_DEX] ?: true,
            assetAes256Encryption = prefs[Keys.LAYER_ASSET] ?: true,
            antiTamperSignature = prefs[Keys.LAYER_TAMPER] ?: true,
            antiDebugFridaShield = prefs[Keys.LAYER_ANTIDEBUG] ?: true,
            stringMetadataObfuscation = prefs[Keys.LAYER_OBFUSCATE] ?: true,
            zeroCrashExceptionGuard = prefs[Keys.LAYER_ZEROCRASH] ?: true,
            rootMagiskDetection = prefs[Keys.LAYER_ROOT] ?: true,
            emulatorCloneShield = prefs[Keys.LAYER_EMULATOR] ?: true,
            sslPinningProxyBlock = prefs[Keys.LAYER_SSL_PROXY] ?: true,
            screenTapjackingShield = prefs[Keys.LAYER_SCREEN_SECURE] ?: true,
            nativeJniStubPacking = prefs[Keys.NATIVE_JNI_STUB] ?: true
        )
    }

    suspend fun saveProfile(profile: ProtectionProfile) {
        context.protectionDataStore.edit { prefs ->
            prefs[Keys.APP_NAME] = profile.appName
            prefs[Keys.PACKAGE_NAME] = profile.packageName
            prefs[Keys.EXPECTED_SHA] = profile.expectedSignatureSha256
            prefs[Keys.DEX_DISGUISE] = profile.dexDisguiseExtension
            prefs[Keys.LAYER_DEX] = profile.dexBytecodeGuard
            prefs[Keys.LAYER_ASSET] = profile.assetAes256Encryption
            prefs[Keys.LAYER_TAMPER] = profile.antiTamperSignature
            prefs[Keys.LAYER_ANTIDEBUG] = profile.antiDebugFridaShield
            prefs[Keys.LAYER_OBFUSCATE] = profile.stringMetadataObfuscation
            prefs[Keys.LAYER_ZEROCRASH] = profile.zeroCrashExceptionGuard
            prefs[Keys.LAYER_ROOT] = profile.rootMagiskDetection
            prefs[Keys.LAYER_EMULATOR] = profile.emulatorCloneShield
            prefs[Keys.LAYER_SSL_PROXY] = profile.sslPinningProxyBlock
            prefs[Keys.LAYER_SCREEN_SECURE] = profile.screenTapjackingShield
            prefs[Keys.NATIVE_JNI_STUB] = profile.nativeJniStubPacking
        }
    }
}

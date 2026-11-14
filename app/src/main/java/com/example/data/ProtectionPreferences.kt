package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.security.ProtectionProfile
import com.example.security.WallProtectionTier
import com.example.ui.theme.AppThemePreset
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
        val THEME_PRESET = stringPreferencesKey("theme_preset_name")
        val WALL_TIER = stringPreferencesKey("wall_protection_tier")
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
        val SHRINK_RESOURCES = booleanPreferencesKey("shrink_resources")
        val SHRINK_DRAWABLES = booleanPreferencesKey("shrink_drawables")
        val SHRINK_LAYOUTS = booleanPreferencesKey("shrink_layouts")
        val STRIP_DEBUG = booleanPreferencesKey("strip_debug")
        val DETECT_HTTPCANARY = booleanPreferencesKey("detect_httpcanary")
        val CUSTOM_TOAST = booleanPreferencesKey("custom_toast")
        val CUSTOM_TOAST_MSG = stringPreferencesKey("custom_toast_msg")
        val DIALOG_CANCELABLE = booleanPreferencesKey("dialog_cancelable")
        val CHECK_INTERNET = booleanPreferencesKey("check_internet")
        val LOGIN_VALIDATE = booleanPreferencesKey("login_validate")
        val SIGN_OUTPUT_APK = booleanPreferencesKey("sign_output_apk")
        val SIGNER_CN = stringPreferencesKey("signer_cn")
        val SIGNER_ORG = stringPreferencesKey("signer_org")
    }

    val profileFlow: Flow<ProtectionProfile> = context.protectionDataStore.data.map { prefs ->
        ProtectionProfile(
            appName = prefs[Keys.APP_NAME] ?: "Bloody Team",
            packageName = prefs[Keys.PACKAGE_NAME] ?: "com.bloodyteam.app",
            expectedSignatureSha256 = prefs[Keys.EXPECTED_SHA] ?: "",
            dexDisguiseExtension = prefs[Keys.DEX_DISGUISE] ?: ".png",
            themePresetName = prefs[Keys.THEME_PRESET] ?: AppThemePreset.CRIMSON_FORGE.name,
            wallProtectionTier = prefs[Keys.WALL_TIER] ?: WallProtectionTier.WALL_V4_FORTRESS.name,
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
            nativeJniStubPacking = prefs[Keys.NATIVE_JNI_STUB] ?: true,
            shrinkResources = prefs[Keys.SHRINK_RESOURCES] ?: true,
            shrinkDrawablesAndAnim = prefs[Keys.SHRINK_DRAWABLES] ?: true,
            shrinkLayoutsAndXml = prefs[Keys.SHRINK_LAYOUTS] ?: true,
            stripDebugMetadata = prefs[Keys.STRIP_DEBUG] ?: true,
            detectHttpCanary = prefs[Keys.DETECT_HTTPCANARY] ?: true,
            customSecurityToast = prefs[Keys.CUSTOM_TOAST] ?: true,
            customToastMessage = prefs[Keys.CUSTOM_TOAST_MSG] ?: "Secured by Bloody Team",
            dialogCancelable = prefs[Keys.DIALOG_CANCELABLE] ?: false,
            checkInternetRequired = prefs[Keys.CHECK_INTERNET] ?: false,
            loginValidateGate = prefs[Keys.LOGIN_VALIDATE] ?: false,
            signOutputApk = prefs[Keys.SIGN_OUTPUT_APK] ?: true,
            signerCommonName = prefs[Keys.SIGNER_CN] ?: "Bloody Team",
            signerOrg = prefs[Keys.SIGNER_ORG] ?: "Bloody Team Security"
        )
    }

    suspend fun saveProfile(profile: ProtectionProfile) {
        context.protectionDataStore.edit { prefs ->
            prefs[Keys.APP_NAME] = profile.appName
            prefs[Keys.PACKAGE_NAME] = profile.packageName
            prefs[Keys.EXPECTED_SHA] = profile.expectedSignatureSha256
            prefs[Keys.DEX_DISGUISE] = profile.dexDisguiseExtension
            prefs[Keys.THEME_PRESET] = profile.themePresetName
            prefs[Keys.WALL_TIER] = profile.wallProtectionTier
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
            prefs[Keys.SHRINK_RESOURCES] = profile.shrinkResources
            prefs[Keys.SHRINK_DRAWABLES] = profile.shrinkDrawablesAndAnim
            prefs[Keys.SHRINK_LAYOUTS] = profile.shrinkLayoutsAndXml
            prefs[Keys.STRIP_DEBUG] = profile.stripDebugMetadata
            prefs[Keys.DETECT_HTTPCANARY] = profile.detectHttpCanary
            prefs[Keys.CUSTOM_TOAST] = profile.customSecurityToast
            prefs[Keys.CUSTOM_TOAST_MSG] = profile.customToastMessage
            prefs[Keys.DIALOG_CANCELABLE] = profile.dialogCancelable
            prefs[Keys.CHECK_INTERNET] = profile.checkInternetRequired
            prefs[Keys.LOGIN_VALIDATE] = profile.loginValidateGate
            prefs[Keys.SIGN_OUTPUT_APK] = profile.signOutputApk
            prefs[Keys.SIGNER_CN] = profile.signerCommonName
            prefs[Keys.SIGNER_ORG] = profile.signerOrg
        }
    }
}

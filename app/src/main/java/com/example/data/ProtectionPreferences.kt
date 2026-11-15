package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.security.ProtectionProfile
import com.example.ui.theme.AppThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.protectionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dexshield_profile_v3"
)

class ProtectionPreferences(private val context: Context) {

    private object Keys {
        val APP_NAME = stringPreferencesKey("target_app_name")
        val PACKAGE_NAME = stringPreferencesKey("target_package_name")
        val EXPECTED_SHA = stringPreferencesKey("expected_sha256")
        val DEX_DISGUISE = stringPreferencesKey("dex_disguise_ext")
        val THEME_PRESET = stringPreferencesKey("theme_preset_name")
        val APP_LANGUAGE = stringPreferencesKey("app_language_code")
        val WALL_TIER = stringPreferencesKey("wall_protection_tier")

        // 10 Core Security Layers + Stub
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

        // 16 Resources Confusing + Strip Metadata
        val SHRINK_RESOURCES = booleanPreferencesKey("shrink_resources")
        val SHRINK_ANIM = booleanPreferencesKey("shrink_anim")
        val SHRINK_DRAWABLES = booleanPreferencesKey("shrink_drawables")
        val SHRINK_COLOR = booleanPreferencesKey("shrink_color")
        val SHRINK_ANIMATOR = booleanPreferencesKey("shrink_animator")
        val SHRINK_LAYOUTS = booleanPreferencesKey("shrink_layouts")
        val SHRINK_RAW = booleanPreferencesKey("shrink_raw")
        val SHRINK_FONT = booleanPreferencesKey("shrink_font")
        val SHRINK_MENU = booleanPreferencesKey("shrink_menu")
        val SHRINK_MIPMAP = booleanPreferencesKey("shrink_mipmap")
        val SHRINK_INTERPOLATOR = booleanPreferencesKey("shrink_interpolator")
        val SHRINK_NAVIGATION = booleanPreferencesKey("shrink_navigation")
        val SHRINK_TRANSITION = booleanPreferencesKey("shrink_transition")
        val SHRINK_XML = booleanPreferencesKey("shrink_xml")
        val SHRINK_RESOURCE_XML_V1 = booleanPreferencesKey("shrink_resource_xml_v1")
        val SHRINK_RESOURCE_XML_V2 = booleanPreferencesKey("shrink_resource_xml_v2")
        val STRIP_DEBUG = booleanPreferencesKey("strip_debug")

        // Configuration & Policy
        val DETECT_HTTPCANARY = booleanPreferencesKey("detect_httpcanary")
        val CUSTOM_TOAST = booleanPreferencesKey("custom_toast")
        val CUSTOM_TOAST_MSG = stringPreferencesKey("custom_toast_msg")
        val DIALOG_CANCELABLE = booleanPreferencesKey("dialog_cancelable")
        val CHECK_INTERNET = booleanPreferencesKey("check_internet")
        val REMOVE_DETECT_VPN = booleanPreferencesKey("remove_detect_vpn")
        val REMOVE_DETECT_CANARY = booleanPreferencesKey("remove_detect_canary")
        val REMOVE_BUNDLING_SW = booleanPreferencesKey("remove_bundling_sw")
        val REMOVE_UPDATE_DIALOG = booleanPreferencesKey("remove_update_dialog")
        val HIDE_ROOT = booleanPreferencesKey("hide_root")

        // Advanced Settings
        val LOGIN_VALIDATE = booleanPreferencesKey("login_validate")
        val LOGIN_VALIDATE_V2 = booleanPreferencesKey("login_validate_v2")

        // APK Signer
        val SIGN_OUTPUT_APK = booleanPreferencesKey("sign_output_apk")
        val SIGNER_CN = stringPreferencesKey("signer_cn")
        val SIGNER_ORG = stringPreferencesKey("signer_org")

        // Voice Guide & Telegram Gate
        val VOICE_GUIDE_ENABLED = booleanPreferencesKey("voice_guide_enabled")
        val TELEGRAM_GATE_VERIFIED = booleanPreferencesKey("telegram_gate_verified")
    }

    val profileFlow: Flow<ProtectionProfile> = context.protectionDataStore.data.map { prefs ->
        ProtectionProfile(
            appName = prefs[Keys.APP_NAME] ?: "Bloody Team",
            packageName = prefs[Keys.PACKAGE_NAME] ?: "com.bloodyteam.app",
            expectedSignatureSha256 = prefs[Keys.EXPECTED_SHA] ?: "",
            dexDisguiseExtension = prefs[Keys.DEX_DISGUISE] ?: ".png",
            themePresetName = prefs[Keys.THEME_PRESET] ?: AppThemePreset.CRIMSON_FORGE.name,
            languageCode = prefs[Keys.APP_LANGUAGE] ?: "en",
            wallProtectionTier = prefs[Keys.WALL_TIER] ?: "",
            dexBytecodeGuard = prefs[Keys.LAYER_DEX] ?: false,
            assetAes256Encryption = prefs[Keys.LAYER_ASSET] ?: false,
            antiTamperSignature = prefs[Keys.LAYER_TAMPER] ?: false,
            antiDebugFridaShield = prefs[Keys.LAYER_ANTIDEBUG] ?: false,
            stringMetadataObfuscation = prefs[Keys.LAYER_OBFUSCATE] ?: false,
            zeroCrashExceptionGuard = prefs[Keys.LAYER_ZEROCRASH] ?: false,
            rootMagiskDetection = prefs[Keys.LAYER_ROOT] ?: false,
            emulatorCloneShield = prefs[Keys.LAYER_EMULATOR] ?: false,
            sslPinningProxyBlock = prefs[Keys.LAYER_SSL_PROXY] ?: false,
            screenTapjackingShield = prefs[Keys.LAYER_SCREEN_SECURE] ?: false,
            nativeJniStubPacking = prefs[Keys.NATIVE_JNI_STUB] ?: false,
            shrinkResources = prefs[Keys.SHRINK_RESOURCES] ?: false,
            shrinkAnim = prefs[Keys.SHRINK_ANIM] ?: false,
            shrinkDrawablesAndAnim = prefs[Keys.SHRINK_DRAWABLES] ?: false,
            shrinkColor = prefs[Keys.SHRINK_COLOR] ?: false,
            shrinkAnimator = prefs[Keys.SHRINK_ANIMATOR] ?: false,
            shrinkLayoutsAndXml = prefs[Keys.SHRINK_LAYOUTS] ?: false,
            shrinkRaw = prefs[Keys.SHRINK_RAW] ?: false,
            shrinkFont = prefs[Keys.SHRINK_FONT] ?: false,
            shrinkMenu = prefs[Keys.SHRINK_MENU] ?: false,
            shrinkMipmap = prefs[Keys.SHRINK_MIPMAP] ?: false,
            shrinkInterpolator = prefs[Keys.SHRINK_INTERPOLATOR] ?: false,
            shrinkNavigation = prefs[Keys.SHRINK_NAVIGATION] ?: false,
            shrinkTransition = prefs[Keys.SHRINK_TRANSITION] ?: false,
            shrinkXml = prefs[Keys.SHRINK_XML] ?: false,
            shrinkResourceXmlV1 = prefs[Keys.SHRINK_RESOURCE_XML_V1] ?: false,
            shrinkResourceXmlV2 = prefs[Keys.SHRINK_RESOURCE_XML_V2] ?: false,
            stripDebugMetadata = prefs[Keys.STRIP_DEBUG] ?: false,
            detectHttpCanary = prefs[Keys.DETECT_HTTPCANARY] ?: false,
            customSecurityToast = prefs[Keys.CUSTOM_TOAST] ?: false,
            customToastMessage = prefs[Keys.CUSTOM_TOAST_MSG] ?: "Secured by Bloody Team",
            dialogCancelable = prefs[Keys.DIALOG_CANCELABLE] ?: false,
            checkInternetRequired = prefs[Keys.CHECK_INTERNET] ?: false,
            removeDetectVpnProxy = prefs[Keys.REMOVE_DETECT_VPN] ?: false,
            removeDetectHttpCanary = prefs[Keys.REMOVE_DETECT_CANARY] ?: false,
            removeBundlingSoftware = prefs[Keys.REMOVE_BUNDLING_SW] ?: false,
            removeUpdateAlertDialog = prefs[Keys.REMOVE_UPDATE_DIALOG] ?: false,
            hideRoot = prefs[Keys.HIDE_ROOT] ?: false,
            loginValidateGate = prefs[Keys.LOGIN_VALIDATE] ?: false,
            loginValidateV2 = prefs[Keys.LOGIN_VALIDATE_V2] ?: false,
            signOutputApk = prefs[Keys.SIGN_OUTPUT_APK] ?: false,
            signerCommonName = prefs[Keys.SIGNER_CN] ?: "Bloody Team",
            signerOrg = prefs[Keys.SIGNER_ORG] ?: "Bloody Team Security",
            voiceGuideEnabled = prefs[Keys.VOICE_GUIDE_ENABLED] ?: true,
            telegramGateVerified = prefs[Keys.TELEGRAM_GATE_VERIFIED] ?: false
        )
    }

    suspend fun saveProfile(profile: ProtectionProfile) {
        context.protectionDataStore.edit { prefs ->
            prefs[Keys.APP_NAME] = profile.appName
            prefs[Keys.PACKAGE_NAME] = profile.packageName
            prefs[Keys.EXPECTED_SHA] = profile.expectedSignatureSha256
            prefs[Keys.DEX_DISGUISE] = profile.dexDisguiseExtension
            prefs[Keys.THEME_PRESET] = profile.themePresetName
            prefs[Keys.APP_LANGUAGE] = profile.languageCode
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
            prefs[Keys.SHRINK_ANIM] = profile.shrinkAnim
            prefs[Keys.SHRINK_DRAWABLES] = profile.shrinkDrawablesAndAnim
            prefs[Keys.SHRINK_COLOR] = profile.shrinkColor
            prefs[Keys.SHRINK_ANIMATOR] = profile.shrinkAnimator
            prefs[Keys.SHRINK_LAYOUTS] = profile.shrinkLayoutsAndXml
            prefs[Keys.SHRINK_RAW] = profile.shrinkRaw
            prefs[Keys.SHRINK_FONT] = profile.shrinkFont
            prefs[Keys.SHRINK_MENU] = profile.shrinkMenu
            prefs[Keys.SHRINK_MIPMAP] = profile.shrinkMipmap
            prefs[Keys.SHRINK_INTERPOLATOR] = profile.shrinkInterpolator
            prefs[Keys.SHRINK_NAVIGATION] = profile.shrinkNavigation
            prefs[Keys.SHRINK_TRANSITION] = profile.shrinkTransition
            prefs[Keys.SHRINK_XML] = profile.shrinkXml
            prefs[Keys.SHRINK_RESOURCE_XML_V1] = profile.shrinkResourceXmlV1
            prefs[Keys.SHRINK_RESOURCE_XML_V2] = profile.shrinkResourceXmlV2
            prefs[Keys.STRIP_DEBUG] = profile.stripDebugMetadata
            prefs[Keys.DETECT_HTTPCANARY] = profile.detectHttpCanary
            prefs[Keys.CUSTOM_TOAST] = profile.customSecurityToast
            prefs[Keys.CUSTOM_TOAST_MSG] = profile.customToastMessage
            prefs[Keys.DIALOG_CANCELABLE] = profile.dialogCancelable
            prefs[Keys.CHECK_INTERNET] = profile.checkInternetRequired
            prefs[Keys.REMOVE_DETECT_VPN] = profile.removeDetectVpnProxy
            prefs[Keys.REMOVE_DETECT_CANARY] = profile.removeDetectHttpCanary
            prefs[Keys.REMOVE_BUNDLING_SW] = profile.removeBundlingSoftware
            prefs[Keys.REMOVE_UPDATE_DIALOG] = profile.removeUpdateAlertDialog
            prefs[Keys.HIDE_ROOT] = profile.hideRoot
            prefs[Keys.LOGIN_VALIDATE] = profile.loginValidateGate
            prefs[Keys.LOGIN_VALIDATE_V2] = profile.loginValidateV2
            prefs[Keys.SIGN_OUTPUT_APK] = profile.signOutputApk
            prefs[Keys.SIGNER_CN] = profile.signerCommonName
            prefs[Keys.SIGNER_ORG] = profile.signerOrg
            prefs[Keys.VOICE_GUIDE_ENABLED] = profile.voiceGuideEnabled
            prefs[Keys.TELEGRAM_GATE_VERIFIED] = profile.telegramGateVerified
        }
    }
}

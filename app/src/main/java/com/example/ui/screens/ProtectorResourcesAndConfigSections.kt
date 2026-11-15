package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.SoundFxManager
import com.example.security.ProtectionProfile

@Composable
fun ResourcesConfusingSection(
    profile: ProtectionProfile,
    onUpdateProfile: ((ProtectionProfile) -> ProtectionProfile) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExpandableSectionCard(
        title = "Resources Confusing (${profile.selectedResourceConfuseCount}/16)",
        expanded = expanded,
        onToggleExpand = {
            SoundFxManager.playClick()
            expanded = !expanded
        }
    ) {
        OptionRow(
            title = "Shrink Resources",
            subtitle = "Strict resource shrinker config (safe IDs kept for Sketchware APKs)",
            checked = profile.shrinkResources,
            testTag = "res_shrink_all"
        ) { c -> onUpdateProfile { it.copy(shrinkResources = c) } }

        OptionRow(
            title = "Shrink Anim",
            subtitle = "Applies R8/AAPT2 shrinking rules for res/anim/*",
            checked = profile.shrinkAnim,
            badge = "VIP",
            testTag = "res_shrink_anim"
        ) { c -> onUpdateProfile { it.copy(shrinkAnim = c) } }

        OptionRow(
            title = "Shrink Drawable",
            subtitle = "Confuses & shrinks res/drawable/* rules in Kit",
            checked = profile.shrinkDrawablesAndAnim,
            badge = "VIP",
            testTag = "res_shrink_drawable"
        ) { c -> onUpdateProfile { it.copy(shrinkDrawablesAndAnim = c) } }

        OptionRow(
            title = "Shrink Color",
            subtitle = "Confuses & prunes res/color/* rules in Kit",
            checked = profile.shrinkColor,
            badge = "VIP",
            testTag = "res_shrink_color"
        ) { c -> onUpdateProfile { it.copy(shrinkColor = c) } }

        OptionRow(
            title = "Shrink Animator",
            subtitle = "Confuses & prunes res/animator/* rules in Kit",
            checked = profile.shrinkAnimator,
            badge = "VIP",
            testTag = "res_shrink_animator"
        ) { c -> onUpdateProfile { it.copy(shrinkAnimator = c) } }

        OptionRow(
            title = "Shrink Layout",
            subtitle = "Confuses res/layout/* entries safely in Kit",
            checked = profile.shrinkLayoutsAndXml,
            badge = "VIP",
            testTag = "res_shrink_layout"
        ) { c -> onUpdateProfile { it.copy(shrinkLayoutsAndXml = c) } }

        OptionRow(
            title = "Shrink Raw",
            subtitle = "Compresses & optimizes res/raw/* entries",
            checked = profile.shrinkRaw,
            badge = "VIP",
            testTag = "res_shrink_raw"
        ) { c -> onUpdateProfile { it.copy(shrinkRaw = c) } }

        OptionRow(
            title = "Shrink Font",
            subtitle = "Optimizes & prunes res/font/* entries in Kit",
            checked = profile.shrinkFont,
            badge = "VIP",
            testTag = "res_shrink_font"
        ) { c -> onUpdateProfile { it.copy(shrinkFont = c) } }

        OptionRow(
            title = "Shrink Menu",
            subtitle = "Prunes unused res/menu/* definitions in Kit",
            checked = profile.shrinkMenu,
            badge = "VIP",
            testTag = "res_shrink_menu"
        ) { c -> onUpdateProfile { it.copy(shrinkMenu = c) } }

        OptionRow(
            title = "Shrink Mipmap",
            subtitle = "Prunes redundant res/mipmap/* densities in Kit",
            checked = profile.shrinkMipmap,
            badge = "VIP",
            testTag = "res_shrink_mipmap"
        ) { c -> onUpdateProfile { it.copy(shrinkMipmap = c) } }

        OptionRow(
            title = "Shrink Interpolator",
            subtitle = "Optimizes res/interpolator/* definitions in Kit",
            checked = profile.shrinkInterpolator,
            badge = "VIP",
            testTag = "res_shrink_interpolator"
        ) { c -> onUpdateProfile { it.copy(shrinkInterpolator = c) } }

        OptionRow(
            title = "Shrink Navigation",
            subtitle = "Optimizes res/navigation/* graphs in Kit",
            checked = profile.shrinkNavigation,
            badge = "VIP",
            testTag = "res_shrink_navigation"
        ) { c -> onUpdateProfile { it.copy(shrinkNavigation = c) } }

        OptionRow(
            title = "Shrink Transition",
            subtitle = "Optimizes res/transition/* sets in Kit",
            checked = profile.shrinkTransition,
            badge = "VIP",
            testTag = "res_shrink_transition"
        ) { c -> onUpdateProfile { it.copy(shrinkTransition = c) } }

        OptionRow(
            title = "Shrink XML",
            subtitle = "Confuses res/xml/* configuration entries in Kit",
            checked = profile.shrinkXml,
            badge = "VIP",
            testTag = "res_shrink_xml"
        ) { c -> onUpdateProfile { it.copy(shrinkXml = c) } }

        OptionRow(
            title = "Shrink Resource XML V1",
            subtitle = "Flat AAPT2 resource XML shrinking pass",
            checked = profile.shrinkResourceXmlV1,
            badge = "VIP",
            testTag = "res_shrink_xml_v1"
        ) { c -> onUpdateProfile { it.copy(shrinkResourceXmlV1 = c) } }

        OptionRow(
            title = "Shrink Resource XML V2",
            subtitle = "Deep attribute pruning pass (non-breaking in APK)",
            checked = profile.shrinkResourceXmlV2,
            badge = "VIP",
            testTag = "res_shrink_xml_v2"
        ) { c -> onUpdateProfile { it.copy(shrinkResourceXmlV2 = c) } }

        OptionRow(
            title = "Strip Build & Debug Metadata (APK)",
            subtitle = "Removes DebugProbesKt.bin & Kotlin tooling metadata from APK",
            checked = profile.stripDebugMetadata,
            testTag = "res_strip_debug"
        ) { c -> onUpdateProfile { it.copy(stripDebugMetadata = c) } }
    }
}

@Composable
fun ConfigurationSection(
    profile: ProtectionProfile,
    onUpdateProfile: ((ProtectionProfile) -> ProtectionProfile) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExpandableSectionCard(
        title = "Configuration",
        expanded = expanded,
        onToggleExpand = {
            SoundFxManager.playClick()
            expanded = !expanded
        }
    ) {
        OptionRow(
            title = "Add Custom Toast",
            subtitle = "Displays custom protection banner on APK launch",
            checked = profile.customSecurityToast,
            testTag = "cfg_custom_toast"
        ) { c -> onUpdateProfile { it.copy(customSecurityToast = c) } }

        if (profile.customSecurityToast) {
            OutlinedTextField(
                value = profile.customToastMessage,
                onValueChange = { v -> onUpdateProfile { it.copy(customToastMessage = v) } },
                label = { Text("Custom Toast Text") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("cfg_custom_toast_input")
            )
        }

        OptionRow(
            title = "Detect HttpCanary",
            subtitle = "Blocks HttpCanary, SSL Capture & packet sniffers",
            checked = profile.detectHttpCanary,
            badge = "VIP",
            testTag = "cfg_httpcanary"
        ) { c -> onUpdateProfile { it.copy(detectHttpCanary = c) } }

        OptionRow(
            title = "Dialog Cancelable",
            subtitle = "Allows user to dismiss network or validation prompts",
            checked = profile.dialogCancelable,
            badge = "VIP",
            testTag = "cfg_dialog_cancelable"
        ) { c -> onUpdateProfile { it.copy(dialogCancelable = c) } }

        OptionRow(
            title = "Detect Emulator",
            subtitle = "Blocks QEMU, BlueStacks & emulator builds",
            checked = profile.emulatorCloneShield,
            badge = "VIP",
            testTag = "cfg_detect_emulator"
        ) { c -> onUpdateProfile { it.copy(emulatorCloneShield = c) } }

        OptionRow(
            title = "Check Internet",
            subtitle = "Requires active internet connection before launch",
            checked = profile.checkInternetRequired,
            badge = "VIP",
            testTag = "cfg_check_net"
        ) { c -> onUpdateProfile { it.copy(checkInternetRequired = c) } }

        OptionRow(
            title = "Remove Detect Vpn Proxy",
            subtitle = "Bypasses strict VPN & proxy detection inside runtime",
            checked = profile.removeDetectVpnProxy,
            badge = "VIP",
            testTag = "cfg_remove_vpn"
        ) { c -> onUpdateProfile { it.copy(removeDetectVpnProxy = c) } }

        OptionRow(
            title = "Remove Detect HttpCanary",
            subtitle = "Allows traffic inspection without sniffer termination",
            checked = profile.removeDetectHttpCanary,
            badge = "VIP",
            testTag = "cfg_remove_httpcanary"
        ) { c -> onUpdateProfile { it.copy(removeDetectHttpCanary = c) } }

        OptionRow(
            title = "Remove Bundling Software",
            subtitle = "Blocks DualSpace, Parallel Space & virtual clone wrappers",
            checked = profile.removeBundlingSoftware,
            badge = "VIP",
            testTag = "cfg_remove_bundling"
        ) { c -> onUpdateProfile { it.copy(removeBundlingSoftware = c) } }

        OptionRow(
            title = "Remove Update AlertDialog",
            subtitle = "Guards window callbacks against forced update popups",
            checked = profile.removeUpdateAlertDialog,
            badge = "VIP",
            testTag = "cfg_remove_update_dialog"
        ) { c -> onUpdateProfile { it.copy(removeUpdateAlertDialog = c) } }

        OptionRow(
            title = "Hide Root",
            subtitle = "Masks root/test-keys checks so rooted devices can run",
            checked = profile.hideRoot,
            badge = "VIP",
            testTag = "cfg_hide_root"
        ) { c -> onUpdateProfile { it.copy(hideRoot = c) } }

        OptionRow(
            title = "Anti Screenshots",
            subtitle = "Enforces FLAG_SECURE & blocks screen capture",
            checked = profile.screenTapjackingShield,
            badge = "VIP",
            testTag = "cfg_anti_screenshots"
        ) { c -> onUpdateProfile { it.copy(screenTapjackingShield = c) } }

        OptionRow(
            title = "Detect VPN Proxy",
            subtitle = "Blocks active tun0/ppp0 VPN & HTTP proxy tunnels",
            checked = profile.sslPinningProxyBlock,
            badge = "VIP",
            testTag = "cfg_detect_vpn_proxy"
        ) { c -> onUpdateProfile { it.copy(sslPinningProxyBlock = c) } }

        OptionRow(
            title = "Detect Xposed Framework",
            subtitle = "Scans /proc/self/maps for Xposed, LSPosed & Frida",
            checked = profile.antiDebugFridaShield,
            badge = "VIP",
            testTag = "cfg_detect_xposed"
        ) { c -> onUpdateProfile { it.copy(antiDebugFridaShield = c) } }
    }
}

@Composable
fun AdvancedSettingsSection(
    profile: ProtectionProfile,
    onUpdateProfile: ((ProtectionProfile) -> ProtectionProfile) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExpandableSectionCard(
        title = "Advanced Settings",
        expanded = expanded,
        onToggleExpand = {
            SoundFxManager.playClick()
            expanded = !expanded
        }
    ) {
        OptionRow(
            title = "Add Login Validate V1",
            subtitle = "Shows Bloody Team Access Token verification prompt on launch",
            checked = profile.loginValidateGate,
            badge = "VIP",
            testTag = "adv_login_v1"
        ) { c -> onUpdateProfile { it.copy(loginValidateGate = c) } }

        OptionRow(
            title = "Add Login Validate V2",
            subtitle = "Shows dual-field User ID + VIP License Key authentication gate",
            checked = profile.loginValidateV2,
            badge = "VIP",
            testTag = "adv_login_v2"
        ) { c -> onUpdateProfile { it.copy(loginValidateV2 = c) } }
    }
}

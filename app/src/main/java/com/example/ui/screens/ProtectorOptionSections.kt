package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.SoundFxManager
import com.example.security.ProtectionProfile
import com.example.security.WallProtectionTier

@Composable
fun QuickThreatMatrixCard(
    profile: ProtectionProfile,
    onUpdateProfile: ((ProtectionProfile) -> ProtectionProfile) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Quick Threat Matrix",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactCheckTile(
                    label = "Check Emulator",
                    checked = profile.emulatorCloneShield,
                    testTag = "quick_emulator",
                    modifier = Modifier.weight(1f)
                ) { c ->
                    SoundFxManager.playToggle(c)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onUpdateProfile { it.copy(emulatorCloneShield = c) }
                }
                CompactCheckTile(
                    label = "Check Root",
                    checked = profile.rootMagiskDetection,
                    testTag = "quick_root",
                    modifier = Modifier.weight(1f)
                ) { c ->
                    SoundFxManager.playToggle(c)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onUpdateProfile { it.copy(rootMagiskDetection = c) }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactCheckTile(
                    label = "Check Xposed",
                    checked = profile.antiDebugFridaShield,
                    testTag = "quick_xposed",
                    modifier = Modifier.weight(1f)
                ) { c ->
                    SoundFxManager.playToggle(c)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onUpdateProfile { it.copy(antiDebugFridaShield = c) }
                }
                CompactCheckTile(
                    label = "Check VPN",
                    checked = profile.sslPinningProxyBlock,
                    testTag = "quick_vpn",
                    modifier = Modifier.weight(1f)
                ) { c ->
                    SoundFxManager.playToggle(c)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onUpdateProfile { it.copy(sslPinningProxyBlock = c) }
                }
            }
        }
    }
}

@Composable
fun WallProtectionTierCard(
    profile: ProtectionProfile,
    onApplyWallTier: (WallProtectionTier) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val activeTier = profile.resolvedWallTier
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Wall Protection Engine",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            WallProtectionTier.entries.forEach { tier ->
                val selected = tier == activeTier
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wall_tier_${tier.name.lowercase()}")
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onApplyWallTier(tier)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onApplyWallTier(tier)
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tier.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (tier.badge == "VIP") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = tier.badge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (tier.badge == "VIP") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = tier.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResourcesAndConfigAccordionCard(
    profile: ProtectionProfile,
    onUpdateProfile: ((ProtectionProfile) -> ProtectionProfile) -> Unit,
    onSelectAllLayers: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showLayers by rememberSaveable { mutableStateOf(true) }
    var showResources by rememberSaveable { mutableStateOf(false) }
    var showConfig by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. 10-Layer Security Suite
        ExpandableSectionCard(
            title = "10-Layer Security Suite (${profile.enabledLayersCount}/10)",
            expanded = showLayers,
            onToggleExpand = {
                SoundFxManager.playClick()
                showLayers = !showLayers
            },
            headerActionText = if (profile.enabledLayersCount < 10) "Select All" else "Clear All",
            onHeaderAction = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelectAllLayers(profile.enabledLayersCount < 10)
            }
        ) {
            OptionRow("1. DEX Packing & 65K Method Guard", "Disguises classes*.dex into ${profile.dexDisguiseExtension}", profile.dexBytecodeGuard, "opt_dex") { c ->
                onUpdateProfile { it.copy(dexBytecodeGuard = c) }
            }
            OptionRow("Native C++ JNI + InMemoryDexClassLoader Stub", "Generates Phase B RAM-only loader & Manifest patch", profile.nativeJniStubPacking, "opt_native_stub") { c ->
                onUpdateProfile { it.copy(nativeJniStubPacking = c) }
            }
            OptionRow("2. Asset & Resource AES-256-GCM", "Encrypts app assets with authenticated DXS1 cipher", profile.assetAes256Encryption, "opt_asset") { c ->
                onUpdateProfile { it.copy(assetAes256Encryption = c) }
            }
            OptionRow("3. Anti-Tamper & Signature Lock", "Blocks re-signed or tampered clones", profile.antiTamperSignature, "opt_tamper") { c ->
                onUpdateProfile { it.copy(antiTamperSignature = c) }
            }
            OptionRow("4. Anti-Debug & Frida / Xposed Shield", "Blocks TracerPid, Frida-server & Xposed hooks", profile.antiDebugFridaShield, "opt_frida") { c ->
                onUpdateProfile { it.copy(antiDebugFridaShield = c) }
            }
            OptionRow("5. String & API Key Obfuscation", "Hides keys inside XOR byte-rotated StringVault", profile.stringMetadataObfuscation, "opt_strings") { c ->
                onUpdateProfile { it.copy(stringMetadataObfuscation = c) }
            }
            OptionRow("6. Zero-Crash & ANR Exception Guard", "Global thread handler preventing fatal crashes", profile.zeroCrashExceptionGuard, "opt_crash") { c ->
                onUpdateProfile { it.copy(zeroCrashExceptionGuard = c) }
            }
            OptionRow("7. Root & Magisk Environment Shield", "Detects su, Magisk & test-keys", profile.rootMagiskDetection, "opt_root") { c ->
                onUpdateProfile { it.copy(rootMagiskDetection = c) }
            }
            OptionRow("8. Emulator & Virtual-Clone Blocker", "Blocks QEMU & Parallel Space clones", profile.emulatorCloneShield, "opt_emulator") { c ->
                onUpdateProfile { it.copy(emulatorCloneShield = c) }
            }
            OptionRow("9. SSL Pinning & Proxy/VPN Block", "SPKI pinning + HTTP proxy & tun0/ppp0 block", profile.sslPinningProxyBlock, "opt_ssl") { c ->
                onUpdateProfile { it.copy(sslPinningProxyBlock = c) }
            }
            OptionRow("10. Anti-Screenshot & Tapjacking", "Enforces FLAG_SECURE & obscured touch protection", profile.screenTapjackingShield, "opt_screen") { c ->
                onUpdateProfile { it.copy(screenTapjackingShield = c) }
            }
        }

        // 2. Resources Confusing & Shrinking
        ExpandableSectionCard(
            title = "Resources Confusing & Optimization",
            expanded = showResources,
            onToggleExpand = {
                SoundFxManager.playClick()
                showResources = !showResources
            }
        ) {
            OptionRow("Shrink Resources (Kit / R8 Config)", "Generates AAPT2 & R8 shrinkResources config in Developer Kit", profile.shrinkResources, "res_shrink_all") { c ->
                onUpdateProfile { it.copy(shrinkResources = c) }
            }
            OptionRow("Confuse Drawable, Anim & Color (Kit)", "Adds drawable/anim/color obfuscation rules to Developer Kit", profile.shrinkDrawablesAndAnim, "res_shrink_drawable") { c ->
                onUpdateProfile { it.copy(shrinkDrawablesAndAnim = c) }
            }
            OptionRow("Confuse Layout, Font, Raw & XML (Kit)", "Adds layout/raw/font/xml R8 rules to Developer Kit", profile.shrinkLayoutsAndXml, "res_shrink_layout") { c ->
                onUpdateProfile { it.copy(shrinkLayoutsAndXml = c) }
            }
            OptionRow("Strip Build & Debug Metadata (APK)", "Directly removes DebugProbesKt.bin & build metadata from output APK", profile.stripDebugMetadata, "res_strip_debug") { c ->
                onUpdateProfile { it.copy(stripDebugMetadata = c) }
            }
        }

        // 3. Configuration & Runtime Guards
        ExpandableSectionCard(
            title = "Configuration & Runtime Policy",
            expanded = showConfig,
            onToggleExpand = {
                SoundFxManager.playClick()
                showConfig = !showConfig
            }
        ) {
            OptionRow("Detect HttpCanary & Traffic Sniffers", "Blocks packet capture & MITM diagnostic tools", profile.detectHttpCanary, "cfg_httpcanary") { c ->
                onUpdateProfile { it.copy(detectHttpCanary = c) }
            }
            OptionRow("Custom Security Toast Banner", "Shows protection notice at launch", profile.customSecurityToast, "cfg_custom_toast") { c ->
                onUpdateProfile { it.copy(customSecurityToast = c) }
            }
            if (profile.customSecurityToast) {
                OutlinedTextField(
                    value = profile.customToastMessage,
                    onValueChange = { v -> onUpdateProfile { it.copy(customToastMessage = v) } },
                    label = { Text("Custom Toast Text") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
            OptionRow("Non-Cancelable Security Dialog", "Locks alert modal until check resolves", !profile.dialogCancelable, "cfg_dialog_lock") { c ->
                onUpdateProfile { it.copy(dialogCancelable = !c) }
            }
            OptionRow("Check Internet Before Verification", "Requires active network before remote license check", profile.checkInternetRequired, "cfg_check_net") { c ->
                onUpdateProfile { it.copy(checkInternetRequired = c) }
            }
            OptionRow("Login Validation Gate (V1/V2)", "Injects token verification hook policy", profile.loginValidateGate, "cfg_login_gate") { c ->
                onUpdateProfile { it.copy(loginValidateGate = c) }
            }
        }
    }
}

@Composable
private fun CompactCheckTile(
    label: String,
    checked: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = modifier
            .height(48.dp)
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ExpandableSectionCard(
    title: String,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    headerActionText: String? = null,
    onHeaderAction: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                if (headerActionText != null && onHeaderAction != null) {
                    TextButton(onClick = onHeaderAction) {
                        Text(headerActionText)
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse section" else "Expand section",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                SoundFxManager.playToggle(!checked)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = { c ->
                SoundFxManager.playToggle(c)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(c)
            },
            modifier = Modifier.testTag(testTag)
        )
    }
}

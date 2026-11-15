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
                text = "Wall Protection Engine (${if (activeTier != null) activeTier.title else "None Selected"})",
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
                                VipOrFreeBadge(badge = tier.badge)
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

        // 2. Resources Confusing (16 options + metadata strip)
        ResourcesConfusingSection(profile = profile, onUpdateProfile = onUpdateProfile)

        // 3. Configuration (13 options)
        ConfigurationSection(profile = profile, onUpdateProfile = onUpdateProfile)

        // 4. Advanced Settings (Login Validate V1 & V2)
        AdvancedSettingsSection(profile = profile, onUpdateProfile = onUpdateProfile)
    }
}

@Composable
fun VipOrFreeBadge(badge: String) {
    Surface(
        shape = CircleShape,
        color = if (badge == "VIP") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (badge == "VIP") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
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
fun ExpandableSectionCard(
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
fun OptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    badge: String? = null,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (badge != null) {
                    VipOrFreeBadge(badge = badge)
                }
            }
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

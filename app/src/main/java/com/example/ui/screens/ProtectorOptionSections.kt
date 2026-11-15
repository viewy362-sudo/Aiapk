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
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.SoundFxManager
import com.example.i18n.AppStrings
import com.example.i18n.FeatureIds
import com.example.security.ProtectionProfile
import com.example.security.WallProtectionTier
import com.example.ui.components.ExplainInfoIconButton

@Composable
fun QuickThreatMatrixCard(
    profile: ProtectionProfile,
    isExplainMode: Boolean,
    onInspectFeature: (String) -> Unit,
    onUpdateProfile: ((ProtectionProfile) -> ProtectionProfile) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val strings = AppStrings.get(profile.resolvedLanguage)
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
                text = strings.quickThreatMatrixTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactCheckTile(
                    label = "Check Emulator",
                    checked = profile.emulatorCloneShield,
                    featureId = FeatureIds.EMULATOR_SHIELD,
                    isExplainMode = isExplainMode,
                    onInspectFeature = onInspectFeature,
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
                    featureId = FeatureIds.ROOT_SHIELD,
                    isExplainMode = isExplainMode,
                    onInspectFeature = onInspectFeature,
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
                    featureId = FeatureIds.ANTI_DEBUG_FRIDA,
                    isExplainMode = isExplainMode,
                    onInspectFeature = onInspectFeature,
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
                    featureId = FeatureIds.SSL_VPN_BLOCK,
                    isExplainMode = isExplainMode,
                    onInspectFeature = onInspectFeature,
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
    isExplainMode: Boolean,
    onInspectFeature: (String) -> Unit,
    onApplyWallTier: (WallProtectionTier) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val activeTier = profile.resolvedWallTier
    val strings = AppStrings.get(profile.resolvedLanguage)
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
                text = "${strings.wallProtectionTitle} (${if (activeTier != null) activeTier.title else "None"})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            WallProtectionTier.entries.forEach { tier ->
                val selected = tier == activeTier
                val featureId = "wall_${tier.name.lowercase()}"
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
                            if (isExplainMode) {
                                onInspectFeature(featureId)
                            } else {
                                onApplyWallTier(tier)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isExplainMode) {
                                    onInspectFeature(featureId)
                                } else {
                                    onApplyWallTier(tier)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
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
                        ExplainInfoIconButton(
                            contentDescription = "Explain ${tier.title}",
                            testTag = "info_$featureId",
                            onClick = { onInspectFeature(featureId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResourcesAndConfigAccordionCard(
    profile: ProtectionProfile,
    isExplainMode: Boolean,
    onInspectFeature: (String) -> Unit,
    onUpdateProfile: ((ProtectionProfile) -> ProtectionProfile) -> Unit,
    onSelectAllLayers: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val strings = AppStrings.get(profile.resolvedLanguage)
    var showLayers by rememberSaveable { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. 10-Layer Security Suite
        ExpandableSectionCard(
            title = "${strings.core10Title} (${profile.enabledLayersCount}/10)",
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
            OptionRow(
                title = "1. DEX Packing & 65K Method Guard",
                subtitle = "Disguises classes*.dex into ${profile.dexDisguiseExtension}",
                checked = profile.dexBytecodeGuard,
                testTag = "opt_dex",
                featureId = FeatureIds.DEX_PACKING,
                isExplainMode = isExplainMode,
                onInspectFeature = onInspectFeature
            ) { c -> onUpdateProfile { it.copy(dexBytecodeGuard = c) } }

            OptionRow(
                title = "Native C++ JNI + InMemoryDexClassLoader Stub",
                subtitle = "Generates Phase B RAM-only loader & Manifest patch",
                checked = profile.nativeJniStubPacking,
                testTag = "opt_native_stub",
                featureId = FeatureIds.NATIVE_JNI_STUB,
                isExplainMode = isExplainMode,
                onInspectFeature = onInspectFeature
            ) { c -> onUpdateProfile { it.copy(nativeJniStubPacking = c) } }

            OptionRow(
                title = "2. Asset & Resource AES-256-GCM",
                subtitle = "Encrypts app assets with authenticated DXS1 cipher",
                checked = profile.assetAes256Encryption,
                testTag = "opt_asset",
                featureId = FeatureIds.ASSET_AES,
                isExplainMode = isExplainMode,
                onInspectFeature = onInspectFeature
            ) { c -> onUpdateProfile { it.copy(assetAes256Encryption = c) } }

            OptionRow(
                title = "3. Anti-Tamper & Signature Lock",
                subtitle = "Blocks re-signed or tampered clones",
                checked = profile.antiTamperSignature,
                testTag = "opt_tamper",
                featureId = FeatureIds.ANTI_TAMPER,
                isExplainMode = isExplainMode,
                onInspectFeature = onInspectFeature
            ) { c -> onUpdateProfile { it.copy(antiTamperSignature = c) } }

            OptionRow(
                title = "4. Anti-Debug & Frida / Xposed Shield",
                subtitle = "Blocks TracerPid, Frida-server & Xposed hooks",
                checked = profile.antiDebugFridaShield,
                testTag = "opt_frida",
                featureId = FeatureIds.ANTI_DEBUG_FRIDA,
                isExplainMode = isExplainMode,
                onInspectFeature = onInspectFeature
            ) { c -> onUpdateProfile { it.copy(antiDebugFridaShield = c) } }

            OptionRow(
                title = "5. String & API Key Obfuscation",
                subtitle = "Hides keys inside XOR byte-rotated StringVault",
                checked = profile.stringMetadataObfuscation,
                testTag = "opt_strings",
                featureId = FeatureIds.STRING_OBFUSCATE,
                isExplainMode = isExplainMode,
                onInspectFeature = onInspectFeature
            ) { c -> onUpdateProfile { it.copy(stringMetadataObfuscation = c) } }

            OptionRow(
                title = "6. Zero-Crash & ANR Exception Guard",
                subtitle = "Global thread handler preventing fatal crashes",
                checked = profile.zeroCrashExceptionGuard,
                testTag = "opt_crash",
                featureId = FeatureIds.ZERO_CRASH,
                isExplainMode = isExplainMode,
                onInspectFeature = onInspectFeature
            ) { c -> onUpdateProfile { it.copy(zeroCrashExceptionGuard = c) } }

            OptionRow(
                title = "7. Root & Magisk Environment Shield",
                subtitle = "Detects su, Magisk & test-keys",
                checked = profile.rootMagiskDetection,
                testTag = "opt_root",
                featureId = FeatureIds.ROOT_SHIELD,
                isExplainMode = isExplainMode,
                onInspectFeature = onInspectFeature
            ) { c -> onUpdateProfile { it.copy(rootMagiskDetection = c) } }

            OptionRow(
                title = "8. Emulator & Virtual-Clone Blocker",
                subtitle = "Blocks QEMU & Parallel Space clones",
                checked = profile.emulatorCloneShield,
                testTag = "opt_emulator",
                featureId = FeatureIds.EMULATOR_SHIELD,
                isExplainMode = isExplainMode,
                onInspectFeature = onInspectFeature
            ) { c -> onUpdateProfile { it.copy(emulatorCloneShield = c) } }

            OptionRow(
                title = "9. SSL Pinning & Proxy/VPN Block",
                subtitle = "SPKI pinning + HTTP proxy & tun0/ppp0 block",
                checked = profile.sslPinningProxyBlock,
                testTag = "opt_ssl",
                featureId = FeatureIds.SSL_VPN_BLOCK,
                isExplainMode = isExplainMode,
                onInspectFeature = onInspectFeature
            ) { c -> onUpdateProfile { it.copy(sslPinningProxyBlock = c) } }

            OptionRow(
                title = "10. Anti-Screenshot & Tapjacking",
                subtitle = "Enforces FLAG_SECURE & obscured touch protection",
                checked = profile.screenTapjackingShield,
                testTag = "opt_screen",
                featureId = FeatureIds.SCREEN_TAPJACKING,
                isExplainMode = isExplainMode,
                onInspectFeature = onInspectFeature
            ) { c -> onUpdateProfile { it.copy(screenTapjackingShield = c) } }
        }

        // 2. Resources Confusing (16 options + metadata strip)
        ResourcesConfusingSection(
            profile = profile,
            isExplainMode = isExplainMode,
            onInspectFeature = onInspectFeature,
            onUpdateProfile = onUpdateProfile
        )

        // 3. Configuration (13 options)
        ConfigurationSection(
            profile = profile,
            isExplainMode = isExplainMode,
            onInspectFeature = onInspectFeature,
            onUpdateProfile = onUpdateProfile
        )

        // 4. Advanced Settings (Login Validate V1 & V2)
        AdvancedSettingsSection(
            profile = profile,
            isExplainMode = isExplainMode,
            onInspectFeature = onInspectFeature,
            onUpdateProfile = onUpdateProfile
        )
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
    featureId: String,
    isExplainMode: Boolean,
    onInspectFeature: (String) -> Unit,
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
            .clickable {
                if (isExplainMode) {
                    onInspectFeature(featureId)
                } else {
                    onCheckedChange(!checked)
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 2.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { c ->
                    if (isExplainMode) onInspectFeature(featureId) else onCheckedChange(c)
                },
                modifier = Modifier.testTag(testTag)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 28.dp, height = 48.dp)
                    .clickable { onInspectFeature(featureId) }
                    .testTag("info_$testTag")
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Info,
                    contentDescription = "Explain $label",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
            }
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
    featureId: String? = null,
    isExplainMode: Boolean = false,
    onInspectFeature: ((String) -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isExplainMode && featureId != null && onInspectFeature != null) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onInspectFeature(featureId)
                } else {
                    SoundFxManager.playToggle(!checked)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(!checked)
                }
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
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
        if (featureId != null && onInspectFeature != null) {
            ExplainInfoIconButton(
                contentDescription = "Explain $title",
                testTag = "info_$testTag",
                onClick = { onInspectFeature(featureId) }
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Switch(
            checked = checked,
            onCheckedChange = { c ->
                if (isExplainMode && featureId != null && onInspectFeature != null) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onInspectFeature(featureId)
                } else {
                    SoundFxManager.playToggle(c)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(c)
                }
            },
            modifier = Modifier.testTag(testTag)
        )
    }
}

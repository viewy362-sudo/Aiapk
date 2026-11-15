package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.SoundFxManager
import com.example.i18n.AppStrings
import com.example.i18n.FeatureIds
import com.example.security.ProtectionProfile
import com.example.security.WallProtectionTier
import com.example.security.WarningSeverity
import com.example.ui.ShieldUiState
import com.example.ui.components.ExplainInfoIconButton
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFamily

private const val YOUTUBE_CHANNEL_URL = "https://youtube.com/@howtomakeinjector?si=x_oDekn8iZqGbJRM"
private const val TELEGRAM_CHANNEL_URL = "https://t.me/BLOODYTEAMOFFICIAL"

@Composable
fun ProtectorStudioScreen(
    uiState: ShieldUiState,
    profile: ProtectionProfile,
    isExplainMode: Boolean,
    onToggleExplainMode: () -> Unit,
    onOpenFeatureGuide: () -> Unit,
    onInspectFeature: (String) -> Unit,
    onPickInputArchive: (Uri) -> Unit,
    onLoadSelfApk: () -> Unit,
    onUpdateProfile: ((ProtectionProfile) -> ProtectionProfile) -> Unit,
    onApplyWallTier: (WallProtectionTier) -> Unit,
    onReapplySmartAuto: () -> Unit,
    onApplyWarningFix: ((ProtectionProfile) -> ProtectionProfile) -> Unit,
    onOpenThemeStudio: () -> Unit,
    onRandomTheme: () -> Unit,
    onSelectAllLayers: (Boolean) -> Unit,
    onAdoptDeviceCert: () -> Unit,
    onUpdateProtectorInputs: (passphrase: String, secretKey: String) -> Unit,
    onRunProtection: () -> Unit,
    onSaveProtectedOutput: (Uri) -> Unit,
    onSaveCompanionKit: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val strings = AppStrings.get(profile.resolvedLanguage)

    val inputPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onPickInputArchive(uri)
    }

    val apkOutputSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri ->
        if (uri != null) onSaveProtectedOutput(uri)
    }

    val zipOutputSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) onSaveProtectedOutput(uri)
    }

    val companionKitSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) onSaveCompanionKit(uri)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Forge Card with Theme Selector & Randomizer
            item {
                val activeTheme = profile.resolvedThemePreset
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_bloody_hero),
                            contentDescription = "Bloody Team forge banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xA809090D),
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                            Color(0xEB09090D)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusPill(
                                    text = "${activeTheme.displayName.uppercase()} • ${profile.enabledLayersCount}/10",
                                    isPositive = profile.enabledLayersCount >= 6
                                )
                                StatusPill(
                                    text = if (profile.signOutputApk) "APK v1+v2 SIGNER" else "ZIP MODE",
                                    isPositive = profile.signOutputApk
                                )
                            }

                            Text(
                                text = strings.heroTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = strings.heroSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE2E8F0)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onRandomTheme()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("hero_random_theme_button")
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.randomDesign, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onOpenThemeStudio()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("hero_open_theme_studio_button")
                                ) {
                                    Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.selectTheme, maxLines = 1)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onOpenFeatureGuide()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("hero_open_guide_button")
                                ) {
                                    Icon(Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.guideButtonLabel, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onToggleExplainMode()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isExplainMode) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                        contentColor = if (isExplainMode) MaterialTheme.colorScheme.onSecondary else Color.White
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isExplainMode) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("hero_explain_mode_button")
                                ) {
                                    Icon(Icons.Rounded.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isExplainMode) strings.explainModeOn else strings.explainModeOff, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            if (isExplainMode) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("explain_mode_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HelpOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = strings.explainModeBanner,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // STEP 1: Input Application (.apk / .zip)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.step1Title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    SoundFxManager.playClick()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    inputPicker.launch(
                                        arrayOf(
                                            "application/vnd.android.package-archive",
                                            "application/zip",
                                            "application/octet-stream",
                                            "*/*"
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("protector_pick_input_button")
                            ) {
                                Icon(Icons.Rounded.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.selectApkButton, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onLoadSelfApk()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("protector_load_self_apk_button")
                            ) {
                                Icon(Icons.Rounded.Android, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.useThisApkButton, maxLines = 1)
                            }
                        }

                        AnimatedVisibility(
                            visible = uiState.selectedInputInspection != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            uiState.selectedInputInspection?.let { info ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Loaded: ${uiState.selectedInputName}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Text(
                                                    text = "${info.detectedFramework} • Size: ${info.totalArchiveSizeBytes / 1024} KB",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            ExplainInfoIconButton(
                                                contentDescription = "Explain Smart Auto",
                                                testTag = "info_smart_auto",
                                                onClick = { onInspectFeature(FeatureIds.SMART_AUTO) }
                                            )
                                            FilledTonalButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    if (isExplainMode) onInspectFeature(FeatureIds.SMART_AUTO) else onReapplySmartAuto()
                                                },
                                                modifier = Modifier
                                                    .height(40.dp)
                                                    .testTag("smart_auto_reapply_button")
                                            ) {
                                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(strings.smartAutoButton, maxLines = 1)
                                            }
                                        }
                                        Text(
                                            text = "DEX: ${info.dexReports.size} • Assets: ${info.assetEntriesCount} • Data files: ${info.dataFileEntriesCount} • Perms: ${info.usesPermissions.size}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (uiState.autoProfileSummary.isNotEmpty()) {
                                            Text(
                                                text = "Auto-Selected Safe Checkmarks:",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            uiState.autoProfileSummary.forEach { reason ->
                                                Text(
                                                    text = "• $reason",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = strings.assetEncryptionTitle,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = if (info.assetEntriesCount > 0 || info.dataFileEntriesCount > 0) {
                                                            "Found ${info.assetEntriesCount} asset(s) & ${info.dataFileEntriesCount} data file(s) — toggle ON/OFF as needed"
                                                        } else {
                                                            "No asset files found — keep OFF or toggle ON for Kit"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                ExplainInfoIconButton(
                                                    contentDescription = "Explain Asset & Data File Encryption",
                                                    testTag = "info_quick_asset_enc",
                                                    onClick = { onInspectFeature(FeatureIds.ASSET_AES) }
                                                )
                                                Switch(
                                                    checked = profile.assetAes256Encryption,
                                                    onCheckedChange = { c ->
                                                        if (isExplainMode) {
                                                            onInspectFeature(FeatureIds.ASSET_AES)
                                                        } else {
                                                            SoundFxManager.playToggle(c)
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            onUpdateProfile { it.copy(assetAes256Encryption = c) }
                                                        }
                                                    },
                                                    modifier = Modifier.testTag("quick_asset_encryption_switch")
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Live Crash-Safety & Compatibility Warnings
            if (uiState.compatibilityWarnings.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.compatibilityWarnings.forEachIndexed { idx, warn ->
                            val containerColor = when (warn.severity) {
                                WarningSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                                WarningSeverity.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
                                WarningSeverity.INFO -> MaterialTheme.colorScheme.secondaryContainer
                            }
                            val contentColor = when (warn.severity) {
                                WarningSeverity.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
                                WarningSeverity.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
                                WarningSeverity.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = containerColor,
                                border = BorderStroke(1.dp, contentColor.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("compatibility_warning_$idx")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (warn.severity == WarningSeverity.INFO) {
                                            Icons.Rounded.Info
                                        } else {
                                            Icons.Rounded.WarningAmber
                                        },
                                        contentDescription = null,
                                        tint = contentColor
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = warn.title,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = contentColor
                                        )
                                        Text(
                                            text = warn.detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = contentColor
                                        )
                                    }
                                    val fix = warn.fixTransform
                                    val actionLabel = warn.actionLabel
                                    if (fix != null && actionLabel != null) {
                                        TextButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onApplyWarningFix(fix)
                                            },
                                            modifier = Modifier.testTag("warning_fix_button_$idx")
                                        ) {
                                            Text(
                                                text = actionLabel,
                                                color = contentColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Threat Matrix (Check Emulator, Check Root, Check Xposed, Check VPN)
            item {
                QuickThreatMatrixCard(
                    profile = profile,
                    isExplainMode = isExplainMode,
                    onInspectFeature = onInspectFeature,
                    onUpdateProfile = onUpdateProfile
                )
            }

            // Wall Protection Tier Selector
            item {
                WallProtectionTierCard(
                    profile = profile,
                    isExplainMode = isExplainMode,
                    onInspectFeature = onInspectFeature,
                    onApplyWallTier = onApplyWallTier
                )
            }

            // Target Profile, APK Signer Config & Encryption Keys
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.step2Title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = profile.appName,
                                onValueChange = { v -> onUpdateProfile { it.copy(appName = v) } },
                                label = { Text("App Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("protector_app_name_input")
                            )
                            OutlinedTextField(
                                value = profile.packageName,
                                onValueChange = { v -> onUpdateProfile { it.copy(packageName = v) } },
                                label = { Text("Package ID") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("protector_package_input")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Sign Output APK (v1 JAR + v2 Block)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ZipAligns resources.arsc/.so & applies RSA-2048 cert",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ExplainInfoIconButton(
                                contentDescription = "Explain Auto-Sign Output APK",
                                testTag = "info_sign_output_apk",
                                onClick = { onInspectFeature(FeatureIds.SIGN_OUTPUT_APK) }
                            )
                            Switch(
                                checked = profile.signOutputApk,
                                onCheckedChange = { c ->
                                    if (isExplainMode) {
                                        onInspectFeature(FeatureIds.SIGN_OUTPUT_APK)
                                    } else {
                                        SoundFxManager.playToggle(c)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateProfile { it.copy(signOutputApk = c) }
                                    }
                                },
                                modifier = Modifier.testTag("toggle_sign_output_apk")
                            )
                        }

                        if (profile.signOutputApk) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = profile.signerCommonName,
                                    onValueChange = { v -> onUpdateProfile { it.copy(signerCommonName = v) } },
                                    label = { Text("Signer CN") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = profile.signerOrg,
                                    onValueChange = { v -> onUpdateProfile { it.copy(signerOrg = v) } },
                                    label = { Text("Organization") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Disguise Packed classes.dex Vault As:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            ExplainInfoIconButton(
                                contentDescription = "Explain Disguise Format",
                                testTag = "info_disguise_format",
                                onClick = { onInspectFeature(FeatureIds.DISGUISE_FORMAT) }
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(".png", ".dat", ".bin").forEach { ext ->
                                FilterChip(
                                    selected = profile.dexDisguiseExtension == ext,
                                    onClick = {
                                        SoundFxManager.playToggle(true)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateProfile { it.copy(dexDisguiseExtension = ext) }
                                    },
                                    label = { Text(ext) },
                                    modifier = Modifier.testTag("forge_disguise_${ext.removePrefix(".")}")
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.protectorPassphrase,
                            onValueChange = { onUpdateProtectorInputs(it, uiState.protectorSecretKey) },
                            label = { Text("AES-256-GCM Master Passphrase") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("protector_passphrase_input")
                        )

                        OutlinedTextField(
                            value = uiState.protectorSecretKey,
                            onValueChange = { onUpdateProtectorInputs(uiState.protectorPassphrase, it) },
                            label = { Text("Sensitive API Key / Secret to Hide") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("protector_secret_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = profile.expectedSignatureSha256,
                                onValueChange = { v -> onUpdateProfile { it.copy(expectedSignatureSha256 = v) } },
                                label = { Text("Signing SHA-256 Lock (Optional)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("protector_sha_input")
                            )
                            FilledTonalButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAdoptDeviceCert()
                                },
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("protector_adopt_cert_button")
                            ) {
                                Icon(Icons.Rounded.Fingerprint, contentDescription = "Use device cert")
                            }
                        }
                    }
                }
            }

            // Expandable 10-Layer Suite, Resources Confusing, and Configuration Cards
            item {
                ResourcesAndConfigAccordionCard(
                    profile = profile,
                    isExplainMode = isExplainMode,
                    onInspectFeature = onInspectFeature,
                    onUpdateProfile = onUpdateProfile,
                    onSelectAllLayers = onSelectAllLayers
                )
            }

            if (uiState.protectorError != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.protectorError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Primary Run Action
            item {
                val isApkSelected = uiState.selectedInputName.lowercase().endsWith(".apk") && profile.signOutputApk
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isExplainMode) onInspectFeature(FeatureIds.PIPELINE_FORGE) else onRunProtection()
                        },
                        enabled = !uiState.isProtecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("run_protection_pipeline_button")
                    ) {
                        Icon(Icons.Rounded.EnhancedEncryption, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                uiState.isProtecting -> "Encrypting & Signing..."
                                isApkSelected -> strings.protectAndSignBtn
                                else -> strings.encryptBuildBtn
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                    }
                    ExplainInfoIconButton(
                        contentDescription = "Explain Protect & Sign Pipeline",
                        testTag = "info_pipeline_forge",
                        onClick = { onInspectFeature(FeatureIds.PIPELINE_FORGE) }
                    )
                }
            }

            if (uiState.isProtecting) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = uiState.protectionSteps.isNotEmpty(),
                    enter = fadeIn() + expandVertically()
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Forge & Signing Pipeline Log",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            uiState.protectionSteps.forEach { step ->
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = uiState.protectedOutput != null,
                    enter = fadeIn() + expandVertically()
                ) {
                    uiState.protectedOutput?.let { out ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (out.signedVerification?.v2RsaVerified == true) {
                                            Icons.Rounded.VerifiedUser
                                        } else {
                                            Icons.Rounded.CheckCircle
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (out.signedVerification != null) {
                                            "Protected & Signed APK Ready!"
                                        } else {
                                            "Protected Output Forged!"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                val sigInfo = out.signedVerification?.let { sig ->
                                    "\nv1 JAR: ${if (sig.hasV1JarSignature) "VERIFIED" else "NO"} • v2 Block: ${if (sig.v2RsaVerified) "RSA-2048 VERIFIED" else "NO"}\n" +
                                        "Cert SHA-256: ${sig.certSha256Colon}"
                                } ?: ""

                                val stubLine = if (out.stubDexInjected) {
                                    "\nStub DEX: INJECTED (${out.detectedPackageName}) • AXML Patched: YES"
                                } else {
                                    ""
                                }

                                Text(
                                    text = "Output: ${out.suggestedPrimaryName} (${out.primarySizeBytes / 1024} KB)\n" +
                                        "Packed DEX: ${out.packedDexCount} ${out.disguisedDexNames} • Stripped Meta: ${out.strippedMetadataCount}$stubLine$sigInfo",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (out.primaryMimeType == "application/vnd.android.package-archive") {
                                            apkOutputSaver.launch(out.suggestedPrimaryName)
                                        } else {
                                            zipOutputSaver.launch(out.suggestedPrimaryName)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("save_protected_output_button")
                                ) {
                                    Icon(Icons.Rounded.SaveAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save ${out.suggestedPrimaryName}")
                                }

                                if (out.companionKitFile != null && out.suggestedKitName != null) {
                                    OutlinedButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            companionKitSaver.launch(out.suggestedKitName)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("save_companion_kit_button")
                                    ) {
                                        Icon(Icons.Rounded.FolderZip, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Save Developer Kit (${out.suggestedKitName})")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Official Community Channels (YouTube & Telegram)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Bloody Team Official Channels",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tutorials, updates & community support from @BLOODYTEAMOFFICIAL.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    SoundFxManager.playClick()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(YOUTUBE_CHANNEL_URL))
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("youtube_channel_button")
                            ) {
                                Icon(Icons.Rounded.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("YouTube", maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = {
                                    SoundFxManager.playClick()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_CHANNEL_URL))
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("telegram_channel_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Telegram", maxLines = 1)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

package com.example.ui.screens

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
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.SoundFxManager
import com.example.security.ProtectionProfile
import com.example.security.WallProtectionTier
import com.example.ui.ShieldUiState
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun ProtectorStudioScreen(
    uiState: ShieldUiState,
    profile: ProtectionProfile,
    onPickInputArchive: (Uri) -> Unit,
    onLoadSelfApk: () -> Unit,
    onUpdateProfile: ((ProtectionProfile) -> ProtectionProfile) -> Unit,
    onApplyWallTier: (WallProtectionTier) -> Unit,
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
    val haptic = LocalHapticFeedback.current

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
                                text = "Bloody Team Apk Shield",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Select any APK or Sketchware ZIP, customize your protection profile & theme, and export a ZipAligned v1+v2 signed APK + JNI/Stub kit.",
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
                                    Text("Random Design", maxLines = 1)
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
                                    Text("Select Theme", maxLines = 1)
                                }
                            }
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
                            text = "Step 1 • Select Input Application (.apk / .zip)",
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
                                Text("Select APK / File", maxLines = 1)
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
                                Text("Use This APK", maxLines = 1)
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
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Loaded: ${uiState.selectedInputName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "Size: ${info.totalArchiveSizeBytes / 1024} KB • Entries: ${info.totalEntries} • DEX: ${info.dexReports.size} • Assets: ${info.assetEntriesCount}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
                    onUpdateProfile = onUpdateProfile
                )
            }

            // Wall Protection Tier Selector
            item {
                WallProtectionTierCard(
                    profile = profile,
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
                            text = "Step 2 • APK Signing Identity & Disguise Keys",
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
                            Switch(
                                checked = profile.signOutputApk,
                                onCheckedChange = { c ->
                                    SoundFxManager.playToggle(c)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateProfile { it.copy(signOutputApk = c) }
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

                        Text(
                            text = "Disguise Packed classes.dex Vault As:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRunProtection()
                    },
                    enabled = !uiState.isProtecting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("run_protection_pipeline_button")
                ) {
                    Icon(Icons.Rounded.EnhancedEncryption, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            uiState.isProtecting -> "Encrypting & Signing..."
                            isApkSelected -> "Protect & Sign Output APK"
                            else -> "Encrypt & Build Protected Output"
                        },
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
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

                                Text(
                                    text = "Output: ${out.suggestedPrimaryName} (${out.primarySizeBytes / 1024} KB)\n" +
                                        "Packed DEX: ${out.packedDexCount} ${out.disguisedDexNames} • Stripped Meta: ${out.strippedMetadataCount}$sigInfo",
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

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

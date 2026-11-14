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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.SoundFxManager
import com.example.security.ProtectionProfile
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
    onSelectAllLayers: (Boolean) -> Unit,
    onAdoptDeviceCert: () -> Unit,
    onUpdateProtectorInputs: (passphrase: String, secretKey: String) -> Unit,
    onRunProtection: () -> Unit,
    onSaveProtectedOutput: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val inputPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onPickInputArchive(uri)
    }

    val outputSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) onSaveProtectedOutput(uri)
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
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
                                        listOf(Color(0xBB09090D), Color(0xEC09090D))
                                    )
                                )
                        )
                        Column(modifier = Modifier.padding(18.dp)) {
                            StatusPill(
                                text = "${profile.enabledLayersCount}/10 LAYERS • DEX PACKER",
                                isPositive = profile.enabledLayersCount >= 6
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Bloody Team Forge",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Load .apk or Sketchware .zip, pack classes.dex into disguised assets with JNI + InMemoryDexClassLoader, and export your hardened bundle.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD1D5DB)
                            )
                        }
                    }
                }
            }

            // STEP 1: Input Application
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
                            text = "Step 1 • Input Application (.apk / .zip)",
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
                                Text("Pick APK / Zip", maxLines = 1)
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

            // STEP 2: Target Details, Disguise Extension & Keys
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
                            text = "Step 2 • Target Profile, DEX Disguise & Keys",
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

                        Text(
                            text = "Disguise Packed classes.dex As:",
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
                                label = { Text("Signing SHA-256 Lock") },
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

            // STEP 3: Select Security Options
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Step 3 • Security Layers (${profile.enabledLayersCount}/10)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectAllLayers(profile.enabledLayersCount < 10)
                                },
                                modifier = Modifier.testTag("protector_toggle_all_button")
                            ) {
                                Text(if (profile.enabledLayersCount < 10) "Select All" else "Clear All")
                            }
                        }

                        SecurityOptionCheckRow(
                            title = "1. DEX Packing & 65K Method Guard",
                            subtitle = "Encrypts classes*.dex into disguised ${profile.dexDisguiseExtension} assets",
                            checked = profile.dexBytecodeGuard,
                            testTag = "opt_dex",
                            onCheckedChange = { c ->
                                SoundFxManager.playToggle(c)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateProfile { it.copy(dexBytecodeGuard = c) }
                            }
                        )
                        SecurityOptionCheckRow(
                            title = "Native C++ JNI + InMemoryDexClassLoader Stub",
                            subtitle = "Generates Phase B RAM-only loader & Manifest stub patch",
                            checked = profile.nativeJniStubPacking,
                            testTag = "opt_native_stub",
                            onCheckedChange = { c ->
                                SoundFxManager.playToggle(c)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateProfile { it.copy(nativeJniStubPacking = c) }
                            }
                        )
                        SecurityOptionCheckRow(
                            title = "2. Asset & Resource AES-256-GCM",
                            subtitle = "Encrypts all assets/*.json/media into DXS1 .enc files",
                            checked = profile.assetAes256Encryption,
                            testTag = "opt_asset",
                            onCheckedChange = { c ->
                                SoundFxManager.playToggle(c)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateProfile { it.copy(assetAes256Encryption = c) }
                            }
                        )
                        SecurityOptionCheckRow(
                            title = "3. Anti-Tamper & Signature Lock",
                            subtitle = "Blocks re-signed or repackaged APKs",
                            checked = profile.antiTamperSignature,
                            testTag = "opt_tamper",
                            onCheckedChange = { c ->
                                SoundFxManager.playToggle(c)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateProfile { it.copy(antiTamperSignature = c) }
                            }
                        )
                        SecurityOptionCheckRow(
                            title = "4. Anti-Debug & Frida / Xposed Shield",
                            subtitle = "TracerPid + /proc/self/maps hook & debugger blocker",
                            checked = profile.antiDebugFridaShield,
                            testTag = "opt_frida",
                            onCheckedChange = { c ->
                                SoundFxManager.playToggle(c)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateProfile { it.copy(antiDebugFridaShield = c) }
                            }
                        )
                        SecurityOptionCheckRow(
                            title = "5. String & API Key Obfuscation",
                            subtitle = "XOR + bit-rotation vault (zero plaintext in DEX)",
                            checked = profile.stringMetadataObfuscation,
                            testTag = "opt_strings",
                            onCheckedChange = { c ->
                                SoundFxManager.playToggle(c)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateProfile { it.copy(stringMetadataObfuscation = c) }
                            }
                        )
                        SecurityOptionCheckRow(
                            title = "6. Zero-Crash & ANR Exception Guard",
                            subtitle = "Global thread handler preventing runtime crashes",
                            checked = profile.zeroCrashExceptionGuard,
                            testTag = "opt_crash",
                            onCheckedChange = { c ->
                                SoundFxManager.playToggle(c)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateProfile { it.copy(zeroCrashExceptionGuard = c) }
                            }
                        )
                        SecurityOptionCheckRow(
                            title = "7. Root & Magisk Environment Shield",
                            subtitle = "Blocks su binaries, Magisk & test-keys environments",
                            checked = profile.rootMagiskDetection,
                            testTag = "opt_root",
                            onCheckedChange = { c ->
                                SoundFxManager.playToggle(c)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateProfile { it.copy(rootMagiskDetection = c) }
                            }
                        )
                        SecurityOptionCheckRow(
                            title = "8. Emulator & Virtual-Clone Blocker",
                            subtitle = "Blocks QEMU/emulators & Parallel Space app cloners",
                            checked = profile.emulatorCloneShield,
                            testTag = "opt_emulator",
                            onCheckedChange = { c ->
                                SoundFxManager.playToggle(c)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateProfile { it.copy(emulatorCloneShield = c) }
                            }
                        )
                        SecurityOptionCheckRow(
                            title = "9. SSL Pinning & Proxy/VPN Sniffer Block",
                            subtitle = "SPKI cert pinning + HTTP proxy & tun0/ppp0 block",
                            checked = profile.sslPinningProxyBlock,
                            testTag = "opt_ssl",
                            onCheckedChange = { c ->
                                SoundFxManager.playToggle(c)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateProfile { it.copy(sslPinningProxyBlock = c) }
                            }
                        )
                        SecurityOptionCheckRow(
                            title = "10. Screenshot & Tapjacking Shield",
                            subtitle = "Enforces FLAG_SECURE + obscured touch filtering",
                            checked = profile.screenTapjackingShield,
                            testTag = "opt_screen",
                            onCheckedChange = { c ->
                                SoundFxManager.playToggle(c)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateProfile { it.copy(screenTapjackingShield = c) }
                            }
                        )
                    }
                }
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

            // STEP 4: Run Protection & Download Output
            item {
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
                        text = if (uiState.isProtecting) "Forging Protected App..." else "Encrypt & Build Protected Output",
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
                                text = "Forge Pipeline Execution Log",
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
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Protected Output Forged!",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Archive: ${out.suggestedOutputName} (${out.outputSizeBytes / 1024} KB)\n" +
                                        "Packed DEX -> Disguised: ${out.packedDexCount} ${out.disguisedDexNames}\n" +
                                        "Encrypted Assets: ${out.encryptedAssetsCount} • Security Files: ${out.injectedSecurityFilesCount}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        outputSaver.launch(out.suggestedOutputName)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("save_protected_output_button")
                                ) {
                                    Icon(Icons.Rounded.SaveAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save ${out.suggestedOutputName}")
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

@Composable
private fun SecurityOptionCheckRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
        Spacer(modifier = Modifier.width(6.dp))
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
    }
}

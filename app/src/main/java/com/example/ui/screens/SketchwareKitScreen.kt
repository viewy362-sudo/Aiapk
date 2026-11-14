package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.security.ProtectionProfile
import com.example.security.SketchwareKitGenerator
import com.example.ui.components.MonospaceCodePanel

@Composable
fun SketchwareKitScreen(
    profile: ProtectionProfile,
    onUpdateProfile: ((ProtectionProfile) -> ProtectionProfile) -> Unit,
    onAdoptDeviceCert: () -> Unit,
    onBuildExportBundle: () -> String,
    onCopiedNotice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val guardJava = remember(profile) { SketchwareKitGenerator.generateSketchwareJavaGuard(profile) }
    val assetJava = remember(profile) { SketchwareKitGenerator.generateAssetVaultHelperJava(profile) }
    val proguardRules = remember(profile) { SketchwareKitGenerator.generateProguardRules(profile) }

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
                Text(
                    text = "Sketchware Pro Protection Kit",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Configure your target app and export zero-dependency Dalvik/ART Java classes and R8/ProGuard rules.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Target Application Profile",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = profile.appName,
                            onValueChange = { v -> onUpdateProfile { it.copy(appName = v) } },
                            label = { Text("App Name (e.g., BLOODY TEAM)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_app_name_input")
                        )

                        OutlinedTextField(
                            value = profile.packageName,
                            onValueChange = { v -> onUpdateProfile { it.copy(packageName = v) } },
                            label = { Text("Package Identifier") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_package_input")
                        )

                        OutlinedTextField(
                            value = profile.expectedSignatureSha256,
                            onValueChange = { v -> onUpdateProfile { it.copy(expectedSignatureSha256 = v) } },
                            label = { Text("Expected Signing Certificate SHA-256") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("target_sha256_input")
                        )

                        OutlinedButton(
                            onClick = onAdoptDeviceCert,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("adopt_device_cert_button")
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Current Build Signing SHA-256")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Mandatory Security Layers",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        LayerToggleRow(
                            label = "DEX Bytecode & 65K Method Guard",
                            checked = profile.dexBytecodeGuard,
                            testTag = "toggle_layer_dex",
                            onCheckedChange = { c -> onUpdateProfile { it.copy(dexBytecodeGuard = c) } }
                        )
                        LayerToggleRow(
                            label = "Asset & Resource AES-256-GCM",
                            checked = profile.assetAes256Encryption,
                            testTag = "toggle_layer_asset",
                            onCheckedChange = { c -> onUpdateProfile { it.copy(assetAes256Encryption = c) } }
                        )
                        LayerToggleRow(
                            label = "Anti-Tamper & Signature Verification",
                            checked = profile.antiTamperSignature,
                            testTag = "toggle_layer_tamper",
                            onCheckedChange = { c -> onUpdateProfile { it.copy(antiTamperSignature = c) } }
                        )
                        LayerToggleRow(
                            label = "Anti-Debug & Frida Hook Shield",
                            checked = profile.antiDebugFridaShield,
                            testTag = "toggle_layer_antidebug",
                            onCheckedChange = { c -> onUpdateProfile { it.copy(antiDebugFridaShield = c) } }
                        )
                        LayerToggleRow(
                            label = "String & Metadata Obfuscation",
                            checked = profile.stringMetadataObfuscation,
                            testTag = "toggle_layer_obfuscate",
                            onCheckedChange = { c -> onUpdateProfile { it.copy(stringMetadataObfuscation = c) } }
                        )
                        LayerToggleRow(
                            label = "Zero-Crash Exception Guard",
                            checked = profile.zeroCrashExceptionGuard,
                            testTag = "toggle_layer_zerocrash",
                            onCheckedChange = { c -> onUpdateProfile { it.copy(zeroCrashExceptionGuard = c) } }
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val bundleText = onBuildExportBundle()
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "DexShield Kit - ${profile.appName}")
                            putExtra(Intent.EXTRA_TEXT, bundleText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Export Sketchware Security Kit"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("export_sketchware_kit_button")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Complete Sketchware Security Kit")
                }
            }

            item {
                MonospaceCodePanel(
                    title = "DexShieldGuard.java (Sketchware Custom Class)",
                    code = guardJava,
                    copyTestTag = "copy_dexshield_guard_java_button",
                    onCopied = { onCopiedNotice("Copied DexShieldGuard.java") }
                )
            }

            item {
                MonospaceCodePanel(
                    title = "AssetVaultHelper.java (AES-256-GCM Loader)",
                    code = assetJava,
                    copyTestTag = "copy_asset_vault_java_button",
                    onCopied = { onCopiedNotice("Copied AssetVaultHelper.java") }
                )
            }

            item {
                MonospaceCodePanel(
                    title = "proguard-rules.pro (R8 Hardening)",
                    code = proguardRules,
                    copyTestTag = "copy_proguard_rules_button",
                    onCopied = { onCopiedNotice("Copied proguard-rules.pro") }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun LayerToggleRow(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

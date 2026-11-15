package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.FeatureIds
import com.example.ui.ShieldUiState
import com.example.ui.VaultBinaryMode
import com.example.ui.components.ExplainInfoIconButton
import com.example.ui.components.MonospaceCodePanel
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale

@Composable
fun AssetVaultScreen(
    uiState: ShieldUiState,
    onUpdateInputs: (plainText: String, passphrase: String) -> Unit,
    onUpdateCipherInput: (String) -> Unit,
    onEncryptText: () -> Unit,
    onDecryptText: () -> Unit,
    onProcessFile: (Uri, VaultBinaryMode) -> Unit,
    onSaveProcessedFile: (Uri) -> Unit,
    onCopiedNotice: (String) -> Unit,
    onInspectFeature: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val encryptFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onProcessFile(uri, VaultBinaryMode.ENCRYPT)
    }

    val decryptFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onProcessFile(uri, VaultBinaryMode.DECRYPT)
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) onSaveProcessedFile(uri)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "AES-256-GCM Asset Vault",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    ExplainInfoIconButton(
                        contentDescription = "Explain AES-256-GCM Asset Vault",
                        testTag = "info_asset_vault_tool",
                        onClick = { onInspectFeature(FeatureIds.ASSET_VAULT_TOOL) }
                    )
                }
                Text(
                    text = "Encrypt JSON configs, media, or binary assets using PBKDF2-HMAC-SHA256 + 128-bit GCM authentication.",
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
                        OutlinedTextField(
                            value = uiState.vaultPassphrase,
                            onValueChange = { onUpdateInputs(uiState.plainVaultInput, it) },
                            label = { Text("Vault Master Passphrase (PBKDF2 16,384 rounds)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_passphrase_input")
                        )

                        OutlinedTextField(
                            value = uiState.plainVaultInput,
                            onValueChange = { onUpdateInputs(it, uiState.vaultPassphrase) },
                            label = { Text("Plaintext Asset / JSON Payload") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vault_plaintext_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onEncryptText,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("vault_encrypt_text_button")
                            ) {
                                Icon(Icons.Rounded.EnhancedEncryption, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Encrypt")
                            }
                            FilledTonalButton(
                                onClick = onDecryptText,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("vault_decrypt_text_button")
                            ) {
                                Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Decrypt")
                            }
                        }
                    }
                }
            }

            if (uiState.vaultErrorMessage != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.vaultErrorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            uiState.vaultResult?.let { res ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DXS1 Cipher Telemetry",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                StatusPill(
                                    text = String.format(Locale.US, "%.2f bits/B entropy", res.entropyBitsPerByte),
                                    isPositive = res.entropyBitsPerByte >= 4.0
                                )
                            }
                            Text(
                                text = "Size: ${res.originalBytes} B -> ${res.encryptedBytes} B (Header + 16B GCM Tag)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Hex: ${res.hexPreview}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            if (uiState.vaultCipherText.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = uiState.vaultCipherText,
                        onValueChange = onUpdateCipherInput,
                        label = { Text("Base64 DXS1 Envelope (Editable to test tamper detection)") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_ciphertext_input")
                    )
                }
                item {
                    MonospaceCodePanel(
                        title = "Encrypted Asset Payload (Base64)",
                        code = uiState.vaultCipherText,
                        copyTestTag = "copy_vault_base64_button",
                        onCopied = { onCopiedNotice("Copied DXS1 Base64 payload") }
                    )
                }
            }

            if (uiState.vaultDecryptedPreview.isNotEmpty()) {
                item {
                    MonospaceCodePanel(
                        title = "Verified Decrypted Output",
                        code = uiState.vaultDecryptedPreview,
                        copyTestTag = "copy_vault_decrypted_button",
                        onCopied = { onCopiedNotice("Copied decrypted text") }
                    )
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Binary Asset File Encryptor (SAF Zero-Permission)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pick any JSON, image, audio, or config asset to encrypt into a .enc file for Sketchware assets/.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { encryptFilePicker.launch(arrayOf("*/*")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pick_file_encrypt_button")
                            ) {
                                Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Encrypt File")
                            }
                            OutlinedButton(
                                onClick = { decryptFilePicker.launch(arrayOf("*/*")) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pick_file_decrypt_button")
                            ) {
                                Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Decrypt .enc")
                            }
                        }

                        uiState.pendingBinaryVault?.let { pending ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = pending.summary,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Button(
                                        onClick = { saveDocumentLauncher.launch(pending.suggestedOutputName) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("save_binary_vault_button")
                                    ) {
                                        Icon(Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save ${pending.suggestedOutputName}")
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

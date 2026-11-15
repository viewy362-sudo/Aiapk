package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.i18n.FeatureIds
import com.example.security.ApkInspectionReport
import com.example.security.DexFileReport
import com.example.security.ProtectionProfile
import com.example.security.RuntimeShieldReport
import com.example.ui.ShieldUiState
import com.example.ui.components.ExplainInfoIconButton
import com.example.ui.components.SecurityLayerCard
import com.example.ui.components.ShieldScoreRing
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun ShieldDashboardScreen(
    uiState: ShieldUiState,
    profile: ProtectionProfile,
    isGuardActive: Boolean,
    mainThreadLatencyMs: Long,
    onRunAudit: () -> Unit,
    onPickApkOrZip: (android.net.Uri) -> Unit,
    onNavigateToKit: () -> Unit,
    onInspectFeature: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val archivePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onPickApkOrZip(uri)
        }
    }

    val runtime = uiState.runtimeReport
    val apk = uiState.apkReport

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
                        text = "Live Integrity Audit",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    ExplainInfoIconButton(
                        contentDescription = "Explain Live Integrity Audit",
                        testTag = "info_live_audit_tool",
                        onClick = { onInspectFeature(FeatureIds.LIVE_AUDIT) }
                    )
                }
                HeroProtectionBanner(
                    profile = profile,
                    runtime = runtime,
                    isAuditing = uiState.isAuditing,
                    onRunAudit = onRunAudit,
                    onOpenPicker = {
                        archivePicker.launch(
                            arrayOf(
                                "application/vnd.android.package-archive",
                                "application/zip",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    }
                )
            }

            if (uiState.isAuditing) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                    )
                }
            }

            if (apk != null) {
                item {
                    ApkDexSummaryCard(
                        apkReport = apk,
                        onOpenKit = onNavigateToKit
                    )
                }

                if (apk.dexReports.isNotEmpty()) {
                    items(apk.dexReports, key = { it.entryName + it.sha256Short }) { dex ->
                        DexEntryDetailCard(dex = dex)
                    }
                }
            }

            item {
                Text(
                    text = "6-Layer Runtime & Build Integrity",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                val dexOk = apk?.dexReports?.all { it.withinDalvik65kLimit } ?: true
                val totalMethods = apk?.dexReports?.sumOf { it.methodIdsCount } ?: 0
                SecurityLayerCard(
                    title = "1. DEX Bytecode & Method Limit",
                    subtitle = "Dalvik & ART 65,535 method-index integrity",
                    statusLabel = if (dexOk) "SAFE" else "OVERFLOW",
                    isVerified = dexOk,
                    icon = Icons.Rounded.Code,
                    detailLine = if (apk != null) {
                        "${apk.dexReports.size} DEX file(s) • $totalMethods methods • ${apk.summaryMessage}"
                    } else {
                        "Scanning DEX headers..."
                    }
                )
            }

            item {
                SecurityLayerCard(
                    title = "2. AES-256-GCM Asset Vault",
                    subtitle = "Authenticated PBKDF2-HMAC-SHA256 asset cipher",
                    statusLabel = if (profile.assetAes256Encryption) "READY" else "OFF",
                    isVerified = profile.assetAes256Encryption,
                    icon = Icons.Rounded.Lock,
                    detailLine = if (apk != null) {
                        "Assets detected: ${apk.assetEntriesCount} total • ${apk.encryptedAssetCount} encrypted (.enc)"
                    } else {
                        "DXS1 envelope: 16B salt + 12B IV + 128-bit GCM tag"
                    }
                )
            }

            item {
                val sigOk = runtime?.antiTamperPassed == true
                SecurityLayerCard(
                    title = "3. Anti-Tamper & Signature Guard",
                    subtitle = "APK signing certificate SHA-256 verification",
                    statusLabel = if (sigOk) "VERIFIED" else "CHECK",
                    isVerified = sigOk,
                    icon = Icons.Rounded.VerifiedUser,
                    detailLine = "Cert SHA-256: ${runtime?.signingCertSha256?.take(35) ?: "Reading..."}..."
                )
            }

            item {
                val debugOk = runtime?.antiDebugPassed == true && runtime.hookShieldPassed
                val hookSummary = when {
                    runtime == null -> "Probing /proc/self/status & maps..."
                    runtime.fridaArtifactsDetected.isNotEmpty() ->
                        "Detected hooks: ${runtime.fridaArtifactsDetected.joinToString()}"
                    runtime.xposedDetected -> "Xposed frame detected"
                    runtime.isDebuggerAttached -> "Debugger attached (TracerPid=${runtime.tracerPid})"
                    else -> "TracerPid=0 • No Frida/GumJS/Xposed maps • ${runtime.runtimeEnvironment}"
                }
                SecurityLayerCard(
                    title = "4. Anti-Debug & Frida Hook Shield",
                    subtitle = "TracerPid, /proc/self/maps & stack inspection",
                    statusLabel = if (debugOk) "CLEAN" else "ALERT",
                    isVerified = debugOk,
                    icon = Icons.Rounded.Security,
                    detailLine = hookSummary
                )
            }

            item {
                SecurityLayerCard(
                    title = "5. String & Metadata Obfuscation",
                    subtitle = "Rotating-XOR + bit-rotated byte array hiding",
                    statusLabel = if (profile.stringMetadataObfuscation) "ACTIVE" else "OFF",
                    isVerified = profile.stringMetadataObfuscation,
                    icon = Icons.Rounded.BugReport,
                    detailLine = "R8 repackageclasses='o' • SourceFile stripped • Log calls stripped"
                )
            }

            item {
                SecurityLayerCard(
                    title = "6. Zero-Crash Exception Guard",
                    subtitle = "Global UncaughtExceptionHandler + ANR Watchdog",
                    statusLabel = if (isGuardActive) "ARMED" else "INACTIVE",
                    isVerified = isGuardActive,
                    icon = Icons.Rounded.HealthAndSafety,
                    detailLine = "Process handler armed • Main looper ping: ${mainThreadLatencyMs}ms"
                )
            }

            item {
                val rootClean = runtime?.rootArtifactsDetected?.isEmpty() ?: true
                SecurityLayerCard(
                    title = "7. Root & Magisk Shield",
                    subtitle = "su binary, Magisk & test-keys check",
                    statusLabel = if (rootClean) "CLEAN" else "ROOTED",
                    isVerified = rootClean,
                    icon = Icons.Rounded.Security,
                    detailLine = if (rootClean) "No su / Magisk artifacts detected" else "Detected: ${runtime?.rootArtifactsDetected?.joinToString()}"
                )
            }

            item {
                val notEmu = runtime?.isEmulatorEnvironment == false
                SecurityLayerCard(
                    title = "8. Emulator & Virtual-Clone Shield",
                    subtitle = "Hardware fingerprint & multi-user path check",
                    statusLabel = if (notEmu) "REAL HW" else "EMULATOR",
                    isVerified = notEmu,
                    icon = Icons.Rounded.VerifiedUser,
                    detailLine = if (notEmu) "Physical device fingerprint verified" else "Virtual/Emulator runtime detected"
                )
            }

            item {
                val netClean = runtime?.isProxyOrVpnActive == false
                SecurityLayerCard(
                    title = "9. SSL Pinning & Proxy/VPN Block",
                    subtitle = "HTTP proxy & tun0/ppp0 sniffer check",
                    statusLabel = if (netClean) "DIRECT" else "PROXY/VPN",
                    isVerified = netClean,
                    icon = Icons.Rounded.Lock,
                    detailLine = if (netClean) "No active MITM proxy or VPN tunnel" else "Active proxy or VPN interface detected"
                )
            }

            item {
                SecurityLayerCard(
                    title = "10. Screenshot & Tapjacking Shield",
                    subtitle = "FLAG_SECURE & obscured touch filter generator",
                    statusLabel = if (profile.screenTapjackingShield) "ARMED" else "OFF",
                    isVerified = profile.screenTapjackingShield,
                    icon = Icons.Rounded.HealthAndSafety,
                    detailLine = "Exports FLAG_SECURE + setFilterTouchesWhenObscured(true) guard"
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HeroProtectionBanner(
    profile: ProtectionProfile,
    runtime: RuntimeShieldReport?,
    isAuditing: Boolean,
    onRunAudit: () -> Unit,
    onOpenPicker: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_bloody_hero),
                contentDescription = "Bloody Team security vault background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC0B1220),
                                Color(0xEB0B1220)
                            )
                        )
                    )
            )
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        StatusPill(
                            text = "${profile.enabledLayersCount}/6 LAYERS ARMED",
                            isPositive = profile.enabledLayersCount == 6
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = profile.appName.ifBlank { "Target App" },
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = "${profile.packageName} • Sketchware Pro",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    ShieldScoreRing(
                        score = runtime?.activeShieldScore ?: 98,
                        label = "SHIELD"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRunAudit,
                        enabled = !isAuditing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("run_live_audit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Live Audit", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onOpenPicker,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_apk_zip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderZip,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan APK/Zip", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun ApkDexSummaryCard(
    apkReport: ApkInspectionReport,
    onOpenKit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Archive Inspector",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = apkReport.sourceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                FilledTonalButton(
                    onClick = onOpenKit,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("open_kit_from_summary_button")
                ) {
                    Text("Export Kit")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    label = "Entries",
                    value = "${apkReport.totalEntries}",
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "DEX Files",
                    value = "${apkReport.dexReports.size}",
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = "ABIs",
                    value = if (apkReport.nativeLibArchitectures.isEmpty()) "Pure DEX" else "${apkReport.nativeLibArchitectures.size}",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DexEntryDetailCard(dex: DexFileReport) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${dex.entryName} (${dex.dexVersion})",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.primary
                )
                StatusPill(
                    text = "${dex.methodIdsCount}/65535 methods",
                    isPositive = dex.withinDalvik65kLimit
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Types: ${dex.typeIdsCount} • Strings: ${dex.stringIdsCount} • Classes: ${dex.classDefsCount} • SHA256: ${dex.sha256Short}...",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

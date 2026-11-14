package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.SoundFxManager
import com.example.security.DexPackerEngine
import com.example.security.ProtectionProfile
import com.example.ui.ShieldUiState
import com.example.ui.components.MonospaceCodePanel
import com.example.ui.components.StatusPill
import com.example.ui.theme.JetBrainsMonoFamily
import java.util.Locale

@Composable
fun DexPackerLabScreen(
    uiState: ShieldUiState,
    profile: ProtectionProfile,
    onUpdateProfile: ((ProtectionProfile) -> ProtectionProfile) -> Unit,
    onRunRamPackTest: () -> Unit,
    onCopiedNotice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val pulse = rememberInfiniteTransition(label = "flow_pulse")
    val arrowAlpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_alpha"
    )

    val jniCpp = remember(profile) { DexPackerEngine.generateNativeJniCpp(profile) }
    val cmakeTxt = remember { DexPackerEngine.generateCMakeLists() }
    val stubJava = remember(profile) { DexPackerEngine.generateStubApplicationJava(profile) }
    val manifestPatch = remember(profile) { DexPackerEngine.generateManifestStubPatch(profile) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "classes.dex Packer & JNI Lab",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Build-Time AES/XOR DEX disguise + Runtime NDK C++ RAM decryption & InMemoryDexClassLoader architecture.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Disguise Format & Live RAM Test
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Disguise Format & Live RAM Unpack Test",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(".png", ".dat", ".bin").forEach { ext ->
                                val selected = profile.dexDisguiseExtension == ext
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        SoundFxManager.playToggle(true)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateProfile { it.copy(dexDisguiseExtension = ext) }
                                    },
                                    label = { Text("Disguise as $ext") },
                                    modifier = Modifier.testTag("disguise_chip_${ext.removePrefix(".")}")
                                )
                            }
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRunRamPackTest()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("run_dex_ram_pack_test_button")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test DEX Pack + RAM Decrypt")
                        }

                        AnimatedVisibility(
                            visible = uiState.dexRamTest != null,
                            enter = fadeIn() + expandVertically()
                        ) {
                            uiState.dexRamTest?.let { test ->
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
                                            Text(
                                                text = "assets/${test.disguisedName}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            StatusPill(
                                                text = if (test.roundTripVerified) "RAM VERIFIED" else "FAILED",
                                                isPositive = test.roundTripVerified
                                            )
                                        }
                                        Text(
                                            text = "DEX ${test.originalSize}B -> Packed ${test.packedSize}B • Entropy ${String.format(Locale.US, "%.2f", test.entropyBits)} b/B • SHA256 ${test.sha256Short}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Phase A & Phase B Animated Architecture Breakdown
            item {
                PhasePipelineCard(
                    phaseTitle = "Phase A • Build-Time Packing Flow",
                    accentColor = MaterialTheme.colorScheme.primary,
                    arrowAlpha = arrowAlpha,
                    steps = listOf(
                        "1. Original Code Compilation -> classes.dex (Dalvik Bytecode)",
                        "2. AES-256-GCM + XOR Mask -> Encrypts DEX & strips plaintext magic",
                        "3. Asset Disguise -> Hides payload in assets/tex_atlas_0${profile.dexDisguiseExtension}",
                        "4. Stub + NDK (.so) -> Replaces entrypoint with DexStubApplication + JNI",
                        "5. Manifest Patch -> Boots Stub Application before real activities"
                    )
                )
            }

            item {
                PhasePipelineCard(
                    phaseTitle = "Phase B • Runtime In-Memory Loading",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    arrowAlpha = arrowAlpha,
                    steps = listOf(
                        "1. Stub Launches First -> attachBaseContext() & ZeroCrashGuard arming",
                        "2. Native Anti-Tamper -> C++ checks TracerPid=0 & /proc/self/maps (Frida/Xposed)",
                        "3. RAM-Only Decryption -> AAssetManager -> DirectByteBuffer (0 bytes written to disk)",
                        "4. InMemoryDexClassLoader -> Loads ByteBuffer directly on Android 8.0+ (API 26+)",
                        "5. Reflection Handover -> Swaps LoadedApk mClassLoader & starts real app"
                    )
                )
            }

            // Reality Check Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Security Reality Check & Play Policy",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "• Memory Dumping: Decrypted DEX still resides in RAM at runtime (Frida/GameGuardian can dump RAM unless anti-hook blocks them).\n" +
                                "• Google Play Policy: Never download external .dex/.so remotely; bundle all native code & assets inside the signed APK/AAB.\n" +
                                "• Standard Hardening: Pair with R8/ProGuard shrinking and keep sensitive API keys on a backend server.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                MonospaceCodePanel(
                    title = "dex_shield_jni.cpp (Android NDK C++ RAM Decryptor)",
                    code = jniCpp,
                    copyTestTag = "copy_jni_cpp_button",
                    onCopied = { onCopiedNotice("Copied dex_shield_jni.cpp") }
                )
            }

            item {
                MonospaceCodePanel(
                    title = "CMakeLists.txt (NDK Build Script)",
                    code = cmakeTxt,
                    copyTestTag = "copy_cmake_button",
                    onCopied = { onCopiedNotice("Copied CMakeLists.txt") }
                )
            }

            item {
                MonospaceCodePanel(
                    title = "DexStubApplication.java (InMemoryDexClassLoader)",
                    code = stubJava,
                    copyTestTag = "copy_stub_java_button",
                    onCopied = { onCopiedNotice("Copied DexStubApplication.java") }
                )
            }

            item {
                MonospaceCodePanel(
                    title = "AndroidManifest_stub_patch.xml",
                    code = manifestPatch,
                    copyTestTag = "copy_manifest_patch_button",
                    onCopied = { onCopiedNotice("Copied Manifest patch") }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun PhasePipelineCard(
    phaseTitle: String,
    accentColor: androidx.compose.ui.graphics.Color,
    arrowAlpha: Float,
    steps: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (phaseTitle.contains("Phase A")) Icons.Rounded.Security else Icons.Rounded.Memory,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = phaseTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }

            steps.forEachIndexed { idx, step ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                if (idx < steps.lastIndex) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier
                                .size(16.dp)
                                .alpha(arrowAlpha)
                        )
                    }
                }
            }
        }
    }
}

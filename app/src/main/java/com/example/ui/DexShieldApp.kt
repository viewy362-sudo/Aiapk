package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AssetVaultScreen
import com.example.ui.screens.ObfuscatorScreen
import com.example.ui.screens.ShieldDashboardScreen
import com.example.ui.screens.SketchwareKitScreen
import kotlinx.coroutines.launch

enum class ShieldTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    SHIELD("Shield", Icons.Rounded.Shield, Icons.Outlined.Shield, "nav_tab_shield"),
    VAULT("Vault", Icons.Rounded.Lock, Icons.Outlined.Lock, "nav_tab_vault"),
    LAB("Obfuscate", Icons.Rounded.Code, Icons.Outlined.Code, "nav_tab_obfuscate"),
    KIT("Sketchware", Icons.Rounded.Build, Icons.Outlined.Build, "nav_tab_kit")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexShieldApp(
    viewModel: DexShieldViewModel = viewModel()
) {
    var currentTab by rememberSaveable { mutableStateOf(ShieldTab.SHIELD) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val faultLog by viewModel.faultLog.collectAsStateWithLifecycle()
    val isGuardActive by viewModel.isGuardActive.collectAsStateWithLifecycle()
    val mainThreadLatencyMs by viewModel.mainThreadLatencyMs.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = currentTab != ShieldTab.SHIELD) {
        currentTab = ShieldTab.SHIELD
    }

    LaunchedEffect(uiState.statusToast) {
        val msg = uiState.statusToast
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusToast()
        }
    }

    val showToast: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "DexShield",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        ShieldTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        ShieldTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (currentTab) {
                        ShieldTab.SHIELD -> ShieldDashboardScreen(
                            uiState = uiState,
                            profile = profile,
                            isGuardActive = isGuardActive,
                            mainThreadLatencyMs = mainThreadLatencyMs,
                            onRunAudit = viewModel::runSelfAudit,
                            onPickApkOrZip = viewModel::inspectPickedArchive,
                            onNavigateToKit = { currentTab = ShieldTab.KIT }
                        )
                        ShieldTab.VAULT -> AssetVaultScreen(
                            uiState = uiState,
                            onUpdateInputs = viewModel::updateVaultInputs,
                            onUpdateCipherInput = viewModel::updateVaultCipherInput,
                            onEncryptText = viewModel::encryptVaultText,
                            onDecryptText = viewModel::decryptVaultText,
                            onProcessFile = viewModel::processBinaryFile,
                            onSaveProcessedFile = viewModel::saveBinaryVaultOutput,
                            onCopiedNotice = showToast
                        )
                        ShieldTab.LAB -> ObfuscatorScreen(
                            uiState = uiState,
                            faultLog = faultLog,
                            isGuardActive = isGuardActive,
                            mainThreadLatencyMs = mainThreadLatencyMs,
                            onUpdateObfuscator = viewModel::updateObfuscatorInput,
                            onGenerateObfuscation = viewModel::obfuscateString,
                            onTriggerCrashTest = viewModel::triggerCrashGuardSimulation,
                            onClearFaults = viewModel::clearFaultLogs,
                            onCopiedNotice = showToast
                        )
                        ShieldTab.KIT -> SketchwareKitScreen(
                            profile = profile,
                            onUpdateProfile = viewModel::updateProfile,
                            onAdoptDeviceCert = viewModel::adoptCurrentDeviceCert,
                            onBuildExportBundle = viewModel::buildCombinedExportBundle,
                            onCopiedNotice = showToast
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.EnhancedEncryption
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.SoundFxManager
import com.example.i18n.AppStrings
import com.example.i18n.UiStrings
import com.example.ui.components.FeatureExplanationSheet
import com.example.ui.components.ThemeStudioSheet
import com.example.ui.components.TopLanguagePickerButton
import com.example.ui.screens.AssetVaultScreen
import com.example.ui.screens.DexPackerLabScreen
import com.example.ui.screens.FeatureGuideScreen
import com.example.ui.screens.ObfuscatorScreen
import com.example.ui.screens.ProtectorStudioScreen
import com.example.ui.screens.ShieldDashboardScreen
import com.example.ui.screens.SketchwareKitScreen
import com.example.ui.theme.DexShieldTheme
import kotlinx.coroutines.launch

enum class ShieldTab(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    FORGE(Icons.Rounded.EnhancedEncryption, Icons.Outlined.EnhancedEncryption, "nav_tab_forge"),
    PACKER(Icons.Rounded.Memory, Icons.Outlined.Memory, "nav_tab_packer"),
    AUDIT(Icons.Rounded.Shield, Icons.Outlined.Shield, "nav_tab_audit"),
    VAULT(Icons.Rounded.Lock, Icons.Outlined.Lock, "nav_tab_vault"),
    KIT(Icons.Rounded.Build, Icons.Outlined.Build, "nav_tab_kit");

    fun label(strings: UiStrings): String = when (this) {
        FORGE -> strings.tabForge
        PACKER -> strings.tabPacker
        AUDIT -> strings.tabAudit
        VAULT -> strings.tabVault
        KIT -> strings.tabKit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexShieldApp(
    viewModel: DexShieldViewModel = viewModel()
) {
    var currentTab by rememberSaveable { mutableStateOf(ShieldTab.FORGE) }
    var kitSubTab by rememberSaveable { mutableIntStateOf(0) }
    var showThemeStudio by rememberSaveable { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val faultLog by viewModel.faultLog.collectAsStateWithLifecycle()
    val isGuardActive by viewModel.isGuardActive.collectAsStateWithLifecycle()
    val mainThreadLatencyMs by viewModel.mainThreadLatencyMs.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()

    val activeThemePreset = profile.resolvedThemePreset
    val activeLanguage = profile.resolvedLanguage
    val strings = AppStrings.get(activeLanguage)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !activeThemePreset.isDark
            insetsController.isAppearanceLightNavigationBars = !activeThemePreset.isDark
        }
    }

    DexShieldTheme(themePreset = activeThemePreset) {
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        BackHandler(enabled = !uiState.showFeatureGuide && currentTab != ShieldTab.FORGE) {
            SoundFxManager.playClick()
            currentTab = ShieldTab.FORGE
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

        val selectTab: (ShieldTab) -> Unit = { target ->
            if (uiState.showFeatureGuide) {
                viewModel.setShowFeatureGuide(false)
            }
            if (currentTab != target) {
                SoundFxManager.playClick()
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                currentTab = target
            }
        }

        if (showThemeStudio) {
            ThemeStudioSheet(
                currentPreset = activeThemePreset,
                onSelectPreset = { preset ->
                    viewModel.selectThemePreset(preset)
                },
                onRandomPreset = {
                    viewModel.randomizeThemePreset()
                },
                onDismiss = { showThemeStudio = false }
            )
        }

        uiState.inspectedFeatureId?.let { featureId ->
            FeatureExplanationSheet(
                featureId = featureId,
                profile = profile,
                onDismiss = viewModel::dismissFeatureExplanation,
                onToggleFeature = viewModel::toggleFeatureById
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = strings.appTitle,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        },
                        actions = {
                            TopLanguagePickerButton(
                                currentLanguage = activeLanguage,
                                onSelectLanguage = viewModel::selectLanguage
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.setShowFeatureGuide(!uiState.showFeatureGuide)
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("topbar_feature_guide_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MenuBook,
                                    contentDescription = strings.guideButtonLabel,
                                    tint = if (uiState.showFeatureGuide) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    }
                                )
                            }
                            IconButton(
                                onClick = {
                                    SoundFxManager.playClick()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showThemeStudio = true
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("theme_studio_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = "Open UI design studio",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.toggleSoundFx()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("sound_fx_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (soundEnabled) {
                                        Icons.AutoMirrored.Rounded.VolumeUp
                                    } else {
                                        Icons.AutoMirrored.Rounded.VolumeOff
                                    },
                                    contentDescription = if (soundEnabled) "Mute click sounds" else "Enable click sounds",
                                    tint = if (soundEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
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
                                val selected = !uiState.showFeatureGuide && currentTab == tab
                                val tabLabel = tab.label(strings)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tabLabel
                                        )
                                    },
                                    label = { Text(tabLabel, maxLines = 1) },
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
                                val selected = !uiState.showFeatureGuide && currentTab == tab
                                val tabLabel = tab.label(strings)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tabLabel
                                        )
                                    },
                                    label = { Text(tabLabel) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        if (uiState.showFeatureGuide) {
                            FeatureGuideScreen(
                                profile = profile,
                                onInspectFeature = viewModel::inspectFeature,
                                onToggleFeature = viewModel::toggleFeatureById,
                                onBack = { viewModel.setShowFeatureGuide(false) }
                            )
                        } else {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    val forward = targetState.ordinal >= initialState.ordinal
                                    val enterSlide = slideInHorizontally(animationSpec = tween(220)) { w -> if (forward) w / 5 else -w / 5 } + fadeIn(tween(220))
                                    val exitSlide = slideOutHorizontally(animationSpec = tween(180)) { w -> if (forward) -w / 6 else w / 6 } + fadeOut(tween(180))
                                    enterSlide togetherWith exitSlide
                                },
                                label = "tab_transition"
                            ) { tab ->
                                when (tab) {
                                    ShieldTab.FORGE -> ProtectorStudioScreen(
                                        uiState = uiState,
                                        profile = profile,
                                        isExplainMode = uiState.isExplainMode,
                                        onToggleExplainMode = viewModel::toggleExplainMode,
                                        onOpenFeatureGuide = { viewModel.setShowFeatureGuide(true) },
                                        onInspectFeature = viewModel::inspectFeature,
                                        onPickInputArchive = viewModel::selectInputArchive,
                                        onLoadSelfApk = viewModel::loadSelfApkAsInput,
                                        onUpdateProfile = viewModel::updateProfile,
                                        onApplyWallTier = viewModel::applyWallTier,
                                        onReapplySmartAuto = viewModel::reapplySmartAutoProfile,
                                        onApplyWarningFix = viewModel::applyWarningFix,
                                        onOpenThemeStudio = {
                                            SoundFxManager.playClick()
                                            showThemeStudio = true
                                        },
                                        onRandomTheme = viewModel::randomizeThemePreset,
                                        onSelectAllLayers = viewModel::selectAllSecurityLayers,
                                        onAdoptDeviceCert = viewModel::adoptCurrentDeviceCert,
                                        onUpdateProtectorInputs = viewModel::updateProtectorInputs,
                                        onRunProtection = viewModel::runProtectionPipeline,
                                        onSaveProtectedOutput = viewModel::saveProtectedArtifactToUri,
                                        onSaveCompanionKit = viewModel::saveCompanionKitToUri
                                    )
                                    ShieldTab.PACKER -> DexPackerLabScreen(
                                        uiState = uiState,
                                        profile = profile,
                                        onUpdateProfile = viewModel::updateProfile,
                                        onRunRamPackTest = { viewModel.runDexRamPackingDemo(playSound = true) },
                                        onCopiedNotice = showToast,
                                        onInspectFeature = viewModel::inspectFeature
                                    )
                                    ShieldTab.AUDIT -> ShieldDashboardScreen(
                                        uiState = uiState,
                                        profile = profile,
                                        isGuardActive = isGuardActive,
                                        mainThreadLatencyMs = mainThreadLatencyMs,
                                        onRunAudit = { viewModel.runSelfAudit(playSound = true) },
                                        onPickApkOrZip = viewModel::inspectPickedArchive,
                                        onNavigateToKit = { selectTab(ShieldTab.KIT) },
                                        onInspectFeature = viewModel::inspectFeature
                                    )
                                    ShieldTab.VAULT -> AssetVaultScreen(
                                        uiState = uiState,
                                        onUpdateInputs = viewModel::updateVaultInputs,
                                        onUpdateCipherInput = viewModel::updateVaultCipherInput,
                                        onEncryptText = { viewModel.encryptVaultText(playSound = true) },
                                        onDecryptText = viewModel::decryptVaultText,
                                        onProcessFile = viewModel::processBinaryFile,
                                        onSaveProcessedFile = viewModel::saveBinaryVaultOutput,
                                        onCopiedNotice = showToast,
                                        onInspectFeature = viewModel::inspectFeature
                                    )
                                    ShieldTab.KIT -> Column(modifier = Modifier.fillMaxSize()) {
                                        SingleChoiceSegmentedButtonRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                        ) {
                                            listOf("Sketchware Kit", "String & Crash Lab").forEachIndexed { idx, title ->
                                                SegmentedButton(
                                                    selected = kitSubTab == idx,
                                                    onClick = {
                                                        SoundFxManager.playClick()
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        kitSubTab = idx
                                                    },
                                                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = 2),
                                                    modifier = Modifier.testTag("kit_subtab_$idx")
                                                ) {
                                                    Text(title, maxLines = 1)
                                                }
                                            }
                                        }
                                        if (kitSubTab == 0) {
                                            SketchwareKitScreen(
                                                profile = profile,
                                                onUpdateProfile = viewModel::updateProfile,
                                                onAdoptDeviceCert = viewModel::adoptCurrentDeviceCert,
                                                onBuildExportBundle = viewModel::buildCombinedExportBundle,
                                                onCopiedNotice = showToast,
                                                onInspectFeature = viewModel::inspectFeature
                                            )
                                        } else {
                                            ObfuscatorScreen(
                                                uiState = uiState,
                                                faultLog = faultLog,
                                                isGuardActive = isGuardActive,
                                                mainThreadLatencyMs = mainThreadLatencyMs,
                                                onUpdateObfuscator = viewModel::updateObfuscatorInput,
                                                onGenerateObfuscation = { viewModel.obfuscateString(playSound = true) },
                                                onTriggerCrashTest = viewModel::triggerCrashGuardSimulation,
                                                onClearFaults = viewModel::clearFaultLogs,
                                                onCopiedNotice = showToast,
                                                onInspectFeature = viewModel::inspectFeature
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
    }
}

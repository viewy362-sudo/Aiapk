package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.i18n.FeatureCategory
import com.example.i18n.FeatureGuideCatalog
import com.example.security.ProtectionProfile
import com.example.ui.components.StatusPill

@Composable
fun FeatureGuideScreen(
    profile: ProtectionProfile,
    onInspectFeature: (String) -> Unit,
    onToggleFeature: (String, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val haptic = LocalHapticFeedback.current
    val lang = profile.resolvedLanguage
    val strings = AppStrings.get(lang)
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategoryName by rememberSaveable { mutableStateOf("ALL") }

    val selectedCategory = FeatureCategory.entries.firstOrNull { it.name == selectedCategoryName }
    val filteredFeatures = remember(searchQuery, selectedCategory, lang) {
        val q = searchQuery.trim().lowercase()
        FeatureGuideCatalog.allFeatures.filter { spec ->
            val catMatch = selectedCategory == null || spec.category == selectedCategory
            if (!catMatch) return@filter false
            if (q.isEmpty()) return@filter true
            val exp = spec.explain(lang)
            spec.defaultTitle.lowercase().contains(q) ||
                exp.title.lowercase().contains(q) ||
                exp.whatItDoes.lowercase().contains(q) ||
                exp.howItWorks.lowercase().contains(q)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("feature_guide_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    SoundFxManager.playClick()
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onBack()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("guide_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.guideScreenTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${FeatureGuideCatalog.allFeatures.size} functions • ${lang.nativeName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        Text(
                            text = strings.guideScreenSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(strings.searchFunctionsHint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("guide_search_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = {
                                    SoundFxManager.playClick()
                                    selectedCategoryName = "ALL"
                                },
                                label = { Text("${strings.allCategories} (${FeatureGuideCatalog.allFeatures.size})") }
                            )
                            FeatureCategory.entries.forEach { cat ->
                                val count = FeatureGuideCatalog.allFeatures.count { it.category == cat }
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = {
                                        SoundFxManager.playClick()
                                        selectedCategoryName = cat.name
                                    },
                                    label = { Text("${cat.label(lang)} ($count)") }
                                )
                            }
                        }
                    }
                }
            }

            items(filteredFeatures, key = { it.id }) { spec ->
                val exp = remember(spec.id, lang) { spec.explain(lang) }
                val checked = spec.isChecked?.invoke(profile)
                Card(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onInspectFeature(spec.id)
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (checked == true) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guide_card_${spec.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = exp.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (spec.badge != null) {
                                    VipOrFreeBadge(badge = spec.badge)
                                }
                            }
                            Text(
                                text = exp.whatItDoes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.HelpOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = strings.openInGuideBtn,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                if (checked != null) {
                                    StatusPill(
                                        text = if (checked) strings.statusEnabled else strings.statusDisabled,
                                        isPositive = checked
                                    )
                                }
                            }
                        }

                        if (checked != null && spec.applyToggle != null) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Switch(
                                checked = checked,
                                onCheckedChange = { next ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onToggleFeature(spec.id, next)
                                },
                                modifier = Modifier.testTag("guide_switch_${spec.id}")
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

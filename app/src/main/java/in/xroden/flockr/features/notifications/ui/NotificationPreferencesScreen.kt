/** Choosing which notifications each house sends you. */
package `in`.xroden.flockr.features.notifications.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.notifications.model.NotificationGroup
import `in`.xroden.flockr.features.notifications.model.NotificationType
import `in`.xroden.flockr.features.notifications.presentation.HousePreferences
import `in`.xroden.flockr.features.notifications.presentation.NotificationPreferencesUiState
import `in`.xroden.flockr.features.notifications.presentation.NotificationPreferencesViewModel
import `in`.xroden.flockr.ui.components.AnimatedGlyph
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.GlyphMotion
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.SkeletonHeroScreen
import `in`.xroden.flockr.ui.components.forms.ToggleRow
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

private val SwitchInset = Spacing.lg + ComponentHeight.avatar + Spacing.md

private fun NotificationGroup.icon() = when (this) {
    NotificationGroup.MONEY -> Icons.Rounded.Payments
    NotificationGroup.CHORES -> Icons.Rounded.CleaningServices
    NotificationGroup.HOUSE -> Icons.Rounded.Home
    NotificationGroup.MESSAGES -> Icons.Rounded.Forum
}

/** The cobalt says how much is on across every house; each house is a section, its switches grouped under what they're about. */
@Composable
fun NotificationPreferencesScreen(onNavigateBack: () -> Unit, viewModel: NotificationPreferencesViewModel = hiltViewModel()) {
    val haptics = rememberHaptics()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val error = (state as? NotificationPreferencesUiState.Ready)?.error

    LaunchedEffect(error) {
        error ?: return@LaunchedEffect
        haptics.error()
        snackbarHostState.showSnackbar(error)
        viewModel.dismissError()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when (val current = state) {
            NotificationPreferencesUiState.Loading -> SkeletonHeroScreen()
            is NotificationPreferencesUiState.Error -> Column(Modifier.fillMaxSize()) {
                HeroHeader(title = "Notifications")
                Box(Modifier.weight(1f)) { ErrorState(current.message, onRetry = viewModel::load) }
            }
            is NotificationPreferencesUiState.Ready -> if (current.houses.isEmpty()) {
                Column(Modifier.fillMaxSize()) {
                    HeroHeader(title = "Notifications")
                    Box(Modifier.weight(1f)) {
                        EmptyState(icon = Icons.Rounded.NotificationsOff, title = "No houses yet", subtitle = "Join or create a house to choose what it tells you about.")
                    }
                }
            } else {
                PreferenceList(current.houses, bottomPadding = padding.calculateBottomPadding(), onSet = viewModel::set)
            }
        }
    }
}

@Composable
private fun PreferenceList(
    houses: List<HousePreferences>,
    bottomPadding: Dp,
    onSet: (houseId: String, type: NotificationType, isEnabled: Boolean) -> Unit,
) {
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = bottomPadding + Spacing.xxl)) {
            item(key = "hero") { PreferencesHero(houses) }
            houses.forEach { prefs ->
                val houseId = prefs.house.id
                item(key = "house_$houseId") { SectionTitle(prefs.house.name) }
                NotificationGroup.entries.forEach { group ->
                    val kinds = prefs.enabled.keys.filter { it.group == group }
                    if (kinds.isEmpty()) return@forEach
                    item(key = "group_${houseId}_${group.channelId}") {
                        ListRow(
                            headline = group.label,
                            supporting = "${kinds.count { prefs.enabled.getValue(it) }} of ${kinds.size} on",
                            leading = { IconBadge(group.icon(), BadgeTone.COBALT) },
                        )
                    }
                    items(kinds, key = { "type_${houseId}_${it.key}" }) { type ->
                        ToggleRow(
                            title = type.label,
                            checked = prefs.enabled.getValue(type),
                            onCheckedChange = { onSet(houseId, type, it) },
                            modifier = Modifier.padding(start = SwitchInset, end = Spacing.lg),
                        )
                    }
                }
            }
        }
        HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)
    }
}

/** How many alerts are on across every house, with a bell that rings when that changes. */
@Composable
private fun PreferencesHero(houses: List<HousePreferences>) {
    val total = houses.sumOf { it.enabled.size }
    val on = houses.sumOf { prefs -> prefs.enabled.values.count { it } }
    HeroHeader(title = "Notifications") {
        Row(
            modifier = Modifier.padding(top = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            AnimatedGlyph(
                if (on == 0) Icons.Rounded.NotificationsOff else Icons.Rounded.NotificationsActive,
                trigger = on,
                motion = GlyphMotion.WIGGLE,
                contentDescription = null,
                modifier = Modifier.size(IconSize.lg),
            )
            Text(
                when (on) {
                    total -> "Everything is on"
                    0 -> "Everything is muted"
                    else -> "$on of $total alerts are on"
                },
                style = MaterialTheme.typography.headlineSmallEmphasized,
            )
        }
        HeroCaption(
            if (houses.size == 1) {
                "For ${houses.first().house.name}. Turn off what you'd rather not hear about."
            } else {
                "Across your ${houses.size} houses. Turn off what you'd rather not hear about."
            },
        )
    }
}

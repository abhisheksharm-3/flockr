package `in`.xroden.flockr.ui.components.inputs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import `in`.xroden.flockr.ui.theme.Motion
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.spatialSpec
import `in`.xroden.flockr.utils.rememberHaptics
import kotlinx.datetime.*
import java.util.Locale
import kotlin.time.Clock

/**
 * Steps through months one at a time, never past the current one, with an optional action that
 * clears the month filter entirely.
 *
 * @param timezone IANA zone id deciding which month counts as current; falls back to the system
 * zone when null or unparseable.
 */
@Composable
fun MonthSelector(
    selectedMonth: LocalDate,
    onMonthChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    showClearButton: Boolean = false,
    onClearFilter: (() -> Unit)? = null,
    subtitle: String? = null,
    timezone: String? = null
) {
    val haptics = rememberHaptics()
    val currentMonthStart = remember(timezone) {
        val zone = timezone?.let { runCatching { TimeZone.of(it) }.getOrNull() }
            ?: TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(zone).date
        LocalDate(today.year, today.month, 1)
    }
    val hasReachedCurrentMonth = selectedMonth.plus(1, DateTimeUnit.MONTH) > currentMonthStart

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptics.select()
                    onMonthChange(selectedMonth.minus(1, DateTimeUnit.MONTH))
                }
            ) {
                Icon(Icons.Default.ChevronLeft, "Previous month")
            }

            MonthLabel(month = selectedMonth, subtitle = subtitle)

            IconButton(
                onClick = {
                    haptics.select()
                    onMonthChange(selectedMonth.plus(1, DateTimeUnit.MONTH))
                },
                enabled = !hasReachedCurrentMonth
            ) {
                Icon(Icons.Default.ChevronRight, "Next month")
            }
        }

        onClearFilter?.let { clearFilter ->
            AnimatedVisibility(
                visible = showClearButton,
                enter = fadeIn(Motion.effects) + expandVertically(spatialSpec()),
                exit = fadeOut(Motion.effects) + shrinkVertically(spatialSpec())
            ) {
                OutlinedButton(
                    onClick = {
                        haptics.select()
                        clearFilter()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Show all months", style = MaterialTheme.typography.labelLargeEmphasized)
                }
            }
        }
    }
}

@Composable
private fun MonthLabel(month: LocalDate, subtitle: String?) {
    val label = remember(month) {
        val name = month.month.name.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        "$name ${month.year}"
    }
    val fade = Motion.effects

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
            contentAlignment = Alignment.Center
        ) { animatedLabel ->
            Text(
                text = animatedLabel,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

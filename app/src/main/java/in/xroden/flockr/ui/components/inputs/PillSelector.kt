package `in`.xroden.flockr.ui.components.inputs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import `in`.xroden.flockr.ui.theme.*
import `in`.xroden.flockr.utils.rememberHapticFeedback

/**
 * Standardized pill selector for tab-like switching.
 */
@Composable
fun PillSelector(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    counts: List<Int>? = null
) {
    val haptics = rememberHapticFeedback()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(AnimationDuration.fast),
                label = "pill_bg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(AnimationDuration.fast),
                label = "pill_content"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.xs)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable {
                        haptics.performSelection()
                        onTabSelected(index)
                    }
                    .padding(vertical = Spacing.md),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    if (counts != null && index < counts.size && counts[index] > 0) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = counts[index].toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter chip row for multiple filter options.
 */
@Composable
fun FilterChipRow(
    options: List<String>,
    selectedOptions: Set<Int>,
    onOptionToggled: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHapticFeedback()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index in selectedOptions
            FilterChip(
                selected = isSelected,
                onClick = {
                    haptics.performSelection()
                    onOptionToggled(index)
                },
                label = {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(CornerRadius.lg)
            )
        }
    }
}

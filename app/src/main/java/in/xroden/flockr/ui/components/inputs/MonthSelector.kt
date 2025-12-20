package `in`.xroden.flockr.ui.components.inputs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.*
import java.util.Locale

/**
 * Unified Month Selector component for consistent styling across all screens.
 * Supports both filter mode (with clear button) and navigation mode.
 */
@Composable
fun MonthSelector(
    selectedMonth: LocalDate,
    onMonthChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    showClearButton: Boolean = false,
    onClearFilter: (() -> Unit)? = null,
    subtitle: String? = null
) {
    val currentMonthStart = remember {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        LocalDate(now.year, now.month, 1)
    }

    val monthName = remember(selectedMonth) {
        selectedMonth.month.name.lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    val isFutureDisabled = selectedMonth.plus(1, DateTimeUnit.MONTH) > currentMonthStart

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { onMonthChange(selectedMonth.minus(1, DateTimeUnit.MONTH)) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        "Previous month",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "$monthName ${selectedMonth.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = { if (!isFutureDisabled) onMonthChange(selectedMonth.plus(1, DateTimeUnit.MONTH)) },
                    enabled = !isFutureDisabled,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        "Next month",
                        tint = if (!isFutureDisabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }

            if (showClearButton && onClearFilter != null) {
                OutlinedButton(
                    onClick = onClearFilter,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Show All", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

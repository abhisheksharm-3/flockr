/** Choosing a calendar date, shown in the house's date layout. */
package `in`.xroden.flockr.ui.components.inputs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.rememberHaptics
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * A tappable field showing [date] in the house's layout, opening a date picker on tap.
 */
@Composable
fun DatePickerField(
    label: String,
    date: LocalDate,
    houseConfig: HouseConfig?,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var isPickerOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { isPickerOpen = true },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(date.formatWithHouseConfig(houseConfig), style = MaterialTheme.typography.bodyLargeEmphasized)
            }
            Icon(Icons.Filled.CalendarMonth, contentDescription = "Change $label", tint = MaterialTheme.colorScheme.primary)
        }
    }

    if (isPickerOpen) {
        FlockrDatePickerDialog(
            initialDate = date,
            onDateSelected = { onDateChange(it); isPickerOpen = false },
            onDismiss = { isPickerOpen = false },
        )
    }
}

/**
 * Material's date picker, taking and returning a calendar date.
 *
 * The picker works in milliseconds at midnight UTC, so both conversions go through UTC. Converting
 * through the device time zone instead shifts the date by a day: east of UTC the picker opens on the
 * day before, and west of UTC the chosen day is saved as the day before.
 */
@Composable
fun FlockrDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberHaptics()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialDate.toPickerMillis())

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
                        haptics.select()
                        onDateSelected(it.toPickerDate())
                    }
                }
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

private fun LocalDate.toPickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toPickerDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

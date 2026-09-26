/** Material's date picker dialog, speaking calendar dates and the house's week start. */
package `in`.xroden.flockr.ui.components.inputs

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import java.util.Locale
import androidx.compose.runtime.setValue
import `in`.xroden.flockr.utils.rememberHaptics
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

/**
 * Material's date picker, taking and returning a calendar date, with weeks starting on
 * [firstDayOfWeek] (0 for Sunday, as the house stores it) or the device's usual day when null.
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
    firstDayOfWeek: Int? = null,
) {
    val haptics = rememberHaptics()
    val state = remember(firstDayOfWeek) {
        DatePickerState(locale = localeStartingWeekOn(firstDayOfWeek), initialSelectedDateMillis = initialDate.toPickerMillis())
    }

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

/**
 * The device locale with its week starting on [day], through the Unicode "fw" keyword, which the
 * picker's calendar reads its first day of the week from.
 */
private fun localeStartingWeekOn(day: Int?): Locale {
    val locale = Locale.getDefault()
    if (day == null) return locale
    val keyword = listOf("sun", "mon", "tue", "wed", "thu", "fri", "sat")[day.coerceIn(0, 6)]
    return Locale.Builder().setLocale(locale).setUnicodeLocaleKeyword("fw", keyword).build()
}

private fun LocalDate.toPickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toPickerDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

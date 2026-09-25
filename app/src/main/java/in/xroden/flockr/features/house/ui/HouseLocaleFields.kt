/** The four settings that decide how a house's money and dates read, shared by creating a house and its settings. */
package `in`.xroden.flockr.features.house.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import `in`.xroden.flockr.features.house.model.DateLayout
import `in`.xroden.flockr.ui.components.inputs.ChoiceField
import `in`.xroden.flockr.utils.SUPPORTED_CURRENCIES
import `in`.xroden.flockr.utils.currencySymbol
import `in`.xroden.flockr.utils.example
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

/** Zones offered first, before the device's own; any zone Android knows can be chosen. */
private val COMMON_TIME_ZONES = listOf(
    "UTC", "Asia/Kolkata", "Asia/Singapore", "Asia/Tokyo", "Asia/Shanghai", "Asia/Dubai", "Europe/London", "Europe/Berlin",
    "Europe/Paris", "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles", "Australia/Sydney",
)

/**
 * [firstDayOfWeek] is 0 for Sunday through 6 for Saturday, as `house_config` stores it. When
 * [isCurrencyLocked] the currency is shown but can't change, because the house has recorded money
 * and every amount would be relabelled.
 */
@Composable
fun HouseLocaleFields(
    currencyCode: String,
    onCurrencyChange: (String) -> Unit,
    dateFormat: String,
    onDateFormatChange: (String) -> Unit,
    firstDayOfWeek: Int,
    onFirstDayOfWeekChange: (Int) -> Unit,
    timezone: String,
    onTimezoneChange: (String) -> Unit,
    enabled: Boolean = true,
    isCurrencyLocked: Boolean = false,
) {
    ChoiceField(
        label = "Currency",
        selected = currencyCode,
        options = SUPPORTED_CURRENCIES,
        onSelect = onCurrencyChange,
        optionLabel = { "${currencySymbol(it)}  $it" },
        enabled = enabled && !isCurrencyLocked,
        modifier = Modifier.fillMaxWidth(),
    )
    if (isCurrencyLocked) {
        Text("The currency is fixed once the house has recorded money.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    ChoiceField(
        label = "Dates",
        selected = DateLayout.fromPattern(dateFormat) ?: DateLayout.ISO,
        options = DateLayout.entries,
        onSelect = { onDateFormatChange(it.pattern) },
        optionLabel = { "${it.example()} · ${it.order}" },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
    ChoiceField(
        label = "Weeks start on",
        selected = firstDayOfWeek,
        options = (0..6).toList(),
        onSelect = onFirstDayOfWeekChange,
        optionLabel = { dayName(it) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
    val zones = remember(timezone) {
        (listOf(timezone, ZoneId.systemDefault().id) + COMMON_TIME_ZONES + ZoneId.getAvailableZoneIds().sorted()).distinct()
    }
    ChoiceField(
        label = "Time zone",
        selected = timezone,
        options = zones,
        onSelect = onTimezoneChange,
        optionLabel = ::zoneLabel,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** "Kolkata · GMT+05:30": the zone's city and its current offset from GMT. */
private fun zoneLabel(id: String): String {
    val offset = runCatching { ZonedDateTime.now(ZoneId.of(id)).offset.id }.getOrNull()?.takeIf { it != "Z" } ?: ""
    return "${id.substringAfterLast('/').replace('_', ' ')} · GMT$offset"
}

/** The stored day number's name in the device language: 0 is Sunday. */
private fun dayName(day: Int): String = DayOfWeek.SUNDAY.plus(day.toLong()).getDisplayName(TextStyle.FULL, Locale.getDefault())

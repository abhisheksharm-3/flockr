/** The sentence that decides how a house's money and dates read, shared by creating a house and its settings. */
package `in`.xroden.flockr.features.house.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import `in`.xroden.flockr.features.house.model.DateLayout
import `in`.xroden.flockr.ui.components.forms.Sentence
import `in`.xroden.flockr.ui.components.forms.SentenceToken
import `in`.xroden.flockr.ui.components.forms.SentenceWords
import `in`.xroden.flockr.ui.components.inputs.OptionSheet
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.SUPPORTED_CURRENCIES
import `in`.xroden.flockr.utils.currencySymbol
import `in`.xroden.flockr.utils.example
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Zones offered first, before the device's own; any zone Android knows can be chosen. */
private val COMMON_TIME_ZONES = listOf(
    "UTC", "Asia/Kolkata", "Asia/Singapore", "Asia/Tokyo", "Asia/Shanghai", "Asia/Dubai", "Europe/London", "Europe/Berlin",
    "Europe/Paris", "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles", "Australia/Sydney",
)

/**
 * Every zone Android knows, sorted, with its label. Each label loads that zone's rules, which for
 * some six hundred zones is slow enough to stall the sheet opening, so the map is built once per
 * process and only ever read off the main thread.
 */
private val ALL_ZONE_LABELS: Map<String, String> by lazy {
    ZoneId.getAvailableZoneIds().sorted().associateWith(::zoneLabel)
}

private enum class LocalePicker { CURRENCY, DATES, WEEK, ZONE }

/**
 * "Count money in **₹ INR** and write dates like **30/12/2025**. Weeks start on **Monday** and the
 * clock follows **Kolkata**", each value a token opening its sheet.
 *
 * [firstDayOfWeek] is 0 for Sunday through 6 for Saturday, as `house_config` stores it. When
 * [isCurrencyLocked] the currency shows but can't change, because the house has recorded money and
 * every amount would be relabelled.
 */
@Composable
fun HouseLocaleSentence(
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
    var picker by remember { mutableStateOf<LocalePicker?>(null) }
    val layout = DateLayout.fromPattern(dateFormat) ?: DateLayout.ISO
    val zoneLabels by produceState(emptyMap<String, String>()) {
        value = withContext(Dispatchers.Default) { ALL_ZONE_LABELS }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Sentence {
            SentenceWords("Count money in")
            SentenceToken(
                "${currencySymbol(currencyCode)} $currencyCode",
                onClick = { picker = LocalePicker.CURRENCY },
                enabled = enabled && !isCurrencyLocked,
                icon = if (isCurrencyLocked) Icons.Rounded.Lock else Icons.Rounded.Payments,
            )
            SentenceWords("and write dates like")
            SentenceToken(layout.example(), onClick = { picker = LocalePicker.DATES }, enabled = enabled, icon = Icons.Rounded.CalendarMonth)
        }
        Sentence {
            SentenceWords("Weeks start on")
            SentenceToken(dayName(firstDayOfWeek), onClick = { picker = LocalePicker.WEEK }, enabled = enabled, icon = Icons.Rounded.Today)
            SentenceWords("and the clock follows")
            SentenceToken(zoneCity(timezone), onClick = { picker = LocalePicker.ZONE }, enabled = enabled, icon = Icons.Rounded.Public)
        }
        if (isCurrencyLocked) {
            Text(
                "The currency is fixed once the house has recorded money.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }
    }

    when (picker) {
        LocalePicker.CURRENCY -> OptionSheet(
            options = SUPPORTED_CURRENCIES,
            selected = currencyCode,
            onSelect = onCurrencyChange,
            onDismiss = { picker = null },
            title = "Which currency?",
            optionLabel = { "${currencySymbol(it)}  $it" },
        )
        LocalePicker.DATES -> OptionSheet(
            options = DateLayout.entries,
            selected = layout,
            onSelect = { onDateFormatChange(it.pattern) },
            onDismiss = { picker = null },
            title = "How should dates read?",
            optionLabel = { "${it.example()} · ${it.order}" },
        )
        LocalePicker.WEEK -> OptionSheet(
            options = (0..6).toList(),
            selected = firstDayOfWeek,
            onSelect = onFirstDayOfWeekChange,
            onDismiss = { picker = null },
            title = "When does the week start?",
            optionLabel = ::dayName,
        )
        LocalePicker.ZONE -> OptionSheet(
            options = remember(timezone, zoneLabels) {
                (listOf(timezone, ZoneId.systemDefault().id) + COMMON_TIME_ZONES + zoneLabels.keys).distinct()
            },
            selected = timezone,
            onSelect = onTimezoneChange,
            onDismiss = { picker = null },
            title = "Which time zone?",
            optionLabel = { zoneLabels[it] ?: zoneLabel(it) },
        )
        null -> Unit
    }
}

/** "Kolkata · GMT+05:30": the zone's city and its current offset from GMT. */
internal fun zoneLabel(id: String): String {
    val offset = runCatching { ZonedDateTime.now(ZoneId.of(id)).offset.id }.getOrNull()?.takeIf { it != "Z" } ?: ""
    return "${zoneCity(id)} · GMT$offset"
}

/** The city part of a zone id, "New York" for "America/New_York". */
private fun zoneCity(id: String): String = id.substringAfterLast('/').replace('_', ' ')

/** The stored day number's name in the device language: 0 is Sunday. */
internal fun dayName(day: Int): String = DayOfWeek.SUNDAY.plus(day.toLong()).getDisplayName(TextStyle.FULL, Locale.getDefault())

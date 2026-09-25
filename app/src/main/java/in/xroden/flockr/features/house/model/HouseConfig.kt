/** A house's locale settings, and the defaults every screen falls back to when none are loaded. */
package `in`.xroden.flockr.features.house.model

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.data.serialization.InstantSerializer
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val DEFAULT_CURRENCY_CODE = "USD"

@Immutable
@Serializable
data class HouseConfig(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("currency_code")
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    @SerialName("date_format")
    val dateFormat: String = DateLayout.ISO.stored,
    @SerialName("first_day_of_week")
    val firstDayOfWeek: Int = 0,
    val timezone: String = "UTC",
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @SerialName("updated_at")
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null
)

/**
 * The date layouts a house can choose. [pattern] is a `java.time` pattern and [stored] is the value
 * written to `house_config.date_format`.
 *
 * Stored values have been written in both cases over time, so they are matched case-insensitively
 * and never handed to a formatter directly: in a `java.time` pattern, `YYYY` is the week-based year
 * and `DD` is the day of the year, so "YYYY-MM-DD" would print 30 December 2025 as "2026-12-364".
 */
enum class DateLayout(val stored: String, val pattern: String) {
    DAY_MONTH_YEAR("dd/MM/yyyy", "dd/MM/yyyy"),
    MONTH_DAY_YEAR("MM/dd/yyyy", "MM/dd/yyyy"),
    ISO("yyyy-MM-dd", "yyyy-MM-dd");

    companion object {
        /** The layout matching a stored value, or null for a value no layout recognises. */
        fun fromStored(value: String): DateLayout? = entries.firstOrNull { it.stored.equals(value, ignoreCase = true) }
    }
}

/** The house currency, or [DEFAULT_CURRENCY_CODE] before the config has loaded. */
fun HouseConfig?.currency(): String = this?.currencyCode ?: DEFAULT_CURRENCY_CODE

/** The house time zone, or the device's when the config is missing or names an unknown zone. */
fun HouseConfig?.timeZone(): TimeZone =
    this?.timezone?.let { runCatching { TimeZone.of(it) }.getOrNull() } ?: TimeZone.currentSystemDefault()

/** Today's date where the house is, which is what "due today" and "overdue" are measured against. */
fun HouseConfig?.today(): LocalDate = Clock.System.todayIn(timeZone())

/** The house's date layout, or ISO when none is set or the stored value is unrecognised. */
fun HouseConfig?.dateLayout(): DateLayout = this?.dateFormat?.let(DateLayout::fromStored) ?: DateLayout.ISO

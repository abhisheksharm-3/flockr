/** A house's locale settings, and the defaults every screen falls back to when none are loaded. */
package `in`.xroden.flockr.features.house.model

import androidx.compose.runtime.Immutable
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val DEFAULT_CURRENCY_CODE = "USD"

@Immutable
@Serializable
data class HouseConfig(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("currency_code")
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    @SerialName("date_format")
    val dateFormat: String = DateLayout.ISO.pattern,
    @SerialName("first_day_of_week")
    val firstDayOfWeek: Int = 0,
    val timezone: String = "UTC"
)

/**
 * The date layouts a house can choose. [pattern] is both the `java.time` pattern and the value stored
 * in `house_config.date_format`, which the database restricts to these three.
 */
enum class DateLayout(val pattern: String) {
    DAY_MONTH_YEAR("dd/MM/yyyy"),
    MONTH_DAY_YEAR("MM/dd/yyyy"),
    ISO("yyyy-MM-dd");

    companion object {
        fun fromPattern(pattern: String): DateLayout? = entries.firstOrNull { it.pattern == pattern }
    }
}

/** The house currency, or [DEFAULT_CURRENCY_CODE] before the config has loaded. */
fun HouseConfig?.currency(): String = this?.currencyCode ?: DEFAULT_CURRENCY_CODE

/** The house time zone, or the device's when the config is missing or names an unknown zone. */
fun HouseConfig?.timeZone(): TimeZone =
    this?.timezone?.let { runCatching { TimeZone.of(it) }.getOrNull() } ?: TimeZone.currentSystemDefault()

/** Today's date where the house is, which is what "due today" and "overdue" are measured against. */
fun HouseConfig?.today(): LocalDate = Clock.System.todayIn(timeZone())

/** The house's date layout, or ISO before the config has loaded. */
fun HouseConfig?.dateLayout(): DateLayout = this?.dateFormat?.let(DateLayout::fromPattern) ?: DateLayout.ISO

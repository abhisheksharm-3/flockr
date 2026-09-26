/** Turns dates into the text a house sees, in the layout that house chose. */
package `in`.xroden.flockr.utils

import `in`.xroden.flockr.features.house.model.DateLayout
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.dateLayout
import `in`.xroden.flockr.features.house.model.today
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** This date in the house's chosen layout, such as 30/12/2025 for day-month-year. */
fun LocalDate.formatWithHouseConfig(config: HouseConfig?): String =
    toJavaLocalDate().format(DateTimeFormatter.ofPattern(config.dateLayout().pattern, Locale.getDefault()))

/** The month and year, such as "September 2026", that a month of activity is grouped under. */
fun LocalDate.monthYearLabel(): String =
    toJavaLocalDate().format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))

/** The short month name, such as "Sep". */
fun LocalDate.shortMonthLabel(): String =
    toJavaLocalDate().format(DateTimeFormatter.ofPattern("LLL", Locale.getDefault()))

/** How [layout] shows 30 December 2025, which is what a setting's choices are labelled with. */
fun DateLayout.example(): String =
    java.time.LocalDate.of(2025, 12, 30).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))

/** When something falls due, relative to today, such as "Due tomorrow" or "Overdue by 3 days". */
fun dueLabel(daysUntil: Int): String = when {
    daysUntil < -1 -> "Overdue by ${-daysUntil} days"
    daysUntil == -1 -> "Overdue by a day"
    daysUntil == 0 -> "Due today"
    daysUntil == 1 -> "Due tomorrow"
    else -> "Due in $daysUntil days"
}

/** "today", "yesterday", or this date in the house's layout, as a form reads it back to the user. */
fun LocalDate.relativeDayLabel(config: HouseConfig?): String {
    val today = config.today()
    return when (this) {
        today -> "today"
        today.minus(DatePeriod(days = 1)) -> "yesterday"
        else -> formatWithHouseConfig(config)
    }
}

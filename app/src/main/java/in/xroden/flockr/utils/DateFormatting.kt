/** Turns dates into the text a house sees, in the layout that house chose. */
package `in`.xroden.flockr.utils

import `in`.xroden.flockr.features.house.model.DateLayout
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.dateLayout
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

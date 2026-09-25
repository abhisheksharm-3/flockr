/** Turns dates into the text a house sees, in the layout that house chose. */
package `in`.xroden.flockr.utils

import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.dateLayout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** This date in the house's chosen layout, such as 30/12/2025 for day-month-year. */
fun LocalDate.formatWithHouseConfig(config: HouseConfig?): String =
    toJavaLocalDate().format(DateTimeFormatter.ofPattern(config.dateLayout().pattern, Locale.getDefault()))

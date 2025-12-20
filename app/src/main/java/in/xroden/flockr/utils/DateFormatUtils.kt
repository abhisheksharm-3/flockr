package `in`.xroden.flockr.utils

import `in`.xroden.flockr.features.house.model.HouseConfig
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DayOfWeek

/**
 * Format a LocalDate using the house's date format preference
 */
fun LocalDate.formatWithHouseConfig(config: HouseConfig?): String {
    val format = config?.dateFormat ?: "dd/MM/yyyy"
    return formatDateWithPattern(this, format)
}

/**
 * Format date string to display format using house config
 */
fun formatDateWithPattern(date: LocalDate, pattern: String): String {
    return when (pattern.lowercase()) {
        "dd/mm/yyyy" -> "${date.day.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year}"
        "mm/dd/yyyy" -> "${date.monthNumber.toString().padStart(2, '0')}/${date.day.toString().padStart(2, '0')}/${date.year}"
        "yyyy-mm-dd" -> "${date.year}-${date.monthNumber.toString().padStart(2, '0')}-${date.day.toString().padStart(2, '0')}"
        else -> {
            // Default short format: "Jan 15"
            val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            "$month ${date.day}"
        }
    }
}

/**
 * Get today's date in the house's timezone
 */
fun HouseConfig?.getTodayInHouseTimezone(): LocalDate {
    val timezoneId = this?.timezone ?: TimeZone.currentSystemDefault().id
    return try {
        kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.of(timezoneId)).date
    } catch (_: Exception) {
        kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
}

/**
 * Get the TimeZone from HouseConfig, falling back to system default
 */
fun HouseConfig?.getTimezone(): TimeZone {
    val timezoneId = this?.timezone ?: return TimeZone.currentSystemDefault()
    return runCatching { TimeZone.of(timezoneId) }.getOrDefault(TimeZone.currentSystemDefault())
}

/**
 * Format date in friendly short format: "Jan 15" or "Dec 31, 2024" if different year
 * Uses house timezone to determine "today"
 */
fun LocalDate.toFriendlyString(config: HouseConfig? = null): String {
    val today = config.getTodayInHouseTimezone()
    val month = this.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return if (this.year == today.year) {
        "$month ${this.day}"
    } else {
        "$month ${this.day}, ${this.year}"
    }
}

/**
 * Get first day of week from HouseConfig
 * 0 = Sunday, 1 = Monday, etc.
 */
fun HouseConfig?.getFirstDayOfWeek(): DayOfWeek {
    return when (this?.firstDayOfWeek ?: 0) {
        0 -> DayOfWeek.SUNDAY
        1 -> DayOfWeek.MONDAY
        2 -> DayOfWeek.TUESDAY
        3 -> DayOfWeek.WEDNESDAY
        4 -> DayOfWeek.THURSDAY
        5 -> DayOfWeek.FRIDAY
        6 -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }
}


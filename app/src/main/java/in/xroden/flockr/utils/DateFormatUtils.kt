package `in`.xroden.flockr.utils

import `in`.xroden.flockr.features.house.model.HouseConfig
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime

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
        "dd/mm/yyyy" -> "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year}"
        "mm/dd/yyyy" -> "${date.monthNumber.toString().padStart(2, '0')}/${date.dayOfMonth.toString().padStart(2, '0')}/${date.year}"
        "yyyy-mm-dd" -> "${date.year}-${date.monthNumber.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}"
        else -> {
            // Default short format: "Jan 15"
            val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
            "$month ${date.dayOfMonth}"
        }
    }
}

/**
 * Get today's date in the house's timezone
 */
fun HouseConfig?.getTodayInHouseTimezone(): LocalDate {
    val timezoneId = this?.timezone ?: TimeZone.currentSystemDefault().id
    return try {
        Clock.System.now().toLocalDateTime(TimeZone.of(timezoneId)).date
    } catch (_: Exception) {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
}

/**
 * Format date in friendly short format: "Jan 15" or "Dec 31, 2024" if different year
 */
fun LocalDate.toFriendlyString(): String {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val month = this.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return if (this.year == today.year) {
        "$month ${this.dayOfMonth}"
    } else {
        "$month ${this.dayOfMonth}, ${this.year}"
    }
}

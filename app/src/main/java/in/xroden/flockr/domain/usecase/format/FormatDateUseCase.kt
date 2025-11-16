package `in`.xroden.flockr.domain.usecase.format

import `in`.xroden.flockr.features.house.model.HouseConfig
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * Use case to format dates consistently across the app
 */
class FormatDateUseCase @Inject constructor() {

    /**
     * Format LocalDate according to house config
     * 
     * @param date LocalDate to format
     * @param config House configuration with date format settings
     * @return Formatted date string
     */
    operator fun invoke(date: LocalDate, config: HouseConfig?): String {
        val format = config?.dateFormat ?: "YYYY-MM-DD"
        
        return when (format) {
            "YYYY-MM-DD" -> formatYearMonthDay(date)
            "DD-MM-YYYY" -> formatDayMonthYear(date)
            "MM-DD-YYYY" -> formatMonthDayYear(date)
            else -> formatYearMonthDay(date) // Default
        }
    }

    /**
     * Format Instant according to house config
     */
    fun formatInstant(instant: Instant, config: HouseConfig?): String {
        val timezone = TimeZone.of(config?.timezone ?: "UTC")
        val localDateTime = instant.toLocalDateTime(timezone)
        
        val dateStr = invoke(localDateTime.date, config)
        val timeStr = formatTime(localDateTime.hour, localDateTime.minute)
        
        return "$dateStr $timeStr"
    }

    /**
     * Format time as HH:MM
     */
    fun formatTime(hour: Int, minute: Int): String {
        return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    }

    /**
     * Format as relative time (e.g., "2 days ago", "in 3 hours")
     */
    fun formatRelative(instant: Instant): String {
        val now = kotlinx.datetime.Clock.System.now()
        val diff = (now - instant).inWholeSeconds
        
        return when {
            diff < 0 -> "in the future"
            diff < 60 -> "just now"
            diff < 3600 -> {
                val minutes = diff / 60
                if (minutes == 1L) "1 minute ago" else "$minutes minutes ago"
            }
            diff < 86400 -> {
                val hours = diff / 3600
                if (hours == 1L) "1 hour ago" else "$hours hours ago"
            }
            diff < 604800 -> {
                val days = diff / 86400
                if (days == 1L) "1 day ago" else "$days days ago"
            }
            diff < 2592000 -> {
                val weeks = diff / 604800
                if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
            }
            else -> {
                val months = diff / 2592000
                if (months == 1L) "1 month ago" else "$months months ago"
            }
        }
    }

    private fun formatYearMonthDay(date: LocalDate): String {
        return "${date.year}-${date.monthNumber.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}"
    }

    private fun formatDayMonthYear(date: LocalDate): String {
        return "${date.dayOfMonth.toString().padStart(2, '0')}-${date.monthNumber.toString().padStart(2, '0')}-${date.year}"
    }

    private fun formatMonthDayYear(date: LocalDate): String {
        return "${date.monthNumber.toString().padStart(2, '0')}-${date.dayOfMonth.toString().padStart(2, '0')}-${date.year}"
    }

    /**
     * Parse date string to LocalDate
     */
    fun parseDate(dateString: String, format: String = "YYYY-MM-DD"): Result<LocalDate> {
        return try {
            val parts = dateString.split("-")
            val date = when (format) {
                "YYYY-MM-DD" -> LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                "DD-MM-YYYY" -> LocalDate(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                "MM-DD-YYYY" -> LocalDate(parts[2].toInt(), parts[0].toInt(), parts[1].toInt())
                else -> LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            }
            Result.success(date)
        } catch (e: Exception) {
            Result.failure(Exception("Invalid date format"))
        }
    }
}



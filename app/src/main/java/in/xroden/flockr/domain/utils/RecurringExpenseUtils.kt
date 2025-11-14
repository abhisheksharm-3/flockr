package `in`.xroden.flockr.domain.utils

import `in`.xroden.flockr.data.model.RecurringExpense
import `in`.xroden.flockr.features.expenses.model.RecurringFrequency
import `in`.xroden.flockr.features.expenses.model.DueStatus

/**
 * Utility functions for recurring expenses
 */
object RecurringExpenseUtils {

    /**
     * Format due status message
     */
    fun formatDueStatusMessage(expense: RecurringExpense): String {
        return when (expense.dueStatus) {
            "overdue" -> {
                val days = kotlin.math.abs(expense.daysUntilDue ?: 0)
                "Overdue by $days day${if (days != 1) "s" else ""}"
            }
            "due_today" -> "Due today"
            "due_soon" -> {
                val days = expense.daysUntilDue ?: 0
                "Due in $days day${if (days != 1) "s" else ""}"
            }
            "upcoming" -> {
                val days = expense.daysUntilDue ?: 0
                "Due in $days day${if (days != 1) "s" else ""}"
            }
            else -> "Not scheduled"
        }
    }

    /**
     * Get frequency description
     */
    fun getFrequencyDescription(frequency: String, customDays: Int? = null): String {
        return when (frequency) {
            "daily" -> "Every day"
            "weekly" -> "Every week"
            "biweekly" -> "Every 2 weeks"
            "monthly" -> "Every month"
            "quarterly" -> "Every 3 months"
            "semiannual" -> "Every 6 months"
            "annual" -> "Every year"
            "custom" -> if (customDays != null) "Every $customDays days" else "Custom"
            else -> "Monthly"
        }
    }

    /**
     * Check if expense needs reminder
     */
    fun shouldShowReminder(expense: RecurringExpense): Boolean {
        if (!expense.reminderEnabled || expense.nextDueDate == null) return false

        val daysUntil = expense.daysUntilDue ?: return false
        val reminderDays = expense.reminderDaysBefore

        return daysUntil in 0..reminderDays
    }

    /**
     * Format amount with currency
     */
    fun formatAmount(amount: Double, currencySymbol: String): String {
        return "$currencySymbol${String.format("%.2f", amount)}"
    }

    /**
     * Parse date string (yyyy-MM-dd) to display format
     */
    fun formatDate(dateString: String?): String {
        if (dateString == null) return "Not set"

        return try {
            val parts = dateString.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1]
                val day = parts[2]
                val monthName = when (month) {
                    "01" -> "Jan"
                    "02" -> "Feb"
                    "03" -> "Mar"
                    "04" -> "Apr"
                    "05" -> "May"
                    "06" -> "Jun"
                    "07" -> "Jul"
                    "08" -> "Aug"
                    "09" -> "Sep"
                    "10" -> "Oct"
                    "11" -> "Nov"
                    "12" -> "Dec"
                    else -> month
                }
                "$monthName $day, $year"
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }
}


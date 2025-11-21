package `in`.xroden.flockr.domain.usecase.expense

import `in`.xroden.flockr.data.enums.ExpenseFrequency
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import javax.inject.Inject

/**
 * Use case to calculate next due date for recurring expenses
 */
class CalculateRecurringExpenseNextDueDateUseCase @Inject constructor() {

    /**
     * Calculate next due date based on frequency
     * 
     * @param lastDate Last payment or creation date
     * @param frequency Recurrence frequency
     * @param customFrequencyDays Custom frequency in days (for CUSTOM frequency)
     * @param dueDay Day of month expense is due
     * @return Next due date
     */
    operator fun invoke(
        lastDate: LocalDate,
        frequency: ExpenseFrequency,
        customFrequencyDays: Int? = null,
        dueDay: Int
    ): LocalDate {
        return when (frequency) {
            ExpenseFrequency.DAILY -> 
                lastDate.plus(1, DateTimeUnit.DAY)
            
            ExpenseFrequency.WEEKLY -> 
                lastDate.plus(1, DateTimeUnit.WEEK)
            
            ExpenseFrequency.BIWEEKLY -> 
                lastDate.plus(2, DateTimeUnit.WEEK)
            
            ExpenseFrequency.MONTHLY -> 
                calculateNextMonthDueDate(lastDate, dueDay)
            
            ExpenseFrequency.QUARTERLY -> 
                lastDate.plus(3, DateTimeUnit.MONTH)
            
            ExpenseFrequency.SEMIANNUAL -> 
                lastDate.plus(6, DateTimeUnit.MONTH)
            
            ExpenseFrequency.ANNUAL -> 
                lastDate.plus(1, DateTimeUnit.YEAR)
            
            ExpenseFrequency.CUSTOM -> {
                val days = customFrequencyDays ?: 30
                lastDate.plus(days, DateTimeUnit.DAY)
            }
        }
    }

    /**
     * Calculate days until due
     */
    fun calculateDaysUntilDue(dueDate: LocalDate): Int {
        val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        return (dueDate.toEpochDays() - today.toEpochDays())
    }

    /**
     * Determine due status based on days until due
     */
    fun determineDueStatus(daysUntilDue: Int): `in`.xroden.flockr.data.enums.ExpenseDueStatus {
        return when {
            daysUntilDue < 0 -> `in`.xroden.flockr.data.enums.ExpenseDueStatus.OVERDUE
            daysUntilDue == 0 -> `in`.xroden.flockr.data.enums.ExpenseDueStatus.DUE_TODAY
            daysUntilDue <= 3 -> `in`.xroden.flockr.data.enums.ExpenseDueStatus.DUE_SOON
            daysUntilDue <= 7 -> `in`.xroden.flockr.data.enums.ExpenseDueStatus.UPCOMING
            else -> `in`.xroden.flockr.data.enums.ExpenseDueStatus.NOT_SET
        }
    }

    private fun calculateNextMonthDueDate(lastDate: LocalDate, dueDay: Int): LocalDate {
        val nextMonth = lastDate.plus(1, DateTimeUnit.MONTH)
        val daysInMonth = when (nextMonth.monthNumber) {
            2 -> if (isLeapYear(nextMonth.year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        val validDueDay = minOf(dueDay, daysInMonth)
        
        return LocalDate(nextMonth.year, nextMonth.monthNumber, validDueDay)
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}



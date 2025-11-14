package `in`.xroden.flockr.features.expenses.model

enum class RecurringFrequency(val value: String, val displayName: String) {
    DAILY("daily", "Daily"),
    WEEKLY("weekly", "Weekly"),
    BIWEEKLY("biweekly", "Bi-weekly"),
    MONTHLY("monthly", "Monthly"),
    QUARTERLY("quarterly", "Quarterly"),
    SEMIANNUAL("semiannual", "Semi-annual"),
    ANNUAL("annual", "Annual"),
    CUSTOM("custom", "Custom");

    companion object {
        fun fromValue(value: String): RecurringFrequency {
            return values().find { it.value == value } ?: MONTHLY
        }

        fun getAllOptions(): List<RecurringFrequency> {
            return values().toList()
        }
    }
}

enum class DueStatus(val value: String, val displayName: String, val colorKey: String) {
    NOT_SET("not_set", "Not Set", "gray"),
    OVERDUE("overdue", "Overdue", "red"),
    DUE_TODAY("due_today", "Due Today", "orange"),
    DUE_SOON("due_soon", "Due Soon", "yellow"),
    UPCOMING("upcoming", "Upcoming", "green");

    companion object {
        fun fromValue(value: String?): DueStatus {
            return values().find { it.value == value } ?: NOT_SET
        }
    }
}


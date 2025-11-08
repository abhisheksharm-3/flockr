package `in`.xroden.flockr.utils

/**
 * Application-wide constants
 * Single source of truth for magic numbers, date formats, and other constants
 */
object Constants {

    // Date & Time Formats
    object DateFormats {
        const val YEAR_MONTH = "yyyy-MM"
        const val YEAR_MONTH_DAY = "yyyy-MM-dd"
        const val DISPLAY_DATE = "MMM dd, yyyy"
        const val DISPLAY_DATE_TIME = "MMM dd, yyyy HH:mm"
        const val TIME_ONLY = "HH:mm"
    }

    // Database Limits
    object DatabaseLimits {
        const val MIN_DUE_DAY = 1
        const val MAX_DUE_DAY = 31
        const val MAX_HOUSE_NAME_LENGTH = 100
        const val MAX_EXPENSE_NAME_LENGTH = 200
        const val MAX_NOTES_LENGTH = 1000
        const val INVITE_CODE_LENGTH = 6
    }

    // Currency
    object Currency {
        const val DEFAULT_CODE = "USD"
        const val DEFAULT_SYMBOL = "$"
        val SUPPORTED_CURRENCIES = mapOf(
            "USD" to "$",
            "EUR" to "€",
            "GBP" to "£",
            "INR" to "₹",
            "JPY" to "¥"
        )
    }

    // Roles
    object Roles {
        const val OWNER = "Owner"
        const val ADMIN = "Admin"
        const val MEMBER = "Member"
    }

    // Notification Types
    object NotificationTypes {
        const val GENERAL = "general"
        const val EXPENSE = "expense"
        const val EXPENSE_SPLIT = "expense_split"
        const val SETTLEMENT = "settlement"
        const val CHORE = "chore"
        const val SHOPPING = "shopping"
        const val PER_DIEM = "per_diem"
        const val HOUSE_INVITE = "house_invite"
    }

    // Expense Categories
    object ExpenseCategories {
        val DEFAULT_CATEGORIES = listOf(
            "Groceries",
            "Utilities",
            "Rent",
            "Transportation",
            "Entertainment",
            "Healthcare",
            "Other"
        )
    }

    // Per Diem Categories
    object PerDiemCategories {
        val DEFAULT_CATEGORIES = listOf(
            "Water",
            "Electricity",
            "Gas",
            "Internet",
            "Other"
        )
    }

    // Recurrence Patterns
    object RecurrencePatterns {
        const val DAILY = "daily"
        const val WEEKLY = "weekly"
        const val MONTHLY = "monthly"
        const val YEARLY = "yearly"
    }

    // UI Constants
    object UI {
        const val DEBOUNCE_DELAY_MS = 300L
        const val ANIMATION_DURATION_MS = 300L
        const val SNACKBAR_DURATION_MS = 3000L
        const val MAX_LINES_COLLAPSED = 3
        const val STANDARD_PADDING_DP = 24
        const val CARD_SPACING_DP = 20
        const val CARD_CORNER_RADIUS_DP = 12
        const val FAB_CORNER_RADIUS_DP = 16
    }

    // Chart Colors (in hex for easy theming)
    object ChartColors {
        val PIE_CHART_COLORS = listOf(
            0xFF6366F1, // Indigo
            0xFF8B5CF6, // Purple
            0xFFEC4899, // Pink
            0xFFF59E0B, // Amber
            0xFF10B981, // Emerald
            0xFF06B6D4, // Cyan
            0xFFF97316, // Orange
            0xFF84CC16  // Lime
        )

        val BAR_CHART_COLORS = listOf(
            0xFF6366F1, // Indigo
            0xFF8B5CF6, // Purple
            0xFFEC4899, // Pink
            0xFFF59E0B  // Amber
        )
    }

    // Storage Paths
    object StoragePaths {
        const val DOCUMENTS = "documents"
        const val HOUSE_HEADERS = "house-headers"
        const val PROFILES = "profiles"
    }

    // Validation Messages
    object ValidationMessages {
        const val REQUIRED_FIELD = "This field is required"
        const val INVALID_EMAIL = "Please enter a valid email"
        const val INVALID_AMOUNT = "Please enter a valid amount"
        const val INVALID_DUE_DAY = "Due day must be between 1 and 31"
        const val PASSWORD_TOO_SHORT = "Password must be at least 6 characters"
    }

    // Error Messages
    object ErrorMessages {
        const val NETWORK_ERROR = "Network error. Please check your connection."
        const val UNAUTHORIZED = "You are not authorized to perform this action."
        const val NOT_FOUND = "Resource not found."
        const val SERVER_ERROR = "Server error. Please try again later."
        const val UNKNOWN_ERROR = "An unknown error occurred."
        const val NO_USER_LOGGED_IN = "No user logged in"
    }

    // Success Messages
    object SuccessMessages {
        const val EXPENSE_CREATED = "Expense created successfully"
        const val EXPENSE_DELETED = "Expense deleted successfully"
        const val HOUSE_CREATED = "House created successfully"
        const val BALANCE_SETTLED = "Balance settled successfully"
        const val PROFILE_UPDATED = "Profile updated successfully"
    }
}


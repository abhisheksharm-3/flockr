package `in`.xroden.flockr.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using kotlinx.serialization.
 * These replace string-based routes in Screen.kt for compile-time type safety.
 */

// Auth routes
@Serializable object WelcomeRoute
@Serializable object LoginRoute
@Serializable object SignupRoute
@Serializable object OnboardingRoute

// Main routes
@Serializable object HomeRoute
@Serializable data class HouseDetailsRoute(val houseId: String)
@Serializable object NotificationsRoute

// Expense routes
@Serializable data class ExpenseDashboardRoute(val houseId: String)
@Serializable data class OneTimeExpensesRoute(
    val houseId: String,
    val category: String? = null,
    val userId: String? = null
)
@Serializable data class AddExpenseRoute(val houseId: String)
@Serializable data class AddExpenseAdvancedRoute(
    val houseId: String,
    val itemName: String? = null,
    val quantity: Int? = null
)
@Serializable data class EditExpenseRoute(
    val houseId: String,
    val expenseId: String
)
@Serializable data class ExpenseDetailRoute(
    val houseId: String,
    val expenseId: String
)
@Serializable data class BalancesRoute(val houseId: String)
@Serializable data class BalancesDetailedRoute(val houseId: String)
@Serializable data class RecurringExpensesRoute(val houseId: String)
@Serializable data class AddRecurringExpenseRoute(val houseId: String)
@Serializable data class EditRecurringExpenseRoute(
    val houseId: String,
    val expenseId: String
)
@Serializable data class BillHistoryRoute(
    val houseId: String,
    val expenseId: String,
    val expenseName: String
)
@Serializable data class MonthlyReportsRoute(val houseId: String)

// Per Diem routes
@Serializable data class AddPerDiemEntryRoute(
    val houseId: String,
    val configId: String
)
@Serializable data class QuickPerDiemEntryRoute(val houseId: String)
@Serializable data class PerDiemTransactionsRoute(val houseId: String)
@Serializable data class PerDiemConfigRoute(val houseId: String)
@Serializable data class AddPerDiemConfigRoute(val houseId: String)
@Serializable data class EditPerDiemConfigRoute(
    val houseId: String,
    val configId: String,
    val itemName: String,
    val rate: Double,
    val category: String,
    val unit: String
)

// House management routes
@Serializable data class ManageMembersRoute(val houseId: String)
@Serializable data class HouseSettingsRoute(val houseId: String)
@Serializable data class HouseAuditLogRoute(val houseId: String)
@Serializable object CreateHouseRoute
@Serializable object JoinHouseRoute
@Serializable data class JoinHousePreviewRoute(val inviteCode: String)

// Feature routes
@Serializable data class ShoppingListRoute(val houseId: String)
@Serializable data class ShoppingListDetailedRoute(val houseId: String)
@Serializable data class AddShoppingItemRoute(val houseId: String)
@Serializable data class ChoresRoute(val houseId: String)
@Serializable data class ChoresDetailedRoute(val houseId: String)
@Serializable data class AddChoreRoute(val houseId: String)
@Serializable data class ProductivityRoute(val houseId: String)
@Serializable data class ChatRoute(val houseId: String)
@Serializable data class DocumentsRoute(val houseId: String)

// Settings routes
@Serializable object SettingsRoute
@Serializable object EditProfileRoute
@Serializable object NotificationPreferencesRoute
@Serializable object SecuritySettingsRoute

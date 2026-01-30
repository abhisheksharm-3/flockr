package `in`.xroden.flockr.ui.navigation.typed

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using kotlinx.serialization.
 * These routes provide compile-time safety and eliminate string manipulation errors.
 */

@Serializable
object WelcomeRoute

@Serializable
object LoginRoute

@Serializable
object SignupRoute

@Serializable
object OnboardingRoute

@Serializable
object HomeRoute

@Serializable
data class HouseDetailsRoute(val houseId: String)

@Serializable
object NotificationsRoute

@Serializable
data class ExpensesRoute(val houseId: String)

@Serializable
data class AddExpenseRoute(val houseId: String)

@Serializable
data class ShoppingListRoute(val houseId: String)

@Serializable
data class ChoresRoute(val houseId: String)

@Serializable
data class ChatRoute(val houseId: String)

@Serializable
data class DocumentsRoute(val houseId: String)

@Serializable
data class BalancesRoute(val houseId: String)

@Serializable
data class ExpenseDashboardRoute(val houseId: String)

@Serializable
data class AddPerDiemEntryRoute(val houseId: String)

@Serializable
object SettingsRoute

@Serializable
object CreateHouseRoute

@Serializable
object JoinHouseRoute

@Serializable
data class JoinHousePreviewRoute(val inviteCode: String)

@Serializable
data class ManageMembersRoute(val houseId: String)

@Serializable
data class HouseSettingsRoute(val houseId: String)

@Serializable
data class PerDiemConfigRoute(val houseId: String)

@Serializable
object EditProfileRoute

// Finance Screens
@Serializable
data class OneTimeExpensesRoute(
    val houseId: String,
    val category: String? = null,
    val userId: String? = null
)

@Serializable
data class RecurringExpensesRoute(val houseId: String)

@Serializable
data class AddRecurringExpenseRoute(val houseId: String)

@Serializable
data class BillHistoryRoute(
    val houseId: String,
    val expenseId: String,
    val expenseName: String
)

@Serializable
data class MonthlyReportsRoute(val houseId: String)

@Serializable
data class AddExpenseAdvancedRoute(
    val houseId: String,
    val itemName: String? = null,
    val quantity: Int? = null
)

@Serializable
data class BalancesDetailedRoute(val houseId: String)

@Serializable
data class QuickPerDiemEntryRoute(val houseId: String)

@Serializable
data class PerDiemTransactionsRoute(val houseId: String)

// Organization Screens
@Serializable
data class ShoppingListDetailedRoute(val houseId: String)

@Serializable
data class ChoresDetailedRoute(val houseId: String)

// Feature Screens
@Serializable
object NotificationPreferencesRoute

@Serializable
object SecuritySettingsRoute

@Serializable
data class HouseAuditLogRoute(val houseId: String)

@Serializable
data class AddChoreRoute(val houseId: String)

@Serializable
data class ProductivityRoute(val houseId: String)

@Serializable
data class AddShoppingItemRoute(val houseId: String)

@Serializable
data class AddPerDiemConfigRoute(val houseId: String)

@Serializable
data class EditPerDiemConfigRoute(
    val houseId: String,
    val configId: String,
    val itemName: String,
    val rate: String,
    val category: String,
    val unit: String
)

@Serializable
data class ExpenseDetailRoute(
    val houseId: String,
    val expenseId: String
)

@Serializable
data class EditExpenseRoute(
    val houseId: String,
    val expenseId: String
)

@Serializable
data class EditRecurringExpenseRoute(
    val houseId: String,
    val expenseId: String
)


package `in`.xroden.flockr.ui.navigation

import androidx.navigation.NavController
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig

/**
 * Type-safe navigation extensions for NavController.
 * These extensions use @Serializable routes for compile-time safety.
 */

// Auth routes
fun NavController.navigateToWelcome() = navigate(WelcomeRoute)
fun NavController.navigateToLogin() = navigate(LoginRoute)
fun NavController.navigateToSignup() = navigate(SignupRoute)
fun NavController.navigateToOnboarding() = navigate(OnboardingRoute)

// Main routes
fun NavController.navigateToHome(clearBackStack: Boolean = false) {
    navigate(HomeRoute) {
        if (clearBackStack) {
            popUpTo<HomeRoute> { inclusive = true }
        }
    }
}

fun NavController.navigateToNotifications() = navigate(NotificationsRoute)
fun NavController.navigateToSettings() = navigate(SettingsRoute)

// House routes
fun NavController.navigateToCreateHouse() = navigate(CreateHouseRoute)
fun NavController.navigateToJoinHouse() = navigate(JoinHouseRoute)

fun NavController.navigateToJoinHousePreview(inviteCode: String) =
    navigate(JoinHousePreviewRoute(inviteCode))

fun NavController.navigateToHouseDetails(houseId: String, popToHome: Boolean = false) {
    navigate(HouseDetailsRoute(houseId)) {
        if (popToHome) {
            popUpTo<HomeRoute>()
        }
    }
}

fun NavController.navigateToManageMembers(houseId: String) =
    navigate(ManageMembersRoute(houseId))

fun NavController.navigateToHouseSettings(houseId: String) =
    navigate(HouseSettingsRoute(houseId))

fun NavController.navigateToHouseAuditLog(houseId: String) =
    navigate(HouseAuditLogRoute(houseId))

// Expense routes
fun NavController.navigateToExpenseDashboard(houseId: String) =
    navigate(ExpenseDashboardRoute(houseId))

fun NavController.navigateToOneTimeExpenses(
    houseId: String,
    category: String? = null,
    userId: String? = null
) = navigate(OneTimeExpensesRoute(houseId, category, userId))

fun NavController.navigateToAddExpense(houseId: String) =
    navigate(AddExpenseRoute(houseId))

fun NavController.navigateToAddExpenseAdvanced(
    houseId: String,
    itemName: String? = null,
    quantity: Int? = null
) = navigate(AddExpenseAdvancedRoute(houseId, itemName, quantity))

fun NavController.navigateToExpenseDetail(houseId: String, expenseId: String) =
    navigate(ExpenseDetailRoute(houseId, expenseId))

fun NavController.navigateToEditExpense(houseId: String, expenseId: String) =
    navigate(EditExpenseRoute(houseId, expenseId))

fun NavController.navigateToBalances(houseId: String) =
    navigate(BalancesRoute(houseId))

fun NavController.navigateToRecurringExpenses(houseId: String) =
    navigate(RecurringExpensesRoute(houseId))

fun NavController.navigateToAddRecurringExpense(houseId: String) =
    navigate(AddRecurringExpenseRoute(houseId))

fun NavController.navigateToEditRecurringExpense(houseId: String, expenseId: String) =
    navigate(EditRecurringExpenseRoute(houseId, expenseId))

fun NavController.navigateToBillHistory(houseId: String, expenseId: String, expenseName: String) =
    navigate(BillHistoryRoute(houseId, expenseId, expenseName))

fun NavController.navigateToMonthlyReports(houseId: String) =
    navigate(MonthlyReportsRoute(houseId))

// Per Diem routes
fun NavController.navigateToPerDiemConfig(houseId: String) =
    navigate(PerDiemConfigRoute(houseId))

fun NavController.navigateToAddPerDiemConfig(houseId: String) =
    navigate(AddPerDiemConfigRoute(houseId))

fun NavController.navigateToEditPerDiemConfig(houseId: String, config: PerDiemConfig) =
    navigate(EditPerDiemConfigRoute(
        houseId = houseId,
        configId = config.id,
        itemName = config.itemName,
        rate = config.rate.toDouble(),
        category = config.category,
        unit = config.unit
    ))

fun NavController.navigateToQuickPerDiemEntry(houseId: String) =
    navigate(QuickPerDiemEntryRoute(houseId))

fun NavController.navigateToPerDiemTransactions(houseId: String) =
    navigate(PerDiemTransactionsRoute(houseId))

// Shopping routes
fun NavController.navigateToShoppingList(houseId: String) =
    navigate(ShoppingListRoute(houseId))

fun NavController.navigateToAddShoppingItem(houseId: String) =
    navigate(AddShoppingItemRoute(houseId))

// Chores routes
fun NavController.navigateToChores(houseId: String) =
    navigate(ChoresRoute(houseId))

fun NavController.navigateToAddChore(houseId: String) =
    navigate(AddChoreRoute(houseId))

fun NavController.navigateToProductivity(houseId: String) =
    navigate(ProductivityRoute(houseId))

// Chat & Documents routes
fun NavController.navigateToChat(houseId: String) =
    navigate(ChatRoute(houseId))

fun NavController.navigateToDocuments(houseId: String) =
    navigate(DocumentsRoute(houseId))

// Settings routes
fun NavController.navigateToEditProfile() = navigate(EditProfileRoute)
fun NavController.navigateToSecuritySettings() = navigate(SecuritySettingsRoute)
fun NavController.navigateToNotificationPreferences() = navigate(NotificationPreferencesRoute)

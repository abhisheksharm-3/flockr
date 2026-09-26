/** Every screen's type-safe route, grouped auth, main, money, house, features, settings. */
package `in`.xroden.flockr.ui.navigation

import kotlinx.serialization.Serializable

@Serializable object WelcomeRoute
@Serializable object LoginRoute
@Serializable object SignupRoute
@Serializable object OnboardingRoute

@Serializable object HomeRoute
@Serializable data class HouseDetailsRoute(val houseId: String)
@Serializable object NotificationsRoute

@Serializable data class ExpensesRoute(val houseId: String)
@Serializable data class ExpenseDetailRoute(val houseId: String, val expenseId: String)
@Serializable data class ExpenseFormRoute(
    val houseId: String,
    val expenseId: String? = null,
    val prefillName: String? = null,
    val prefillQuantity: Int? = null
)
@Serializable data class BalancesRoute(val houseId: String)

/** [amount] is a plain decimal string, so it crosses navigation without passing through a float. */
@Serializable data class SettleUpRoute(
    val houseId: String,
    val fromUserId: String? = null,
    val toUserId: String? = null,
    val amount: String? = null
)
@Serializable data class BillsRoute(val houseId: String)
@Serializable data class BillFormRoute(val houseId: String, val billId: String? = null)
@Serializable data class BillHistoryRoute(val houseId: String, val billId: String)
@Serializable data class ReportsRoute(val houseId: String)
@Serializable data class PerDiemRoute(val houseId: String)
@Serializable data class PerDiemItemFormRoute(val houseId: String, val configId: String? = null)
@Serializable data class PerDiemEntryFormRoute(val houseId: String, val configId: String? = null)

@Serializable data class ManageMembersRoute(val houseId: String)
@Serializable data class HouseSettingsRoute(val houseId: String)
@Serializable data class HouseAuditLogRoute(val houseId: String)
@Serializable object CreateHouseRoute
@Serializable object JoinHouseRoute
@Serializable data class JoinHousePreviewRoute(val inviteCode: String)

@Serializable data class ShoppingListRoute(val houseId: String)
@Serializable data class ChoresRoute(val houseId: String)
@Serializable data class ChoreFormRoute(val houseId: String, val choreId: String? = null)
@Serializable data class ChatRoute(val houseId: String)
@Serializable data class DocumentsRoute(val houseId: String)

@Serializable object SettingsRoute
@Serializable object EditProfileRoute
@Serializable object NotificationPreferencesRoute
@Serializable object SecuritySettingsRoute

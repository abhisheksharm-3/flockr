/** Where tapping a notification takes the member. */
package `in`.xroden.flockr.ui.navigation

import `in`.xroden.flockr.features.notifications.model.Notification
import `in`.xroden.flockr.features.notifications.model.NotificationType

/**
 * The route a notification opens: the exact expense, or the screen its subject lives on. An
 * invitation opens home, where pending invitations are answered; a kind this build doesn't know
 * opens its house, or home when it has none.
 */
fun Notification.destination(): Any {
    val houseId = houseId ?: return HomeRoute
    return when (kind) {
        NotificationType.EXPENSE_ADDED, NotificationType.EXPENSE_UPDATED, NotificationType.SETTLEMENT_RECEIVED,
        NotificationType.SETTLEMENT_RECORDED, NotificationType.BILL_PAID ->
            dataId("expense_id")?.let { ExpenseDetailRoute(houseId, it) } ?: ExpensesRoute(houseId)
        NotificationType.BILL_DUE -> BillsRoute(houseId)
        NotificationType.CHORE_ASSIGNED, NotificationType.CHORE_COMPLETED -> ChoresRoute(houseId)
        NotificationType.SHOPPING_ITEM_ADDED -> ShoppingListRoute(houseId)
        NotificationType.MESSAGE -> ChatRoute(houseId)
        NotificationType.DOCUMENT_UPLOADED -> DocumentsRoute(houseId)
        NotificationType.MEMBER_JOINED, NotificationType.MEMBER_LEFT, NotificationType.LOCATION_SHARED, null -> HouseDetailsRoute(houseId)
        NotificationType.INVITATION -> HomeRoute
    }
}

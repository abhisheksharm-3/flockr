/** What a house's home screens summarise, loaded once so the houses list and a house's hub agree. */
package `in`.xroden.flockr.features.house.presentation

import `in`.xroden.flockr.features.chat.data.ChatRepository
import `in`.xroden.flockr.features.chat.model.Message
import `in`.xroden.flockr.features.chores.data.ChoreRepository
import `in`.xroden.flockr.features.documents.data.DocumentRepository
import `in`.xroden.flockr.features.chores.model.Chore
import `in`.xroden.flockr.features.expenses.data.ExpenseRepository
import `in`.xroden.flockr.features.expenses.data.RecurringExpenseRepository
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.HouseStanding
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.model.today
import `in`.xroden.flockr.features.shopping.data.ShoppingRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

private const val BILLS_SHOWN = 2
private const val BILL_HORIZON_DAYS = 7
private const val CHORES_SHOWN = 2
private const val RECENT_SHOWN = 3

/**
 * One house at a glance for the viewer. [standing] is null when balances failed to load; every other
 * part falls back to empty, so one failed summary never blanks the screen. [upcomingBills] and
 * [myChores] are already trimmed to what a home screen shows. [recent], [toBuy], [lastMessage] and
 * [documentCount] are only loaded for a house's own hub; [toBuy] and [documentCount] are null when
 * they failed to load, so the hub can say nothing rather than a wrong zero.
 */
data class HouseDigest(
    val members: List<MemberWithProfile>,
    val standing: HouseStanding?,
    val today: LocalDate,
    val upcomingBills: List<RecurringExpense>,
    val myChores: List<Chore>,
    val recent: List<Expense>,
    val toBuy: Int? = null,
    val lastMessage: Message? = null,
    val documentCount: Int? = null,
)

class HouseDigestLoader @Inject constructor(
    private val houseRepository: HouseRepository,
    private val expenseRepository: ExpenseRepository,
    private val billRepository: RecurringExpenseRepository,
    private val choreRepository: ChoreRepository,
    private val shoppingRepository: ShoppingRepository,
    private val chatRepository: ChatRepository,
    private val documentRepository: DocumentRepository,
) {
    /** [forHub] also fetches what only a house's own hub shows: recent spending, and a line from each part of the house. */
    suspend fun load(houseId: String, viewerId: String, forHub: Boolean): HouseDigest = coroutineScope {
        val members = async { houseRepository.getHouseMembers(houseId).getOrElse { emptyList() } }
        val standing = async { expenseRepository.getStanding(houseId).getOrNull() }
        val today = async { houseRepository.getHouseConfig(houseId).getOrNull().today() }
        val bills = async { billRepository.getRecurringExpenses(houseId).getOrElse { emptyList() } }
        val chores = async { choreRepository.getChoresFlow(houseId).first().getOrElse { emptyList() } }
        val recent = async {
            if (forHub) expenseRepository.getExpensesFlow(houseId).first().getOrElse { emptyList() }.take(RECENT_SHOWN) else emptyList()
        }
        val toBuy = async { if (forHub) shoppingRepository.getShoppingItemsFlow(houseId).first().getOrNull()?.count { !it.isPurchased } else null }
        val lastMessage = async { if (forHub) chatRepository.getMessagesFlow(houseId).first().getOrNull()?.maxByOrNull { it.createdAt } else null }
        val documentCount = async { if (forHub) documentRepository.getHouseDocuments(houseId).getOrNull()?.size else null }
        HouseDigest(
            members = members.await(),
            standing = standing.await(),
            today = today.await(),
            upcomingBills = bills.await().filter { it.daysUntilDue <= BILL_HORIZON_DAYS }.sortedBy { it.daysUntilDue }.take(BILLS_SHOWN),
            myChores = chores.await()
                .filter { !it.isCompleted && it.assignedTo == viewerId }
                .sortedWith(compareBy(nullsLast()) { it.dueDate })
                .take(CHORES_SHOWN),
            recent = recent.await(),
            toBuy = toBuy.await(),
            lastMessage = lastMessage.await(),
            documentCount = documentCount.await(),
        )
    }
}

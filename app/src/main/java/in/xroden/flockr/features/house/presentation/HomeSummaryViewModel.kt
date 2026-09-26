/** The houses list's summary across every house: who owes whom, and what is due this week. */
package `in`.xroden.flockr.features.house.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.chores.model.Chore
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.expenses.model.SettleUpPayment
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.HouseCardData
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/** A payment the settle-up plan asks of the viewer or owes them, in one house. [other] is null for a former housemate. */
data class ViewerPayment(val house: HouseCardData, val payment: SettleUpPayment, val other: MemberWithProfile?, val isOwedToViewer: Boolean)

/** A bill or one of the viewer's chores, due soon in [house]. */
sealed interface DueSoon {
    val house: HouseCardData
    data class BillDue(override val house: HouseCardData, val bill: RecurringExpense) : DueSoon
    data class ChoreDue(override val house: HouseCardData, val chore: Chore, val today: LocalDate) : DueSoon
}

/** [payments] lists what is owed to the viewer first; [dueSoon] runs from most overdue to latest. */
data class HomeSummary(
    val payments: List<ViewerPayment>,
    val dueSoon: List<DueSoon>,
    val membersByHouse: Map<String, List<MemberWithProfile>>,
)

@HiltViewModel
class HomeSummaryViewModel @Inject constructor(
    private val houseRepository: HouseRepository,
    private val digestLoader: HouseDigestLoader,
) : ViewModel() {

    private val _summary = MutableStateFlow<HomeSummary?>(null)
    val summary: StateFlow<HomeSummary?> = _summary.asStateFlow()

    private var job: Job? = null

    /** Rebuilds the summary for [houses], replacing any load still running for an older list. */
    fun load(houses: List<HouseCardData>) {
        job?.cancel()
        job = viewModelScope.launch {
            val viewerId = houseRepository.getCurrentUserId().orEmpty()
            val digests = houses.map { house -> async { house to digestLoader.load(house.id, viewerId, forHub = false) } }.awaitAll()
            _summary.value = HomeSummary(
                payments = digests.flatMap { (house, digest) ->
                    val byId = digest.members.associateBy { it.userId }
                    digest.standing?.paymentsOf(viewerId).orEmpty().map { payment ->
                        val isOwed = payment.toUserId == viewerId
                        ViewerPayment(house, payment, byId[if (isOwed) payment.fromUserId else payment.toUserId], isOwed)
                    }
                }.sortedByDescending { it.isOwedToViewer },
                dueSoon = digests.flatMap { (house, digest) ->
                    digest.upcomingBills.map { DueSoon.BillDue(house, it) } + digest.myChores.map { DueSoon.ChoreDue(house, it, digest.today) }
                }.sortedBy(::daysUntil),
                membersByHouse = digests.associate { (house, digest) -> house.id to digest.members.filter { it.isActive } },
            )
        }
    }

    private fun daysUntil(item: DueSoon): Int = when (item) {
        is DueSoon.BillDue -> item.bill.daysUntilDue
        is DueSoon.ChoreDue -> item.chore.dueDate?.let { item.today.daysUntil(it) } ?: Int.MAX_VALUE
    }
}

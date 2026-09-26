/** The expenses hub: the house's activity with where the signed-in user stands, searchable and exportable. */
package `in`.xroden.flockr.features.expenses.presentation

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.core.presentation.Notice
import `in`.xroden.flockr.features.expenses.data.ExpenseRepository
import `in`.xroden.flockr.features.expenses.data.ledgerCsv
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.HouseStanding
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.model.currency
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** How many expenses a page adds as the list scrolls. */
private const val PAGE_SIZE = 50

/** How long typing must pause before a search runs. */
private const val SEARCH_PAUSE_MILLIS = 300L

sealed interface ExpensesUiState {
    data object Loading : ExpensesUiState
    data class Error(val message: String) : ExpensesUiState

    /**
     * [members] includes past members, so old expenses still name who was on them. [hasMore] says an
     * older page exists. [search] is what the list is narrowed to, blank for everything.
     */
    data class Ready(
        val expenses: List<Expense>,
        val members: Map<String, MemberWithProfile>,
        val standing: HouseStanding,
        val viewerId: String,
        val hasMore: Boolean,
        val search: String,
    ) : ExpensesUiState
}

/** The slice of the ledger on screen: the newest [limit] rows, narrowed to [search]. */
private data class Window(val limit: Int = PAGE_SIZE, val search: String = "")

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ExpensesUiState>(ExpensesUiState.Loading)
    val state: StateFlow<ExpensesUiState> = _state.asStateFlow()

    private val _notices = Channel<Notice>(Channel.BUFFERED)
    val notices = _notices.receiveAsFlow()

    private val window = MutableStateFlow(Window())
    private var loadJob: Job? = null

    /**
     * Follows the house's expenses in the current window, re-reading balances and members each time
     * they change. One row past the window is fetched to learn whether an older page exists.
     */
    fun load(houseId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            window
                .debounce { if (it.search.isBlank()) 0L else SEARCH_PAUSE_MILLIS }
                .flatMapLatest { slice -> expenseRepository.getExpensesFlow(houseId, slice.limit + 1, slice.search).map { slice to it } }
                .collect { (slice, result) ->
                    _state.value = result.fold(
                        onSuccess = { rows -> ready(houseId, rows.take(slice.limit), rows.size > slice.limit, slice.search) },
                        onFailure = { ExpensesUiState.Error(it.userMessage()) },
                    )
                }
        }
    }

    /** Adds the next older page, once the list nears its end. */
    fun loadMore() {
        val current = _state.value as? ExpensesUiState.Ready ?: return
        if (current.hasMore) window.update { it.copy(limit = it.limit + PAGE_SIZE) }
    }

    fun search(text: String) = window.update { Window(search = text) }

    /** Writes every expense and payment the house has, as CSV, to [destination]. */
    fun export(houseId: String, destination: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val expenses = async { expenseRepository.getAllExpenses(houseId).getOrThrow() }
                    val members = async { houseRepository.getHouseMembers(houseId).getOrThrow() }
                    val currency = houseRepository.getHouseConfig(houseId).getOrNull().currency()
                    val csv = ledgerCsv(expenses.await(), members.await(), currency)
                    withContext(Dispatchers.IO) {
                        resolver.openOutputStream(destination)?.use { it.write(csv.toByteArray()) } ?: error("Couldn't write that file")
                    }
                    expenses.await().size
                }
            }.fold(
                onSuccess = { count -> _notices.send(Notice("Exported $count ${if (count == 1) "entry" else "entries"}", isError = false)) },
                onFailure = { _notices.send(Notice(it.userMessage(), isError = true)) },
            )
        }
    }

    private suspend fun ready(houseId: String, expenses: List<Expense>, hasMore: Boolean, search: String): ExpensesUiState = coroutineScope {
        val standing = async { expenseRepository.getStanding(houseId) }
        val members = async { houseRepository.getHouseMembers(houseId) }
        runCatching {
            ExpensesUiState.Ready(
                expenses = expenses,
                members = members.await().getOrThrow().associateBy { it.userId },
                standing = standing.await().getOrThrow(),
                viewerId = expenseRepository.getCurrentUserId().orEmpty(),
                hasMore = hasMore,
                search = search,
            )
        }.getOrElse { ExpensesUiState.Error(it.userMessage()) }
    }
}

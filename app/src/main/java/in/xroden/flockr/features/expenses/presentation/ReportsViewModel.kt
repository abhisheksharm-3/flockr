/** A calendar month's spending: the totals, where it went, who paid, and per-diem by item. */
package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.expenses.data.ExpenseAnalyticsRepository
import `in`.xroden.flockr.features.expenses.data.PerDiemRepository
import `in`.xroden.flockr.features.expenses.model.MonthlySummary
import `in`.xroden.flockr.features.expenses.model.PerDiemBillItemized
import `in`.xroden.flockr.features.expenses.model.SpendByCategory
import `in`.xroden.flockr.features.expenses.model.SpendByMember
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.today
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

sealed interface ReportsUiState {
    data object Loading : ReportsUiState
    data class Error(val message: String) : ReportsUiState
    data class Ready(
        val summary: MonthlySummary,
        val byCategory: List<SpendByCategory>,
        val byMember: List<SpendByMember>,
        val usage: List<PerDiemBillItemized>,
    ) : ReportsUiState
}

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val analyticsRepository: ExpenseAnalyticsRepository,
    private val perDiemRepository: PerDiemRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ReportsUiState>(ReportsUiState.Loading)
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    private val _month = MutableStateFlow<LocalDate?>(null)

    /** The first day of the month shown; the house's current month until the user picks another. */
    val month: StateFlow<LocalDate?> = _month.asStateFlow()

    private var houseId: String? = null
    private var loadJob: Job? = null

    fun load(houseId: String) {
        this.houseId = houseId
        viewModelScope.launch {
            val month = _month.value ?: houseRepository.getHouseConfig(houseId).getOrNull().today().let { LocalDate(it.year, it.month, 1) }
            onMonthChange(month)
        }
    }

    fun onMonthChange(month: LocalDate) {
        val houseId = houseId ?: return
        _month.value = month
        loadJob?.cancel()
        _state.value = ReportsUiState.Loading
        loadJob = viewModelScope.launch {
            _state.value = runCatching {
                coroutineScope {
                    val summary = async { analyticsRepository.getMonthlySummary(houseId, month).getOrThrow() }
                    val byCategory = async { analyticsRepository.getSpendByCategory(houseId, month).getOrThrow() }
                    val byMember = async { analyticsRepository.getSpendByMember(houseId, month).getOrThrow() }
                    val usage = async { perDiemRepository.getPerDiemBill(houseId, month).getOrThrow() }
                    ReportsUiState.Ready(summary.await(), byCategory.await(), byMember.await(), usage.await())
                }
            }.getOrElse { ReportsUiState.Error(it.userMessage()) }
        }
    }
}

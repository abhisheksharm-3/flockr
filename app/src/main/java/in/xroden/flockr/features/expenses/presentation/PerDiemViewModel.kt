/** A month of per-diem usage: the items, who used what, the log, and billing the month. */
package `in`.xroden.flockr.features.expenses.presentation

import `in`.xroden.flockr.core.presentation.Notice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.expenses.data.PerDiemRepository
import `in`.xroden.flockr.features.expenses.model.PerDiemEntryWithDetails
import `in`.xroden.flockr.features.expenses.model.PerDiemMonth
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.today
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

sealed interface PerDiemUiState {
    data object Loading : PerDiemUiState
    data class Error(val message: String) : PerDiemUiState
    data class Ready(val data: PerDiemMonth, val viewerId: String, val isBilling: Boolean = false) : PerDiemUiState
}

@HiltViewModel
class PerDiemViewModel @Inject constructor(
    private val perDiemRepository: PerDiemRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<PerDiemUiState>(PerDiemUiState.Loading)
    val state: StateFlow<PerDiemUiState> = _state.asStateFlow()

    private val _month = MutableStateFlow<LocalDate?>(null)

    /** The first day of the month shown; the house's current month until the user picks another. */
    val month: StateFlow<LocalDate?> = _month.asStateFlow()

    private val _messages = Channel<Notice>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var loadJob: Job? = null
    private var houseId: String? = null

    fun load(houseId: String) {
        this.houseId = houseId
        if (_month.value == null) {
            viewModelScope.launch {
                val today = houseRepository.getHouseConfig(houseId).getOrNull().today()
                _month.value = LocalDate(today.year, today.month, 1)
                follow(houseId)
            }
        } else {
            follow(houseId)
        }
    }

    fun onMonthChange(month: LocalDate) {
        _month.value = month
        houseId?.let(::follow)
    }

    private fun follow(houseId: String) {
        val month = _month.value ?: return
        loadJob?.cancel()
        _state.value = PerDiemUiState.Loading
        loadJob = viewModelScope.launch {
            perDiemRepository.getMonthFlow(houseId, month).collect { result ->
                _state.value = result.fold(
                    onSuccess = { PerDiemUiState.Ready(it, houseRepository.getCurrentUserId().orEmpty()) },
                    onFailure = { PerDiemUiState.Error(it.userMessage()) },
                )
            }
        }
    }

    /** Bills the shown month to the signed-in user as the one who paid for it. */
    fun billMonth() {
        val houseId = houseId ?: return
        val month = _month.value ?: return
        val ready = _state.value as? PerDiemUiState.Ready ?: return
        if (ready.isBilling) return
        _state.value = ready.copy(isBilling = true)
        viewModelScope.launch {
            val result = perDiemRepository.billMonth(houseId, month)
            _state.update { (it as? PerDiemUiState.Ready)?.copy(isBilling = false) ?: it }
            _messages.send(result.fold({ Notice("Billed. Everyone now owes you for what they used.", isError = false) }, { Notice(it.userMessage(), isError = true) }))
        }
    }

    fun updateEntry(entry: PerDiemEntryWithDetails, quantity: BigDecimal, date: LocalDate, notes: String?) {
        viewModelScope.launch {
            perDiemRepository.updatePerDiemEntry(entry.id, quantity, date, notes)
                .onFailure { _messages.send(Notice(it.userMessage(), isError = true)) }
        }
    }

    fun deleteEntry(entry: PerDiemEntryWithDetails) {
        viewModelScope.launch {
            perDiemRepository.deletePerDiemEntry(entry.id).onFailure { _messages.send(Notice(it.userMessage(), isError = true)) }
        }
    }
}

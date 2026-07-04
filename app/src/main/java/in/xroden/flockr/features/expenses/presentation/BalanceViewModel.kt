package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.dto.expense.DebtBreakdownItem
import `in`.xroden.flockr.features.expenses.data.IExpenseAnalyticsRepository
import `in`.xroden.flockr.features.expenses.data.ITransactionRepository
import `in`.xroden.flockr.features.expenses.domain.usecase.SettleBalanceUseCase
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.model.HouseConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class BalanceViewModel @Inject constructor(
    private val analyticsRepository: IExpenseAnalyticsRepository,
    private val transactionRepository: ITransactionRepository,
    private val houseRepository: IHouseRepository,
    private val settleBalanceUseCase: SettleBalanceUseCase
) : ViewModel() {

    private val _balanceState = MutableStateFlow<BalanceUiState>(BalanceUiState.Loading)
    val balanceState: StateFlow<BalanceUiState> = _balanceState.asStateFlow()

    private val _debtBreakdownState = MutableStateFlow<Map<String, List<DebtBreakdownItem>>>(emptyMap())
    val debtBreakdownState: StateFlow<Map<String, List<DebtBreakdownItem>>> = _debtBreakdownState.asStateFlow()

    private val _loadingBreakdowns = MutableStateFlow<Set<String>>(emptySet())
    val loadingBreakdowns: StateFlow<Set<String>> = _loadingBreakdowns.asStateFlow()

    private val _houseConfigState = MutableStateFlow<HouseConfig?>(null)
    val houseConfig: StateFlow<HouseConfig?> = _houseConfigState.asStateFlow()

    fun loadBalances(houseId: String) {
        viewModelScope.launch {
            _balanceState.value = BalanceUiState.Loading
            val userId = houseRepository.getCurrentUserId()
            if (userId == null) {
                _balanceState.value = BalanceUiState.Error("You are not signed in")
                return@launch
            }
            // Pairwise balances relative to the current user — correct for houses of any size.
            analyticsRepository.getPairwiseBalances(houseId, userId).fold(
                onSuccess = { balances ->
                    _balanceState.value = BalanceUiState.Success(balances)
                },
                onFailure = { error ->
                    _balanceState.value = BalanceUiState.Error(
                        message = error.message ?: "Failed to load balances"
                    )
                }
            )
        }
    }

    fun loadDebtBreakdown(houseId: String, payerId: String, payeeId: String) {
        viewModelScope.launch {
            val key = "${payerId}_${payeeId}"
            _loadingBreakdowns.value = _loadingBreakdowns.value + key

            analyticsRepository.getDebtBreakdown(houseId, payerId, payeeId).fold(
                onSuccess = { breakdown ->
                    _debtBreakdownState.value = _debtBreakdownState.value + (key to breakdown)
                    _loadingBreakdowns.value = _loadingBreakdowns.value - key
                },
                onFailure = {
                    _loadingBreakdowns.value = _loadingBreakdowns.value - key
                }
            )
        }
    }

    fun settleBalance(
        houseId: String,
        currentUserId: String,
        payeeId: String,
        payerName: String,
        payeeName: String,
        amount: BigDecimal,
        notes: String?
    ) {
        viewModelScope.launch {
            val tz = _houseConfigState.value?.timezone
                ?.let { runCatching { kotlinx.datetime.TimeZone.of(it) }.getOrNull() }
                ?: kotlinx.datetime.TimeZone.currentSystemDefault()
            val date = Clock.System.now().toLocalDateTime(tz).date

            settleBalanceUseCase(
                houseId = houseId,
                payerId = currentUserId,
                payeeId = payeeId,
                amount = amount,
                payerName = payerName,
                payeeName = payeeName,
                notes = notes,
                date = date
            ).fold(
                onSuccess = { loadBalances(houseId) },
                onFailure = { error ->
                    _balanceState.value = BalanceUiState.Error(
                        message = error.message ?: "Failed to settle balance"
                    )
                }
            )
        }
    }

    fun getCurrentUserId(): String? = houseRepository.getCurrentUserId()

    fun loadHouseConfig(houseId: String) {
        viewModelScope.launch {
            houseRepository.getHouseConfig(houseId).fold(
                onSuccess = { config -> _houseConfigState.value = config },
                onFailure = { _houseConfigState.value = null }
            )
        }
    }
}

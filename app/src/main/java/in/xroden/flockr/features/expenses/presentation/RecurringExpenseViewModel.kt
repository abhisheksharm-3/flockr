package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.enums.ExpenseFrequency
import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.data.IRecurringExpenseRepository
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.expenses.model.PaymentHistory
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class RecurringExpenseViewModel @Inject constructor(
    private val recurringExpenseRepository: IRecurringExpenseRepository,
    private val houseRepository: IHouseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecurringExpenseUiState>(RecurringExpenseUiState.Loading)
    val uiState: StateFlow<RecurringExpenseUiState> = _uiState.asStateFlow()

    private val _createState = MutableStateFlow<CreateExpenseUiState>(CreateExpenseUiState.Idle)
    val createState: StateFlow<CreateExpenseUiState> = _createState.asStateFlow()

    private val _paymentHistoryState = MutableStateFlow<List<PaymentHistory>>(emptyList())
    val paymentHistoryState: StateFlow<List<PaymentHistory>> = _paymentHistoryState.asStateFlow()

    private val _houseConfig = MutableStateFlow<HouseConfig?>(null)
    val houseConfig: StateFlow<HouseConfig?> = _houseConfig.asStateFlow()

    private var currentHouseId: String? = null

    fun loadRecurringExpenses(houseId: String) {
        val skipLoading = currentHouseId == houseId && _uiState.value is RecurringExpenseUiState.Success
        currentHouseId = houseId

        viewModelScope.launch {
            if (!skipLoading) {
                _uiState.value = RecurringExpenseUiState.Loading
            }

            recurringExpenseRepository.getRecurringExpenses(houseId).fold(
                onSuccess = { expenses ->
                    _uiState.value = RecurringExpenseUiState.Success(expenses)
                },
                onFailure = { error ->
                    _uiState.value = RecurringExpenseUiState.Error(
                        message = error.message ?: "Failed to load recurring expenses",
                        cause = error
                    )
                }
            )
        }
    }

    fun createRecurringExpense(
        houseId: String,
        name: String,
        amount: BigDecimal,
        dueDay: Int,
        category: String,
        frequency: ExpenseFrequency = ExpenseFrequency.MONTHLY,
        customFrequencyDays: Int? = null,
        reminderDaysBefore: Int = 3,
        reminderEnabled: Boolean = true,
        notes: String? = null,
        splitWith: List<String>? = null,
        splitType: ExpenseSplitType? = null,
        splitAmounts: Map<String, BigDecimal>? = null,
        prepayEnabled: Boolean = false,
        firstPaymentDate: LocalDate? = null
    ) {
        viewModelScope.launch {
            _createState.value = CreateExpenseUiState.Loading
            
            recurringExpenseRepository.createRecurringExpense(
                houseId = houseId,
                name = name,
                amount = amount,
                dueDay = dueDay,
                category = category,
                frequency = frequency,
                customFrequencyDays = customFrequencyDays,
                reminderDaysBefore = reminderDaysBefore,
                reminderEnabled = reminderEnabled,
                notes = notes,
                splitWith = splitWith,
                splitType = splitType,
                splitAmounts = splitAmounts,
                prepayEnabled = prepayEnabled,
                firstPaymentDate = firstPaymentDate
            ).fold(
                onSuccess = {
                    _createState.value = CreateExpenseUiState.Success
                    loadRecurringExpenses(houseId)
                },
                onFailure = { error ->
                    _createState.value = CreateExpenseUiState.Error(
                        message = error.message ?: "Failed to create recurring expense"
                    )
                }
            )
        }
    }

    fun updateRecurringExpense(
        houseId: String,
        expenseId: String,
        name: String?,
        amount: BigDecimal?,
        dueDay: Int?,
        category: String?,
        isActive: Boolean?
    ) {
        viewModelScope.launch {
            recurringExpenseRepository.updateRecurringExpense(
                expenseId = expenseId,
                name = name,
                amount = amount,
                dueDay = dueDay,
                category = category,
                isActive = isActive,
                frequency = null,
                lastPaidDate = null,
                customFrequencyDays = null,
                reminderDaysBefore = null,
                reminderEnabled = null,
                notes = null,
                splitWith = null,
                splitType = null,
                splitAmounts = null
            ).fold(
                onSuccess = {
                    loadRecurringExpenses(houseId)
                },
                onFailure = { error ->
                    _uiState.value = RecurringExpenseUiState.Error(
                        message = error.message ?: "Failed to update expense",
                        cause = error
                    )
                }
            )
        }
    }

    fun markAsPaid(
        houseId: String,
        expenseId: String,
        amount: BigDecimal,
        paymentDate: LocalDate
    ) {
        viewModelScope.launch {
            _uiState.value = RecurringExpenseUiState.Loading
            
            recurringExpenseRepository.markRecurringExpenseAsPaid(expenseId, amount, paymentDate).fold(
                onSuccess = {
                    loadRecurringExpenses(houseId)
                },
                onFailure = { error ->
                    _uiState.value = RecurringExpenseUiState.Error(
                        message = error.message ?: "Failed to mark expense as paid",
                        cause = error
                    )
                }
            )
        }
    }

    fun deleteRecurringExpense(houseId: String, expenseId: String) {
        viewModelScope.launch {
            recurringExpenseRepository.deleteRecurringExpense(expenseId).fold(
                onSuccess = {
                    loadRecurringExpenses(houseId)
                },
                onFailure = { error ->
                    _uiState.value = RecurringExpenseUiState.Error(
                        message = error.message ?: "Failed to delete expense",
                        cause = error
                    )
                }
            )
        }
    }

    fun resetCreateState() {
        _createState.value = CreateExpenseUiState.Idle
    }

    fun loadPaymentHistory(recurringExpenseId: String) {
        viewModelScope.launch {
            recurringExpenseRepository.getPaymentHistory(recurringExpenseId).fold(
                onSuccess = { history ->
                    _paymentHistoryState.value = history
                },
                onFailure = {
                    _paymentHistoryState.value = emptyList()
                }
            )
        }
    }

    fun loadHouseConfig(houseId: String) {
        viewModelScope.launch {
            houseRepository.getHouseConfig(houseId).fold(
                onSuccess = { config ->
                    _houseConfig.value = config
                },
                onFailure = {
                    _houseConfig.value = null
                }
            )
        }
    }

    suspend fun getHouseMembers(houseId: String): List<MemberWithProfile> {
        return houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }
    }
}

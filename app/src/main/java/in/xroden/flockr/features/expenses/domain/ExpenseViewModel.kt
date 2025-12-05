package `in`.xroden.flockr.features.expenses.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.data.ExpenseRepository
import `in`.xroden.flockr.features.expenses.data.PerDiemRepository
import `in`.xroden.flockr.features.house.data.HouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import javax.inject.Inject
import `in`.xroden.flockr.features.expenses.model.OneTimeExpense

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val perDiemRepository: PerDiemRepository,
    private val houseRepository: HouseRepository
) : ViewModel() {

    private val _expenseState =
        MutableStateFlow<OneTimeExpenseUiState>(OneTimeExpenseUiState.Loading)
    val expenseState: StateFlow<OneTimeExpenseUiState> = _expenseState.asStateFlow()

    private val _balanceState = MutableStateFlow<BalanceUiState>(BalanceUiState.Loading)
    val balanceState: StateFlow<BalanceUiState> = _balanceState.asStateFlow()

    private val _debtBreakdownState =
        MutableStateFlow<Map<String, List<ExpenseRepository.DebtBreakdownItem>>>(emptyMap())
    val debtBreakdownState: StateFlow<Map<String, List<ExpenseRepository.DebtBreakdownItem>>> =
        _debtBreakdownState.asStateFlow()

    private val _loadingBreakdowns = MutableStateFlow<Set<String>>(emptySet())
    val loadingBreakdowns: StateFlow<Set<String>> = _loadingBreakdowns.asStateFlow()

    private val _summaryState =
        MutableStateFlow<MonthlySummaryUiState>(MonthlySummaryUiState.Loading)
    val summaryState: StateFlow<MonthlySummaryUiState> = _summaryState.asStateFlow()

    private val _createState = MutableStateFlow<CreateExpenseUiState>(CreateExpenseUiState.Idle)
    val createState: StateFlow<CreateExpenseUiState> = _createState.asStateFlow()

    private val _houseConfigState =
        MutableStateFlow<`in`.xroden.flockr.features.house.model.HouseConfig?>(null)
    val houseConfig: StateFlow<`in`.xroden.flockr.features.house.model.HouseConfig?> =
        _houseConfigState.asStateFlow()

    private val _perDiemBillItemizedState = MutableStateFlow<Map<String, BigDecimal>>(emptyMap())
    val perDiemBillItemized: StateFlow<Map<String, BigDecimal>> =
        _perDiemBillItemizedState.asStateFlow()

    private val _spendByMemberState =
        MutableStateFlow<List<`in`.xroden.flockr.features.expenses.model.SpendByMember>>(emptyList())
    val spendByMember: StateFlow<List<`in`.xroden.flockr.features.expenses.model.SpendByMember>> =
        _spendByMemberState.asStateFlow()

    private val _spendByCategoryState =
        MutableStateFlow<List<`in`.xroden.flockr.features.expenses.model.SpendByCategory>>(emptyList())
    val spendByCategory: StateFlow<List<`in`.xroden.flockr.features.expenses.model.SpendByCategory>> =
        _spendByCategoryState.asStateFlow()

    fun getCurrentUserId(): String? = expenseRepository.getCurrentUserId()

    fun loadExpenses(houseId: String) {
        viewModelScope.launch {
            _expenseState.value = OneTimeExpenseUiState.Loading

            expenseRepository.getOneTimeExpensesFlow(houseId).collect { result ->
                result.fold(
                    onSuccess = { expenses ->
                        _expenseState.value = OneTimeExpenseUiState.Success(expenses)
                    },
                    onFailure = { error ->
                        _expenseState.value = OneTimeExpenseUiState.Error(
                            message = error.message ?: "Failed to load expenses",
                            cause = error
                        )
                    }
                )
            }
        }
    }

    private val _selectedExpenseState = MutableStateFlow<OneTimeExpense?>(null)
    val selectedExpense: StateFlow<OneTimeExpense?> = _selectedExpenseState.asStateFlow()

    fun loadOneTimeExpense(expenseId: String) {
        viewModelScope.launch {
            expenseRepository.getOneTimeExpense(expenseId).fold(
                onSuccess = { expense ->
                    _selectedExpenseState.value = expense
                },
                onFailure = {
                    _selectedExpenseState.value = null
                }
            )
        }
    }

        fun loadMonthlySummary(houseId: String, month: String) {
            viewModelScope.launch {
                android.util.Log.d(
                    "ExpenseViewModel",
                    "loadMonthlySummary called - houseId: $houseId, month: $month"
                )
                _summaryState.value = MonthlySummaryUiState.Loading

                val summaryResult = expenseRepository.getMonthlySummary(houseId, month)
                val memberResult = expenseRepository.getSpendByMember(houseId, month)
                val categoryResult = expenseRepository.getSpendByCategory(houseId, month)

                if (summaryResult.isSuccess) {
                    val summary = summaryResult.getOrNull()
                    if (summary != null) {
                        android.util.Log.d(
                            "ExpenseViewModel",
                            "Summary loaded successfully: ${summary.totalExpenses}"
                        )
                        _summaryState.value = MonthlySummaryUiState.Success(
                            summary = summary,
                            spendByMember = memberResult.getOrElse { emptyList() },
                            spendByCategory = categoryResult.getOrElse { emptyList() }
                        )
                    } else {
                        android.util.Log.w("ExpenseViewModel", "Summary result was null")
                        _summaryState.value =
                            MonthlySummaryUiState.Error("No summary data available")
                    }
                } else {
                    val error = summaryResult.exceptionOrNull()
                    android.util.Log.e("ExpenseViewModel", "Failed to load summary", error)
                    _summaryState.value = MonthlySummaryUiState.Error(
                        message = error?.message ?: "Failed to load summary"
                    )
                }
            }
        }

        fun createOneTimeExpense(
            houseId: String,
            name: String,
            amount: BigDecimal,
            category: String,
            date: LocalDate,
            notes: String?,
            splitWith: List<String>?,
            splitType: ExpenseSplitType?,
            splitAmounts: Map<String, BigDecimal>?
        ) {
            viewModelScope.launch {
                _createState.value = CreateExpenseUiState.Loading

                expenseRepository.createOneTimeExpense(
                    houseId = houseId,
                    name = name,
                    amount = amount,
                    category = category,
                    date = date,
                    notes = notes,
                    splitWith = splitWith,
                    splitType = splitType,
                    splitAmounts = splitAmounts
                ).fold(
                    onSuccess = {
                        _createState.value = CreateExpenseUiState.Success
                        kotlinx.coroutines.delay(1000)
                        _createState.value = CreateExpenseUiState.Idle
                    },
                    onFailure = { error ->
                        _createState.value = CreateExpenseUiState.Error(
                            message = error.message ?: "Failed to create expense"
                        )
                    }
                )
            }
        }

        fun updateOneTimeExpense(
            houseId: String,
            expenseId: String,
            name: String?,
            amount: BigDecimal?,
            category: String?,
            date: LocalDate?,
            notes: String?,
            splitAmounts: Map<String, BigDecimal>? = null
        ) {
            viewModelScope.launch {
                expenseRepository.updateOneTimeExpense(
                    expenseId = expenseId,
                    name = name,
                    amount = amount,
                    category = category,
                    date = date,
                    notes = notes,
                    splitAmounts = splitAmounts
                ).fold(
                    onSuccess = {
                        // Success - state updated via flow
                    },
                    onFailure = { error ->
                        _expenseState.value = OneTimeExpenseUiState.Error(
                            message = error.message ?: "Failed to update expense",
                            cause = error
                        )
                    }
                )
            }
        }

        fun deleteOneTimeExpense(houseId: String, expenseId: String) {
            viewModelScope.launch {
                expenseRepository.deleteOneTimeExpense(expenseId).fold(
                    onSuccess = {
                        // Success - state updated via flow
                    },
                    onFailure = { error ->
                        _expenseState.value = OneTimeExpenseUiState.Error(
                            message = error.message ?: "Failed to delete expense",
                            cause = error
                        )
                    }
                )
            }
        }

        fun settleBalance(
            houseId: String,
            payerId: String,
            payeeId: String,
            amount: BigDecimal,
            description: String?
        ) {
            viewModelScope.launch {
                expenseRepository.settleBalance(
                    houseId = houseId,
                    payerId = payerId,
                    payeeId = payeeId,
                    amount = amount,
                    description = description
                ).fold(
                    onSuccess = {
                        loadBalances(houseId)
                    },
                    onFailure = { error ->
                        _balanceState.value = BalanceUiState.Error(
                            message = error.message ?: "Failed to settle balance"
                        )
                    }
                )
            }
        }

        suspend fun getHouseMembers(houseId: String) =
            houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }

        suspend fun getHouseConfig(houseId: String) =
            houseRepository.getHouseConfig(houseId).getOrNull()

        fun resetCreateState() {
            _createState.value = CreateExpenseUiState.Idle
        }

        fun loadHouseConfig(houseId: String) {
            viewModelScope.launch {
                houseRepository.getHouseConfig(houseId).fold(
                    onSuccess = { config ->
                        _houseConfigState.value = config
                    },
                    onFailure = { error ->
                        // Log error but don't update UI state
                        _houseConfigState.value = null
                    }
                )
            }
        }

        fun loadPerDiemBillItemized(houseId: String, month: String) {
            viewModelScope.launch {
                perDiemRepository.getPerDiemBill(houseId, month).fold(
                    onSuccess = { billList ->
                        // Convert List<PerDiemBillItemized> to Map<String, BigDecimal>
                        val billMap = billList.associate { it.itemName to it.totalAmount }
                        _perDiemBillItemizedState.value = billMap
                    },
                    onFailure = { error ->
                        _perDiemBillItemizedState.value = emptyMap()
                    }
                )
            }
        }

        fun loadSpendByMember(houseId: String, month: String) {
            viewModelScope.launch {
                expenseRepository.getSpendByMember(houseId, month).fold(
                    onSuccess = { spending ->
                        _spendByMemberState.value = spending
                    },
                    onFailure = { error ->
                        _spendByMemberState.value = emptyList()
                    }
                )
            }
        }

        fun loadSpendByCategory(houseId: String, month: String) {
            viewModelScope.launch {
                expenseRepository.getSpendByCategory(houseId, month).fold(
                    onSuccess = { spending ->
                        _spendByCategoryState.value = spending
                    },
                    onFailure = { error ->
                        _spendByCategoryState.value = emptyList()
                    }
                )
            }
        }

        fun loadRecurringExpenses(houseId: String) {
            viewModelScope.launch {
                _expenseState.value = OneTimeExpenseUiState.Loading

                expenseRepository.getRecurringExpenses(houseId).fold(
                    onSuccess = { expenses ->
                        // We need to convert RecurringExpense list to something compatible with OneTimeExpenseUiState
                        // For now, just mark as success with empty list - BillsScreen will need separate state
                        _expenseState.value = OneTimeExpenseUiState.Success(emptyList())
                    },
                    onFailure = { error ->
                        _expenseState.value = OneTimeExpenseUiState.Error(
                            message = error.message ?: "Failed to load recurring expenses",
                            cause = error
                        )
                    }
                )
            }
        }

        fun markRecurringExpenseAsPaid(
            houseId: String,
            expenseId: String,
            amount: BigDecimal,
            paymentDate: LocalDate
        ) {
            viewModelScope.launch {
                expenseRepository.markRecurringExpenseAsPaid(expenseId, amount, paymentDate).fold(
                    onSuccess = {
                        loadRecurringExpenses(houseId)
                    },
                    onFailure = { error ->
                        _expenseState.value = OneTimeExpenseUiState.Error(
                            message = error.message ?: "Failed to record payment",
                            cause = error
                        )
                    }
                )
            }
        }

        fun deleteRecurringExpense(houseId: String, expenseId: String) {
            viewModelScope.launch {
                expenseRepository.deleteRecurringExpense(expenseId).fold(
                    onSuccess = {
                        loadRecurringExpenses(houseId)
                    },
                    onFailure = { error ->
                        _expenseState.value = OneTimeExpenseUiState.Error(
                            message = error.message ?: "Failed to delete expense",
                            cause = error
                        )
                    }
                )
            }
        }

        fun formatAmount(amount: BigDecimal, currencySymbol: String = "$"): String {
            return "$currencySymbol${String.format("%.2f", amount.toDouble())}"
        }

        fun formatDueStatusMessage(
            dueStatus: `in`.xroden.flockr.data.enums.ExpenseDueStatus?,
            daysUntilDue: Int?
        ): String {
            return when (dueStatus) {
                `in`.xroden.flockr.data.enums.ExpenseDueStatus.OVERDUE -> "Overdue"
                `in`.xroden.flockr.data.enums.ExpenseDueStatus.DUE_TODAY -> "Due Today"
                `in`.xroden.flockr.data.enums.ExpenseDueStatus.DUE_SOON -> "Due in $daysUntilDue days"
                `in`.xroden.flockr.data.enums.ExpenseDueStatus.UPCOMING -> "Upcoming in $daysUntilDue days"
                else -> "Not scheduled"
            }
        }

        fun getFrequencyDescription(frequency: `in`.xroden.flockr.data.enums.ExpenseFrequency): String {
            return when (frequency) {
                `in`.xroden.flockr.data.enums.ExpenseFrequency.DAILY -> "Daily"
                `in`.xroden.flockr.data.enums.ExpenseFrequency.WEEKLY -> "Weekly"
                `in`.xroden.flockr.data.enums.ExpenseFrequency.BIWEEKLY -> "Bi-weekly"
                `in`.xroden.flockr.data.enums.ExpenseFrequency.MONTHLY -> "Monthly"
                `in`.xroden.flockr.data.enums.ExpenseFrequency.QUARTERLY -> "Quarterly"
                `in`.xroden.flockr.data.enums.ExpenseFrequency.SEMIANNUAL -> "Semi-annual"
                `in`.xroden.flockr.data.enums.ExpenseFrequency.ANNUAL -> "Annual"
                `in`.xroden.flockr.data.enums.ExpenseFrequency.CUSTOM -> "Custom"
            }
        }
        fun loadBalances(houseId: String) {
            viewModelScope.launch {
                _balanceState.value = BalanceUiState.Loading
                expenseRepository.getUserBalances(houseId).fold(
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
                
                expenseRepository.getDebtBreakdown(houseId, payerId, payeeId).fold(
                    onSuccess = { breakdown ->
                        _debtBreakdownState.value = _debtBreakdownState.value + (key to breakdown)
                        _loadingBreakdowns.value = _loadingBreakdowns.value - key
                    },
                    onFailure = {
                         // On error, just clear loading state
                        _loadingBreakdowns.value = _loadingBreakdowns.value - key
                    }
                )
            }
        }
    }

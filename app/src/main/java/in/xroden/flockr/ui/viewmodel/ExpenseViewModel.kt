package `in`.xroden.flockr.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.*
import `in`.xroden.flockr.data.repository.ExpenseRepository
import `in`.xroden.flockr.data.repository.HouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val houseRepository: HouseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExpenseUiState>(ExpenseUiState.Loading)
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    private val _balances = MutableStateFlow<List<UserBalance>>(emptyList())
    val balances: StateFlow<List<UserBalance>> = _balances.asStateFlow()

    private val _monthlySummary = MutableStateFlow<MonthlySummary?>(null)
    val monthlySummary: StateFlow<MonthlySummary?> = _monthlySummary.asStateFlow()

    private val _spendByMember = MutableStateFlow<List<SpendByMember>>(emptyList())
    val spendByMember: StateFlow<List<SpendByMember>> = _spendByMember.asStateFlow()

    private val _spendByCategory = MutableStateFlow<List<SpendByCategory>>(emptyList())
    val spendByCategory: StateFlow<List<SpendByCategory>> = _spendByCategory.asStateFlow()

    private val _perDiemBillItemized = MutableStateFlow<List<PerDiemBillItemized>>(emptyList())
    val perDiemBillItemized: StateFlow<List<PerDiemBillItemized>> = _perDiemBillItemized.asStateFlow()

    private val _perDiemBillByMember = MutableStateFlow<List<PerDiemBillByMember>>(emptyList())
    val perDiemBillByMember: StateFlow<List<PerDiemBillByMember>> = _perDiemBillByMember.asStateFlow()

    private val _houseConfig = MutableStateFlow<HouseConfig?>(null)
    val houseConfig: StateFlow<HouseConfig?> = _houseConfig.asStateFlow()

    private val _recurringExpenses = MutableStateFlow<List<RecurringExpense>>(emptyList())
    val recurringExpenses: StateFlow<List<RecurringExpense>> = _recurringExpenses.asStateFlow()

    fun getCurrentUserId(): String? = expenseRepository.getCurrentUserId()

    fun loadExpenses(houseId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "loadExpenses: Collecting expenses flow from repository")
                expenseRepository.getOneTimeExpensesFlow(houseId).collect { expenses ->
                    Log.d(TAG, "loadExpenses: Received ${expenses.size} expenses")
                    _uiState.value = ExpenseUiState.Success(expenses, recurringExpenses.value)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadExpenses: Failed to load expenses", e)
                _uiState.value = ExpenseUiState.Error(e.message ?: "Failed to load expenses")
            }
        }
    }


    fun loadBalances(houseId: String) {
        Log.d(TAG, "loadBalances() called for houseId=$houseId")
        viewModelScope.launch {
            try {
                Log.d(TAG, "loadBalances: Fetching balances from repository")
                val result = expenseRepository.getUserBalances(houseId)
                Log.d(TAG, "loadBalances: Received ${result.size} balances")
                _balances.value = result
            } catch (e: Exception) {
                Log.e(TAG, "loadBalances: Failed to load balances", e)
            }
        }
    }

    fun loadMonthlySummary(houseId: String, month: String) {
        Log.d(TAG, "loadMonthlySummary() called for houseId=$houseId, month=$month")
        viewModelScope.launch {
            try {
                Log.d(TAG, "loadMonthlySummary: Fetching summary from repository")
                val summary = expenseRepository.getMonthlySummary(houseId, month)
                Log.d(TAG, "loadMonthlySummary: Received summary - totalExpenses=${summary?.totalExpenses}")
                _monthlySummary.value = summary
            } catch (e: Exception) {
                Log.e(TAG, "loadMonthlySummary: Failed to load monthly summary", e)
            }
        }
    }

    fun loadSpendByMember(houseId: String, month: String) {
        Log.d(TAG, "loadSpendByMember() called for houseId=$houseId, month=$month")
        viewModelScope.launch {
            try {
                Log.d(TAG, "loadSpendByMember: Fetching from repository")
                val result = expenseRepository.getSpendByMember(houseId, month)
                Log.d(TAG, "loadSpendByMember: Received ${result.size} members")
                _spendByMember.value = result
            } catch (e: Exception) {
                Log.e(TAG, "loadSpendByMember: Failed to load spend by member", e)
            }
        }
    }

    fun loadSpendByCategory(houseId: String, month: String) {
        Log.d(TAG, "loadSpendByCategory() called for houseId=$houseId, month=$month")
        viewModelScope.launch {
            try {
                Log.d(TAG, "loadSpendByCategory: Fetching from repository")
                val result = expenseRepository.getSpendByCategory(houseId, month)
                Log.d(TAG, "loadSpendByCategory: Received ${result.size} categories")
                _spendByCategory.value = result
            } catch (e: Exception) {
                Log.e(TAG, "loadSpendByCategory: Failed to load spend by category", e)
            }
        }
    }

    fun loadPerDiemBillItemized(houseId: String, month: String) {
        Log.d(TAG, "loadPerDiemBillItemized() called for houseId=$houseId, month=$month")
        viewModelScope.launch {
            try {
                Log.d(TAG, "loadPerDiemBillItemized: Fetching from repository")
                val result = expenseRepository.getPerDiemBillItemized(houseId, month)
                Log.d(TAG, "loadPerDiemBillItemized: Received ${result.size} items")
                _perDiemBillItemized.value = result
            } catch (e: Exception) {
                Log.e(TAG, "loadPerDiemBillItemized: Failed to load per-diem bill itemized", e)
            }
        }
    }

    fun loadPerDiemBillByMember(houseId: String, month: String) {
        Log.d(TAG, "loadPerDiemBillByMember() called for houseId=$houseId, month=$month")
        viewModelScope.launch {
            try {
                Log.d(TAG, "loadPerDiemBillByMember: Fetching from repository")
                val result = expenseRepository.getPerDiemBillByMember(houseId, month)
                Log.d(TAG, "loadPerDiemBillByMember: Received ${result.size} members")
                _perDiemBillByMember.value = result
            } catch (e: Exception) {
                Log.e(TAG, "loadPerDiemBillByMember: Failed to load per-diem bill by member", e)
            }
        }
    }

    fun loadHouseConfig(houseId: String) {
        Log.d(TAG, "loadHouseConfig() called for houseId=$houseId")
        viewModelScope.launch {
            try {
                Log.d(TAG, "loadHouseConfig: Fetching from repository")
                val config = houseRepository.getHouseConfig(houseId)
                Log.d(TAG, "loadHouseConfig: Received config - currency=${config?.currencySymbol}")
                _houseConfig.value = config
            } catch (e: Exception) {
                Log.e(TAG, "loadHouseConfig: Failed to load house config", e)
            }
        }
    }

    fun loadRecurringExpenses(houseId: String) {
        Log.d(TAG, "loadRecurringExpenses() called for houseId=$houseId")
        viewModelScope.launch {
            try {
                Log.d(TAG, "loadRecurringExpenses: Fetching from repository - NOT IMPLEMENTED YET")
                // TODO: Implement recurring expenses repository method
                _recurringExpenses.value = emptyList()
                _uiState.value = ExpenseUiState.Success(_uiState.value.let {
                    if (it is ExpenseUiState.Success) it.expenses else emptyList()
                }, emptyList())
            } catch (e: Exception) {
                Log.e(TAG, "loadRecurringExpenses: Failed to load recurring expenses", e)
            }
        }
    }

    fun createExpense(
        houseId: String,
        name: String,
        amount: Double,
        date: String,
        category: String,
        notes: String?,
        splits: List<Pair<String, Double>>?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        Log.d(TAG, "createExpense() called - name=$name, amount=$amount, houseId=$houseId, hasSplits=${splits != null}")
        viewModelScope.launch {
            try {
                Log.d(TAG, "createExpense: Calling repository")
                expenseRepository.createOneTimeExpense(
                    houseId, name, amount, date, category, notes, splits
                ).fold(
                    onSuccess = {
                        Log.d(TAG, "createExpense: Success, reloading expenses")
                        loadExpenses(houseId)
                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "createExpense: Failed", error)
                        val errorMessage = error.message ?: "Failed to create expense"
                        _uiState.value = ExpenseUiState.Error(errorMessage)
                        onError(errorMessage)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "createExpense: Exception occurred", e)
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun settleBalance(
        houseId: String,
        payeeId: String,
        amount: Double,
        description: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        Log.d(TAG, "settleBalance() called - payeeId=$payeeId, amount=$amount, houseId=$houseId")
        viewModelScope.launch {
            try {
                Log.d(TAG, "settleBalance: Calling repository")
                expenseRepository.settleBalance(houseId, payeeId, amount, description).fold(
                    onSuccess = {
                        Log.d(TAG, "settleBalance: Success, reloading balances")
                        loadBalances(houseId)
                        onSuccess()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "settleBalance: Failed", error)
                        onError(error.message ?: "Failed to settle balance")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "settleBalance: Exception occurred", e)
                onError(e.message ?: "Unknown error")
            }
        }
    }

    companion object {
        private const val TAG = "ExpenseViewModel"
    }
}

sealed class ExpenseUiState {
    object Loading : ExpenseUiState()
    data class Success(
        val expenses: List<OneTimeExpense> = emptyList(),
        val recurringExpenses: List<RecurringExpense> = emptyList()
    ) : ExpenseUiState()
    data class Error(val message: String) : ExpenseUiState()
}


package `in`.xroden.flockr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.*
import `in`.xroden.flockr.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
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

    fun loadExpenses(houseId: String) {
        viewModelScope.launch {
            _uiState.value = ExpenseUiState.Loading
            try {
                val expenses = expenseRepository.getOneTimeExpenses(houseId)
                _uiState.value = ExpenseUiState.Success(expenses)
            } catch (e: Exception) {
                _uiState.value = ExpenseUiState.Error(e.message ?: "Failed to load expenses")
            }
        }
    }

    fun loadBalances(houseId: String) {
        viewModelScope.launch {
            try {
                val result = expenseRepository.getUserBalances(houseId)
                _balances.value = result
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadMonthlySummary(houseId: String, month: String) {
        viewModelScope.launch {
            try {
                val summary = expenseRepository.getMonthlySummary(houseId, month)
                _monthlySummary.value = summary
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadSpendByMember(houseId: String, month: String) {
        viewModelScope.launch {
            try {
                val result = expenseRepository.getSpendByMember(houseId, month)
                _spendByMember.value = result
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadSpendByCategory(houseId: String, month: String) {
        viewModelScope.launch {
            try {
                val result = expenseRepository.getSpendByCategory(houseId, month)
                _spendByCategory.value = result
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadPerDiemBillItemized(houseId: String, month: String) {
        viewModelScope.launch {
            try {
                val result = expenseRepository.getPerDiemBillItemized(houseId, month)
                _perDiemBillItemized.value = result
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun loadPerDiemBillByMember(houseId: String, month: String) {
        viewModelScope.launch {
            try {
                val result = expenseRepository.getPerDiemBillByMember(houseId, month)
                _perDiemBillByMember.value = result
            } catch (e: Exception) {
                // Handle error
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
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            expenseRepository.createOneTimeExpense(
                houseId, name, amount, date, category, notes, splits
            ).fold(
                onSuccess = {
                    loadExpenses(houseId)
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.value = ExpenseUiState.Error(error.message ?: "Failed to create expense")
                }
            )
        }
    }

    fun settleBalance(houseId: String, payeeId: String, amount: Double, description: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            expenseRepository.settleBalance(houseId, payeeId, amount, description).fold(
                onSuccess = {
                    loadBalances(houseId)
                    onSuccess()
                },
                onFailure = { error ->
                    // Handle error
                }
            )
        }
    }
}

sealed class ExpenseUiState {
    object Loading : ExpenseUiState()
    data class Success(val expenses: List<OneTimeExpense>) : ExpenseUiState()
    data class Error(val message: String) : ExpenseUiState()
}


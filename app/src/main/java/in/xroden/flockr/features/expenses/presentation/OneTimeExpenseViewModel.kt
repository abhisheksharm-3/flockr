package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.expenses.data.IExpenseRepository
import `in`.xroden.flockr.features.expenses.model.OneTimeExpense
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.model.HouseConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OneTimeExpenseViewModel @Inject constructor(
    private val expenseRepository: IExpenseRepository,
    private val houseRepository: IHouseRepository
) : ViewModel() {

    private val _expenseState = MutableStateFlow<OneTimeExpenseUiState>(OneTimeExpenseUiState.Loading)
    val expenseState: StateFlow<OneTimeExpenseUiState> = _expenseState.asStateFlow()

    private val _selectedExpenseState = MutableStateFlow<OneTimeExpense?>(null)
    val selectedExpense: StateFlow<OneTimeExpense?> = _selectedExpenseState.asStateFlow()

    private val _houseConfigState = MutableStateFlow<HouseConfig?>(null)
    val houseConfig: StateFlow<HouseConfig?> = _houseConfigState.asStateFlow()

    private var expenseJob: Job? = null
    private var currentHouseId: String? = null

    fun getCurrentUserId(): String? = expenseRepository.getCurrentUserId()

    fun loadExpenses(houseId: String) {
        if (currentHouseId == houseId && expenseJob?.isActive == true) return

        expenseJob?.cancel()
        currentHouseId = houseId

        expenseJob = viewModelScope.launch {
            if (_expenseState.value !is OneTimeExpenseUiState.Success) {
                _expenseState.value = OneTimeExpenseUiState.Loading
            }

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

    fun loadOneTimeExpense(expenseId: String) {
        viewModelScope.launch {
            expenseRepository.getOneTimeExpense(expenseId).fold(
                onSuccess = { expense -> _selectedExpenseState.value = expense },
                onFailure = { _selectedExpenseState.value = null }
            )
        }
    }

    fun deleteOneTimeExpense(houseId: String, expenseId: String) {
        viewModelScope.launch {
            expenseRepository.deleteOneTimeExpense(expenseId).onFailure { error ->
                _expenseState.value = OneTimeExpenseUiState.Error(
                    message = error.message ?: "Failed to delete expense",
                    cause = error
                )
            }
        }
    }

    fun loadHouseConfig(houseId: String) {
        viewModelScope.launch {
            houseRepository.getHouseConfig(houseId).fold(
                onSuccess = { config -> _houseConfigState.value = config },
                onFailure = { _houseConfigState.value = null }
            )
        }
    }

    suspend fun getHouseMembers(houseId: String) =
        houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }
}

package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.data.IExpenseRepository
import `in`.xroden.flockr.features.expenses.domain.usecase.CreateOneTimeExpenseUseCase
import `in`.xroden.flockr.features.expenses.model.OneTimeExpense
import `in`.xroden.flockr.features.house.data.IHouseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import java.math.BigDecimal
import javax.inject.Inject

/**
 * ViewModel for managing one-time expenses.
 * Handles CRUD operations and reactive expense list.
 */
@HiltViewModel
class OneTimeExpenseViewModel @Inject constructor(
    private val expenseRepository: IExpenseRepository,
    private val houseRepository: IHouseRepository,
    private val createExpenseUseCase: CreateOneTimeExpenseUseCase
) : ViewModel() {

    private val _expenseState = MutableStateFlow<OneTimeExpenseUiState>(OneTimeExpenseUiState.Loading)
    val expenseState: StateFlow<OneTimeExpenseUiState> = _expenseState.asStateFlow()

    private val _selectedExpenseState = MutableStateFlow<OneTimeExpense?>(null)
    val selectedExpense: StateFlow<OneTimeExpense?> = _selectedExpenseState.asStateFlow()

    private val _createState = MutableStateFlow<CreateExpenseUiState>(CreateExpenseUiState.Idle)
    val createState: StateFlow<CreateExpenseUiState> = _createState.asStateFlow()

    private val _houseConfigState = MutableStateFlow<`in`.xroden.flockr.features.house.model.HouseConfig?>(null)
    val houseConfig: StateFlow<`in`.xroden.flockr.features.house.model.HouseConfig?> = _houseConfigState.asStateFlow()

    private val _events = Channel<OneTimeExpenseEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var expenseJob: Job? = null
    private var currentHouseId: String? = null

    fun getCurrentUserId(): String? = expenseRepository.getCurrentUserId()

    fun loadExpenses(houseId: String) {
        // Skip if already loading the same house
        if (currentHouseId == houseId && expenseJob?.isActive == true) return

        expenseJob?.cancel()
        currentHouseId = houseId

        expenseJob = viewModelScope.launch {
            // Only show loading on first load
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
                onSuccess = { expense ->
                    _selectedExpenseState.value = expense
                },
                onFailure = {
                    _selectedExpenseState.value = null
                }
            )
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

            val currentUserId = getCurrentUserId() ?: run {
                _createState.value = CreateExpenseUiState.Error("User not authenticated")
                return@launch
            }

            createExpenseUseCase(
                houseId = houseId,
                name = name,
                amount = amount,
                category = category,
                paidBy = currentUserId,
                date = date,
                notes = notes,
                splitWith = splitWith ?: emptyList(),
                splitType = splitType,
                customAmounts = splitAmounts
            ).fold(
                onSuccess = {
                    _createState.value = CreateExpenseUiState.Success
                    _events.send(OneTimeExpenseEvent.ExpenseCreated)
                    // Realtime flow subscription handles list updates automatically
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
        date: LocalDate?,
        category: String?,
        notes: String?,
        splitAmounts: Map<String, BigDecimal>? = null
    ) {
        viewModelScope.launch {
            expenseRepository.updateOneTimeExpense(
                expenseId = expenseId,
                name = name,
                amount = amount,
                date = date,
                category = category,
                notes = notes,
                splitAmounts = splitAmounts
            ).onSuccess {
                // Realtime flow subscription handles list updates automatically
            }.onFailure { error ->
                _expenseState.value = OneTimeExpenseUiState.Error(
                    message = error.message ?: "Failed to update expense",
                    cause = error
                )
            }
        }
    }

    fun deleteOneTimeExpense(houseId: String, expenseId: String) {
        viewModelScope.launch {
            expenseRepository.deleteOneTimeExpense(expenseId).fold(
                onSuccess = {
                    // Realtime flow subscription handles list updates automatically
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

    fun loadHouseConfig(houseId: String) {
        viewModelScope.launch {
            houseRepository.getHouseConfig(houseId).fold(
                onSuccess = { config ->
                    _houseConfigState.value = config
                },
                onFailure = {
                    _houseConfigState.value = null
                }
            )
        }
    }

    suspend fun getHouseMembers(houseId: String) =
        houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }

    fun resetCreateState() {
        _createState.value = CreateExpenseUiState.Idle
    }
}

sealed class OneTimeExpenseEvent {
    object ExpenseCreated : OneTimeExpenseEvent()
}


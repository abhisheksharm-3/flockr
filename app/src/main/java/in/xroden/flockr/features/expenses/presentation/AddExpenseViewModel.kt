package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.domain.usecase.CreateOneTimeExpenseUseCase
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.model.HouseConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val houseRepository: IHouseRepository,
    private val createExpenseUseCase: CreateOneTimeExpenseUseCase
) : ViewModel() {

    private val _formState = MutableStateFlow(AddExpenseFormState())
    val formState: StateFlow<AddExpenseFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<AddExpenseUiState>(AddExpenseUiState.Idle)
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _houseConfig = MutableStateFlow<HouseConfig?>(null)
    val houseConfig: StateFlow<HouseConfig?> = _houseConfig.asStateFlow()

    private var submitJob: Job? = null

    private val _events = Channel<AddExpenseEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun initialize(houseId: String, initialName: String?, initialQuantity: Int?) {
        viewModelScope.launch {
            val houseConfig = houseRepository.getHouseConfig(houseId).getOrNull()
            _houseConfig.value = houseConfig
            val members = houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }

            val today = resolveToday(houseConfig)
            val initialNotes = if (initialQuantity != null) "Quantity: $initialQuantity" else ""

            _formState.value = AddExpenseFormState(
                name = initialName ?: "",
                date = today,
                notes = initialNotes,
                houseMembers = members,
                currencySymbol = houseConfig?.getCurrencySymbol() ?: "$"
            )
        }
    }

    private fun resolveToday(houseConfig: HouseConfig?): LocalDate {
        val tz = houseConfig?.timezone?.let { runCatching { TimeZone.of(it) }.getOrNull() }
            ?: TimeZone.currentSystemDefault()
        return Clock.System.todayIn(tz)
    }

    fun getCurrentUserId(): String? = houseRepository.getCurrentUserId()

    fun onNameChange(name: String) {
        _formState.value = _formState.value.copy(name = name)
    }

    fun onAmountChange(amount: String) {
        _formState.value = _formState.value.copy(amount = amount)
    }

    fun onDateChange(date: LocalDate) {
        _formState.value = _formState.value.copy(date = date)
    }

    fun onNotesChange(notes: String) {
        _formState.value = _formState.value.copy(notes = notes)
    }

    fun onCategoryChange(category: String) {
        _formState.value = _formState.value.copy(category = category)
    }

    fun onSplitEnabledChange(enabled: Boolean) {
        _formState.value = _formState.value.copy(isSplitEnabled = enabled)
    }

    fun onSplitEqualChange(equal: Boolean) {
        _formState.value = _formState.value.copy(isSplitEqual = equal)
    }

    fun onMemberSelectionChange(userId: String, selected: Boolean) {
        val current = _formState.value.selectedMemberIds
        _formState.value = _formState.value.copy(
            selectedMemberIds = if (selected) current + userId else current - userId
        )
    }

    fun onCustomSplitChange(userId: String, amount: String) {
        _formState.value = _formState.value.copy(
            customSplits = _formState.value.customSplits + (userId to amount)
        )
    }

    fun submit(houseId: String) {
        val form = _formState.value
        val amount = form.amount.toBigDecimalOrNull() ?: return
        val date = form.date ?: return
        val currentUserId = houseRepository.getCurrentUserId() ?: run {
            _uiState.value = AddExpenseUiState.Error("User not authenticated")
            return
        }

        val splitWith = form.selectedMemberIds.takeIf { form.isSplitEnabled && it.isNotEmpty() }?.toList()
        val splitType = if (form.isSplitEnabled && form.selectedMemberIds.isNotEmpty()) {
            if (form.isSplitEqual) ExpenseSplitType.EQUAL else ExpenseSplitType.AMOUNT
        } else null
        val splitAmounts = if (form.isSplitEnabled && !form.isSplitEqual && form.selectedMemberIds.isNotEmpty()) {
            form.selectedMemberIds.mapNotNull { userId ->
                form.customSplits[userId]?.toBigDecimalOrNull()?.let { userId to it }
            }.toMap()
        } else null

        // Guard a rapid double-tap from enqueuing two identical expenses.
        if (submitJob?.isActive == true) return
        submitJob = viewModelScope.launch {
            _uiState.value = AddExpenseUiState.Loading

            createExpenseUseCase(
                houseId = houseId,
                name = form.name,
                amount = amount,
                category = form.category,
                paidBy = currentUserId,
                date = date,
                notes = form.notes.takeIf { it.isNotBlank() },
                splitWith = splitWith ?: emptyList(),
                splitType = splitType,
                customAmounts = splitAmounts
            ).fold(
                onSuccess = {
                    _uiState.value = AddExpenseUiState.Success
                    _events.send(AddExpenseEvent.ExpenseAdded)
                },
                onFailure = { error ->
                    _uiState.value = AddExpenseUiState.Error(error.message ?: "Failed to create expense")
                }
            )
        }
    }

    fun resetUiState() {
        _uiState.value = AddExpenseUiState.Idle
    }
}

sealed class AddExpenseEvent {
    data object ExpenseAdded : AddExpenseEvent()
}

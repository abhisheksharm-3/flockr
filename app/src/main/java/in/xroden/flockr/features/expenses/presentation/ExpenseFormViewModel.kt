package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.expenses.model.SplitMethod
import `in`.xroden.flockr.features.expenses.data.ExpenseRepository
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.model.today
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import javax.inject.Inject

/**
 * Adds an expense or edits an existing one. Both save [ExpenseFormState.shares], so the split the
 * user previews is the split that is saved.
 */
@HiltViewModel
class ExpenseFormViewModel @Inject constructor(
    private val houseRepository: HouseRepository,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val _formState = MutableStateFlow(ExpenseFormState())
    val formState: StateFlow<ExpenseFormState> = _formState.asStateFlow()

    private val _uiState = MutableStateFlow<ExpenseFormUiState>(ExpenseFormUiState.Idle)
    val uiState: StateFlow<ExpenseFormUiState> = _uiState.asStateFlow()

    private val _houseConfig = MutableStateFlow<HouseConfig?>(null)
    val houseConfig: StateFlow<HouseConfig?> = _houseConfig.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)

    /** Emits once each time the expense has been written, which is the only time to leave the form. */
    val saved = _saved.receiveAsFlow()

    private val _savedAndReset = Channel<Unit>(Channel.BUFFERED)

    /** Emits after "add and start another" saved, once the form has been cleared for the next expense. */
    val savedAndReset = _savedAndReset.receiveAsFlow()

    private var blankForm = ExpenseFormState()

    private var saveJob: Job? = null

    /**
     * Prepares the form. With [expenseId] it loads that expense for editing; otherwise it starts a
     * new expense dated today in the house, optionally prefilled from a shopping item.
     */
    fun initialize(houseId: String, expenseId: String?, initialName: String?, initialQuantity: Int?) {
        viewModelScope.launch {
            val config = houseRepository.getHouseConfig(houseId).getOrNull()
            _houseConfig.value = config
            val viewerId = houseRepository.getCurrentUserId().orEmpty()
            val members = houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }
            val base = ExpenseFormState(
                name = initialName.orEmpty(),
                date = config.today(),
                notes = initialQuantity?.let { "Quantity: $it" }.orEmpty(),
                houseMembers = members,
                split = SplitDraft.everyone(members),
                currencyCode = config.currency(),
                payerId = viewerId,
                viewerId = viewerId,
                isLoaded = expenseId == null,
            )
            _formState.value = base
            blankForm = base
            if (expenseId == null) return@launch

            expenseRepository.getExpense(expenseId).fold(
                onSuccess = { _formState.value = ExpenseFormState.editing(it, base) },
                onFailure = { _uiState.value = ExpenseFormUiState.Error(it.userMessage()) },
            )
        }
    }

    fun onNameChange(name: String) = _formState.update { it.copy(name = name) }
    fun onAmountChange(amount: String) = _formState.update { it.copy(amount = amount) }
    fun onDateChange(date: LocalDate) = _formState.update { it.copy(date = date) }
    fun onNotesChange(notes: String) = _formState.update { it.copy(notes = notes) }
    fun onCategoryChange(category: String) = _formState.update { it.copy(category = category) }
    fun onPayerChange(userId: String) = _formState.update { it.copy(payerId = userId) }
    fun onSplitEnabledChange(enabled: Boolean) = _formState.update { it.copy(split = it.split.copy(isEnabled = enabled)) }
    fun onSplitMethodChange(method: SplitMethod) = _formState.update { it.copy(split = it.split.withMethod(method, it.houseMembers)) }
    fun onParticipantChange(userId: String, isIncluded: Boolean) = _formState.update { it.copy(split = it.split.withParticipant(userId, isIncluded)) }
    fun onSplitValueChange(userId: String, value: String) = _formState.update { it.copy(split = it.split.withValue(userId, value)) }

    fun dismissError() {
        _uiState.value = ExpenseFormUiState.Idle
    }

    /**
     * Saves the expense. With [startAnother] the form stays open, cleared for the next one but
     * keeping the date, category, payer and who it is split between, for entering a batch of receipts.
     */
    fun save(houseId: String, startAnother: Boolean = false) {
        val form = _formState.value
        val date = form.date ?: return
        val amount = form.parsedAmount ?: return fail("Enter an amount in ${form.currencyCode}")
        val shares = form.shares ?: return fail("The split doesn't add up to the total yet")
        if (saveJob?.isActive == true) return

        saveJob = viewModelScope.launch {
            _uiState.value = ExpenseFormUiState.Saving
            expenseRepository.saveExpense(
                houseId = houseId,
                expenseId = form.expenseId,
                name = form.name,
                amount = amount,
                category = form.category,
                date = date,
                notes = form.notes.takeIf { it.isNotBlank() },
                splitMethod = form.split.savedMethod,
                shares = shares,
            ).fold(
                onSuccess = {
                    _uiState.value = ExpenseFormUiState.Idle
                    if (startAnother) {
                        _formState.value = blankForm.copy(
                            date = form.date,
                            category = form.category,
                            payerId = form.payerId,
                            split = form.split.copy(values = if (form.split.method == SplitMethod.SHARES) form.split.values else emptyMap()),
                        )
                        _savedAndReset.send(Unit)
                    } else {
                        _saved.send(Unit)
                    }
                },
                onFailure = { fail(it.userMessage()) },
            )
        }
    }

    private fun fail(message: String) {
        _uiState.value = ExpenseFormUiState.Error(message)
    }
}

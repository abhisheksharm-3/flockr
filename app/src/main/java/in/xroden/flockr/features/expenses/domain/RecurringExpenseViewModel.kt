package `in`.xroden.flockr.features.expenses.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.RecurringExpense
import `in`.xroden.flockr.data.model.RecurringExpenseInsert
import `in`.xroden.flockr.data.model.RecurringExpenseUpdate
import `in`.xroden.flockr.data.model.PaymentHistoryInsert
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class RecurringExpenseViewModel @Inject constructor(
    private val supabase: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecurringExpenseUiState>(RecurringExpenseUiState.Loading)
    val uiState: StateFlow<RecurringExpenseUiState> = _uiState.asStateFlow()

    fun loadRecurringExpenses(houseId: String) {
        viewModelScope.launch {
            _uiState.value = RecurringExpenseUiState.Loading
            try {
                val expenses = supabase.from("recurring_expenses")
                    .select(Columns.ALL) {
                        filter {
                            eq("house_id", houseId)
                        }
                        order("name", Order.ASCENDING)
                    }
                    .decodeList<RecurringExpense>()

                _uiState.value = RecurringExpenseUiState.Success(expenses)
            } catch (e: Exception) {
                _uiState.value = RecurringExpenseUiState.Error(e.message ?: "Failed to load expenses")
            }
        }
    }

    fun addRecurringExpense(houseId: String, name: String, amount: Double, dueDay: Int, category: String) {
        viewModelScope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch

                val expenseInsert = RecurringExpenseInsert(
                    houseId = houseId,
                    name = name,
                    amount = amount,
                    dueDay = dueDay,
                    category = category,
                    createdBy = userId
                )

                supabase.from("recurring_expenses")
                    .insert(expenseInsert) {
                        select()
                    }

                loadRecurringExpenses(houseId)
            } catch (e: Exception) {
                android.util.Log.e("RecurringExpenseViewModel", "Error adding expense", e)
            }
        }
    }

    fun createRecurringExpense(
        houseId: String,
        name: String,
        amount: Double,
        dueDay: Int,
        category: String,
        frequency: String = "monthly",
        customFrequencyDays: Int? = null,
        reminderDaysBefore: Int = 3,
        reminderEnabled: Boolean = true,
        notes: String? = null,
        splitWith: List<String>? = null,
        splitType: String? = null,
        splitAmounts: Map<String, Double>? = null,
        prepayEnabled: Boolean = false,
        firstPaymentDate: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: run {
                    onError("No user logged in")
                    return@launch
                }

                val expenseInsert = RecurringExpenseInsert(
                    houseId = houseId,
                    name = name,
                    amount = amount,
                    dueDay = dueDay,
                    category = category,
                    createdBy = userId,
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
                )

                supabase.from("recurring_expenses")
                    .insert(expenseInsert) {
                        select()
                    }

                loadRecurringExpenses(houseId)
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("RecurringExpenseViewModel", "Error creating expense", e)
                onError(e.message ?: "Failed to create recurring expense")
            }
        }
    }

    fun markAsPaid(expenseId: String, houseId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                val paymentInsert = PaymentHistoryInsert(
                    recurringExpenseId = expenseId,
                    paidBy = userId,
                    amount = amount,
                    paymentDate = currentDate
                )

                supabase.from("payment_history")
                    .insert(paymentInsert) {
                        select()
                    }

                loadRecurringExpenses(houseId)
            } catch (e: Exception) {
                android.util.Log.e("RecurringExpenseViewModel", "Error marking as paid", e)
            }
        }
    }

    fun toggleActive(expenseId: String, houseId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                val update = RecurringExpenseUpdate(isActive = isActive)

                supabase.from("recurring_expenses")
                    .update(update) {
                        filter {
                            eq("id", expenseId)
                        }
                    }

                loadRecurringExpenses(houseId)
            } catch (e: Exception) {
                android.util.Log.e("RecurringExpenseViewModel", "Error toggling active status", e)
            }
        }
    }

    fun deleteRecurringExpense(expenseId: String, houseId: String) {
        viewModelScope.launch {
            try {
                supabase.from("recurring_expenses")
                    .delete {
                        filter {
                            eq("id", expenseId)
                        }
                    }

                loadRecurringExpenses(houseId)
            } catch (e: Exception) {
                android.util.Log.e("RecurringExpenseViewModel", "Error deleting expense", e)
            }
        }
    }
}

sealed class RecurringExpenseUiState {
    object Loading : RecurringExpenseUiState()
    data class Success(val expenses: List<RecurringExpense>) : RecurringExpenseUiState()
    data class Error(val message: String) : RecurringExpenseUiState()
}


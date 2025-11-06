package `in`.xroden.flockr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.RecurringExpense
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

                supabase.from("recurring_expenses")
                    .insert(
                        mapOf<String, Any>(
                            "house_id" to houseId,
                            "name" to name,
                            "amount" to amount,
                            "due_day" to dueDay,
                            "category" to category,
                            "created_by" to userId
                        )
                    )

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
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: run {
                    onError("No user logged in")
                    return@launch
                }

                supabase.from("recurring_expenses")
                    .insert(
                        mapOf<String, Any>(
                            "house_id" to houseId,
                            "name" to name,
                            "amount" to amount,
                            "due_day" to dueDay,
                            "category" to category,
                            "created_by" to userId
                        )
                    )

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

                supabase.from("payment_history")
                    .insert(
                        mapOf<String, Any>(
                            "recurring_expense_id" to expenseId,
                            "paid_by" to userId,
                            "amount" to amount,
                            "payment_date" to currentDate
                        )
                    )

                // Could show a snackbar here
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun toggleActive(expenseId: String, houseId: String, isActive: Boolean) {
        viewModelScope.launch {
            try {
                supabase.from("recurring_expenses")
                    .update(
                        mapOf("is_active" to isActive)
                    ) {
                        filter {
                            eq("id", expenseId)
                        }
                    }

                loadRecurringExpenses(houseId)
            } catch (e: Exception) {
                // Handle error
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
                // Handle error
            }
        }
    }
}

sealed class RecurringExpenseUiState {
    object Loading : RecurringExpenseUiState()
    data class Success(val expenses: List<RecurringExpense>) : RecurringExpenseUiState()
    data class Error(val message: String) : RecurringExpenseUiState()
}


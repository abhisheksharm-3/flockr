package `in`.xroden.flockr.features.expenses.presentation

import `in`.xroden.flockr.features.expenses.model.MonthlySummary
import `in`.xroden.flockr.features.expenses.model.OneTimeExpense
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.expenses.model.SpendByCategory
import `in`.xroden.flockr.features.expenses.model.SpendByMember
import `in`.xroden.flockr.features.expenses.model.Transaction
import `in`.xroden.flockr.features.expenses.model.UserBalance

sealed interface OneTimeExpenseUiState {
    data object Loading : OneTimeExpenseUiState
    data class Success(val expenses: List<OneTimeExpense>) : OneTimeExpenseUiState
    data class Error(val message: String, val cause: Throwable? = null) : OneTimeExpenseUiState
}

sealed interface RecurringExpenseUiState {
    data object Loading : RecurringExpenseUiState
    data class Success(val expenses: List<RecurringExpense>) : RecurringExpenseUiState
    data class Error(val message: String, val cause: Throwable? = null) : RecurringExpenseUiState
}

sealed interface TransactionUiState {
    data object Loading : TransactionUiState
    data class Success(val transactions: List<Transaction>) : TransactionUiState
    data class Error(val message: String, val cause: Throwable? = null) : TransactionUiState
}

sealed interface BalanceUiState {
    data object Loading : BalanceUiState
    data class Success(val balances: List<UserBalance>) : BalanceUiState
    data class Error(val message: String) : BalanceUiState
}

sealed interface MonthlySummaryUiState {
    data object Loading : MonthlySummaryUiState
    data class Success(
        val summary: MonthlySummary,
        val spendByMember: List<SpendByMember>,
        val spendByCategory: List<SpendByCategory>
    ) : MonthlySummaryUiState
    data class Error(val message: String) : MonthlySummaryUiState
}

sealed interface CreateExpenseUiState {
    data object Idle : CreateExpenseUiState
    data object Loading : CreateExpenseUiState
    data object Success : CreateExpenseUiState
    data class Error(val message: String) : CreateExpenseUiState
}



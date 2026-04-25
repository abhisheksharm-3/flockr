package `in`.xroden.flockr.features.expenses.presentation

import `in`.xroden.flockr.features.house.model.MemberWithProfile
import kotlinx.datetime.LocalDate

data class AddExpenseFormState(
    val name: String = "",
    val amount: String = "",
    val date: LocalDate? = null,
    val notes: String = "",
    val category: String = "Groceries",
    val isSplitEnabled: Boolean = false,
    val isSplitEqual: Boolean = true,
    val selectedMemberIds: Set<String> = emptySet(),
    val customSplits: Map<String, String> = emptyMap(),
    val houseMembers: List<MemberWithProfile> = emptyList(),
    val currencySymbol: String = "$"
)

sealed interface AddExpenseUiState {
    data object Idle : AddExpenseUiState
    data object Loading : AddExpenseUiState
    data object Success : AddExpenseUiState
    data class Error(val message: String) : AddExpenseUiState
}

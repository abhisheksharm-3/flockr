package `in`.xroden.flockr.features.expenses.presentation

import `in`.xroden.flockr.features.expenses.model.PerDiemBillByMember
import `in`.xroden.flockr.features.expenses.model.PerDiemBillItemized
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig
import `in`.xroden.flockr.features.expenses.model.PerDiemEntry
import `in`.xroden.flockr.features.expenses.model.PerDiemEntryWithDetails

sealed interface PerDiemConfigUiState {
    data object Loading : PerDiemConfigUiState
    data class Success(val configs: List<PerDiemConfig>) : PerDiemConfigUiState
    data class Error(val message: String) : PerDiemConfigUiState
}

sealed interface PerDiemEntryUiState {
    data object Loading : PerDiemEntryUiState
    data class Success(val entries: List<PerDiemEntryWithDetails>) : PerDiemEntryUiState
    data class Error(val message: String) : PerDiemEntryUiState
}

sealed interface PerDiemBillUiState {
    data object Loading : PerDiemBillUiState
    data class Success(
        val itemized: List<PerDiemBillItemized>,
        val byMember: List<PerDiemBillByMember>
    ) : PerDiemBillUiState
    data class Error(val message: String) : PerDiemBillUiState
}



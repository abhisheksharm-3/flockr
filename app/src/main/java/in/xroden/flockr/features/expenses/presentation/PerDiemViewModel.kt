package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.expenses.data.IPerDiemRepository
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.model.HouseConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlinx.datetime.LocalDate
import javax.inject.Inject

@HiltViewModel
class PerDiemViewModel @Inject constructor(
    private val perDiemRepository: IPerDiemRepository,
    private val houseRepository: IHouseRepository
) : ViewModel() {

    private val _configState = MutableStateFlow<PerDiemConfigUiState>(PerDiemConfigUiState.Loading)
    val configState: StateFlow<PerDiemConfigUiState> = _configState.asStateFlow()

    private val _entryState = MutableStateFlow<PerDiemEntryUiState>(PerDiemEntryUiState.Loading)
    val entryState: StateFlow<PerDiemEntryUiState> = _entryState.asStateFlow()

    private val _billState = MutableStateFlow<PerDiemBillUiState>(PerDiemBillUiState.Loading)
    val billState: StateFlow<PerDiemBillUiState> = _billState.asStateFlow()

    private val _houseConfigState = MutableStateFlow<HouseConfig?>(null)
    val houseConfig: StateFlow<HouseConfig?> = _houseConfigState.asStateFlow()

    private var currentHouseId: String? = null

    fun loadConfigs(houseId: String) {
        val skipLoading = currentHouseId == houseId && _configState.value is PerDiemConfigUiState.Success
        currentHouseId = houseId

        viewModelScope.launch {
            if (!skipLoading) {
                _configState.value = PerDiemConfigUiState.Loading
            }

            perDiemRepository.getPerDiemConfigs(houseId).fold(
                onSuccess = { configs ->
                    _configState.value = PerDiemConfigUiState.Success(configs)
                },
                onFailure = { error ->
                    _configState.value = PerDiemConfigUiState.Error(
                        message = error.message ?: "Failed to load per-diem configurations"
                    )
                }
            )
        }
    }

    fun loadEntriesWithDetails(houseId: String, month: String? = null) {
        viewModelScope.launch {
            if (_entryState.value !is PerDiemEntryUiState.Success) {
                _entryState.value = PerDiemEntryUiState.Loading
            }

            val monthDate: LocalDate? = month?.let {
                runCatching {
                    LocalDate.parse(if (it.length == 7) "$it-01" else it)
                }.getOrNull()
            }

            perDiemRepository.getPerDiemEntriesWithDetails(houseId, monthDate).fold(
                onSuccess = { entries ->
                    _entryState.value = PerDiemEntryUiState.Success(entries)
                },
                onFailure = { error ->
                    _entryState.value = PerDiemEntryUiState.Error(
                        message = error.message ?: "Failed to load per-diem entries"
                    )
                }
            )
        }
    }

    fun loadPerDiemReports(houseId: String, month: String) {
        viewModelScope.launch {
            if (_billState.value !is PerDiemBillUiState.Success) {
                _billState.value = PerDiemBillUiState.Loading
            }

            val itemizedResult = perDiemRepository.getPerDiemBill(houseId, month)
            val byMemberResult = perDiemRepository.getPerDiemBillByMember(houseId, month)

            if (itemizedResult.isSuccess && byMemberResult.isSuccess) {
                _billState.value = PerDiemBillUiState.Success(
                    itemized = itemizedResult.getOrElse { emptyList() },
                    byMember = byMemberResult.getOrElse { emptyList() }
                )
            } else {
                val error = itemizedResult.exceptionOrNull() ?: byMemberResult.exceptionOrNull()
                _billState.value = PerDiemBillUiState.Error(
                    message = error?.message ?: "Failed to load per-diem reports"
                )
            }
        }
    }

    fun createConfig(
        houseId: String,
        itemName: String,
        rate: BigDecimal,
        category: String,
        unit: String
    ) {
        viewModelScope.launch {
            perDiemRepository.createPerDiemConfig(houseId, itemName, rate, category, unit).fold(
                onSuccess = { loadConfigs(houseId) },
                onFailure = { error ->
                    _configState.value = PerDiemConfigUiState.Error(
                        message = error.message ?: "Failed to create config"
                    )
                }
            )
        }
    }

    fun updateConfig(
        houseId: String,
        configId: String,
        itemName: String?,
        rate: BigDecimal?,
        category: String?,
        unit: String?
    ) {
        viewModelScope.launch {
            perDiemRepository.updatePerDiemConfig(configId, itemName, rate, category, unit).fold(
                onSuccess = { loadConfigs(houseId) },
                onFailure = { error ->
                    _configState.value = PerDiemConfigUiState.Error(
                        message = error.message ?: "Failed to update config"
                    )
                }
            )
        }
    }

    fun deleteConfig(houseId: String, configId: String, deleteUsage: Boolean = false) {
        viewModelScope.launch {
            perDiemRepository.deletePerDiemConfig(configId, deleteUsage).fold(
                onSuccess = { loadConfigs(houseId) },
                onFailure = { error ->
                    _configState.value = PerDiemConfigUiState.Error(
                        message = error.message ?: "Failed to delete config"
                    )
                }
            )
        }
    }

    fun createPerDiemEntry(
        houseId: String,
        configId: String,
        quantity: BigDecimal,
        date: LocalDate,
        itemName: String,
        notes: String?
    ) {
        viewModelScope.launch {
            perDiemRepository.addPerDiemEntry(houseId, configId, quantity, date, itemName, notes).fold(
                onSuccess = { loadEntriesWithDetails(houseId) },
                onFailure = { error ->
                    _entryState.value = PerDiemEntryUiState.Error(
                        message = error.message ?: "Failed to create entry"
                    )
                }
            )
        }
    }

    fun deletePerDiemEntry(houseId: String, entryId: String) {
        viewModelScope.launch {
            perDiemRepository.deletePerDiemEntry(entryId).fold(
                onSuccess = { loadEntriesWithDetails(houseId) },
                onFailure = { error ->
                    _entryState.value = PerDiemEntryUiState.Error(
                        message = error.message ?: "Failed to delete entry"
                    )
                }
            )
        }
    }

    fun updatePerDiemEntry(
        houseId: String,
        entryId: String,
        quantity: BigDecimal?,
        date: LocalDate?,
        notes: String?
    ) {
        viewModelScope.launch {
            perDiemRepository.updatePerDiemEntry(entryId, quantity, date, notes).fold(
                onSuccess = { loadEntriesWithDetails(houseId) },
                onFailure = { error ->
                    _entryState.value = PerDiemEntryUiState.Error(
                        message = error.message ?: "Failed to update entry"
                    )
                }
            )
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
}

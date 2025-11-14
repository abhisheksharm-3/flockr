package `in`.xroden.flockr.features.expenses.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.*
import `in`.xroden.flockr.features.expenses.data.ExpenseRepository
import `in`.xroden.flockr.features.expenses.data.PerDiemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PerDiemViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val perDiemRepository: PerDiemRepository,
    private val houseRepository: `in`.xroden.flockr.data.repository.HouseRepository
) : ViewModel() {

    private val _configs = MutableStateFlow<List<PerDiemConfig>>(emptyList())
    val configs: StateFlow<List<PerDiemConfig>> = _configs.asStateFlow()

    private val _entries = MutableStateFlow<List<PerDiemEntry>>(emptyList())
    val entries: StateFlow<List<PerDiemEntry>> = _entries.asStateFlow()

    private val _entriesWithDetails = MutableStateFlow<List<PerDiemEntryWithDetails>>(emptyList())
    val entriesWithDetails: StateFlow<List<PerDiemEntryWithDetails>> = _entriesWithDetails.asStateFlow()

    private val _perDiemBillItemized = MutableStateFlow<List<PerDiemBillItemized>>(emptyList())
    val perDiemBillItemized: StateFlow<List<PerDiemBillItemized>> = _perDiemBillItemized.asStateFlow()

    private val _perDiemBillByMember = MutableStateFlow<List<PerDiemBillByMember>>(emptyList())
    val perDiemBillByMember: StateFlow<List<PerDiemBillByMember>> = _perDiemBillByMember.asStateFlow()

    private val _houseConfig = MutableStateFlow<HouseConfig?>(null)
    val houseConfig: StateFlow<HouseConfig?> = _houseConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadConfigs(houseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val configs = perDiemRepository.getPerDiemConfigs(houseId)
                _configs.value = configs
                loadHouseConfig(houseId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load per-diem configurations"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadHouseConfig(houseId: String) {
        viewModelScope.launch {
            try {
                val config = houseRepository.getHouseConfig(houseId)
                _houseConfig.value = config
            } catch (e: Exception) {
                android.util.Log.e("PerDiemViewModel", "Failed to load house config", e)
            }
        }
    }

    fun loadPerDiemReports(houseId: String, month: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val itemized = expenseRepository.getPerDiemBillItemized(houseId, month)
                val byMember = expenseRepository.getPerDiemBillByMember(houseId, month)

                _perDiemBillItemized.value = itemized
                _perDiemBillByMember.value = byMember
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load per-diem reports"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadEntriesWithDetails(houseId: String, month: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val entries = perDiemRepository.getPerDiemEntriesWithDetails(houseId, month)
                _entriesWithDetails.value = entries
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load per-diem entries"
                android.util.Log.e("PerDiemViewModel", "Failed to load entries with details", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPerDiemEntry(
        configId: String,
        houseId: String,
        quantity: Double,
        date: String,
        notes: String?,
        itemName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            expenseRepository.createPerDiemEntry(
                configId, houseId, quantity, date, notes, itemName
            ).fold(
                onSuccess = { onSuccess() },
                onFailure = {
                    val errorMessage = it.message ?: "Failed to create per-diem entry"
                    _error.value = errorMessage
                    onError(errorMessage)
                }
            )
        }
    }

    fun clearError() {
        _error.value = null
    }


    suspend fun createConfig(
        houseId: String,
        itemName: String,
        rate: Double,
        category: String,
        unit: String
    ): Result<PerDiemConfig> {
        return perDiemRepository.createPerDiemConfig(houseId, itemName, rate, category, unit)
    }

    suspend fun updateConfig(
        configId: String,
        itemName: String,
        rate: Double,
        category: String,
        unit: String
    ): Result<Unit> {
        return perDiemRepository.updatePerDiemConfig(configId, itemName, rate, category, unit)
    }

    suspend fun deleteConfig(configId: String, deleteUsage: Boolean = false): Result<Unit> {
        return perDiemRepository.deletePerDiemConfig(configId, deleteUsage)
    }
}


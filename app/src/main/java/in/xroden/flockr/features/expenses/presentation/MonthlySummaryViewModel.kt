package `in`.xroden.flockr.features.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.expenses.data.IExpenseAnalyticsRepository
import `in`.xroden.flockr.features.expenses.data.IPerDiemRepository
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.model.HouseConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class MonthlySummaryViewModel @Inject constructor(
    private val analyticsRepository: IExpenseAnalyticsRepository,
    private val perDiemRepository: IPerDiemRepository,
    private val houseRepository: IHouseRepository
) : ViewModel() {

    private val _summaryState = MutableStateFlow<MonthlySummaryUiState>(MonthlySummaryUiState.Loading)
    val summaryState: StateFlow<MonthlySummaryUiState> = _summaryState.asStateFlow()

    private val _perDiemBillItemizedState = MutableStateFlow<Map<String, BigDecimal>>(emptyMap())
    val perDiemBillItemized: StateFlow<Map<String, BigDecimal>> = _perDiemBillItemizedState.asStateFlow()

    private val _houseConfigState = MutableStateFlow<HouseConfig?>(null)
    val houseConfig: StateFlow<HouseConfig?> = _houseConfigState.asStateFlow()

    fun loadMonthlySummary(houseId: String, month: String) {
        viewModelScope.launch {
            _summaryState.value = MonthlySummaryUiState.Loading

            coroutineScope {
                val summaryDeferred = async { analyticsRepository.getMonthlySummary(houseId, month) }
                val memberDeferred = async { analyticsRepository.getSpendByMember(houseId, month) }
                val categoryDeferred = async { analyticsRepository.getSpendByCategory(houseId, month) }

                val summaryResult = summaryDeferred.await()
                val memberResult = memberDeferred.await()
                val categoryResult = categoryDeferred.await()

                if (summaryResult.isSuccess) {
                    val summary = summaryResult.getOrNull()
                    if (summary != null) {
                        _summaryState.value = MonthlySummaryUiState.Success(
                            summary = summary,
                            spendByMember = memberResult.getOrElse { emptyList() },
                            spendByCategory = categoryResult.getOrElse { emptyList() }
                        )
                    } else {
                        _summaryState.value = MonthlySummaryUiState.Error("No summary data available")
                    }
                } else {
                    val error = summaryResult.exceptionOrNull()
                    _summaryState.value = MonthlySummaryUiState.Error(
                        message = error?.message ?: "Failed to load summary"
                    )
                }
            }
        }
    }

    fun loadPerDiemBillItemized(houseId: String, month: String) {
        viewModelScope.launch {
            perDiemRepository.getPerDiemBill(houseId, month).fold(
                onSuccess = { billList ->
                    _perDiemBillItemizedState.value = billList.associate { it.itemName to it.totalAmount }
                },
                onFailure = {
                    _perDiemBillItemizedState.value = emptyMap()
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

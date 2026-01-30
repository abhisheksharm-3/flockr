package `in`.xroden.flockr.features.expenses.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.expenses.data.ExpenseAnalyticsRepository
import `in`.xroden.flockr.features.expenses.data.PerDiemRepository
import `in`.xroden.flockr.features.house.data.HouseRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * ViewModel for monthly expense summary and analytics.
 * Handles monthly reports, spending breakdowns, and analytics.
 */
@HiltViewModel
class MonthlySummaryViewModel @Inject constructor(
    private val analyticsRepository: ExpenseAnalyticsRepository,
    private val perDiemRepository: PerDiemRepository,
    private val houseRepository: HouseRepository
) : ViewModel() {

    private val _summaryState = MutableStateFlow<MonthlySummaryUiState>(MonthlySummaryUiState.Loading)
    val summaryState: StateFlow<MonthlySummaryUiState> = _summaryState.asStateFlow()

    private val _spendByMemberState = MutableStateFlow<List<`in`.xroden.flockr.features.expenses.model.SpendByMember>>(emptyList())
    val spendByMember: StateFlow<List<`in`.xroden.flockr.features.expenses.model.SpendByMember>> = _spendByMemberState.asStateFlow()

    private val _spendByCategoryState = MutableStateFlow<List<`in`.xroden.flockr.features.expenses.model.SpendByCategory>>(emptyList())
    val spendByCategory: StateFlow<List<`in`.xroden.flockr.features.expenses.model.SpendByCategory>> = _spendByCategoryState.asStateFlow()

    private val _perDiemBillItemizedState = MutableStateFlow<Map<String, BigDecimal>>(emptyMap())
    val perDiemBillItemized: StateFlow<Map<String, BigDecimal>> = _perDiemBillItemizedState.asStateFlow()

    private val _houseConfigState = MutableStateFlow<`in`.xroden.flockr.features.house.model.HouseConfig?>(null)
    val houseConfig: StateFlow<`in`.xroden.flockr.features.house.model.HouseConfig?> = _houseConfigState.asStateFlow()

    fun loadMonthlySummary(houseId: String, month: String) {
        viewModelScope.launch {
            _summaryState.value = MonthlySummaryUiState.Loading

            // Execute all queries in parallel for 3x faster loading
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

                        // Update individual states as well
                        _spendByMemberState.value = memberResult.getOrElse { emptyList() }
                        _spendByCategoryState.value = categoryResult.getOrElse { emptyList() }
                            .filter { it.category != "Settlement" }
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

    fun loadSpendByMember(houseId: String, month: String) {
        viewModelScope.launch {
            analyticsRepository.getSpendByMember(houseId, month).fold(
                onSuccess = { spending ->
                    _spendByMemberState.value = spending
                },
                onFailure = {
                    _spendByMemberState.value = emptyList()
                }
            )
        }
    }

    fun loadSpendByCategory(houseId: String, month: String) {
        viewModelScope.launch {
            analyticsRepository.getSpendByCategory(houseId, month).fold(
                onSuccess = { spending ->
                    _spendByCategoryState.value = spending.filter { it.category != "Settlement" }
                },
                onFailure = {
                    _spendByCategoryState.value = emptyList()
                }
            )
        }
    }

    fun loadPerDiemBillItemized(houseId: String, month: String) {
        viewModelScope.launch {
            perDiemRepository.getPerDiemBill(houseId, month).fold(
                onSuccess = { billList ->
                    val billMap = billList.associate { it.itemName to it.totalAmount }
                    _perDiemBillItemizedState.value = billMap
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
                onSuccess = { config ->
                    _houseConfigState.value = config
                },
                onFailure = {
                    _houseConfigState.value = null
                }
            )
        }
    }
}

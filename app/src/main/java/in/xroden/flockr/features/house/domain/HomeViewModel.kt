package `in`.xroden.flockr.features.house.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HouseCardData
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.expenses.data.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val houseRepository: HouseRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HouseListUiState>(HouseListUiState.Loading)
    val uiState: StateFlow<HouseListUiState> = _uiState.asStateFlow()

    private val _createState = MutableStateFlow<CreateHouseUiState>(CreateHouseUiState.Idle)
    val createState: StateFlow<CreateHouseUiState> = _createState.asStateFlow()

    private val _joinState = MutableStateFlow<JoinHouseUiState>(JoinHouseUiState.Idle)
    val joinState: StateFlow<JoinHouseUiState> = _joinState.asStateFlow()

    init {
        loadHouses()
    }

    fun loadHouses() {
        viewModelScope.launch {
            _uiState.value = HouseListUiState.Loading
            
            houseRepository.getHousesFlow().collect { result ->
                result.fold(
                    onSuccess = { houses ->
                        // Enrich house data with member count and monthly expenses
                        val enrichedHouses = houses.map { house ->
                            val memberCountDeferred = async {
                                houseRepository.getHouseMembers(house.id).getOrNull()?.size ?: 0
                            }

                            val monthlyExpenseDeferred = async {
                                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                val currentMonth = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}"
                                expenseRepository.getMonthlySummary(house.id, currentMonth).getOrNull()?.totalExpenses ?: java.math.BigDecimal.ZERO
                            }

                            val configDeferred = async {
                                houseRepository.getHouseConfig(house.id).getOrNull()
                            }

                            val config = configDeferred.await()
                            val currencySymbol = config?.getCurrencySymbol() ?: "$"

                            HouseCardData(
                                house = house,
                                memberCount = memberCountDeferred.await(),
                                monthlyExpense = monthlyExpenseDeferred.await(),
                                currencySymbol = currencySymbol
                            )
                        }

                        _uiState.value = HouseListUiState.Success(enrichedHouses)
                    },
                    onFailure = { error ->
                        _uiState.value = HouseListUiState.Error(
                            message = error.message ?: "Failed to load houses",
                            cause = error
                        )
                    }
                )
            }
        }
    }

    fun createHouse(
        name: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        currencyCode: String = "USD"
    ) {
        viewModelScope.launch {
            _createState.value = CreateHouseUiState.Loading
            
            houseRepository.createHouse(name, address, latitude, longitude, currencyCode).fold(
                onSuccess = { house ->
                    _createState.value = CreateHouseUiState.Success(house)
                    kotlinx.coroutines.delay(1000)
                    _createState.value = CreateHouseUiState.Idle
                },
                onFailure = { error ->
                    _createState.value = CreateHouseUiState.Error(
                        message = error.message ?: "Failed to create house"
                    )
                }
            )
        }
    }

    fun joinHouseByInviteCode(inviteCode: String) {
        viewModelScope.launch {
            _joinState.value = JoinHouseUiState.Loading
            
            houseRepository.joinHouseByInviteCode(inviteCode).fold(
                onSuccess = { house ->
                    _joinState.value = JoinHouseUiState.Success(house)
                    kotlinx.coroutines.delay(1000)
                    _joinState.value = JoinHouseUiState.Idle
                },
                onFailure = { error ->
                    _joinState.value = JoinHouseUiState.Error(
                        message = error.message ?: "Failed to join household"
                    )
                }
            )
        }
    }

    fun acceptInvitation(invitationId: String) {
        viewModelScope.launch {
            _joinState.value = JoinHouseUiState.Loading
            
            houseRepository.acceptInvitation(invitationId).fold(
                onSuccess = {
                    // Refresh houses after accepting
                    loadHouses()
                    _joinState.value = JoinHouseUiState.Success(null)
                    kotlinx.coroutines.delay(1000)
                    _joinState.value = JoinHouseUiState.Idle
                },
                onFailure = { error ->
                    _joinState.value = JoinHouseUiState.Error(
                        message = error.message ?: "Failed to accept invitation"
                    )
                }
            )
        }
    }

    suspend fun getHouseById(houseId: String): House? {
        return houseRepository.getHouseById(houseId).getOrNull()
    }

    fun resetCreateState() {
        _createState.value = CreateHouseUiState.Idle
    }

    fun resetJoinState() {
        _joinState.value = JoinHouseUiState.Idle
    }
}

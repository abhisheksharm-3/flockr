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

import `in`.xroden.flockr.features.house.model.InvitationWithHouse

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

    private val _pendingInvitations = MutableStateFlow<List<InvitationWithHouse>>(emptyList())
    val pendingInvitations: StateFlow<List<InvitationWithHouse>> = _pendingInvitations.asStateFlow()

    private val _previewState = MutableStateFlow<HousePreviewUiState>(HousePreviewUiState.Idle)
    val previewState: StateFlow<HousePreviewUiState> = _previewState.asStateFlow()

    init {
        loadHouses()
        loadPendingInvitations()
    }

    fun loadHouses() {
        viewModelScope.launch {
            _uiState.value = HouseListUiState.Loading
            
            // Load invitations in parallel or sequence
            loadPendingInvitations()

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
                                val currentMonth = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-01"
                                val result = expenseRepository.getMonthlySummary(house.id, currentMonth).getOrNull()?.totalExpenses ?: java.math.BigDecimal.ZERO
                                result
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

    fun loadPendingInvitations() {
        viewModelScope.launch {
            houseRepository.getPendingInvitations().fold(
                onSuccess = { invitations ->
                    _pendingInvitations.value = invitations
                },
                onFailure = {
                    // Ignore error or log it
                    _pendingInvitations.value = emptyList()
                }
            )
        }
    }

    fun refresh() {
        loadHouses()
        // loadPendingInvitations is called inside loadHouses (roughly) but let's be explicit
        loadPendingInvitations()
    }

    fun createHouse(
        name: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        currencyCode: String = "USD",
        dateFormat: String = "dd/MM/yyyy",
        firstDayOfWeek: Int = 1,
        timezone: String = "UTC",
        headerImageBytes: ByteArray? = null
    ) {
        viewModelScope.launch {
            _createState.value = CreateHouseUiState.Loading
            
            houseRepository.createHouse(
                name, address, latitude, longitude, currencyCode, 
                dateFormat, firstDayOfWeek, timezone
            ).fold(
                onSuccess = { house ->
                    if (headerImageBytes != null) {
                        houseRepository.uploadHouseHeaderImage(house.id, headerImageBytes)
                            .onSuccess {
                                // Ideally update the local house object or rely on refreshing
                            }
                    }
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
                    // Update invitations list
                    loadPendingInvitations()
                    // Houses will update automatically via Flow if triggered, 
                    // but we can force refresh if needed (Flow handles it mostly)
                    _joinState.value = JoinHouseUiState.Success(null)
                    kotlinx.coroutines.delay(500)
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
    
    fun declineInvitation(invitationId: String) {
        viewModelScope.launch {
            houseRepository.rejectInvitation(invitationId).onSuccess {
                loadPendingInvitations()
            }
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

    fun resetPreviewState() {
        _previewState.value = HousePreviewUiState.Idle
    }

    fun validateInviteCode(code: String) {
        viewModelScope.launch {
            _previewState.value = HousePreviewUiState.Loading
            houseRepository.getHouseByInviteCode(code).fold(
                onSuccess = { preview ->
                    if (preview != null) {
                        _previewState.value = HousePreviewUiState.Success(preview)
                    } else {
                        _previewState.value = HousePreviewUiState.Error("Invalid invite code")
                    }
                },
                onFailure = { error ->
                    _previewState.value = HousePreviewUiState.Error(
                        message = error.message ?: "Failed to validate code"
                    )
                }
            )
        }
    }

    // Helper for Dialog (Suspend)
    suspend fun fetchHouseByInviteCode(code: String): House? {
        // This maps the preview data or fetches full house details if possible
        // Ideally we use a specific RPC that returns House details from code before joining
        // For now, reusing getHouseByInviteCode which returns a Preview object, mapping to House roughly
        // OR better: Assume the user meant to use the Preview flow. But the Dialog expects House.
        // Let's add a repository method that returns House-like object or modify Dialog.
        // Actually, let's keep it simple: existing getHouseByInviteCode returns HousePreview. 
        // We will modify Repository to return House if needed, or just map.
        // Checking HouseRepository... let's implement a direct call here.
        
        return houseRepository.getHouseByInviteCode(code).getOrNull()?.let { preview ->
            // Map Preview to House (Partial)
            House(
                id = preview.id,
                name = preview.name,
                address = null,
                latitude = null,
                longitude = null,
                inviteCode = code, // We know the code
                createdAt = kotlinx.datetime.Clock.System.now(), // Dummy
                ownerId = "unknown", 
                headerImageUrl = preview.headerImageUrl
            )
        }
    }

    fun joinHouse(inviteCode: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            houseRepository.joinHouseByInviteCode(inviteCode).fold(
                onSuccess = { onResult(true, null) },
                onFailure = { onResult(false, it.message) }
            )
        }
    }
}

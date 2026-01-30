package `in`.xroden.flockr.features.house.domain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HouseCardData
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.data.HouseInvitationRepository
import `in`.xroden.flockr.features.expenses.data.ExpenseAnalyticsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import javax.inject.Inject

import `in`.xroden.flockr.features.house.model.InvitationWithHouse
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import kotlin.time.Clock

private const val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val houseRepository: HouseRepository,
    private val houseInvitationRepository: HouseInvitationRepository,
    private val expenseAnalyticsRepository: ExpenseAnalyticsRepository
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

    // Job for tracking the houses flow collection to avoid duplicate collectors
    private var housesJob: Job? = null

    init {
        loadHouses()
        loadPendingInvitations()
    }

    fun loadHouses() {
        Log.d(TAG, "loadHouses() called, current state: ${_uiState.value}")
        // Cancel any existing collection job to prevent duplicate collectors
        housesJob?.cancel()

        housesJob = viewModelScope.launch {
            Log.d(TAG, "loadHouses coroutine started")
            // Only show loading on first load, not refreshes
            if (_uiState.value !is HouseListUiState.Success) {
                _uiState.value = HouseListUiState.Loading
                Log.d(TAG, "Set state to Loading")
            }

            try {
                Log.d(TAG, "Starting to collect getHousesFlow()")
                houseRepository.getHousesFlow().collect { result ->
                    Log.d(TAG, "Received result from getHousesFlow: isSuccess=${result.isSuccess}")
                    result.fold(
                        onSuccess = { houses ->
                            Log.d(TAG, "Success: received ${houses.size} houses")
                            // Show basic house data immediately
                            val basicData = houses.map { house ->
                                HouseCardData(
                                    house = house,
                                    memberCount = 0,
                                    monthlyExpense = BigDecimal.ZERO,
                                    currencySymbol = "$"
                                )
                            }
                            _uiState.value = HouseListUiState.Success(basicData)
                            Log.d(TAG, "Set state to Success with ${basicData.size} basic houses")

                            // Fetch all enrichment data in parallel across all houses
                            coroutineScope {
                                val enrichmentJobs = houses.map { house ->
                                    async {
                                        val memberCount = async {
                                            houseRepository.getHouseMembers(house.id).getOrNull()?.size ?: 0
                                        }
                                        val config = async {
                                            houseRepository.getHouseConfig(house.id).getOrNull()
                                        }
                                        val monthlyExpense = async {
                                            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                            val currentMonth = "${now.year}-${now.month.number.toString().padStart(2, '0')}-01"
                                            expenseAnalyticsRepository.getMonthlySummary(house.id, currentMonth).getOrNull()?.totalExpenses ?: BigDecimal.ZERO
                                        }

                                        val configResult = config.await()
                                        HouseCardData(
                                            house = house,
                                            memberCount = memberCount.await(),
                                            monthlyExpense = monthlyExpense.await(),
                                            currencySymbol = configResult?.getCurrencySymbol() ?: "$"
                                        )
                                    }
                                }

                                val enrichedHouses = enrichmentJobs.awaitAll()
                                _uiState.value = HouseListUiState.Success(enrichedHouses)
                                Log.d(TAG, "Set state to Success with ${enrichedHouses.size} enriched houses")
                            }
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Failed to load houses", error)
                            _uiState.value = HouseListUiState.Error(
                                message = error.message ?: "Failed to load houses",
                                cause = error
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadHouses coroutine", e)
                _uiState.value = HouseListUiState.Error(
                    message = e.message ?: "Failed to load houses",
                    cause = e
                )
            }
        }
    }

    fun loadPendingInvitations() {
        viewModelScope.launch {
            houseInvitationRepository.getPendingInvitations().fold(
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
            
            houseInvitationRepository.joinHouseByInviteCode(inviteCode).fold(
                onSuccess = { house ->
                    _joinState.value = JoinHouseUiState.Success(house)
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
            
            houseInvitationRepository.acceptInvitation(invitationId).fold(
                onSuccess = {
                    loadPendingInvitations()
                    _joinState.value = JoinHouseUiState.Success(null)
                },
                onFailure = { error ->
                    _joinState.value = JoinHouseUiState.Error(
                        message = error.message ?: "Failed to accept invitation"
                    )
                }
            )
        }
    }

    fun rejectInvitation(invitationId: String) {
        viewModelScope.launch {
            houseInvitationRepository.rejectInvitation(invitationId).fold(
                onSuccess = {
                    loadPendingInvitations()
                },
                onFailure = { /* Log error */ }
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
                createdAt = Clock.System.now(), // Dummy
                ownerId = "unknown", 
                headerImageUrl = preview.headerImageUrl
            )
        }
    }

    fun joinHouse(inviteCode: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            houseInvitationRepository.joinHouseByInviteCode(inviteCode).fold(
                onSuccess = { onResult(true, null) },
                onFailure = { onResult(false, it.message) }
            )
        }
    }
}

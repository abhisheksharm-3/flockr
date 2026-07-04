package `in`.xroden.flockr.features.house.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HouseCardData
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.data.IHouseInvitationRepository
import `in`.xroden.flockr.utils.BitmapUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal
import javax.inject.Inject

import `in`.xroden.flockr.features.house.model.InvitationWithHouse
import kotlinx.datetime.number
import kotlin.time.Clock

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val houseRepository: IHouseRepository,
    private val houseInvitationRepository: IHouseInvitationRepository,
    private val bitmapUtils: BitmapUtils
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

    private var housesJob: Job? = null

    init {
        loadHouses()
        loadPendingInvitations()
    }

    fun loadHouses() {
        housesJob?.cancel()
        housesJob = viewModelScope.launch {
            if (_uiState.value !is HouseListUiState.Success) {
                _uiState.value = HouseListUiState.Loading
            }

            houseRepository.getHousesFlow().collect { result ->
                result.fold(
                    onSuccess = { houses ->
                        // Show basic data immediately
                        _uiState.value = HouseListUiState.Success(houses.toBasicCardData())
                        // Then fetch enriched data in one batch call
                        loadEnrichedData()
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

    private fun List<House>.toBasicCardData() = map { house ->
        HouseCardData(house = house, memberCount = 0, monthlyExpense = BigDecimal.ZERO, currencySymbol = "$")
    }

    private fun loadEnrichedData() {
        viewModelScope.launch {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val currentMonth = "${now.year}-${now.month.number.toString().padStart(2, '0')}-01"

            houseRepository.getHousesEnriched(currentMonth)
                .onSuccess { cardData ->
                    _uiState.value = HouseListUiState.Success(cardData)
                }
        }
    }

    fun loadPendingInvitations() {
        viewModelScope.launch {
            houseInvitationRepository.getPendingInvitations().fold(
                onSuccess = { _pendingInvitations.value = it },
                onFailure = { _pendingInvitations.value = emptyList() }
            )
        }
    }

    fun refresh() {
        loadHouses()
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
                        val compressed = withContext(Dispatchers.IO) {
                            bitmapUtils.compressImage(headerImageBytes)
                        }
                        houseRepository.uploadHouseHeaderImage(house.id, compressed)
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
            houseInvitationRepository.rejectInvitation(invitationId)
                .onSuccess { loadPendingInvitations() }
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
            houseInvitationRepository.getHouseByInviteCode(code).fold(
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

    fun joinHouse(inviteCode: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            houseInvitationRepository.joinHouseByInviteCode(inviteCode).fold(
                onSuccess = { onResult(true, null) },
                onFailure = { onResult(false, it.message) }
            )
        }
    }
}

/** The signed-in user's houses and invitations, and creating or joining a house. */
package `in`.xroden.flockr.features.house.presentation

import androidx.lifecycle.ViewModel
import `in`.xroden.flockr.utils.uploadJpegOf
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.house.data.HouseInvitationRepository
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.InvitationWithHouse
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val UNKNOWN_CODE_MESSAGE = "That code doesn't match a house. Check it, or ask for a new one if it has expired."

/** The outcome of joining a house or answering an invitation, or why a create or join failed. */
sealed interface HouseEvent {
    data class Joined(val houseId: String, val houseName: String) : HouseEvent
    data class Failed(val message: String) : HouseEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val houseRepository: HouseRepository,
    private val houseInvitationRepository: HouseInvitationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HouseListUiState>(HouseListUiState.Loading)
    val uiState: StateFlow<HouseListUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _pendingInvitations = MutableStateFlow<List<InvitationWithHouse>>(emptyList())
    val pendingInvitations: StateFlow<List<InvitationWithHouse>> = _pendingInvitations.asStateFlow()

    private val _respondingInvitationId = MutableStateFlow<String?>(null)
    val respondingInvitationId: StateFlow<String?> = _respondingInvitationId.asStateFlow()

    private val _createState = MutableStateFlow<CreateHouseUiState>(CreateHouseUiState.Idle)
    val createState: StateFlow<CreateHouseUiState> = _createState.asStateFlow()

    private val _previewState = MutableStateFlow<HousePreviewUiState>(HousePreviewUiState.Idle)
    val previewState: StateFlow<HousePreviewUiState> = _previewState.asStateFlow()

    private val _isJoining = MutableStateFlow(false)
    val isJoining: StateFlow<Boolean> = _isJoining.asStateFlow()

    private val _events = Channel<HouseEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var housesJob: Job? = null
    private var previewJob: Job? = null

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
                _isRefreshing.value = false
                _uiState.value = result.fold(
                    onSuccess = { HouseListUiState.Success(it) },
                    onFailure = { HouseListUiState.Error(message = it.userMessage(), cause = it) }
                )
            }
        }
    }

    fun loadPendingInvitations() {
        viewModelScope.launch {
            _pendingInvitations.value = houseInvitationRepository.getPendingInvitations().getOrElse { emptyList() }
        }
    }

    fun refresh() {
        _isRefreshing.value = true
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
        if (_createState.value !is CreateHouseUiState.Idle) return
        _createState.value = CreateHouseUiState.Creating
        viewModelScope.launch {
            houseRepository.createHouse(
                name, address, latitude, longitude, currencyCode,
                dateFormat, firstDayOfWeek, timezone
            ).fold(
                onSuccess = { house ->
                    val photoUploaded = headerImageBytes == null || run {
                        val compressed = withContext(Dispatchers.IO) { uploadJpegOf(headerImageBytes) } ?: return@run false
                        houseRepository.uploadHouseHeaderImage(house.id, compressed).isSuccess
                    }
                    _createState.value = CreateHouseUiState.Created(house, photoUploaded)
                },
                onFailure = { error ->
                    _createState.value = CreateHouseUiState.Idle
                    _events.send(HouseEvent.Failed(error.userMessage()))
                }
            )
        }
    }

    /** Looks up the house [code] opens, so the user sees what they are joining before they do. */
    fun validateInviteCode(code: String) {
        previewJob?.cancel()
        _previewState.value = HousePreviewUiState.Loading
        previewJob = viewModelScope.launch {
            _previewState.value = houseInvitationRepository.getHouseByInviteCode(code).fold(
                onSuccess = { preview ->
                    preview?.let { HousePreviewUiState.Success(it) } ?: HousePreviewUiState.Error(UNKNOWN_CODE_MESSAGE)
                },
                onFailure = { HousePreviewUiState.Error(it.userMessage()) }
            )
        }
    }

    fun resetPreviewState() {
        previewJob?.cancel()
        _previewState.value = HousePreviewUiState.Idle
    }

    fun joinHouseByInviteCode(inviteCode: String) {
        if (_isJoining.value) return
        _isJoining.value = true
        viewModelScope.launch {
            val event = houseInvitationRepository.joinHouseByInviteCode(inviteCode).fold(
                onSuccess = { HouseEvent.Joined(it.id, it.name) },
                onFailure = { HouseEvent.Failed(it.userMessage()) }
            )
            _isJoining.value = false
            _events.send(event)
        }
    }

    fun acceptInvitation(invitationId: String) = respondToInvitation(invitationId, accept = true)

    fun rejectInvitation(invitationId: String) = respondToInvitation(invitationId, accept = false)

    private fun respondToInvitation(invitationId: String, accept: Boolean) {
        if (_respondingInvitationId.value != null) return
        _respondingInvitationId.value = invitationId
        viewModelScope.launch {
            houseInvitationRepository.respondToInvitation(invitationId, accept).fold(
                onSuccess = { house ->
                    _pendingInvitations.value = _pendingInvitations.value.filterNot { it.id == invitationId }
                    house?.let { _events.send(HouseEvent.Joined(it.id, it.name)) }
                },
                onFailure = { _events.send(HouseEvent.Failed(it.userMessage())) }
            )
            _respondingInvitationId.value = null
        }
    }
}

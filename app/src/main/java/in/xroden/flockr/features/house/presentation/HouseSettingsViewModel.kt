/** A house's settings form: its name, address, picture, money and dates, and leaving or deleting it. */
package `in`.xroden.flockr.features.house.presentation

import android.content.Context
import `in`.xroden.flockr.utils.uploadJpegOf
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.core.presentation.Notice
import `in`.xroden.flockr.features.house.model.HouseMemberRole
import `in`.xroden.flockr.features.house.data.HouseRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class HouseSettingsViewModel @Inject constructor(
    private val houseRepository: HouseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<HouseSettingsUiState>(HouseSettingsUiState.Loading)
    val uiState: StateFlow<HouseSettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<Notice>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _exited = Channel<Unit>(Channel.CONFLATED)

    /** Fires once the viewer has left or deleted the house, so the screen can leave it too. */
    val exited = _exited.receiveAsFlow()

    private var houseId: String = ""

    fun load(houseId: String) {
        if (this.houseId == houseId && _uiState.value is HouseSettingsUiState.Ready) return
        this.houseId = houseId
        viewModelScope.launch {
            _uiState.value = HouseSettingsUiState.Loading
            val viewerId = houseRepository.getCurrentUserId().orEmpty()
            val members = async { houseRepository.getHouseMembers(houseId).getOrElse { emptyList() } }
            val locked = async { houseRepository.hasRecordedMoney(houseId).getOrDefault(true) }
            val config = async { houseRepository.getHouseConfig(houseId) }
            val house = houseRepository.getHouseById(houseId).getOrElse {
                _uiState.value = HouseSettingsUiState.Error(it.userMessage())
                return@launch
            }
            val loadedConfig = config.await().getOrElse {
                _uiState.value = HouseSettingsUiState.Error(it.userMessage())
                return@launch
            }
            val role = members.await().firstOrNull { it.userId == viewerId && it.isActive }?.role
            _uiState.value = HouseSettingsUiState.Ready(
                house = house,
                viewerId = viewerId,
                canEdit = role == HouseMemberRole.OWNER || role == HouseMemberRole.ADMIN,
                name = house.name,
                address = house.address.orEmpty(),
                latitude = house.latitude,
                longitude = house.longitude,
                currencyCode = loadedConfig.currencyCode,
                dateFormat = loadedConfig.dateFormat,
                firstDayOfWeek = loadedConfig.firstDayOfWeek,
                timezone = loadedConfig.timezone,
                isCurrencyLocked = locked.await(),
            ).let { it.copy(saved = it.values) }
        }
    }

    fun update(transform: (HouseSettingsUiState.Ready) -> HouseSettingsUiState.Ready) {
        (_uiState.value as? HouseSettingsUiState.Ready)?.let { _uiState.value = transform(it) }
    }

    /** Saves the house and its config together, so a failure in either is reported and nothing is half done silently. */
    fun save() {
        val form = _uiState.value as? HouseSettingsUiState.Ready ?: return
        if (!form.canSave) return
        update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = houseRepository.updateHouse(houseId, form.name.trim(), form.address.trim(), form.latitude, form.longitude)
                .mapCatching {
                    houseRepository.updateHouseConfig(houseId, form.currencyCode, form.dateFormat, form.firstDayOfWeek, form.timezone).getOrThrow()
                }
            update { it.copy(isSaving = false) }
            _events.send(
                result.fold(
                    onSuccess = {
                        update { it.copy(house = it.house.copy(name = form.name.trim(), address = form.address.trim(), latitude = form.latitude, longitude = form.longitude), saved = form.values) }
                        Notice("Settings saved", isError = false)
                    },
                    onFailure = { Notice(it.userMessage(), isError = true) },
                )
            )
        }
    }

    fun uploadHeaderImage(uri: Uri) {
        val form = _uiState.value as? HouseSettingsUiState.Ready ?: return
        if (!form.canEdit || form.isUploadingImage) return
        update { it.copy(isUploadingImage = true) }
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                    ?.let(::uploadJpegOf)
            }
            val result = if (bytes == null) null else houseRepository.uploadHouseHeaderImage(houseId, bytes)
            update { it.copy(isUploadingImage = false) }
            when {
                result == null -> _events.send(Notice("That image couldn't be read. Try another.", isError = true))
                else -> result.fold(
                    onSuccess = { url ->
                        update { it.copy(house = it.house.copy(headerImageUrl = url)) }
                        _events.send(Notice("Picture updated", isError = false))
                    },
                    onFailure = { _events.send(Notice(it.userMessage(), isError = true)) },
                )
            }
        }
    }

    fun leaveHouse() = exit { houseRepository.leaveHouse(houseId) }

    fun deleteHouse() = exit { houseRepository.deleteHouse(houseId) }

    private fun exit(call: suspend () -> Result<Unit>) {
        val form = _uiState.value as? HouseSettingsUiState.Ready ?: return
        if (form.isSaving) return
        update { it.copy(isSaving = true) }
        viewModelScope.launch {
            call().fold(
                onSuccess = { _exited.send(Unit) },
                onFailure = { error ->
                    update { it.copy(isSaving = false) }
                    _events.send(Notice(error.userMessage(), isError = true))
                },
            )
        }
    }
}

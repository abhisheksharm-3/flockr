package `in`.xroden.flockr.features.house.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.model.House
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HouseSettingsViewModel @Inject constructor(
    private val houseRepository: IHouseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<HouseSettingsUiState>(HouseSettingsUiState.Loading)
    val uiState: StateFlow<HouseSettingsUiState> = _uiState.asStateFlow()

    private val _updateState = MutableStateFlow<UpdateHouseSettingsUiState>(UpdateHouseSettingsUiState.Idle)
    val updateState: StateFlow<UpdateHouseSettingsUiState> = _updateState.asStateFlow()

    fun loadHouseSettings(houseId: String) {
        viewModelScope.launch {
            _uiState.value = HouseSettingsUiState.Loading

            val houseResult = houseRepository.getHouseById(houseId)
            val configResult = houseRepository.getHouseConfig(houseId)

            if (houseResult.isSuccess) {
                val house = houseResult.getOrNull()
                val config = configResult.getOrNull()

                if (house != null && config != null) {
                    _uiState.value = HouseSettingsUiState.Success(config)
                } else {
                    _uiState.value = HouseSettingsUiState.Error("House or config not found")
                }
            } else {
                _uiState.value = HouseSettingsUiState.Error(
                    message = houseResult.exceptionOrNull()?.message ?: "Failed to load settings"
                )
            }
        }
    }

    fun updateHouse(
        houseId: String,
        name: String?,
        address: String?,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        viewModelScope.launch {
            _updateState.value = UpdateHouseSettingsUiState.Loading

            houseRepository.updateHouse(houseId, name, address, latitude, longitude).fold(
                onSuccess = {
                    _updateState.value = UpdateHouseSettingsUiState.Success
                    loadHouseSettings(houseId)
                },
                onFailure = { error ->
                    _updateState.value = UpdateHouseSettingsUiState.Error(
                        message = error.message ?: "Failed to update house"
                    )
                }
            )
        }
    }

    fun updateHouseConfig(
        houseId: String,
        currencyCode: String? = null,
        dateFormat: String? = null,
        firstDayOfWeek: Int? = null,
        timezone: String? = null
    ) {
        viewModelScope.launch {
            _updateState.value = UpdateHouseSettingsUiState.Loading

            houseRepository.updateHouseConfig(
                houseId = houseId,
                currencyCode = currencyCode,
                dateFormat = dateFormat,
                firstDayOfWeek = firstDayOfWeek,
                timezone = timezone
            ).fold(
                onSuccess = {
                    _updateState.value = UpdateHouseSettingsUiState.Success
                    loadHouseSettings(houseId)
                },
                onFailure = { error ->
                    _updateState.value = UpdateHouseSettingsUiState.Error(
                        message = error.message ?: "Failed to update settings"
                    )
                }
            )
        }
    }

    fun uploadHeaderImage(houseId: String, uri: Uri) {
        viewModelScope.launch {
            _updateState.value = UpdateHouseSettingsUiState.Loading

            val bytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }

            if (bytes == null) {
                _updateState.value = UpdateHouseSettingsUiState.Error("Failed to read image")
                return@launch
            }

            houseRepository.uploadHouseHeaderImage(houseId, bytes).fold(
                onSuccess = {
                    _updateState.value = UpdateHouseSettingsUiState.Success
                    loadHouseSettings(houseId)
                },
                onFailure = { error ->
                    _updateState.value = UpdateHouseSettingsUiState.Error(
                        message = error.message ?: "Failed to upload image"
                    )
                }
            )
        }
    }

    suspend fun deleteHouse(houseId: String): Result<Unit> {
        _updateState.value = UpdateHouseSettingsUiState.Loading

        val result = houseRepository.deleteHouse(houseId)

        result.onSuccess {
            _updateState.value = UpdateHouseSettingsUiState.Success
        }.onFailure { e ->
            _updateState.value = UpdateHouseSettingsUiState.Error(message = e.message ?: "Failed to delete house")
        }

        return result
    }

    fun resetUpdateState() {
        _updateState.value = UpdateHouseSettingsUiState.Idle
    }

    fun getCurrentUserId(): String? = houseRepository.getCurrentUserId()

    suspend fun getHouse(houseId: String): House? = houseRepository.getHouseById(houseId).getOrNull()
}

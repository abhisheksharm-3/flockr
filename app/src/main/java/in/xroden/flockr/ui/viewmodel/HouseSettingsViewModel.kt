package `in`.xroden.flockr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.HouseConfig
import `in`.xroden.flockr.data.repository.HouseRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HouseSettingsViewModel @Inject constructor(
    private val houseRepository: HouseRepository
) : ViewModel() {

    suspend fun getHouseConfig(houseId: String): HouseConfig? {
        return try {
            houseRepository.getHouseConfig(houseId)
        } catch (e: Exception) {
            android.util.Log.e("HouseSettingsViewModel", "Error getting house config", e)
            null
        }
    }

    suspend fun updateHouse(
        houseId: String,
        name: String?,
        address: String?,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<Unit> {
        return try {
            android.util.Log.d("HouseSettingsViewModel", "Updating house: name=$name, address=$address")
            houseRepository.updateHouse(houseId, name, address, latitude, longitude)
        } catch (e: Exception) {
            android.util.Log.e("HouseSettingsViewModel", "Error updating house", e)
            Result.failure(e)
        }
    }

    suspend fun updateCurrency(
        houseId: String,
        currencyCode: String,
        currencySymbol: String
    ): Result<Unit> {
        return try {
            android.util.Log.d("HouseSettingsViewModel", "Updating currency: $currencyCode ($currencySymbol)")
            houseRepository.updateHouseConfig(
                houseId = houseId,
                currencyCode = currencyCode,
                currencySymbol = currencySymbol
            )
        } catch (e: Exception) {
            android.util.Log.e("HouseSettingsViewModel", "Error updating currency", e)
            Result.failure(e)
        }
    }
}


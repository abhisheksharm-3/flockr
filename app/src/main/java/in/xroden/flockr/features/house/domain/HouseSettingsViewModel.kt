package `in`.xroden.flockr.features.house.domain

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.data.HouseRepository
import javax.inject.Inject

@HiltViewModel
class HouseSettingsViewModel @Inject constructor(
    private val houseRepository: HouseRepository
) : ViewModel() {

    suspend fun getHouse(houseId: String): House? {
        return try {
            houseRepository.getHouseById(houseId)
        } catch (e: Exception) {
            android.util.Log.e("HouseSettingsViewModel", "Error getting house", e)
            null
        }
    }

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

    suspend fun updateHouseConfig(
        houseId: String,
        currencyCode: String? = null,
        currencySymbol: String? = null,
        dateFormat: String? = null,
        firstDayOfWeek: Int? = null,
        timezone: String? = null
    ): Result<Unit> {
        return try {
            android.util.Log.d("HouseSettingsViewModel", "Updating house config: currency=$currencyCode, dateFormat=$dateFormat, firstDay=$firstDayOfWeek, timezone=$timezone")
            houseRepository.updateHouseConfig(
                houseId = houseId,
                currencyCode = currencyCode,
                currencySymbol = currencySymbol,
                dateFormat = dateFormat,
                firstDayOfWeek = firstDayOfWeek,
                timezone = timezone
            )
        } catch (e: Exception) {
            android.util.Log.e("HouseSettingsViewModel", "Error updating house config", e)
            Result.failure(e)
        }
    }

    suspend fun deleteHouse(houseId: String): Result<Unit> {
        return try {
            android.util.Log.d("HouseSettingsViewModel", "Deleting house: houseId=$houseId")
            houseRepository.deleteHouse(houseId)
        } catch (e: Exception) {
            android.util.Log.e("HouseSettingsViewModel", "Error deleting house", e)
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return houseRepository.userId
    }
}


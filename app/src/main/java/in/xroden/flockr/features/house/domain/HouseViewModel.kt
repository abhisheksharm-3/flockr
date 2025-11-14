package `in`.xroden.flockr.features.house.domain

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.data.HouseRepository
import javax.inject.Inject

@HiltViewModel
class HouseViewModel @Inject constructor(
    private val houseRepository: HouseRepository
) : ViewModel() {

    suspend fun getHouseById(houseId: String): House? {
        return try {
            houseRepository.getHouseById(houseId)
        } catch (e: Exception) {
            android.util.Log.e("HouseViewModel", "Error getting house by ID", e)
            null
        }
    }
}


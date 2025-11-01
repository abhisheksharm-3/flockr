package `in`.xroden.flockr.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.House
import `in`.xroden.flockr.data.repository.HouseRepository
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


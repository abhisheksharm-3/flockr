/** Gives any screen its house's settings without each feature's view model loading them again. */
package `in`.xroden.flockr.features.house.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.HouseConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HouseConfigViewModel @Inject constructor(private val houseRepository: HouseRepository) : ViewModel() {

    private val _config = MutableStateFlow<HouseConfig?>(null)
    val config: StateFlow<HouseConfig?> = _config.asStateFlow()

    fun load(houseId: String) {
        if (_config.value?.houseId == houseId) return
        viewModelScope.launch { houseRepository.getHouseConfig(houseId).onSuccess { _config.value = it } }
    }
}

/**
 * The settings of [houseId], null until loaded. Screens read the currency, date layout, time zone
 * and first day of the week from here, and the helpers on [HouseConfig] fall back to defaults while
 * it is null.
 */
@Composable
fun rememberHouseConfig(houseId: String): State<HouseConfig?> {
    val viewModel: HouseConfigViewModel = hiltViewModel(key = "house_config_$houseId")
    LaunchedEffect(houseId) { viewModel.load(houseId) }
    return viewModel.config.collectAsState()
}

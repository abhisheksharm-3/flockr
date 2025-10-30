package `in`.xroden.flockr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.ShoppingItem
import `in`.xroden.flockr.data.repository.ShoppingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShoppingUiState>(ShoppingUiState.Loading)
    val uiState: StateFlow<ShoppingUiState> = _uiState.asStateFlow()

    fun loadShoppingItems(houseId: String) {
        viewModelScope.launch {
            try {
                shoppingRepository.getShoppingItemsFlow(houseId).collect { items ->
                    _uiState.value = ShoppingUiState.Success(items)
                }
            } catch (e: Exception) {
                _uiState.value = ShoppingUiState.Error(e.message ?: "Failed to load items")
            }
        }
    }

    fun addItem(houseId: String, itemName: String, quantity: String?) {
        viewModelScope.launch {
            shoppingRepository.addShoppingItem(houseId, itemName, quantity)
        }
    }

    fun markAsPurchased(itemId: String, houseId: String, itemName: String) {
        viewModelScope.launch {
            shoppingRepository.markAsPurchased(itemId, houseId, itemName)
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            shoppingRepository.deleteShoppingItem(itemId)
        }
    }
}

sealed class ShoppingUiState {
    object Loading : ShoppingUiState()
    data class Success(val items: List<ShoppingItem>) : ShoppingUiState()
    data class Error(val message: String) : ShoppingUiState()
}


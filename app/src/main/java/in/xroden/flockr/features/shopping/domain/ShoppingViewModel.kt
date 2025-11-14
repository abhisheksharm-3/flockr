package `in`.xroden.flockr.features.shopping.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.shopping.model.ShoppingItem
import `in`.xroden.flockr.features.shopping.data.ShoppingRepository
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
            _uiState.value = ShoppingUiState.Loading
            try {
                shoppingRepository.getShoppingItemsFlow(houseId).collect { items ->
                    _uiState.value = ShoppingUiState.Success(items)
                }
            } catch (e: Exception) {
                _uiState.value = ShoppingUiState.Error(e.message ?: "Failed to load items")
            }
        }
    }

    fun addItem(
        houseId: String,
        itemName: String,
        quantity: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ShoppingViewModel", "Adding item: $itemName for house: $houseId")
                val result = shoppingRepository.addShoppingItem(houseId, itemName, quantity)
                result.fold(
                    onSuccess = {
                        android.util.Log.d("ShoppingViewModel", "Item added successfully")
                        onSuccess()
                    },
                    onFailure = { error ->
                        val errorMessage = error.message ?: "Failed to add item"
                        android.util.Log.e("ShoppingViewModel", "Failed to add item: $errorMessage", error)
                        onError(errorMessage)
                    }
                )
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to add item"
                android.util.Log.e("ShoppingViewModel", "Exception adding item: $errorMessage", e)
                onError(errorMessage)
            }
        }
    }

    fun markAsPurchased(itemId: String, houseId: String, itemName: String) {
        viewModelScope.launch {
            shoppingRepository.markAsPurchased(itemId, houseId, itemName)
        }
    }

    fun updateItem(
        itemId: String,
        itemName: String,
        quantity: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ShoppingViewModel", "Updating item: $itemId")
                val result = shoppingRepository.updateShoppingItem(itemId, itemName, quantity)
                result.fold(
                    onSuccess = {
                        android.util.Log.d("ShoppingViewModel", "Item updated successfully")
                        onSuccess()
                    },
                    onFailure = { error ->
                        val errorMessage = error.message ?: "Failed to update item"
                        android.util.Log.e("ShoppingViewModel", "Failed to update item: $errorMessage", error)
                        onError(errorMessage)
                    }
                )
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Failed to update item"
                android.util.Log.e("ShoppingViewModel", "Exception updating item: $errorMessage", e)
                onError(errorMessage)
            }
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


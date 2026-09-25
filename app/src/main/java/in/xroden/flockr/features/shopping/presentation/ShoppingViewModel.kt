/** The house shopping list, grouped by aisle, with quick adding and ticking off. */
package `in`.xroden.flockr.features.shopping.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.core.presentation.Notice
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.shopping.data.ShoppingRepository
import `in`.xroden.flockr.features.shopping.model.SHOPPING_CATEGORIES
import `in`.xroden.flockr.features.shopping.model.ShoppingItem
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ShoppingUiState {
    data object Loading : ShoppingUiState
    data class Error(val message: String) : ShoppingUiState

    /** [toBuy] is grouped by aisle in walking order; items without an aisle come last under "Other". */
    data class Ready(
        val toBuy: List<Pair<String, List<ShoppingItem>>>,
        val bought: List<ShoppingItem>,
        val members: Map<String, MemberWithProfile>,
        val viewerId: String,
    ) : ShoppingUiState
}

@HiltViewModel
class ShoppingViewModel @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ShoppingUiState>(ShoppingUiState.Loading)
    val state: StateFlow<ShoppingUiState> = _state.asStateFlow()

    private val _notices = Channel<Notice>(Channel.BUFFERED)
    val notices = _notices.receiveAsFlow()

    private var loadJob: Job? = null

    fun load(houseId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val members = async { houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }.associateBy { it.userId } }
            val viewerId = shoppingRepository.getCurrentUserId().orEmpty()
            shoppingRepository.getShoppingItemsFlow(houseId).collect { result ->
                _state.value = result.fold(
                    onSuccess = { items ->
                        val (bought, toBuy) = items.partition { it.isPurchased }
                        val byAisle = toBuy.groupBy { it.category?.takeIf(SHOPPING_CATEGORIES::contains) ?: "Other" }
                        ShoppingUiState.Ready(
                            toBuy = SHOPPING_CATEGORIES.mapNotNull { aisle -> byAisle[aisle]?.let { aisle to it } },
                            bought = bought.sortedByDescending { it.purchasedAt },
                            members = members.await(),
                            viewerId = viewerId,
                        )
                    },
                    onFailure = { ShoppingUiState.Error(it.userMessage()) },
                )
            }
        }
    }

    fun add(houseId: String, itemName: String, quantity: String?, category: String?) = perform { shoppingRepository.addItem(houseId, itemName, quantity, category) }
    fun update(item: ShoppingItem, itemName: String, quantity: String?, category: String?) = perform { shoppingRepository.updateItem(item.id, itemName, quantity, category) }
    fun setPurchased(item: ShoppingItem, isPurchased: Boolean) = perform { shoppingRepository.setPurchased(item.id, isPurchased) }
    fun delete(item: ShoppingItem) = perform { shoppingRepository.deleteItem(item.id) }
    fun clearBought(houseId: String) = perform { shoppingRepository.clearPurchased(houseId) }

    private fun perform(action: suspend () -> Result<Unit>) {
        viewModelScope.launch { action().onFailure { _notices.send(Notice(it.userMessage(), isError = true)) } }
    }
}

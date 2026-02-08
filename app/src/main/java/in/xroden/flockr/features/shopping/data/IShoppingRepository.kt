package `in`.xroden.flockr.features.shopping.data

import `in`.xroden.flockr.features.shopping.model.ShoppingItem
import kotlinx.coroutines.flow.Flow

interface IShoppingRepository {
    fun getCurrentUserId(): String?
    suspend fun getShoppingItems(houseId: String): Result<List<ShoppingItem>>
    fun getShoppingItemsFlow(houseId: String): Flow<Result<List<ShoppingItem>>>
    suspend fun addShoppingItem(houseId: String, itemName: String, quantity: String?): Result<Unit>
    suspend fun updateShoppingItem(itemId: String, itemName: String?, quantity: String?): Result<Unit>
    suspend fun markAsPurchased(itemId: String, houseId: String, itemName: String): Result<Unit>
    suspend fun togglePurchased(itemId: String, houseId: String): Result<Unit>
    suspend fun deleteShoppingItem(itemId: String, houseId: String): Result<Unit>
    suspend fun clearPurchasedItems(houseId: String): Result<Unit>
}

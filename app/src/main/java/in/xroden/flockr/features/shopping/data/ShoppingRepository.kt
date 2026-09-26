/** The house shopping list. Telling the house about new items happens in the database. */
package `in`.xroden.flockr.features.shopping.data

import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.core.realtime.TableWatch
import `in`.xroden.flockr.core.realtime.cachedAs
import `in`.xroden.flockr.core.realtime.liveQuery
import `in`.xroden.flockr.core.serialization.InstantSerializer
import `in`.xroden.flockr.features.shopping.model.ShoppingItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ShoppingItemInsert(
    @SerialName("house_id") val houseId: String,
    @SerialName("item_name") val itemName: String,
    val quantity: String?,
    val category: String?,
    @SerialName("added_by") val addedBy: String,
)

@Serializable
private data class ShoppingItemDetails(
    @SerialName("item_name") val itemName: String,
    val quantity: String?,
    val category: String?,
)

@Serializable
private data class ShoppingItemPurchase(
    @SerialName("is_purchased") val isPurchased: Boolean,
    @SerialName("purchased_by") val purchasedBy: String?,
    @SerialName("purchased_at") @Serializable(with = InstantSerializer::class) val purchasedAt: Instant?,
)

@Singleton
class ShoppingRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    fun getShoppingItemsFlow(houseId: String): Flow<Result<List<ShoppingItem>>> =
        supabase.liveQuery(listOf(TableWatch("shopping_items", "house_id", houseId)), cachedAs<List<ShoppingItem>>("shopping_$houseId")) {
            supabase.from("shopping_items").select {
                filter { eq("house_id", houseId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<ShoppingItem>()
        }

    suspend fun addItem(houseId: String, itemName: String, quantity: String?, category: String?): Result<Unit> = runCatching {
        val userId = requireAuthenticated(getCurrentUserId())
        val name = InputSanitizer.sanitizeText(Validators.validateItemName(itemName).getOrThrow())
        supabase.from("shopping_items").insert(ShoppingItemInsert(houseId, name, quantity.cleaned(), category, userId))
    }

    suspend fun updateItem(itemId: String, itemName: String, quantity: String?, category: String?): Result<Unit> = runCatching {
        val name = InputSanitizer.sanitizeText(Validators.validateItemName(itemName).getOrThrow())
        supabase.from("shopping_items").update(ShoppingItemDetails(name, quantity.cleaned(), category)) { filter { eq("id", itemId) } }
    }

    suspend fun setPurchased(itemId: String, isPurchased: Boolean): Result<Unit> = runCatching {
        val userId = requireAuthenticated(getCurrentUserId())
        val purchase = if (isPurchased) ShoppingItemPurchase(true, userId, Clock.System.now()) else ShoppingItemPurchase(false, null, null)
        supabase.from("shopping_items").update(purchase) { filter { eq("id", itemId) } }
    }

    suspend fun deleteItem(itemId: String): Result<Unit> = runCatching {
        supabase.from("shopping_items").delete { filter { eq("id", itemId) } }
    }

    suspend fun clearPurchased(houseId: String): Result<Unit> = runCatching {
        supabase.from("shopping_items").delete {
            filter {
                eq("house_id", houseId)
                eq("is_purchased", true)
            }
        }
    }

    private fun String?.cleaned(): String? = this?.let(InputSanitizer::sanitizeText)?.ifBlank { null }
}

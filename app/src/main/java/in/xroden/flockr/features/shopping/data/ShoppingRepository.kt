package `in`.xroden.flockr.features.shopping.data

import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.network.RateLimiter
import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.core.notification.NotificationService
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.data.base.BaseRealtimeRepository
import `in`.xroden.flockr.data.dto.ShoppingItemInsert
import `in`.xroden.flockr.data.dto.ShoppingItemUpdate
import `in`.xroden.flockr.features.shopping.model.ShoppingItem
import `in`.xroden.flockr.features.shopping.model.ShoppingItemWithProfiles
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepository @Inject constructor(
    supabase: SupabaseClient,
    connectionManager: RealtimeConnectionManager,
    private val notificationService: NotificationService,
    private val rateLimiter: RateLimiter
) : BaseRealtimeRepository(supabase, connectionManager), IShoppingRepository {

    override fun getCurrentUserId(): String? = authenticatedUserId

    override fun getShoppingItemsFlow(houseId: String): Flow<Result<List<ShoppingItem>>> =
        createRealtimeFlow(
            channelId = "shopping_items_$houseId",
            table = "shopping_items",
            filterColumn = "house_id",
            filterValue = houseId,
            fetchData = { getShoppingItems(houseId) }
        )

    override suspend fun getShoppingItems(houseId: String): Result<List<ShoppingItem>> = runCatching {
        supabase.from("shopping_items")
            .select(Columns.raw("""
                *,
                added_by_profile:profiles!shopping_items_added_by_fkey(full_name),
                purchased_by_profile:profiles!shopping_items_purchased_by_fkey(full_name)
            """.trimIndent())) {
                filter { eq("house_id", houseId) }
                order("is_purchased", Order.ASCENDING)
                order("created_at", Order.DESCENDING)
            }
            .decodeList<ShoppingItemWithProfiles>()
            .map { it.toShoppingItem() }
    }

    override suspend fun clearPurchasedItems(houseId: String): Result<Unit> = runCatching {
        supabase.from("shopping_items")
            .delete {
                filter {
                    eq("house_id", houseId)
                    eq("is_purchased", true)
                }
            }
    }

    override suspend fun addShoppingItem(
        houseId: String,
        itemName: String,
        quantity: String?
    ): Result<Unit> = rateLimiter.throttle("add_shopping_item_$houseId", maxRequestsPerMinute = 60) {
        runCatching {
            val userId = requireAuthenticated(authenticatedUserId)
            val validatedItemName = Validators.validateItemName(itemName).getOrThrow()
            val sanitizedItemName = InputSanitizer.sanitizeText(validatedItemName)
            val sanitizedQuantity = quantity?.trim()?.takeIf { it.isNotBlank() }
                ?.let { InputSanitizer.sanitizeText(it) }

            supabase.from("shopping_items")
                .insert(
                    ShoppingItemInsert(
                        houseId = houseId,
                        itemName = sanitizedItemName,
                        quantity = sanitizedQuantity,
                        addedBy = userId
                    )
                )

            notificationService.sendShoppingItemAdded(houseId, sanitizedItemName, userId)
        }
    }

    override suspend fun updateShoppingItem(
        itemId: String,
        itemName: String?,
        quantity: String?
    ): Result<Unit> = runCatching {
        val sanitizedItemName = itemName?.let { InputSanitizer.sanitizeText(it) }
        val sanitizedQuantity = quantity?.let { InputSanitizer.sanitizeText(it) }

        supabase.from("shopping_items")
            .update(ShoppingItemUpdate(itemName = sanitizedItemName, quantity = sanitizedQuantity)) {
                filter { eq("id", itemId) }
            }
    }

    override suspend fun markAsPurchased(itemId: String, houseId: String, itemName: String): Result<Unit> = runCatching {
        val userId = requireAuthenticated(authenticatedUserId)

        supabase.from("shopping_items")
            .update(ShoppingItemUpdate(isPurchased = true, purchasedBy = userId)) {
                filter { eq("id", itemId) }
            }

        notificationService.sendShoppingItemPurchased(houseId, itemId, itemName, userId)
    }

    override suspend fun deleteShoppingItem(itemId: String, houseId: String): Result<Unit> = runCatching {
        supabase.from("shopping_items").delete { filter { eq("id", itemId) } }
    }

    override suspend fun togglePurchased(itemId: String, houseId: String): Result<Unit> = runCatching {
        val userId = requireAuthenticated(authenticatedUserId)

        val item = supabase.from("shopping_items")
            .select(Columns.raw("is_purchased")) { filter { eq("id", itemId) } }
            .decodeSingleOrNull<JsonObject>()

        val isPurchased = item?.get("is_purchased")?.toString()?.toBooleanStrictOrNull() ?: false

        supabase.from("shopping_items")
            .update(
                ShoppingItemUpdate(
                    isPurchased = !isPurchased,
                    purchasedBy = if (!isPurchased) userId else null
                )
            ) {
                filter { eq("id", itemId) }
            }
    }
}

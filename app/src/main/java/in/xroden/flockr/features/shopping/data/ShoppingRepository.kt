package `in`.xroden.flockr.features.shopping.data

import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.data.base.BaseRealtimeRepository
import `in`.xroden.flockr.data.dto.HouseNotificationParams
import `in`.xroden.flockr.data.dto.ShoppingItemInsert
import `in`.xroden.flockr.data.dto.ShoppingItemUpdate
import `in`.xroden.flockr.features.shopping.model.ShoppingItem
import `in`.xroden.flockr.features.shopping.model.ShoppingItemWithProfiles
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ShoppingRepository @Inject constructor(
    supabase: SupabaseClient,
    connectionManager: RealtimeConnectionManager
) : BaseRealtimeRepository(supabase, connectionManager), IShoppingRepository {

    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    override fun getShoppingItemsFlow(houseId: String): Flow<Result<List<ShoppingItem>>> {
        return createRealtimeFlow(
            channelId = "shopping_items_$houseId",
            table = "shopping_items",
            filterColumn = "house_id",
            filterValue = houseId,
            fetchData = { getShoppingItems(houseId) }
        )
    }

    override suspend fun getShoppingItems(houseId: String): Result<List<ShoppingItem>> = runCatching {
        val itemsWithProfiles = supabase.from("shopping_items")
            .select(Columns.raw("""
                *,
                added_by_profile:profiles!shopping_items_added_by_fkey(full_name),
                purchased_by_profile:profiles!shopping_items_purchased_by_fkey(full_name)
            """.trimIndent())) {
                filter {
                    eq("house_id", houseId)
                }
                order("is_purchased", Order.ASCENDING)
                order("created_at", Order.DESCENDING)
            }
            .decodeList<ShoppingItemWithProfiles>()

        itemsWithProfiles.map { it.toShoppingItem() }
    }

    suspend fun clearPurchasedItems(houseId: String): Result<Unit> = runCatching {
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
    ): Result<Unit> = runCatching {
        val currentUserId = userId ?: throw IllegalStateException("No user logged in")

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
                    addedBy = currentUserId
                )
            )

        runCatching {
            supabase.postgrest.rpc(
                function = "create_notification_for_house",
                parameters = HouseNotificationParams(
                    houseId = houseId,
                    title = "Shopping List Updated",
                    message = "New item added: $sanitizedItemName",
                    type = "shopping",
                    data = """{"type":"shopping","itemName":"${
                            sanitizedItemName.replace(
                                "\"",
                                "\\\""
                            )
                        }"}""",
                    excludeUserId = currentUserId
                )
            )
        }
    }

    suspend fun updateShoppingItem(
        itemId: String,
        itemName: String?,
        quantity: String?
    ): Result<Unit> = runCatching {
        supabase.from("shopping_items")
            .update(
                ShoppingItemUpdate(
                    itemName = itemName,
                    quantity = quantity
                )
            ) {
                filter {
                    eq("id", itemId)
                }
            }
    }

    suspend fun markAsPurchased(itemId: String, houseId: String, itemName: String): Result<Unit> = runCatching {
        val currentUserId = userId ?: throw IllegalStateException("No user logged in")

        supabase.from("shopping_items")
            .update(
                ShoppingItemUpdate(
                    isPurchased = true,
                    purchasedBy = currentUserId
                )
            ) {
                filter {
                    eq("id", itemId)
                }
            }

        runCatching {
            supabase.postgrest.rpc(
                function = "create_notification_for_house",
                parameters = HouseNotificationParams(
                    houseId = houseId,
                    title = "Item Purchased",
                    message = "Purchased: $itemName",
                    type = "shopping",
                    data = """{"type":"shopping","itemId":"$itemId"}""",
                    excludeUserId = currentUserId
                )
            )
        }
    }

    override suspend fun deleteShoppingItem(itemId: String, houseId: String): Result<Unit> = runCatching {
        supabase.from("shopping_items")
            .delete {
                filter {
                    eq("id", itemId)
                }
            }
    }

    override suspend fun togglePurchased(itemId: String, houseId: String): Result<Unit> = runCatching {
        val currentUserId = userId ?: throw IllegalStateException("No user logged in")

        // Get current state
        val item = supabase.from("shopping_items")
            .select(Columns.raw("is_purchased")) {
                filter { eq("id", itemId) }
            }
            .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

        val isPurchased = item?.get("is_purchased")?.toString()?.toBooleanStrictOrNull() ?: false

        supabase.from("shopping_items")
            .update(
                ShoppingItemUpdate(
                    isPurchased = !isPurchased,
                    purchasedBy = if (!isPurchased) currentUserId else null
                )
            ) {
                filter { eq("id", itemId) }
            }
    }

    override fun getCurrentUserId(): String? = userId
}

package `in`.xroden.flockr.features.shopping.data

import `in`.xroden.flockr.data.dto.ShoppingItemInsert
import `in`.xroden.flockr.data.dto.ShoppingItemUpdate
import `in`.xroden.flockr.features.shopping.model.ShoppingItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    fun getShoppingItemsFlow(houseId: String): Flow<Result<List<ShoppingItem>>> = callbackFlow {
        val channelId = "shopping_items_$houseId"
        val channel = supabase.realtime.channel(channelId)

        try {
            send(getShoppingItems(houseId))

            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "shopping_items"
                filter = "house_id=eq.$houseId"
            }

            channel.subscribe(blockUntilSubscribed = true)

            changeFlow.collect {
                kotlinx.coroutines.delay(100)
                send(getShoppingItems(houseId))
            }
        } catch (e: Exception) {
            send(Result.failure(e))
        }

        awaitClose {
            try {
                supabase.realtime.removeChannel(channel)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    suspend fun getShoppingItems(houseId: String): Result<List<ShoppingItem>> {
        return try {
            val response = supabase.from("shopping_items")
                .select(Columns.raw("""
                    *,
                    added_by_profile:profiles!shopping_items_added_by_fkey(full_name),
                    purchased_by_profile:profiles!shopping_items_purchased_by_fkey(full_name)
                """.trimIndent())) {
                    filter {
                        eq("house_id", houseId)
                        eq("is_purchased", false)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<kotlinx.serialization.json.JsonObject>()

            val items = response.map { obj ->
                val addedByName = obj["added_by_profile"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }
                    ?.kotlinx.serialization.json.jsonObject?.get("full_name")?.kotlinx.serialization.json.jsonPrimitive?.content
                val purchasedByName = obj["purchased_by_profile"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }
                    ?.kotlinx.serialization.json.jsonObject?.get("full_name")?.kotlinx.serialization.json.jsonPrimitive?.content

                ShoppingItem(
                    id = obj["id"]?.kotlinx.serialization.json.jsonPrimitive?.content ?: "",
                    houseId = obj["house_id"]?.kotlinx.serialization.json.jsonPrimitive?.content ?: "",
                    itemName = obj["item_name"]?.kotlinx.serialization.json.jsonPrimitive?.content ?: "",
                    quantity = obj["quantity"]?.kotlinx.serialization.json.jsonPrimitive?.kotlinx.serialization.json.contentOrNull,
                    isPurchased = obj["is_purchased"]?.kotlinx.serialization.json.jsonPrimitive?.content?.toBoolean() ?: false,
                    addedBy = obj["added_by"]?.kotlinx.serialization.json.jsonPrimitive?.kotlinx.serialization.json.contentOrNull,
                    addedByName = addedByName,
                    purchasedBy = obj["purchased_by"]?.kotlinx.serialization.json.jsonPrimitive?.kotlinx.serialization.json.contentOrNull,
                    purchasedByName = purchasedByName,
                    purchasedAt = obj["purchased_at"]?.kotlinx.serialization.json.jsonPrimitive?.kotlinx.serialization.json.contentOrNull?.let { Instant.parse(it) },
                    createdAt = obj["created_at"]?.kotlinx.serialization.json.jsonPrimitive?.content?.let { Instant.parse(it) }
                        ?: Instant.DISTANT_PAST
                )
            }

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addShoppingItem(
        houseId: String,
        itemName: String,
        quantity: String?
    ): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            supabase.from("shopping_items")
                .insert(
                    ShoppingItemInsert(
                        houseId = houseId,
                        itemName = itemName,
                        quantity = quantity,
                        addedBy = currentUserId
                    )
                )

            try {
                @Serializable
                data class HouseNotificationParams(
                    @SerialName("p_house_id")
                    val houseId: String,
                    @SerialName("p_title")
                    val title: String,
                    @SerialName("p_message")
                    val message: String,
                    @SerialName("p_type")
                    val type: String,
                    @SerialName("p_data")
                    val data: String,
                    @SerialName("p_exclude_user_id")
                    val excludeUserId: String?
                )

                supabase.postgrest.rpc(
                    function = "create_notification_for_house",
                    parameters = HouseNotificationParams(
                        houseId = houseId,
                        title = "Shopping List Updated",
                        message = "New item added: $itemName",
                        type = "shopping",
                        data = """{"type":"shopping","itemName":"$itemName"}""",
                        excludeUserId = currentUserId
                    )
                )
            } catch (e: Exception) {
                // Ignore notification errors
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateShoppingItem(
        itemId: String,
        itemName: String?,
        quantity: String?
    ): Result<Unit> {
        return try {
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

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsPurchased(itemId: String, houseId: String, itemName: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

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

            try {
                @Serializable
                data class HouseNotificationParams(
                    @SerialName("p_house_id")
                    val houseId: String,
                    @SerialName("p_title")
                    val title: String,
                    @SerialName("p_message")
                    val message: String,
                    @SerialName("p_type")
                    val type: String,
                    @SerialName("p_data")
                    val data: String,
                    @SerialName("p_exclude_user_id")
                    val excludeUserId: String?
                )

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
            } catch (e: Exception) {
                // Ignore notification errors
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteShoppingItem(itemId: String): Result<Unit> {
        return try {
            supabase.from("shopping_items")
                .delete {
                    filter {
                        eq("id", itemId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

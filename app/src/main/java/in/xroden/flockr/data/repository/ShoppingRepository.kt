package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.ShoppingItem
import `in`.xroden.flockr.data.model.ShoppingItemInsert
import `in`.xroden.flockr.data.model.ShoppingItemUpdate
import `in`.xroden.flockr.data.model.ShoppingItemUpdateModel
import `in`.xroden.flockr.data.model.CreateNotificationWithTypeParams
import `in`.xroden.flockr.data.model.Profile
import `in`.xroden.flockr.utils.FlockrLogger
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
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id
    
    companion object {
        private const val TAG = "ShoppingRepository"
    }

    fun getShoppingItemsFlow(houseId: String): Flow<List<ShoppingItem>> {
        FlockrLogger.realtimeEvent(TAG, "getShoppingItemsFlow", "Starting for house=$houseId")
        return kotlinx.coroutines.flow.flow {
            // Emit initial value immediately
            val initialItems = getShoppingItems(houseId)
            FlockrLogger.d(TAG, "getShoppingItemsFlow: Emitting initial ${initialItems.size} items")
            emit(initialItems)

            // Then listen for realtime updates
            val channelId = "shopping_${houseId}_${System.currentTimeMillis()}"
            val channel = supabase.realtime.channel(channelId)

            try {
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "shopping_items"
                }
                
                FlockrLogger.realtimeEvent(TAG, "getShoppingItemsFlow", "Subscribing to channel $channelId")
                channel.subscribe(blockUntilSubscribed = true)
                FlockrLogger.realtimeEvent(TAG, "getShoppingItemsFlow", "Successfully subscribed")

                changeFlow.collect { action ->
                    FlockrLogger.realtimeEvent(TAG, "getShoppingItemsFlow", "Received update: $action")
                    // Small delay to ensure database consistency
                    kotlinx.coroutines.delay(100)
                    val updatedItems = getShoppingItems(houseId)
                    FlockrLogger.d(TAG, "getShoppingItemsFlow: Emitting ${updatedItems.size} items after update")
                    emit(updatedItems)
                }
            } catch (e: Exception) {
                FlockrLogger.repoError(TAG, "getShoppingItemsFlow", e)
            } finally {
                try {
                    FlockrLogger.d(TAG, "getShoppingItemsFlow: Cleaning up channel")
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    FlockrLogger.e(TAG, "getShoppingItemsFlow: Error removing channel", e)
                }
            }
        }
    }

    suspend fun getShoppingItems(houseId: String): List<ShoppingItem> {
        FlockrLogger.repoStart(TAG, "getShoppingItems", mapOf("houseId" to houseId))
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
                    ?.jsonObject?.get("full_name")?.jsonPrimitive?.content
                val purchasedByName = obj["purchased_by_profile"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }
                    ?.jsonObject?.get("full_name")?.jsonPrimitive?.content

                ShoppingItem(
                    id = obj["id"]?.jsonPrimitive?.content ?: "",
                    houseId = obj["house_id"]?.jsonPrimitive?.content ?: "",
                    itemName = obj["item_name"]?.jsonPrimitive?.content ?: "",
                    quantity = obj["quantity"]?.jsonPrimitive?.contentOrNull,
                    isPurchased = obj["is_purchased"]?.jsonPrimitive?.content?.toBoolean() ?: false,
                    addedBy = obj["added_by"]?.jsonPrimitive?.contentOrNull,
                    addedByName = addedByName,
                    purchasedBy = obj["purchased_by"]?.jsonPrimitive?.contentOrNull,
                    purchasedByName = purchasedByName,
                    purchasedAt = obj["purchased_at"]?.jsonPrimitive?.contentOrNull,
                    createdAt = obj["created_at"]?.jsonPrimitive?.content ?: ""
                )
            }
            
            FlockrLogger.repoSuccess(TAG, "getShoppingItems", "Found ${items.size} items")
            items
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getShoppingItems", e)
            emptyList()
        }
    }

    suspend fun addShoppingItem(
        houseId: String,
        itemName: String,
        quantity: String?
    ): Result<Unit> {
        FlockrLogger.repoStart(TAG, "addShoppingItem", mapOf(
            "houseId" to houseId,
            "itemName" to itemName
        ))
        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "addShoppingItem: No user logged in")
                return Result.failure(Exception("No user logged in"))
            }

            val shoppingItemInsert = ShoppingItemInsert(
                houseId = houseId,
                itemName = itemName,
                quantity = quantity,
                addedBy = currentUserId
            )

            supabase.from("shopping_items")
                .insert(shoppingItemInsert)

            FlockrLogger.repoSuccess(TAG, "addShoppingItem", "Item added successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "addShoppingItem", e)
            Result.failure(e)
        }
    }

    suspend fun markAsPurchased(itemId: String, houseId: String, itemName: String): Result<Unit> {
        FlockrLogger.repoStart(TAG, "markAsPurchased", mapOf(
            "itemId" to itemId,
            "itemName" to itemName
        ))
        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "markAsPurchased: No user logged in")
                return Result.failure(Exception("No user logged in"))
            }

            val currentTime = java.time.Instant.now().toString()
            val updateModel = ShoppingItemUpdate(
                isPurchased = true,
                purchasedBy = currentUserId,
                purchasedAt = currentTime
            )

            supabase.from("shopping_items")
                .update(updateModel) {
                    filter {
                        eq("id", itemId)
                    }
                }

            // Get current user's name for notification
            val currentUserProfile = try {
                supabase.from("profiles")
                    .select(Columns.raw("full_name")) {
                        filter {
                            eq("user_id", currentUserId)
                        }
                    }
                    .decodeSingle<Profile>()
            } catch (e: Exception) {
                null
            }
            val purchaserName = currentUserProfile?.fullName ?: "Someone"

            FlockrLogger.d(TAG, "markAsPurchased: Creating notification")
            // Create notification
            val notificationParams = CreateNotificationWithTypeParams(
                houseId = houseId,
                title = "Item Purchased",
                message = "$purchaserName just bought the $itemName.",
                type = "shopping",
                data = """{"id":"$itemId","purchaser":"$purchaserName"}""",
                excludeUserId = currentUserId
            )
            supabase.postgrest.rpc(
                function = "create_notification_for_house",
                parameters = notificationParams
            )

            FlockrLogger.repoSuccess(TAG, "markAsPurchased", "Item marked as purchased")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "markAsPurchased", e)
            Result.failure(e)
        }
    }

    suspend fun updateShoppingItem(
        itemId: String,
        itemName: String,
        quantity: String?
    ): Result<Unit> {
        FlockrLogger.repoStart(TAG, "updateShoppingItem", mapOf(
            "itemId" to itemId,
            "itemName" to itemName
        ))
        return try {
            val updateModel = ShoppingItemUpdateModel(
                itemName = itemName,
                quantity = quantity
            )

            supabase.from("shopping_items")
                .update(updateModel) {
                    filter {
                        eq("id", itemId)
                    }
                }

            FlockrLogger.repoSuccess(TAG, "updateShoppingItem", "Item updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "updateShoppingItem", e)
            Result.failure(e)
        }
    }

    suspend fun deleteShoppingItem(itemId: String): Result<Unit> {
        FlockrLogger.repoStart(TAG, "deleteShoppingItem", mapOf("itemId" to itemId))
        return try {
            supabase.from("shopping_items")
                .delete {
                    filter {
                        eq("id", itemId)
                    }
                }

            FlockrLogger.repoSuccess(TAG, "deleteShoppingItem", "Item deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "deleteShoppingItem", e)
            Result.failure(e)
        }
    }
}


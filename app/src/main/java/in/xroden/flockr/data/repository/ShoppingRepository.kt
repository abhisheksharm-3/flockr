package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.ShoppingItem
import `in`.xroden.flockr.util.FlockrLogger
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
import kotlinx.coroutines.flow.map
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
            val items = supabase.from("shopping_items")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                        eq("is_purchased", false)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<ShoppingItem>()
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

            supabase.from("shopping_items")
                .insert(
                    mapOf(
                        "house_id" to houseId,
                        "item_name" to itemName,
                        "quantity" to quantity,
                        "added_by" to currentUserId
                    )
                )

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

            supabase.from("shopping_items")
                .update(
                    mapOf(
                        "is_purchased" to true,
                        "purchased_by" to currentUserId,
                        "purchased_at" to "now()"
                    )
                ) {
                    filter {
                        eq("id", itemId)
                    }
                }

            FlockrLogger.d(TAG, "markAsPurchased: Creating notification")
            // Create notification
            supabase.postgrest.rpc(
                "create_notification_for_house",
                mapOf(
                    "p_house_id" to houseId,
                    "p_title" to "Item Purchased",
                    "p_message" to "Just bought the $itemName.",
                    "p_type" to "shopping",
                    "p_data" to mapOf("id" to itemId),
                    "p_exclude_user_id" to currentUserId
                )
            )

            FlockrLogger.repoSuccess(TAG, "markAsPurchased", "Item marked as purchased")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "markAsPurchased", e)
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


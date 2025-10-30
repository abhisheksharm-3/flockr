package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.ShoppingItem
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

    fun getShoppingItemsFlow(houseId: String): Flow<List<ShoppingItem>> {
        val channel = supabase.realtime.channel("shopping_$houseId")

        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "shopping_items"
            filter = "house_id=eq.$houseId"
        }.map {
            getShoppingItems(houseId)
        }
    }

    suspend fun getShoppingItems(houseId: String): List<ShoppingItem> {
        return try {
            supabase.from("shopping_items")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                        eq("is_purchased", false)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<ShoppingItem>()
        } catch (e: Exception) {
            emptyList()
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
                    mapOf(
                        "house_id" to houseId,
                        "item_name" to itemName,
                        "quantity" to quantity,
                        "added_by" to currentUserId
                    )
                )

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

            // Create notification
            supabase.postgrest.rpc(
                "create_notification_for_house",
                mapOf(
                    "p_house_id" to houseId,
                    "p_title" to "Item Purchased",
                    "p_message" to "Just bought the $itemName.",
                    "p_data" to mapOf("type" to "shopping", "id" to itemId),
                    "p_exclude_user_id" to currentUserId
                )
            )

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


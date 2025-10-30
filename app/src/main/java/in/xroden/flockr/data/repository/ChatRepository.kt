package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
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
class ChatRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    fun getMessagesFlow(houseId: String): Flow<List<Message>> {
        val channel = supabase.realtime.channel("messages_$houseId")

        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
            filter = "house_id=eq.$houseId"
        }.map {
            getMessages(houseId)
        }
    }

    suspend fun getMessages(houseId: String): List<Message> {
        return try {
            supabase.from("messages")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<Message>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendMessage(houseId: String, content: String, houseName: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            supabase.from("messages")
                .insert(
                    mapOf(
                        "house_id" to houseId,
                        "user_id" to currentUserId,
                        "content" to content
                    )
                )

            // Create notification for house members
            val truncatedContent = if (content.length > 50) {
                content.substring(0, 50) + "..."
            } else {
                content
            }

            supabase.postgrest.rpc(
                "create_notification_for_house",
                mapOf(
                    "p_house_id" to houseId,
                    "p_title" to "New message in $houseName",
                    "p_message" to truncatedContent,
                    "p_data" to mapOf("type" to "message", "houseId" to houseId),
                    "p_exclude_user_id" to currentUserId
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


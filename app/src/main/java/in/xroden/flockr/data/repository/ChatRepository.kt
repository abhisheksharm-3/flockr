package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.Message
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    fun getMessagesFlow(houseId: String): Flow<List<Message>> {
        return kotlinx.coroutines.flow.flow {
            // Emit initial value immediately
            android.util.Log.d("ChatRepository", "Emitting initial messages for house: $houseId")
            val initialMessages = getMessages(houseId)
            emit(initialMessages)

            // Create and subscribe to the channel
            val channelId = "messages_${houseId}_${System.currentTimeMillis()}"
            val channel = supabase.realtime.channel(channelId)

            try {
                // Configure realtime subscription BEFORE subscribing
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                    filter = "house_id=eq.$houseId"
                }
                
                // Subscribe and wait for it to be ready
                channel.subscribe(blockUntilSubscribed = true)
                android.util.Log.d("ChatRepository", "Channel subscribed successfully")

                // Now listen for changes
                changeFlow.collect { change ->
                    android.util.Log.d("ChatRepository", "Realtime update received: ${change.javaClass.simpleName}")
                    // Small delay to ensure database consistency
                    kotlinx.coroutines.delay(200)
                    val updatedMessages = getMessages(houseId)
                    android.util.Log.d("ChatRepository", "Emitting ${updatedMessages.size} messages after update")
                    emit(updatedMessages)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error in realtime subscription", e)
                // Keep the initial messages visible even if realtime fails
            } finally {
                try {
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    android.util.Log.e("ChatRepository", "Error removing channel", e)
                }
            }
        }
    }

    suspend fun getMessages(houseId: String): List<Message> {
        return try {
            android.util.Log.d("ChatRepository", "Fetching messages for houseId: $houseId")

            // Fetch messages with sender name from profiles table
            val response = supabase.from("messages")
                .select(Columns.raw("*, profiles!messages_user_id_fkey(full_name)")) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }

            android.util.Log.d("ChatRepository", "Response received, decoding JSON")

            val result = response.decodeAs<JsonArray>()

            android.util.Log.d("ChatRepository", "Successfully decoded ${result.size} messages")

            val messages = result.mapNotNull { element ->
                val obj = element.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val houseId = obj["house_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val userId = obj["user_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val content = obj["content"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val createdAt = obj["created_at"]?.jsonPrimitive?.content ?: return@mapNotNull null

                // Extract sender name from profiles
                val senderName = obj["profiles"]?.jsonObject?.get("full_name")?.jsonPrimitive?.content

                android.util.Log.d("ChatRepository", "Parsed message: id=$id, content=$content, sender=$senderName")

                Message(
                    id = id,
                    houseId = houseId,
                    userId = userId,
                    content = content,
                    createdAt = createdAt,
                    senderName = senderName ?: "Unknown"
                )
            }

            android.util.Log.d("ChatRepository", "Returning ${messages.size} messages")
            messages
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error fetching messages for houseId: $houseId", e)
            android.util.Log.e("ChatRepository", "Exception type: ${e.javaClass.name}")
            android.util.Log.e("ChatRepository", "Exception message: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun sendMessage(houseId: String, content: String, houseName: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            android.util.Log.d("ChatRepository", "Sending message: houseId=$houseId, userId=$currentUserId, content=$content")

            supabase.from("messages")
                .insert(
                    mapOf(
                        "house_id" to houseId,
                        "user_id" to currentUserId,
                        "content" to content
                    )
                )

            android.util.Log.d("ChatRepository", "Message inserted successfully")

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

            android.util.Log.d("ChatRepository", "Message sent and notification created successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Error sending message", e)
            Result.failure(e)
        }
    }
}


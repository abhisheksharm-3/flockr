package `in`.xroden.flockr.features.chat.data

import `in`.xroden.flockr.data.dto.MessageInsert
import `in`.xroden.flockr.features.chat.model.Message
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
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.launch

@Singleton
class ChatRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    fun getCurrentUserId(): String? = userId

    fun getMessagesFlow(houseId: String): Flow<Result<List<Message>>> = callbackFlow {
        val channelId = "messages_$houseId"
        val channel = supabase.realtime.channel(channelId)

        try {
            send(getMessages(houseId))

            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "messages"
                filter(FilterOperation("house_id", FilterOperator.EQ, houseId))
            }

            channel.subscribe(blockUntilSubscribed = true)

            changeFlow.collect {
                kotlinx.coroutines.delay(100)
                send(getMessages(houseId))
            }
        } catch (e: Exception) {
            send(Result.failure(e))
        }

        awaitClose {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    suspend fun getMessages(houseId: String): Result<List<Message>> {
        return try {
            val response = supabase.from("messages")
                .select(Columns.raw("*, profiles!messages_user_id_fkey(full_name)")) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }

            val result = response.decodeAs<JsonArray>()

            val messages = result.mapNotNull { element ->
                val obj = element.jsonObject
                val senderName = obj["profiles"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }
                    ?.jsonObject?.get("full_name")?.jsonPrimitive?.content

                try {
                    Message(
                        id = obj["id"]?.jsonPrimitive?.content ?: "",
                        houseId = obj["house_id"]?.jsonPrimitive?.content ?: "",
                        userId = obj["user_id"]?.jsonPrimitive?.content ?: "",
                        content = obj["content"]?.jsonPrimitive?.content ?: "",
                        createdAt = obj["created_at"]?.jsonPrimitive?.content?.let { Instant.parse(it) }
                            ?: Instant.DISTANT_PAST,
                        senderName = senderName
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(houseId: String, content: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            supabase.from("messages")
                .insert(
                    MessageInsert(
                        houseId = houseId,
                        userId = currentUserId,
                        content = content
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
                        title = "New Message",
                        message = content.take(50),
                        type = "message_sent",
                        data = """{"type":"message_sent"}""",
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

    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            supabase.from("messages")
                .delete {
                    filter {
                        eq("id", messageId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

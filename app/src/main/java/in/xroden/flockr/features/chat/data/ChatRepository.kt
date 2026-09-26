package `in`.xroden.flockr.features.chat.data

import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.features.chat.data.MessageInsert
import `in`.xroden.flockr.features.chat.model.Message
import `in`.xroden.flockr.features.chat.model.MessageWithProfile
import `in`.xroden.flockr.core.realtime.TableWatch
import `in`.xroden.flockr.core.realtime.liveQuery
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val supabase: SupabaseClient
) {

    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    /** ponytail: re-reads the latest page on every change; append from the change payload if houses get chatty. */
    fun getMessagesFlow(houseId: String): Flow<Result<List<Message>>> =
        supabase.liveQuery(listOf(TableWatch("messages", "house_id", houseId))) { getMessages(houseId) }

    private suspend fun getMessages(houseId: String): List<Message> =
        supabase.from("messages")
            .select(Columns.raw("*, profiles!messages_user_id_fkey(full_name)")) {
                filter { eq("house_id", houseId) }
                order("created_at", Order.DESCENDING)
                limit(MESSAGE_PAGE_SIZE)
            }
            .decodeList<MessageWithProfile>()
            .map { it.toMessage() }
            .reversed()

    private companion object {
        const val MESSAGE_PAGE_SIZE = 100L
    }

    suspend fun sendMessage(houseId: String, content: String): Result<Unit> = runCatching {
        val userId = requireAuthenticated(getCurrentUserId())
        val text = InputSanitizer.sanitizeText(Validators.validateMessageContent(content).getOrThrow())
        supabase.from("messages").insert(MessageInsert(houseId = houseId, userId = userId, content = text))
    }
}

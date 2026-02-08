package `in`.xroden.flockr.features.chat.data

import `in`.xroden.flockr.features.chat.model.Message
import kotlinx.coroutines.flow.Flow

interface IChatRepository {
    fun getMessagesFlow(houseId: String): Flow<Result<List<Message>>>
    suspend fun sendMessage(houseId: String, content: String): Result<Unit>
    fun getCurrentUserId(): String?
}

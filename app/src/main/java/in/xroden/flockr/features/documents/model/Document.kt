package `in`.xroden.flockr.features.documents.model

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.data.serialization.InstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Document(
    val id: String,
    @SerialName("house_id")
    val houseId: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("storage_path")
    val storagePath: String,
    @SerialName("file_name")
    val fileName: String,
    @SerialName("file_size")
    val fileSize: Long? = null,
    @SerialName("mime_type")
    val mimeType: String? = null,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

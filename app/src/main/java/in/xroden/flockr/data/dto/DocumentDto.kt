package `in`.xroden.flockr.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DocumentInsert(
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
    val mimeType: String? = null
)



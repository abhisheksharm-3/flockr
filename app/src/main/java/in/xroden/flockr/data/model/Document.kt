package `in`.xroden.flockr.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Document(
    val id: String,
    val houseId: String? = null,
    val userId: String,
    val storagePath: String,
    val fileName: String,
    val fileSize: Long? = null,
    val mimeType: String? = null,
    val createdAt: String
)


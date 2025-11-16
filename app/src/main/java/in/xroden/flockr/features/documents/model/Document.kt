package `in`.xroden.flockr.features.documents.model

import `in`.xroden.flockr.data.serialization.InstantSerializer
import kotlinx.datetime.Instant
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
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

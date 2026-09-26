/** A housemate's live shared point, as the house sees it while they share. */
package `in`.xroden.flockr.features.location.model

import `in`.xroden.flockr.data.serialization.InstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Visible only between [startedAt] and [expiresAt]; the database deletes it when sharing stops or runs out. */
@Serializable
data class MemberLocation(
    @SerialName("house_id") val houseId: String,
    @SerialName("user_id") val userId: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("accuracy_m") val accuracyMeters: Float? = null,
    @SerialName("started_at") @Serializable(with = InstantSerializer::class) val startedAt: Instant,
    @SerialName("updated_at") @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
    @SerialName("expires_at") @Serializable(with = InstantSerializer::class) val expiresAt: Instant,
)

package `in`.xroden.flockr.features.auth.model

import `in`.xroden.flockr.data.serialization.InstantSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val email: String,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("has_completed_onboarding")
    val hasCompletedOnboarding: Boolean = false,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null
)

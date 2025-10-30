package `in`.xroden.flockr.data.model

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
    val createdAt: String? = null
)


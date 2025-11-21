package `in`.xroden.flockr.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileUpdate(
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("has_completed_onboarding")
    val hasCompletedOnboarding: Boolean? = null
)



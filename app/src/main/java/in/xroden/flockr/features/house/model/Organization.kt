package `in`.xroden.flockr.features.house.model

import kotlinx.serialization.Serializable

// Organization-related models

@Serializable
data class Organization(
    val id: String,
    val name: String,
    val description: String? = null
)


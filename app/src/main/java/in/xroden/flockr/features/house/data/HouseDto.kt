/** Write payloads for the house tables a member may update directly. Null fields are left unchanged. */
package `in`.xroden.flockr.features.house.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HouseUpdate(
    val name: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("header_image_url")
    val headerImageUrl: String? = null
)

@Serializable
data class HouseConfigUpdate(
    @SerialName("currency_code")
    val currencyCode: String? = null,
    @SerialName("date_format")
    val dateFormat: String? = null,
    @SerialName("first_day_of_week")
    val firstDayOfWeek: Int? = null,
    val timezone: String? = null
)

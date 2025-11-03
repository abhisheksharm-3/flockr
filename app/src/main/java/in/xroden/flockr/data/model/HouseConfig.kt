package `in`.xroden.flockr.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HouseConfig(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("currency_code")
    val currencyCode: String = "USD",
    @SerialName("currency_symbol")
    val currencySymbol: String = "$",
    @SerialName("date_format")
    val dateFormat: String = "YYYY-MM-DD",
    @SerialName("first_day_of_week")
    val firstDayOfWeek: Int = 0,
    val timezone: String = "UTC",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)




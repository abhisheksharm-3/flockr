package `in`.xroden.flockr.features.house.model

import `in`.xroden.flockr.data.serialization.InstantSerializer
import `in`.xroden.flockr.utils.getCurrencySymbol
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Currency

@Serializable
data class HouseConfig(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("currency_code")
    val currencyCode: String = "USD",
    @SerialName("date_format")
    val dateFormat: String = "YYYY-MM-DD",
    @SerialName("first_day_of_week")
    val firstDayOfWeek: Int = 0,
    val timezone: String = "UTC",
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @SerialName("updated_at")
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null
) {
    /**
     * Derive currency symbol from currency code
     * No longer stored redundantly in database
     */
    fun getCurrencySymbol(): String {
        return try {
            Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
            "$"
        }
    }
}

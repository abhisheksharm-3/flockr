package `in`.xroden.flockr.data.dto.house

import `in`.xroden.flockr.data.serialization.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class GetHousesEnrichedParams(
    @SerialName("p_user_id")
    val userId: String,
    @SerialName("p_month")
    val month: String
)

@Serializable
data class HouseEnrichedResult(
    val id: String,
    val name: String,
    @SerialName("owner_id")
    val ownerId: String,
    @SerialName("invite_code")
    val inviteCode: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("header_image_url")
    val headerImageUrl: String? = null,
    @SerialName("member_count")
    val memberCount: Int = 0,
    @SerialName("monthly_expense")
    @Serializable(with = BigDecimalSerializer::class)
    val monthlyExpense: BigDecimal = BigDecimal.ZERO,
    @SerialName("currency_code")
    val currencyCode: String = "USD"
)

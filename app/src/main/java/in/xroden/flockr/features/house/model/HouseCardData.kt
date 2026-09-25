/** A house as its home-screen card shows it, one row of `get_my_houses`. */
package `in`.xroden.flockr.features.house.model

import `in`.xroden.flockr.data.serialization.BigDecimalSerializer
import `in`.xroden.flockr.utils.formatMoney
import java.math.BigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [monthlySpend] is everything the house spent this calendar month in its own time zone, per-diem
 * included and payments between housemates excluded, the same total the monthly report shows.
 */
@Serializable
data class HouseCardData(
    val id: String,
    val name: String,
    @SerialName("owner_id")
    val ownerId: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("header_image_url")
    val headerImageUrl: String? = null,
    @SerialName("invite_code")
    val inviteCode: String? = null,
    @SerialName("member_count")
    val memberCount: Int,
    @SerialName("currency_code")
    val currencyCode: String,
    @SerialName("monthly_spend")
    @Serializable(with = BigDecimalSerializer::class)
    val monthlySpend: BigDecimal
) {
    val house: House get() = House(id, name, ownerId, inviteCode, address, latitude, longitude, headerImageUrl)

    val monthlySpendLabel: String get() = monthlySpend.formatMoney(currencyCode)
}

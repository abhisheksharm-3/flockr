package `in`.xroden.flockr.features.house.model

import `in`.xroden.flockr.data.dto.house.HouseEnrichedResult
import `in`.xroden.flockr.utils.formatMoney
import java.math.BigDecimal

/**
 * A house as its home-screen card shows it.
 *
 * [monthlySpend] is null until the enriched load returns, so the card shows nothing rather than a
 * zero in the wrong currency while it waits.
 */
data class HouseCardData(
    val house: House,
    val memberCount: Int = 0,
    val monthlySpend: BigDecimal? = null,
    val currencyCode: String = DEFAULT_CURRENCY_CODE
) {
    /** This month's spend, formatted in the house currency, or null until it has loaded. */
    val monthlySpendLabel: String? get() = monthlySpend?.formatMoney(currencyCode)

    companion object {
        fun fromEnriched(result: HouseEnrichedResult) = HouseCardData(
            house = House(
                id = result.id,
                name = result.name,
                ownerId = result.ownerId,
                inviteCode = result.inviteCode,
                address = result.address,
                latitude = result.latitude,
                longitude = result.longitude,
                headerImageUrl = result.headerImageUrl
            ),
            memberCount = result.memberCount,
            monthlySpend = result.monthlyExpense,
            currencyCode = result.currencyCode
        )
    }
}

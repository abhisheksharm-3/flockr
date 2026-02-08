package `in`.xroden.flockr.features.house.model

import `in`.xroden.flockr.data.dto.house.HouseEnrichedResult
import java.math.BigDecimal

data class HouseCardData(
    val house: House,
    val memberCount: Int = 0,
    val monthlyExpense: BigDecimal = BigDecimal.ZERO,
    val currencySymbol: String = "$"
) {
    companion object {
        fun fromEnriched(result: HouseEnrichedResult): HouseCardData {
            val house = House(
                id = result.id,
                name = result.name,
                ownerId = result.ownerId,
                inviteCode = result.inviteCode,
                address = result.address,
                latitude = result.latitude,
                longitude = result.longitude,
                headerImageUrl = result.headerImageUrl
            )
            return HouseCardData(
                house = house,
                memberCount = result.memberCount,
                monthlyExpense = result.monthlyExpense,
                currencySymbol = getCurrencySymbol(result.currencyCode)
            )
        }

        private fun getCurrencySymbol(currencyCode: String): String = when (currencyCode) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "INR" -> "₹"
            "JPY", "CNY" -> "¥"
            "KRW" -> "₩"
            "AUD", "CAD" -> "$"
            else -> currencyCode
        }
    }
}

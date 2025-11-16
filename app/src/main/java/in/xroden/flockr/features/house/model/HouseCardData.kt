package `in`.xroden.flockr.features.house.model

import java.math.BigDecimal

/**
 * Enriched house data for displaying on home screen cards
 */
data class HouseCardData(
    val house: House,
    val memberCount: Int = 0,
    val monthlyExpense: BigDecimal = BigDecimal.ZERO,
    val currencySymbol: String = "$"
)

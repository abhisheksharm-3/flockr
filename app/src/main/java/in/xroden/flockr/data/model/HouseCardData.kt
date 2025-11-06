package `in`.xroden.flockr.data.model

/**
 * Enriched house data for displaying on home screen cards
 */
data class HouseCardData(
    val house: House,
    val memberCount: Int = 0,
    val monthlyExpense: Double = 0.0,
    val currencySymbol: String = "$"
)


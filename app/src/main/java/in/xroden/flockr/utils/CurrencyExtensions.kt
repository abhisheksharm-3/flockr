package `in`.xroden.flockr.utils

import `in`.xroden.flockr.features.house.model.HouseConfig
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Get currency symbol from currency code
 */
fun getCurrencySymbol(currencyCode: String): String {
    return try {
        Currency.getInstance(currencyCode).symbol
    } catch (_: Exception) {
        when (currencyCode) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "INR" -> "₹"
            "CAD" -> "C$"
            "AUD" -> "A$"
            "CNY" -> "¥"
            else -> "$"
        }
    }
}

/**
 * Extension function to get currency symbol from HouseConfig
 */
fun HouseConfig?.getCurrencySymbol(): String {
    val code = this?.currencyCode ?: "USD"
    return getCurrencySymbol(code)
}

/**
 * Format BigDecimal as currency string
 */
fun BigDecimal.formatAsCurrency(currencyCode: String): String {
    return try {
        val currency = Currency.getInstance(currencyCode)
        val format = NumberFormat.getCurrencyInstance(getLocaleForCurrency(currencyCode))
        format.currency = currency
        format.format(this)
    } catch (_: Exception) {
        val symbol = getCurrencySymbol(currencyCode)
        "$symbol${this.setScale(2, java.math.RoundingMode.HALF_UP)}"
    }
}

/**
 * Format BigDecimal with HouseConfig currency
 */
fun BigDecimal.formatAsCurrency(config: HouseConfig?): String {
    val currencyCode = config?.currencyCode ?: "USD"
    return formatAsCurrency(currencyCode)
}

private fun getLocaleForCurrency(currencyCode: String): Locale {
    return when (currencyCode) {
        "USD", "CAD" -> Locale.US
        "EUR" -> Locale.GERMANY
        "GBP" -> Locale.UK
        "JPY" -> Locale.JAPAN
        "INR" -> Locale.Builder().setLanguage("en").setRegion("IN").build()
        "AUD" -> Locale.Builder().setLanguage("en").setRegion("AU").build()
        "CNY" -> Locale.CHINA
        else -> Locale.getDefault()
    }
}



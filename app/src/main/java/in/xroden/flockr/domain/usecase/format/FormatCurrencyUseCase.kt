package `in`.xroden.flockr.domain.usecase.format

import `in`.xroden.flockr.features.house.model.HouseConfig
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/**
 * Use case to format currency values consistently
 */
class FormatCurrencyUseCase @Inject constructor() {

    /**
     * Format a BigDecimal amount as currency
     * 
     * @param amount Amount to format
     * @param config House configuration with currency settings
     * @return Formatted currency string (e.g., "$123.45", "€50.00")
     */
    operator fun invoke(amount: BigDecimal, config: HouseConfig?): String {
        val currencyCode = config?.currencyCode ?: "USD"
        
        return try {
            val currency = Currency.getInstance(currencyCode)
            val format = NumberFormat.getCurrencyInstance(getLocaleForCurrency(currencyCode))
            format.currency = currency
            format.format(amount)
        } catch (e: Exception) {
            // Fallback to manual formatting
            val symbol = getCurrencySymbol(currencyCode)
            "$symbol${amount.setScale(2, java.math.RoundingMode.HALF_UP)}"
        }
    }

    /**
     * Get currency symbol for a currency code
     */
    fun getCurrencySymbol(currencyCode: String): String {
        return try {
            Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
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
     * Format amount without currency symbol (just the number)
     */
    fun formatAmountOnly(amount: BigDecimal): String {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toString()
    }

    /**
     * Parse string to BigDecimal
     */
    fun parseAmount(amountString: String): Result<BigDecimal> {
        return try {
            val cleaned = amountString.replace(Regex("[^0-9.]"), "")
            Result.success(BigDecimal(cleaned))
        } catch (e: Exception) {
            Result.failure(Exception("Invalid amount format"))
        }
    }

    private fun getLocaleForCurrency(currencyCode: String): Locale {
        return when (currencyCode) {
            "USD", "CAD" -> Locale.US
            "EUR" -> Locale.GERMANY
            "GBP" -> Locale.UK
            "JPY" -> Locale.JAPAN
            "INR" -> Locale("en", "IN")
            "AUD" -> Locale("en", "AU")
            "CNY" -> Locale.CHINA
            else -> Locale.getDefault()
        }
    }
}



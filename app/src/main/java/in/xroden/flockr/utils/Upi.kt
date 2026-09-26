/** Paying a housemate through whichever UPI app the phone has. */
package `in`.xroden.flockr.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder

/**
 * A `upi://pay` link, per NPCI's deep-link spec, asking the payer's UPI app to send [amount] rupees to
 * [upiId]. [payeeName] and [note] are shown in that app. Rupee amounts carry two decimals.
 */
fun upiPayLink(upiId: String, payeeName: String, amount: BigDecimal, note: String): String {
    val params = listOf(
        "pa" to upiId,
        "pn" to payeeName,
        "am" to amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
        "cu" to "INR",
        "tn" to note,
    )
    return "upi://pay?" + params.joinToString("&") { (key, value) -> "$key=${URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")}" }
}

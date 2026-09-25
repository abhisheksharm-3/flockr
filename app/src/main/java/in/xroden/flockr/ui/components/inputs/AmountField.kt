/** The one field every amount in the app is typed into. */
package `in`.xroden.flockr.ui.components.inputs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import `in`.xroden.flockr.utils.currencySymbol
import `in`.xroden.flockr.utils.minorUnitDigits
import `in`.xroden.flockr.utils.parseMoney

/**
 * An amount in [currencyCode], prefixed with its symbol.
 *
 * Text that is not blank but would not parse as an amount in this currency is flagged as it is
 * typed, including one with more decimals than the currency has, such as 12.5 in yen.
 */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    currencyCode: String,
    modifier: Modifier = Modifier,
    label: String? = "Amount",
    enabled: Boolean = true,
) {
    val isInvalid = value.isNotBlank() && parseMoney(value, currencyCode) == null
    FlockrTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        enabled = enabled,
        prefix = currencySymbol(currencyCode),
        keyboardType = KeyboardType.Decimal,
        isError = isInvalid,
        supportingText = if (isInvalid) invalidAmountHint(currencyCode) else null,
    )
}

private fun invalidAmountHint(currencyCode: String): String = when (val digits = minorUnitDigits(currencyCode)) {
    0 -> "Enter a whole amount in $currencyCode"
    else -> "Enter an amount with up to $digits decimal places"
}

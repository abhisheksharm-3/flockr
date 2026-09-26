/** Choosing how a cost is split: on or off, the method, who is in it, and each person's value. */
package `in`.xroden.flockr.features.expenses.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import `in`.xroden.flockr.features.expenses.model.SplitMethod
import `in`.xroden.flockr.features.expenses.presentation.SplitDraft
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.ui.components.forms.ToggleRow
import `in`.xroden.flockr.ui.components.inputs.AmountField
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.inputs.PillSelector
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.rememberHaptics
import java.math.BigDecimal

private val SplitValueFieldWidth = 128.dp

/**
 * [owedByUser] is what each participant would owe as the form stands, shown under their name, and
 * [unassigned] comes from [SplitDraft.unassigned]. Past members stay listed only while they are in
 * the split, so an old expense can still be reopened unchanged.
 */
@Composable
fun SplitEditor(
    draft: SplitDraft,
    members: List<MemberWithProfile>,
    viewerId: String,
    currencyCode: String,
    unassigned: BigDecimal?,
    owedByUser: Map<String, BigDecimal>,
    enabled: Boolean,
    offSubtitle: String,
    onEnabledChange: (Boolean) -> Unit,
    onMethodChange: (SplitMethod) -> Unit,
    onParticipantChange: (String, Boolean) -> Unit,
    onValueChange: (String, String) -> Unit,
) {
    ToggleRow(
        title = "Split it",
        subtitle = if (draft.isEnabled) "Shared with housemates" else offSubtitle,
        checked = draft.isEnabled,
        onCheckedChange = onEnabledChange,
        enabled = enabled,
    )
    if (!draft.isEnabled) return

    PillSelector(
        tabs = SplitMethod.entries.map { it.label },
        selectedIndex = draft.method.ordinal,
        onTabSelected = { onMethodChange(SplitMethod.entries[it]) },
    )
    members.filter { it.isActive || it.userId in draft.participantIds }.forEach { member ->
        key(member.userId) {
            ParticipantRow(
                name = if (member.userId == viewerId) "You" else member.displayName,
                isIncluded = member.userId in draft.participantIds,
                method = draft.method,
                value = draft.values[member.userId].orEmpty(),
                owed = owedByUser[member.userId]?.formatMoney(currencyCode),
                currencyCode = currencyCode,
                enabled = enabled,
                onIncludedChange = { onParticipantChange(member.userId, it) },
                onValueChange = { onValueChange(member.userId, it) },
            )
        }
    }
    unassigned?.let { UnassignedLine(it, draft.method, currencyCode) }
}

@Composable
private fun ParticipantRow(
    name: String,
    isIncluded: Boolean,
    method: SplitMethod,
    value: String,
    owed: String?,
    currencyCode: String,
    enabled: Boolean,
    onIncludedChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
) {
    val haptics = rememberHaptics()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Checkbox(checked = isIncluded, onCheckedChange = { haptics.toggle(it); onIncludedChange(it) }, enabled = enabled)
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLargeEmphasized)
            if (isIncluded && owed != null) {
                Text(owed, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!isIncluded) return@Row
        when (method) {
            SplitMethod.EQUAL -> Unit
            SplitMethod.EXACT -> AmountField(
                value = value,
                onValueChange = onValueChange,
                currencyCode = currencyCode,
                label = null,
                enabled = enabled,
                modifier = Modifier.width(SplitValueFieldWidth),
            )
            SplitMethod.PERCENT, SplitMethod.SHARES -> FlockrTextField(
                value = value,
                onValueChange = onValueChange,
                suffix = if (method == SplitMethod.PERCENT) "%" else "×",
                keyboardType = KeyboardType.Decimal,
                enabled = enabled,
                modifier = Modifier.width(SplitValueFieldWidth),
            )
        }
    }
}

/** How much is still to assign, or how far over the total the values are. */
@Composable
private fun UnassignedLine(remaining: BigDecimal, method: SplitMethod, currencyCode: String) {
    val amount = remaining.abs()
    val label = if (method == SplitMethod.PERCENT) "${amount.stripTrailingZeros().toPlainString()}%" else amount.formatMoney(currencyCode)
    val (text, color) = when (remaining.signum()) {
        0 -> "Adds up to the total" to MaterialTheme.colorScheme.primary
        1 -> "$label left to assign" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> "$label over the total" to MaterialTheme.colorScheme.error
    }
    Text(text, style = MaterialTheme.typography.bodyMediumEmphasized, color = color)
}

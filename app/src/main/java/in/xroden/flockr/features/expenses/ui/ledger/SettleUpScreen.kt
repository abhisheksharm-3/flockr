/**
 * Recording a payment between the viewer and a housemate, whichever way it went: the two faces and
 * the amount on cobalt, then a sentence of who paid whom and when.
 */
package `in`.xroden.flockr.features.expenses.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import android.content.Intent
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import `in`.xroden.flockr.utils.upiPayLink
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.presentation.SettleUpFormState
import `in`.xroden.flockr.features.expenses.presentation.SettleUpViewModel
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.SkeletonFormScreen
import `in`.xroden.flockr.ui.components.HeroColumn
import `in`.xroden.flockr.ui.components.HeroSecondaryButton
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.forms.FormHero
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.HeroAmountInput
import `in`.xroden.flockr.ui.components.forms.HeroNote
import `in`.xroden.flockr.ui.components.forms.Sentence
import `in`.xroden.flockr.ui.components.forms.SentenceNote
import `in`.xroden.flockr.ui.components.forms.SentenceToken
import `in`.xroden.flockr.ui.components.forms.SentenceWords
import `in`.xroden.flockr.ui.components.inputs.FlockrDatePickerDialog
import `in`.xroden.flockr.ui.components.inputs.OptionSheet
import `in`.xroden.flockr.ui.components.inputs.amountHint
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.relativeDayLabel
import `in`.xroden.flockr.utils.rememberHaptics
import `in`.xroden.flockr.utils.toAmountInput
import java.math.BigDecimal

/** UPI moves rupees only, so paying through it is offered only in houses that count in INR. */
private const val UPI_CURRENCY = "INR"

@Composable
fun SettleUpScreen(
    houseId: String,
    fromUserId: String?,
    toUserId: String?,
    amount: BigDecimal?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SettleUpViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPickingPerson by remember { mutableStateOf(false) }
    var isPickingDate by remember { mutableStateOf(false) }

    LaunchedEffect(houseId) { viewModel.initialize(houseId, fromUserId, toUserId, amount) }
    LaunchedEffect(Unit) {
        viewModel.saved.collect {
            haptics.success()
            onSaved()
        }
    }
    LaunchedEffect(form.error) {
        val message = form.error ?: return@LaunchedEffect
        haptics.error()
        snackbarHostState.showSnackbar(message)
        viewModel.dismissError()
    }

    Scaffold(
        bottomBar = {
            if (form.isLoaded && form.members.isNotEmpty()) {
                FormSubmitBar(text = "Record payment", onClick = { viewModel.save(houseId) }, enabled = form.canSave, isLoading = form.isSaving)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            !form.isLoaded -> SkeletonFormScreen()
            form.members.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Group,
                title = "No one to settle with yet",
                subtitle = "Once a housemate joins, you can record payments between you here.",
                modifier = Modifier.padding(padding),
            )
            else -> HeroColumn(
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
                hero = {
                    FormHero("Record a payment") {
                        HeroAmountInput(form.amount, viewModel::onAmountChange, form.currencyCode, enabled = !form.isSaving)
                        SuggestionLine(form, onUse = { viewModel.onAmountChange(it.toAmountInput(form.currencyCode)) })
                    }
                },
            ) {
                val other = form.members.firstOrNull { it.userId == form.otherUserId }
                Direction(
                    form = form,
                    other = other,
                    enabled = !form.isSaving,
                    onSwap = { haptics.select(); viewModel.onDirectionChange(!form.isViewerPaying) },
                    onPickOther = { isPickingPerson = true },
                )
                Sentence {
                    SentenceWords(if (form.isViewerPaying) "You paid ${other?.shortName ?: "them"}" else "${other?.shortName ?: "They"} paid you")
                    form.date?.let {
                        SentenceWords("on")
                        SentenceToken(it.relativeDayLabel(config), onClick = { isPickingDate = true }, enabled = !form.isSaving, icon = Icons.Rounded.CalendarMonth)
                    }
                }
                val upiId = other?.upiId?.takeIf { form.isViewerPaying && form.currencyCode == UPI_CURRENCY }
                val payable = form.parsedAmount
                if (upiId != null && payable != null) {
                    Sentence {
                        SentenceWords("Not paid yet?")
                        SentenceToken(
                            text = "Pay ${payable.formatMoney(form.currencyCode)} with UPI",
                            onClick = {
                                val link = upiPayLink(upiId, other?.displayName.orEmpty(), payable, "Flockr settle-up")
                                val opened = runCatching {
                                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, link.toUri()), "Pay with"))
                                }.isSuccess
                                if (opened && form.note.isBlank()) viewModel.onNoteChange("UPI")
                                if (!opened) scope.launch { snackbarHostState.showSnackbar("No UPI app on this phone") }
                            },
                            enabled = !form.isSaving,
                            icon = Icons.Rounded.CurrencyRupee,
                        )
                    }
                }
                SentenceNote(form.note, viewModel::onNoteChange, placeholder = "Cash, UPI, bank transfer", enabled = !form.isSaving)
            }
        }
    }

    if (isPickingPerson) {
        OptionSheet(
            options = form.members,
            selected = form.members.firstOrNull { it.userId == form.otherUserId },
            onSelect = { viewModel.onOtherChange(it.userId) },
            onDismiss = { isPickingPerson = false },
            title = if (form.isViewerPaying) "Who did you pay?" else "Who paid you?",
            optionLabel = { it.displayName },
        )
    }
    if (isPickingDate) {
        form.date?.let { date ->
            FlockrDatePickerDialog(
                initialDate = date,
                firstDayOfWeek = config?.firstDayOfWeek,
                onDateSelected = { viewModel.onDateChange(it); isPickingDate = false },
                onDismiss = { isPickingDate = false },
            )
        }
    }
}

/**
 * The payer on the left and the payee on the right, with the swap between them in the centre.
 * Tapping the housemate's face, whichever side it is on, picks a different housemate.
 */
@Composable
private fun Direction(form: SettleUpFormState, other: MemberWithProfile?, enabled: Boolean, onSwap: () -> Unit, onPickOther: () -> Unit) {
    val you: @Composable RowScope.() -> Unit = {
        Party("You", if (form.isViewerPaying) "paid" else "got paid", form.viewer?.displayName ?: "You", form.viewer?.avatarUrl, onClick = null, Modifier.weight(1f))
    }
    val them: @Composable RowScope.() -> Unit = {
        Party(other?.shortName ?: "Pick someone", if (form.isViewerPaying) "got paid" else "paid", other?.displayName ?: "?", other?.avatarUrl, onClick = onPickOther.takeIf { enabled }, Modifier.weight(1f))
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
        if (form.isViewerPaying) you() else them()
        FilledTonalIconButton(
            onClick = onSwap,
            enabled = enabled,
            shapes = IconButtonDefaults.shapes(),
            modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
        ) { Icon(Icons.Rounded.SwapHoriz, contentDescription = "Swap who paid") }
        if (form.isViewerPaying) them() else you()
    }
}

@Composable
private fun Party(name: String, role: String, avatarName: String, avatarUrl: String?, onClick: (() -> Unit)?, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "Choose who", onClick = onClick) else Modifier)
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        MemberAvatar(name = avatarName, avatarUrl = avatarUrl, size = ComponentHeight.avatarLarge)
        Text(name, style = MaterialTheme.typography.titleMediumEmphasized)
        Text(role, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Under the amount: why it doesn't parse, that it settles the two exactly, or a button offering the
 * amount that would.
 */
@Composable
private fun SuggestionLine(form: SettleUpFormState, onUse: (BigDecimal) -> Unit) {
    val hint = amountHint(form.amount, form.currencyCode)
    val suggested = form.suggestedAmount
    when {
        hint != null -> HeroNote(hint, isError = true)
        suggested == null -> HeroNote("Any amount, in full or in part")
        form.parsedAmount?.compareTo(suggested) == 0 -> HeroNote("This settles you two exactly")
        else -> HeroSecondaryButton("Use ${suggested.formatMoney(form.currencyCode)}, which settles you two", onClick = { onUse(suggested) })
    }
}

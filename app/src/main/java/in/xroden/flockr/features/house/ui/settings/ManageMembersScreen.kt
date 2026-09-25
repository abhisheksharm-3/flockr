/** Who lives in a house and who used to, the invite code and email invitations, and what admins can change about members. */
package `in`.xroden.flockr.features.house.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.features.house.model.HouseInvitation
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.presentation.HouseManagementViewModel
import `in`.xroden.flockr.features.house.presentation.ManageMembersUiState
import `in`.xroden.flockr.features.house.presentation.splitWeightError
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics
import java.math.BigDecimal
import kotlinx.coroutines.launch

private const val INVITE_LINK_PREFIX = "flockr://invite/"

private sealed interface PendingConfirm {
    data class Remove(val member: MemberWithProfile) : PendingConfirm
    data class MakeOwner(val member: MemberWithProfile) : PendingConfirm
    data object NewCode : PendingConfirm
}

@Composable
fun ManageMembersScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: HouseManagementViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var confirm by remember { mutableStateOf<PendingConfirm?>(null) }
    var editingShareOf by remember { mutableStateOf<MemberWithProfile?>(null) }

    LaunchedEffect(houseId) { viewModel.load(houseId) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event.isError) haptics.error() else haptics.success()
            snackbarHostState.showSnackbar(event.message)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { FlockrTopAppBar(title = "Members", onNavigateBack = onNavigateBack, scrollBehavior = scrollBehavior) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                ManageMembersUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                is ManageMembersUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                is ManageMembersUiState.Ready -> MembersContent(
                    state = current,
                    onCopyCode = { code ->
                        copyToClipboard(context, code)
                        scope.launch { snackbarHostState.showSnackbar("Invite code copied") }
                    },
                    onShareCode = { code -> shareInvite(context, current.houseName, code) },
                    onNewCode = { confirm = PendingConfirm.NewCode },
                    onInviteEmailChange = viewModel::updateInviteEmail,
                    onInvite = viewModel::invite,
                    onCancelInvitation = viewModel::cancelInvitation,
                    onChangeRole = viewModel::changeRole,
                    onMakeOwner = { confirm = PendingConfirm.MakeOwner(it) },
                    onRemove = { confirm = PendingConfirm.Remove(it) },
                    onEditShare = { editingShareOf = it },
                )
            }
        }
    }

    when (val pending = confirm) {
        is PendingConfirm.Remove -> ConfirmDialog(
            title = "Remove ${pending.member.displayName}?",
            message = "They lose access to the house. Expenses they were part of stay on the ledger, and they move to past members.",
            confirmText = "Remove",
            isDestructive = true,
            onConfirm = {
                confirm = null
                viewModel.remove(pending.member)
            },
            onDismiss = { confirm = null },
        )
        is PendingConfirm.MakeOwner -> ConfirmDialog(
            title = "Make ${pending.member.displayName} the owner?",
            message = "Ownership moves to ${pending.member.displayName} and you become an admin. Only the new owner can hand it back.",
            confirmText = "Make owner",
            onConfirm = {
                confirm = null
                viewModel.changeRole(pending.member, HouseMemberRole.OWNER)
            },
            onDismiss = { confirm = null },
        )
        PendingConfirm.NewCode -> ConfirmDialog(
            title = "Make a new invite code?",
            message = "The current code stops working, so anyone you've sent it to will need the new one.",
            confirmText = "Make new code",
            onConfirm = {
                confirm = null
                viewModel.regenerateInviteCode()
            },
            onDismiss = { confirm = null },
        )
        null -> Unit
    }

    editingShareOf?.let { member ->
        SplitShareDialog(
            member = member,
            onConfirm = { text ->
                editingShareOf = null
                viewModel.setSplitWeight(member, text)
            },
            onDismiss = { editingShareOf = null },
        )
    }
}

@Composable
private fun MembersContent(
    state: ManageMembersUiState.Ready,
    onCopyCode: (String) -> Unit,
    onShareCode: (String) -> Unit,
    onNewCode: () -> Unit,
    onInviteEmailChange: (String) -> Unit,
    onInvite: () -> Unit,
    onCancelInvitation: (HouseInvitation) -> Unit,
    onChangeRole: (MemberWithProfile, HouseMemberRole) -> Unit,
    onMakeOwner: (MemberWithProfile) -> Unit,
    onRemove: (MemberWithProfile) -> Unit,
    onEditShare: (MemberWithProfile) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = Spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(key = "current_title") { SectionTitle("Living here · ${state.currentMembers.size}") }
        items(state.currentMembers, key = { "current_${it.userId}" }) { member ->
            MemberRow(
                member = member,
                state = state,
                onChangeRole = { onChangeRole(member, it) },
                onMakeOwner = { onMakeOwner(member) },
                onRemove = { onRemove(member) },
                onEditShare = { onEditShare(member) },
                modifier = Modifier.animateItem(),
            )
        }
        item(key = "invite_code") {
            InviteCodeCard(
                code = state.inviteCode,
                canRegenerate = state.isAdmin,
                isRegenerating = state.isRegenerating,
                onCopy = onCopyCode,
                onShare = onShareCode,
                onNewCode = onNewCode,
                modifier = Modifier.padding(top = Spacing.md),
            )
        }
        item(key = "invite_email") {
            InviteByEmailCard(
                state = state,
                onEmailChange = onInviteEmailChange,
                onInvite = onInvite,
                onCancel = onCancelInvitation,
            )
        }
        if (state.pastMembers.isNotEmpty()) {
            item(key = "past_title") {
                Column(Modifier.padding(top = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    SectionTitle("Past members")
                    Text(
                        "They've left, but the expenses they were part of still name them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.pastMembers, key = { "past_${it.userId}" }) { member ->
                MemberRow(
                    member = member,
                    state = state,
                    onChangeRole = {},
                    onMakeOwner = {},
                    onRemove = {},
                    onEditShare = {},
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A member with their role and default share; admins get a menu of what they can change. */
@Composable
private fun MemberRow(
    member: MemberWithProfile,
    state: ManageMembersUiState.Ready,
    onChangeRole: (HouseMemberRole) -> Unit,
    onMakeOwner: () -> Unit,
    onRemove: () -> Unit,
    onEditShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var isMenuOpen by remember { mutableStateOf(false) }
    val isViewer = member.userId == state.viewerId
    val isOthers = !isViewer && member.isActive && member.role != HouseMemberRole.OWNER
    val canChangeRole = state.isAdmin && isOthers
    val canMakeOwner = state.isOwner && !isViewer && member.isActive
    val canEditShare = state.isAdmin && member.isActive
    val hasMenu = canChangeRole || canMakeOwner || canEditShare
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        MemberAvatar(name = member.displayName, avatarUrl = member.avatarUrl)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(member.displayName, style = MaterialTheme.typography.bodyLargeEmphasized)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
            ) {
                if (isViewer) Tag("You", MaterialTheme.colorScheme.tertiaryContainer)
                if (member.isActive) Tag(roleLabel(member.role), roleColor(member.role))
                Text(
                    shareLabel(member),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (hasMenu) {
            Box {
                IconButton(onClick = { isMenuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Options for ${member.displayName}")
                }
                DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                    if (canChangeRole && member.role == HouseMemberRole.MEMBER) {
                        DropdownMenuItem(
                            text = { Text("Make admin") },
                            onClick = { isMenuOpen = false; haptics.tap(); onChangeRole(HouseMemberRole.ADMIN) },
                        )
                    }
                    if (canChangeRole && member.role == HouseMemberRole.ADMIN) {
                        DropdownMenuItem(
                            text = { Text("Make member") },
                            onClick = { isMenuOpen = false; haptics.tap(); onChangeRole(HouseMemberRole.MEMBER) },
                        )
                    }
                    if (canMakeOwner) {
                        DropdownMenuItem(text = { Text("Make owner") }, onClick = { isMenuOpen = false; onMakeOwner() })
                    }
                    if (canEditShare) {
                        DropdownMenuItem(text = { Text("Change default share") }, onClick = { isMenuOpen = false; onEditShare() })
                    }
                    if (canChangeRole) {
                        DropdownMenuItem(
                            text = { Text("Remove from house", color = MaterialTheme.colorScheme.error) },
                            onClick = { isMenuOpen = false; onRemove() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Surface(shape = MaterialTheme.shapes.small, color = color) {
        Text(text, style = MaterialTheme.typography.labelMediumEmphasized, modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs))
    }
}

private fun roleLabel(role: HouseMemberRole): String = when (role) {
    HouseMemberRole.OWNER -> "Owner"
    HouseMemberRole.ADMIN -> "Admin"
    HouseMemberRole.MEMBER -> "Member"
}

@Composable
private fun roleColor(role: HouseMemberRole): Color = when (role) {
    HouseMemberRole.OWNER -> MaterialTheme.colorScheme.primaryContainer
    HouseMemberRole.ADMIN -> MaterialTheme.colorScheme.secondaryContainer
    HouseMemberRole.MEMBER -> MaterialTheme.colorScheme.surfaceContainerHighest
}

private fun shareLabel(member: MemberWithProfile): String {
    val weight = member.defaultSplitWeight.stripTrailingZeros()
    val shares = "share weight ${weight.stripTrailingZeros().toPlainString()}"
    return if (member.isActive) shares else "Left the house"
}

/** The code shown large, with copying, sharing and, for admins, replacing it. */
@Composable
private fun InviteCodeCard(
    code: String?,
    canRegenerate: Boolean,
    isRegenerating: Boolean,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onNewCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    SectionCard(
        title = "Invite code",
        subtitle = "Anyone with the code can join. Each code works for 7 days after it's made.",
        modifier = modifier,
    ) {
        if (code == null) {
            Text("There's no invite code right now.", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(code, style = MaterialTheme.typography.displaySmallEmphasized, color = MaterialTheme.colorScheme.primary)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (code != null) {
                FilledTonalButton(onClick = { haptics.tap(); onCopy(code) }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(IconSize.sm))
                    Text("Copy", modifier = Modifier.padding(start = Spacing.sm))
                }
                FilledTonalButton(onClick = { haptics.tap(); onShare(code) }) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(IconSize.sm))
                    Text("Share", modifier = Modifier.padding(start = Spacing.sm))
                }
            }
            if (canRegenerate) {
                TextButton(onClick = { haptics.tap(); onNewCode() }, enabled = !isRegenerating) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(IconSize.sm))
                    Text(if (isRegenerating) "Making a new code…" else "New code", modifier = Modifier.padding(start = Spacing.sm))
                }
            }
        }
    }
}

@Composable
private fun InviteByEmailCard(
    state: ManageMembersUiState.Ready,
    onEmailChange: (String) -> Unit,
    onInvite: () -> Unit,
    onCancel: (HouseInvitation) -> Unit,
) {
    val haptics = rememberHaptics()
    SectionCard(
        title = "Invite by email",
        subtitle = "They'll see the invitation in Flockr when they sign in with this email.",
    ) {
        FlockrTextField(
            value = state.inviteEmail,
            onValueChange = onEmailChange,
            label = "Email",
            placeholder = "name@example.com",
            keyboardType = KeyboardType.Email,
            capitalization = KeyboardCapitalization.None,
            enabled = !state.isInviting,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            FilledTonalButton(
                onClick = { haptics.tap(); onInvite() },
                enabled = state.inviteEmail.isNotBlank() && !state.isInviting,
            ) {
                Text(if (state.isInviting) "Sending…" else "Send invite")
            }
        }
        if (state.invitations.isNotEmpty()) {
            Text("Waiting for an answer", style = MaterialTheme.typography.titleSmallEmphasized)
            state.invitations.forEach { invitation ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(invitation.inviteeEmail, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { haptics.tap(); onCancel(invitation) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Cancel") }
                }
            }
        }
    }
}

/** A member's default share for "split by shares": 2 for a couple in one room, 0.5 for someone away half the time. */
@Composable
private fun SplitShareDialog(member: MemberWithProfile, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val haptics = rememberHaptics()
    var text by rememberSaveable { mutableStateOf(member.defaultSplitWeight.stripTrailingZeros().toPlainString()) }
    val error = splitWeightError(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${member.displayName}'s default share") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(
                    "When an expense is split by shares, this is how many shares ${member.displayName} starts with. Everyone else counts as 1 unless you change theirs.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FlockrTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Shares",
                    keyboardType = KeyboardType.Decimal,
                    isError = error != null,
                    supportingText = error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { haptics.tap(); onConfirm(text) }, enabled = error == null) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun copyToClipboard(context: Context, code: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Invite code", code))
}

private fun shareInvite(context: Context, houseName: String, code: String) {
    val text = "Join $houseName on Flockr: $INVITE_LINK_PREFIX$code\n\nOr open Flockr, tap Join with a code and enter $code."
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, "Share invite"))
}

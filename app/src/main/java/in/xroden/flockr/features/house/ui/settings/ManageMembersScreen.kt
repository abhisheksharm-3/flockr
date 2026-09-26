/** Who lives in a house and who used to, the invite code and email invitations, and what admins can change about members. */
package `in`.xroden.flockr.features.house.ui.settings

import `in`.xroden.flockr.features.house.ui.copyInviteCode
import `in`.xroden.flockr.features.house.ui.shareInvite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.house.model.HouseMemberRole
import `in`.xroden.flockr.features.house.model.HouseInvitation
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.presentation.HouseManagementViewModel
import `in`.xroden.flockr.features.house.presentation.ManageMembersUiState
import `in`.xroden.flockr.features.house.presentation.splitWeightError
import `in`.xroden.flockr.ui.components.SkeletonHeroScreen
import `in`.xroden.flockr.ui.components.AnimatedGlyph
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.GlyphMotion
import `in`.xroden.flockr.ui.components.HeroActions
import `in`.xroden.flockr.ui.components.HeroAmount
import `in`.xroden.flockr.ui.components.HeroButton
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.HeroLabel
import `in`.xroden.flockr.ui.components.HeroSecondaryButton
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics
import java.math.BigDecimal
import kotlinx.coroutines.launch


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
        topBar = { if (state !is ManageMembersUiState.Ready) FlockrTopAppBar(title = "Members") },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val current = state) {
            ManageMembersUiState.Loading -> SkeletonHeroScreen()
            is ManageMembersUiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) { ErrorState(current.message, onRetry = { viewModel.load(houseId) }) }
            is ManageMembersUiState.Ready -> MembersContent(
                state = current,
                onCopyCode = { code ->
                    copyInviteCode(context, code)
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

/**
 * The invite code leads, because bringing someone in is what most visits are for; then who lives
 * here, email invitations and their answers, and who used to.
 */
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
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().imePadding(), state = listState, contentPadding = PaddingValues(bottom = Spacing.xxl)) {
            item(key = "hero") {
                InviteCodeHero(
                    state = state,
                    onCopy = onCopyCode,
                    onShare = onShareCode,
                    onNewCode = onNewCode,
                )
            }
            item(key = "current_title") { SectionTitle("Living here", subtitle = peopleCount(state.currentMembers.size)) }
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
            item(key = "invite_email") {
                InviteByEmail(state = state, onEmailChange = onInviteEmailChange, onInvite = onInvite)
            }
            if (state.invitations.isNotEmpty()) {
                item(key = "waiting_title") { SectionTitle("Waiting for an answer") }
                items(state.invitations, key = { "invitation_${it.id}" }) { invitation ->
                    InvitationRow(invitation, onCancel = { onCancelInvitation(invitation) }, modifier = Modifier.animateItem())
                }
            }
            if (state.pastMembers.isNotEmpty()) {
                item(key = "past_title") { SectionTitle("Past members", subtitle = "They've left, but the expenses they were part of still name them.") }
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
            item(key = "inset") { Spacer(Modifier.navigationBarsPadding()) }
        }
        HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)
    }
}

/** The code shown large on the cobalt, with sharing and copying it, and for admins replacing it; the refresh glyph turns once a new code lands. */
@Composable
private fun InviteCodeHero(
    state: ManageMembersUiState.Ready,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onNewCode: () -> Unit,
) {
    val haptics = rememberHaptics()
    val code = state.inviteCode
    HeroHeader(
        title = "Members",
        subtitle = state.houseName,
        actions = {
            if (state.isAdmin) {
                IconButton(onClick = { haptics.tap(); onNewCode() }, enabled = !state.isRegenerating, shapes = IconButtonDefaults.shapes()) {
                    AnimatedGlyph(Icons.Rounded.Refresh, trigger = code, motion = GlyphMotion.SPIN, contentDescription = "Make a new invite code")
                }
            }
        },
    ) {
        HeroLabel(if (state.isRegenerating) "Making a new code…" else "Invite code")
        if (code == null) {
            HeroCaption("There's no invite code right now.")
        } else {
            HeroAmount(code)
            HeroCaption("Anyone with the code can join. Each code works for 7 days after it's made.")
            HeroActions {
                HeroButton("Share invite", onClick = { onShare(code) })
                HeroSecondaryButton("Copy code", onClick = { onCopy(code) })
            }
        }
    }
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
    ListRow(
        headline = if (isViewer) "${member.displayName} (you)" else member.displayName,
        supporting = memberLine(member),
        leading = { MemberAvatar(name = member.displayName, avatarUrl = member.avatarUrl) },
        trailing = if (!hasMenu) null else ({
            Box {
                IconButton(onClick = { isMenuOpen = true }, shapes = IconButtonDefaults.shapes()) {
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
        }),
        modifier = modifier,
    )
}

/** "Owner · 2 shares" for someone living here, "Left the house" for a past member. */
private fun memberLine(member: MemberWithProfile): String {
    if (!member.isActive) return "Left the house"
    val weight = member.defaultSplitWeight.stripTrailingZeros()
    val shares = "${weight.toPlainString()} ${if (weight.compareTo(BigDecimal.ONE) == 0) "share" else "shares"}"
    return "${roleLabel(member.role)} · $shares"
}

private fun roleLabel(role: HouseMemberRole): String = when (role) {
    HouseMemberRole.OWNER -> "Owner"
    HouseMemberRole.ADMIN -> "Admin"
    HouseMemberRole.MEMBER -> "Member"
}

private fun peopleCount(count: Int): String = if (count == 1) "Just you so far" else "$count people"

/** The email field full width, and a send button whose arrow hops each time an invitation goes out. */
@Composable
private fun InviteByEmail(state: ManageMembersUiState.Ready, onEmailChange: (String) -> Unit, onInvite: () -> Unit) {
    val haptics = rememberHaptics()
    SectionTitle("Invite by email", subtitle = "They'll see the invitation in Flockr when they sign in with this email.")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
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
        FilledTonalButton(
            onClick = { haptics.tap(); onInvite() },
            enabled = state.inviteEmail.isNotBlank() && !state.isInviting,
            shapes = ButtonDefaults.shapes(),
        ) {
            AnimatedGlyph(
                Icons.AutoMirrored.Rounded.Send,
                trigger = state.invitations.size,
                motion = GlyphMotion.NUDGE,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(if (state.isInviting) "Sending…" else "Send invite")
        }
    }
}

@Composable
private fun InvitationRow(invitation: HouseInvitation, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    ListRow(
        headline = invitation.inviteeEmail,
        supporting = "Invited, not joined yet",
        leading = { IconBadge(Icons.Rounded.MailOutline, BadgeTone.SUN) },
        trailing = {
            TextButton(
                onClick = { haptics.tap(); onCancel() },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Cancel") }
        },
        modifier = modifier,
    )
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
            TextButton(onClick = { haptics.tap(); onConfirm(text) }, enabled = error == null, shapes = ButtonDefaults.shapes()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text("Cancel") } },
    )
}

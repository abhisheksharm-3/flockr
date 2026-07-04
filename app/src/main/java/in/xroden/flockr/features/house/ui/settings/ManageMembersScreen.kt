package `in`.xroden.flockr.features.house.ui.settings

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.features.house.presentation.HouseManagementViewModel
import `in`.xroden.flockr.features.house.model.InvitationWithHouse
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import kotlinx.coroutines.launch
import kotlin.collections.isNotEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMembersScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: HouseManagementViewModel = hiltViewModel()
) {
    var members by remember { mutableStateOf<List<MemberWithProfile>>(emptyList()) }
    var pendingInvitations by remember { mutableStateOf<List<InvitationWithHouse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf<MemberWithProfile?>(null) }
    var inviteEmail by remember { mutableStateOf("") }
    var isInviting by remember { mutableStateOf(false) }
    var expandedInvitations by remember { mutableStateOf(false) }
    var currentUserRole by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentUserId = viewModel.getCurrentUserId()

    LaunchedEffect(houseId) {
        isLoading = true
        scope.launch {
            viewModel.loadHouseDetails(houseId)
            members = viewModel.getHouseMembers(houseId)
            // Find current user's role
            currentUserRole = members.find { it.userId == currentUserId }?.role?.name
            // Only load invitations if user is owner or admin
            if (currentUserRole == HouseMemberRole.OWNER.name || currentUserRole == HouseMemberRole.ADMIN.name) {
                pendingInvitations = viewModel.getPendingInvitations(houseId)
            }
            isLoading = false
        }
    }

    // Check if user has permission to manage members
    val canManageMembers = currentUserRole == HouseMemberRole.OWNER.name || currentUserRole == HouseMemberRole.ADMIN.name

    // If not authorized, show message and return
    if (!isLoading && !canManageMembers) {
        AccessDeniedScreen(onNavigateBack = onNavigateBack)
        return
    }

    // Invite Dialog (Full Screen)
    if (showInviteDialog) {
        FullScreenInviteMemberDialog(
            email = inviteEmail,
            onEmailChange = { inviteEmail = it },
            isLoading = isInviting,
            onDismiss = {
                showInviteDialog = false
                inviteEmail = ""
            },
            onConfirm = {
                scope.launch {
                    isInviting = true
                    val emailToInvite = inviteEmail // Capture email before clearing
                    val result = viewModel.inviteMember(houseId, emailToInvite)
                    if (result.isSuccess) {
                        showInviteDialog = false
                        inviteEmail = ""
                        // Reload pending invitations
                        pendingInvitations = viewModel.getPendingInvitations(houseId)
                        snackbarHostState.showSnackbar("Invitation sent to $emailToInvite")
                    } else {
                        snackbarHostState.showSnackbar(
                            result.exceptionOrNull()?.message ?: "Failed to send invitation"
                        )
                    }
                    isInviting = false
                }
            }
        )
    }

    // Remove Confirmation Dialog
    showRemoveDialog?.let { memberToRemove ->
        RemoveMemberDialog(
            memberName = memberToRemove.fullName ?: memberToRemove.email,
            onDismiss = { showRemoveDialog = null },
            onConfirm = {
                scope.launch {
                    val result = viewModel.removeMember(houseId, memberToRemove.userId)
                    if (result.isSuccess) {
                        members = viewModel.getHouseMembers(houseId)
                        snackbarHostState.showSnackbar("Member removed")
                    } else {
                        snackbarHostState.showSnackbar("Failed to remove member")
                    }
                    showRemoveDialog = null
                }
            }
        )
    }

    Scaffold(
        topBar = { ManageMembersTopBar(onNavigateBack = onNavigateBack) },
        floatingActionButton = {
            InviteMemberFab(onClick = { showInviteDialog = true })
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Member count label
                item {
                    MembersCountHeader(memberCount = members.size)
                }

                // Pending Invitations - Collapsible Card
                if (pendingInvitations.isNotEmpty()) {
                    item {
                        PendingInvitationsCard(
                            invitations = pendingInvitations,
                            expanded = expandedInvitations,
                            onToggleExpanded = { expandedInvitations = !expandedInvitations },
                            onCancelInvitation = { invitation ->
                                scope.launch {
                                    val result = viewModel.cancelInvitation(houseId, invitation.inviteeEmail)
                                    if (result.isSuccess) {
                                        pendingInvitations = viewModel.getPendingInvitations(houseId)
                                        snackbarHostState.showSnackbar("Invitation cancelled")
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to cancel invitation")
                                    }
                                }
                            },
                            onResendInvitation = { invitation ->
                                scope.launch {
                                    val result = viewModel.resendInvitationNotification(houseId, invitation.inviteeEmail)
                                    if (result.isSuccess) {
                                        snackbarHostState.showSnackbar("Notification resent to ${invitation.inviteeEmail}")
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            result.exceptionOrNull()?.message ?: "Failed to resend notification"
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // Members List
                items(
                    items = members,
                    key = { it.userId }
                ) { member ->
                    MemberListItem(
                        member = member,
                        currentUserRole = currentUserRole,
                        isOwner = member.role == HouseMemberRole.OWNER,
                        onRemove = {
                            if (member.role == HouseMemberRole.OWNER) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Cannot remove the owner of the household")
                                }
                            } else if (member.role == HouseMemberRole.ADMIN && currentUserRole != HouseMemberRole.OWNER.name) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Only the owner can remove admins")
                                }
                            } else {
                                showRemoveDialog = member
                            }
                        },
                        onChangeRole = { newRole ->
                            scope.launch {
                                val result = viewModel.updateMemberRole(houseId, member.userId, newRole)
                                members = viewModel.getHouseMembers(houseId)
                                snackbarHostState.showSnackbar(
                                    if (result.isSuccess) "Updated ${member.fullName ?: member.email}'s role"
                                    else result.exceptionOrNull()?.message ?: "Failed to update role"
                                )
                            }
                        }
                    )
                }

                // Bottom padding for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageMembersTopBar(onNavigateBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "Manage Members",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun InviteMemberFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Default.Add, "Invite") },
        text = { Text("Invite Member", fontWeight = FontWeight.Bold) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccessDeniedScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = { ManageMembersTopBar(onNavigateBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Access Denied",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Only house owners and admins can manage members.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MembersCountHeader(memberCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Members",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "$memberCount member${if (memberCount != 1) "s" else ""}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PendingInvitationsCard(
    invitations: List<InvitationWithHouse>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onCancelInvitation: (InvitationWithHouse) -> Unit,
    onResendInvitation: (InvitationWithHouse) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
    ) {
        Column {
            // Header - Always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Pending Invitations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${invitations.size} invitation${if (invitations.size != 1) "s" else ""} sent",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expandable content
            if (expanded) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    invitations.forEach { invitation ->
                        PendingInvitationItem(
                            invitation = invitation,
                            onCancel = { onCancelInvitation(invitation) },
                            onResend = { onResendInvitation(invitation) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MemberListItem(
    member: MemberWithProfile,
    currentUserRole: String?,
    isOwner: Boolean,
    onRemove: () -> Unit,
    onChangeRole: (HouseMemberRole) -> Unit = {}
) {
    // Only the owner can promote/demote, and never the owner's own row.
    val canChangeRole = currentUserRole == HouseMemberRole.OWNER.name &&
        member.role != HouseMemberRole.OWNER
    val canDelete = when {
        member.role == HouseMemberRole.OWNER -> false
        member.role == HouseMemberRole.ADMIN && currentUserRole != HouseMemberRole.OWNER.name -> false
        else -> currentUserRole == HouseMemberRole.OWNER.name || currentUserRole == HouseMemberRole.ADMIN.name
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar or Icon
            if (!member.avatarUrl.isNullOrBlank()) {
                androidx.compose.foundation.Image(
                    painter = coil.compose.rememberAsyncImagePainter(
                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(member.avatarUrl)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Member Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = member.fullName ?: "Unknown User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Role Badge
                    val roleColor = when (member.role) {
                        HouseMemberRole.OWNER -> Color(0xFFFFD700) // Gold
                        HouseMemberRole.ADMIN -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                    val roleTextColor = when (member.role) {
                        HouseMemberRole.OWNER -> Color(0xFF000000)
                        HouseMemberRole.ADMIN -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    }

                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = roleColor,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = member.role.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = roleTextColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = member.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Role menu (owner only): promote to Admin / demote to Member.
            if (canChangeRole) {
                var roleMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { roleMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Change role")
                    }
                    DropdownMenu(
                        expanded = roleMenuExpanded,
                        onDismissRequest = { roleMenuExpanded = false }
                    ) {
                        if (member.role != HouseMemberRole.ADMIN) {
                            DropdownMenuItem(
                                text = { Text("Make admin") },
                                onClick = { roleMenuExpanded = false; onChangeRole(HouseMemberRole.ADMIN) }
                            )
                        }
                        if (member.role != HouseMemberRole.MEMBER) {
                            DropdownMenuItem(
                                text = { Text("Make member") },
                                onClick = { roleMenuExpanded = false; onChangeRole(HouseMemberRole.MEMBER) }
                            )
                        }
                    }
                }
            }

            // Delete Button (only shown if user has permission)
            if (canDelete) {
                IconButton(
                    onClick = onRemove,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove member"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenInviteMemberDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isValid = email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Invite Member", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = onConfirm,
                            enabled = !isLoading && isValid
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Send", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Text(
                        text = "Enter the email address of the person you want to invite.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email Address") },
                    placeholder = { Text("example@email.com") },
                    leadingIcon = { 
                        Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                // Info text
                if (isValid) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "If this person hasn't signed up yet, they'll receive the invitation after creating an account with this email.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RemoveMemberDialog(
    memberName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Remove Member?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to remove $memberName from this household? They will lose access to all shared data."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Remove", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun PendingInvitationItem(
    invitation: InvitationWithHouse,
    onCancel: () -> Unit,
    onResend: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Email Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Email and Status
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = invitation.inviteeEmail,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary)
                    )
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Resend Button
            IconButton(
                onClick = onResend,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Resend notification",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Cancel Button
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel invitation",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

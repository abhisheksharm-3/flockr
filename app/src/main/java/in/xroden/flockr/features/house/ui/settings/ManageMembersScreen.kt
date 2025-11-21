package `in`.xroden.flockr.features.house.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.features.house.domain.HouseManagementViewModel
import `in`.xroden.flockr.features.house.model.HouseInvitation
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
    var pendingInvitations by remember { mutableStateOf<List<HouseInvitation>>(emptyList()) }
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
            viewModel.loadHouse(houseId)
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
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Manage Members",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
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
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
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
        return
    }

    // Invite Dialog
    if (showInviteDialog) {
        InviteMemberDialog(
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
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Manage Members",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showInviteDialog = true },
                icon = { Icon(Icons.Default.Add, "Invite") },
                text = { Text("Invite Member") },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium
            )
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
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Members",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${members.size} member${if (members.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Pending Invitations - Collapsible Card
                if (pendingInvitations.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column {
                                // Header - Always visible
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedInvitations = !expandedInvitations }
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
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "${pendingInvitations.size} invitation${if (pendingInvitations.size != 1) "s" else ""} sent",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = if (expandedInvitations) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (expandedInvitations) "Collapse" else "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Expandable content
                                if (expandedInvitations) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pendingInvitations.forEach { invitation ->
                                            PendingInvitationItem(
                                                invitation = invitation,
                                                onCancel = {
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
                                                onResend = {
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
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                // Members List
                items(members) { member ->
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

@Composable
fun MemberListItem(
    member: MemberWithProfile,
    currentUserRole: String?,
    isOwner: Boolean,
    onRemove: () -> Unit
) {
    val canDelete = when {
        member.role == HouseMemberRole.OWNER -> false
        member.role == HouseMemberRole.ADMIN && currentUserRole != HouseMemberRole.OWNER.name -> false
        else -> currentUserRole == HouseMemberRole.OWNER.name || currentUserRole == HouseMemberRole.ADMIN.name
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
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
                        fontWeight = FontWeight.SemiBold,
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
                        shape = MaterialTheme.shapes.small,
                        color = roleColor,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = member.role.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = roleTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = member.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete Button (only shown if user has permission)
            if (canDelete) {
                IconButton(
                    onClick = onRemove,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
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

@Composable
fun InviteMemberDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.shapes.extraLarge
                        )
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Title
                Text(
                    text = "Invite Member",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Enter the email address of the person you want to invite to this household",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email Address") },
                    placeholder = { Text("example@email.com") },
                    leadingIcon = { 
                        Icon(Icons.Default.Email, null) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Send Invite")
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
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PendingInvitationItem(
    invitation: HouseInvitation,
    onCancel: () -> Unit,
    onResend: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
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
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                        style = MaterialTheme.typography.bodySmall,
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

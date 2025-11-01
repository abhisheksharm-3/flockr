package `in`.xroden.flockr.ui.screens.house

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.model.MemberWithProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMembersScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: `in`.xroden.flockr.ui.viewmodel.HouseManagementViewModel = hiltViewModel()
) {
    var members by remember { mutableStateOf<List<MemberWithProfile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteEmail by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(houseId) {
        android.util.Log.d("ManageMembersScreen", "Screen launched for house: $houseId")
        isLoading = true
        scope.launch {
            android.util.Log.d("ManageMembersScreen", "Fetching members...")
            members = viewModel.getHouseMembers(houseId)
            android.util.Log.d("ManageMembersScreen", "Loaded ${members.size} members")
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Members") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showInviteDialog = true }) {
                Icon(Icons.Default.Add, "Invite Member")
            }
        },
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Members (${members.size})",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(members) { member ->
                    MemberCard(
                        member = member,
                        onRemove = {
                            scope.launch {
                                android.util.Log.d("ManageMembersScreen", "Removing member: ${member.fullName}")
                                val result = viewModel.removeMember(houseId, member.userId)
                                if (result.isSuccess) {
                                    android.util.Log.d("ManageMembersScreen", "Member removed, refreshing list")
                                    members = viewModel.getHouseMembers(houseId)
                                    snackbarHostState.showSnackbar("Member removed")
                                } else {
                                    android.util.Log.e("ManageMembersScreen", "Failed to remove member")
                                    snackbarHostState.showSnackbar("Failed to remove member")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite Member") },
            text = {
                Column {
                    Text(
                        text = "Enter the email address of the person you want to invite:",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            android.util.Log.d("ManageMembersScreen", "Inviting member: $inviteEmail")
                            val result = viewModel.inviteMember(houseId, inviteEmail)
                            if (result.isSuccess) {
                                android.util.Log.d("ManageMembersScreen", "Invitation sent successfully")
                                showInviteDialog = false
                                inviteEmail = ""
                                snackbarHostState.showSnackbar("Invitation sent!")
                            } else {
                                android.util.Log.e("ManageMembersScreen", "Failed to send invitation: ${result.exceptionOrNull()?.message}")
                                snackbarHostState.showSnackbar("Failed to send invitation")
                            }
                        }
                    },
                    enabled = inviteEmail.isNotBlank()
                ) {
                    Text("Send Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showInviteDialog = false
                    inviteEmail = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MemberCard(
    member: MemberWithProfile,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.fullName ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = member.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove member",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}


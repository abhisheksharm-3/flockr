package `in`.xroden.flockr.features.house.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.domain.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinHouseScreen(
    onHouseJoined: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    var inviteCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val joinState by viewModel.joinState.collectAsState()
    val previewState by viewModel.previewState.collectAsState()

    // Observe join state
    LaunchedEffect(joinState) {
        when (val state = joinState) {
            is `in`.xroden.flockr.features.house.domain.JoinHouseUiState.Success -> {
                delay(300)
                onHouseJoined(state.house?.id ?: "")
                viewModel.resetJoinState()
            }
            is `in`.xroden.flockr.features.house.domain.JoinHouseUiState.Error -> {
                errorMessage = state.message
            }
            else -> {}
        }
    }

    // Observe preview state for errors
    LaunchedEffect(previewState) {
        if (previewState is `in`.xroden.flockr.features.house.domain.HousePreviewUiState.Error) {
             errorMessage = (previewState as `in`.xroden.flockr.features.house.domain.HousePreviewUiState.Error).message
        }
    }

    // Validate code when it's complete
    LaunchedEffect(inviteCode) {
        if (inviteCode.length == 6) {
             viewModel.validateInviteCode(inviteCode)
        } else {
             viewModel.resetPreviewState()
             errorMessage = null
        }
    }

    if (previewState is `in`.xroden.flockr.features.house.domain.HousePreviewUiState.Success) {
        val preview = (previewState as `in`.xroden.flockr.features.house.domain.HousePreviewUiState.Success).preview
        AlertDialog(
            onDismissRequest = { viewModel.resetPreviewState() },
            title = { Text("Join ${preview.name}?") },
            text = {
                Column {
                    Text("Owner: ${preview.ownerName ?: "Unknown"}")
                    Text("Members: ${preview.memberCount ?: 0}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Do you want to join this household?")
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.joinHouseByInviteCode(inviteCode)
                    viewModel.resetPreviewState()
                }) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetPreviewState() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Join Household",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Join a Household",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter the invite code shared by a household member",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Invite Code Input
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Invite Code",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = {
                        inviteCode = it.uppercase().take(6)
                        errorMessage = null // Reset local error, wait for validation
                    },
                    placeholder = { Text("Enter 6-digit code") },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    ),
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    trailingIcon = {
                        if (previewState is `in`.xroden.flockr.features.house.domain.HousePreviewUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (inviteCode.length == 6 && errorMessage == null) {
                             // Maybe waiting for validation
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    singleLine = true,
                    enabled = joinState !is `in`.xroden.flockr.features.house.domain.JoinHouseUiState.Loading,
                    shape = MaterialTheme.shapes.medium
                )

                Text(
                    text = "${inviteCode.length}/6 characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "About Invite Codes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Each household has a unique 6-character code. Ask a member to share their code from the household settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

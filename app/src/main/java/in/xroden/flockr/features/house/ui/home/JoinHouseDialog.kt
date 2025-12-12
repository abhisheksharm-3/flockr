package `in`.xroden.flockr.features.house.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.house.model.HousePreview
import `in`.xroden.flockr.features.house.domain.HomeViewModel
import `in`.xroden.flockr.features.house.domain.HousePreviewUiState
import `in`.xroden.flockr.features.house.domain.JoinHouseUiState
import coil.compose.AsyncImage

/**
 * Stateful wrapper for joining a house by code using [HomeViewModel]
 */
@Composable
fun JoinHouseByCodeDialog(
    inviteCode: String,
    onDismiss: () -> Unit,
    onHouseJoined: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val previewState by viewModel.previewState.collectAsState()
    val joinState by viewModel.joinState.collectAsState()

    LaunchedEffect(inviteCode) {
        viewModel.validateInviteCode(inviteCode)
    }
    
    // Handle Join Success
    LaunchedEffect(joinState) {
        if (joinState is JoinHouseUiState.Success) {
             onHouseJoined()
        }
    }

    when (val state = previewState) {
        is HousePreviewUiState.Loading -> {
             Dialog(onDismissRequest = onDismiss) {
                 Box(
                     modifier = Modifier
                         .size(100.dp)
                         .background(Color(0xFF282A36), MaterialTheme.shapes.medium),
                     contentAlignment = Alignment.Center
                 ) {
                     CircularProgressIndicator(color = Color(0xFFBD93F9))
                 }
             }
        }
        is HousePreviewUiState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Invalid Invite", color = Color.White) },
                text = { Text(state.message, color = Color(0xFFF8F8F2)) },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("Close", color = Color(0xFFFF79C6)) }
                },
                containerColor = Color(0xFF282A36),
                titleContentColor = Color(0xFFBD93F9)
            )
        }
        is HousePreviewUiState.Success -> {
            JoinHouseDialog(
                house = state.preview,
                onDismiss = onDismiss,
                onJoin = { viewModel.joinHouseByInviteCode(inviteCode) },
                isLoading = joinState is JoinHouseUiState.Loading
            )
        }
        else -> {}
    }
}

/**
 * Pure UI Component for Join House Dialog
 */
@Composable
fun JoinHouseDialog(
    house: HousePreview,
    onDismiss: () -> Unit,
    onJoin: () -> Unit,
    isLoading: Boolean
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF282A36) // Dracula Background
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Image or Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    if (!house.headerImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = house.headerImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFF282A36)
                                        )
                                    )
                                )
                        )
                    } else {
                        // Fallback Pattern
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF44475A)), // Dracula Selection
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFF6272A4) // Dracula Comment
                            )
                        }
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = Color.Black.copy(alpha = 0.4f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                modifier = Modifier.padding(6.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Join House",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFBD93F9) // Dracula Purple
                    )

                    Text(
                        text = house.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8F8F2), // Dracula Foreground
                        textAlign = TextAlign.Center
                    )

                    // Owner / Members Info
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF44475A), MaterialTheme.shapes.large)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF8BE9FD), // Dracula Cyan
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Admin: ${house.ownerName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF8F8F2)
                        )
                    }

                    Text(
                        text = "${house.memberCount} members currently",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6272A4) // Dracula Comment
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onJoin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF50FA7B), // Dracula Green
                            contentColor = Color(0xFF282A36),
                            disabledContainerColor = Color(0xFF50FA7B).copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF282A36),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Text(
                                "Join Household",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

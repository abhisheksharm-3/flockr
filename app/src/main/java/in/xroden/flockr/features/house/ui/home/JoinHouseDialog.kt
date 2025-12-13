package `in`.xroden.flockr.features.house.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import `in`.xroden.flockr.features.house.model.HousePreview
import `in`.xroden.flockr.features.house.domain.HomeViewModel
import `in`.xroden.flockr.features.house.domain.HousePreviewUiState
import `in`.xroden.flockr.features.house.domain.JoinHouseUiState

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
                 Card(
                     shape = MaterialTheme.shapes.large,
                     colors = CardDefaults.cardColors(
                         containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                     ),
                     modifier = Modifier.padding(16.dp)
                 ) {
                     Box(
                         modifier = Modifier.padding(24.dp),
                         contentAlignment = Alignment.Center
                     ) {
                         CircularProgressIndicator()
                     }
                 }
             }
        }
        is HousePreviewUiState.Error -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Invalid Invite") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("Close") }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                        .height(180.dp)
                ) {
                    if (!house.headerImageUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = house.headerImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient Overlay
                        
                    } else {
                        // Fallback Pattern
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
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
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f),
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
                        text = "Join Household",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = house.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // Owner / Members Info
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Admin: ${house.ownerName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = "${house.memberCount} members currently",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
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

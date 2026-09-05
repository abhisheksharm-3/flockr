package `in`.xroden.flockr.features.house.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import `in`.xroden.flockr.features.house.model.HousePreview
import `in`.xroden.flockr.features.house.presentation.HomeViewModel
import `in`.xroden.flockr.features.house.presentation.HousePreviewUiState
import `in`.xroden.flockr.features.house.presentation.JoinHouseUiState
import `in`.xroden.flockr.ui.components.loading.ListScreenSkeleton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.utils.rememberHaptics

/**
 * Full-screen Join House Preview Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinHousePreviewScreen(
    inviteCode: String,
    onNavigateBack: () -> Unit,
    onHouseJoined: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val previewState by viewModel.previewState.collectAsStateWithLifecycle()
    val joinState by viewModel.joinState.collectAsStateWithLifecycle()

    LaunchedEffect(inviteCode) {
        viewModel.validateInviteCode(inviteCode)
    }
    
    LaunchedEffect(joinState) {
        if (joinState is JoinHouseUiState.Success) {
            haptics.success()
            onHouseJoined()
        }
    }

    Scaffold(
        topBar = { JoinHousePreviewTopBar(onNavigateBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val state = previewState) {
            is HousePreviewUiState.Loading -> {
                ListScreenSkeleton(modifier = Modifier.fillMaxSize().padding(padding))
            }
            is HousePreviewUiState.Error -> {
                InvalidInviteErrorState(
                    message = state.message,
                    onNavigateBack = onNavigateBack,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
            is HousePreviewUiState.Success -> {
                JoinHousePreviewContent(
                    house = state.preview,
                    onJoin = { viewModel.joinHouseByInviteCode(inviteCode) },
                    isLoading = joinState is JoinHouseUiState.Loading,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JoinHousePreviewTopBar(onNavigateBack: () -> Unit) {
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun InvalidInviteErrorState(
    message: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Home,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                "Invalid Invite",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Button(
                onClick = onNavigateBack,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Go Back", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun JoinHousePreviewContent(
    house: HousePreview,
    onJoin: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        HouseHeaderImageCard(imageUrl = house.headerImageUrl)

        // House Name
        Text(
            text = house.name,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        HouseInfoCardsRow(ownerName = house.ownerName, memberCount = house.memberCount)

        Spacer(modifier = Modifier.weight(1f))

        JoinHouseButton(onJoin = onJoin, isLoading = isLoading)
    }
}

@Composable
private fun HouseHeaderImageCard(imageUrl: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            if (!imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HouseInfoCardsRow(ownerName: String?, memberCount: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OwnerInfoCard(ownerName = ownerName, modifier = Modifier.weight(1f))
        MembersInfoCard(memberCount = memberCount, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun OwnerInfoCard(ownerName: String?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Person,
                        null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                "Admin",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                ownerName ?: "Unknown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MembersInfoCard(memberCount: Long?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.People,
                        null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            Text(
                "Members",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${memberCount ?: 0}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun JoinHouseButton(onJoin: () -> Unit, isLoading: Boolean) {
    val haptics = rememberHaptics()
    Button(
        onClick = { haptics.tap(); onJoin() },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                "Join Household",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Dialog version for inline preview (used by JoinHouseScreen)
 */
@Composable
fun JoinHouseDialog(
    house: HousePreview,
    onDismiss: () -> Unit,
    onJoin: () -> Unit,
    isLoading: Boolean
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                JoinHouseDialogHeaderImage(imageUrl = house.headerImageUrl)

                // Content
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        house.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    JoinHouseDialogInfoRow(ownerName = house.ownerName, memberCount = house.memberCount)

                    JoinHouseDialogButtons(onDismiss = onDismiss, onJoin = onJoin, isLoading = isLoading)
                }
            }
        }
    }
}

@Composable
private fun JoinHouseDialogHeaderImage(imageUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        if (!imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Home,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun JoinHouseDialogInfoRow(ownerName: String?, memberCount: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(ownerName ?: "Admin", style = MaterialTheme.typography.labelMedium)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.People, null, tint = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("${memberCount ?: 0} members", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun JoinHouseDialogButtons(
    onDismiss: () -> Unit,
    onJoin: () -> Unit,
    isLoading: Boolean
) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Cancel")
        }
        Button(
            onClick = { haptics.tap(); onJoin() },
            modifier = Modifier.weight(1f),
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Join", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

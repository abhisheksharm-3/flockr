package `in`.xroden.flockr.features.house.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.house.presentation.HomeViewModel
import `in`.xroden.flockr.features.house.presentation.HouseListUiState
import `in`.xroden.flockr.features.house.model.HouseCardData
import `in`.xroden.flockr.features.notifications.presentation.NotificationViewModel
import `in`.xroden.flockr.features.notifications.presentation.NotificationUiState
import `in`.xroden.flockr.features.settings.presentation.ProfileViewModel
import `in`.xroden.flockr.features.settings.presentation.ProfileUiState
import java.time.LocalTime
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import `in`.xroden.flockr.utils.rememberHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onHouseClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreateHouseClick: () -> Unit,
    onJoinHouseClick: () -> Unit,
    onNavigateToJoinPreview: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationUiState by notificationViewModel.uiState.collectAsStateWithLifecycle()
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val pendingInvitations by viewModel.pendingInvitations.collectAsStateWithLifecycle()

    val isRefreshing = uiState is HouseListUiState.Loading
    val pullToRefreshState = rememberPullToRefreshState()

    val unreadCount = when (val state = notificationUiState) {
        is NotificationUiState.Success -> state.unreadCount
        else -> 0
    }

    val profile = (profileUiState as? ProfileUiState.Success)?.profile
    
    // Greeting Logic - Memoized
    val greeting = remember {
        val hour = LocalTime.now().hour
        when (hour) {
            in 0..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            in 17..20 -> "Good evening,"
            else -> "Good night,"
        }
    }

    val userName = remember(profile?.fullName) {
        profile?.fullName?.split(" ")?.firstOrNull() ?: "there"
    }
    
    var showJoinDialog by rememberSaveable { mutableStateOf(false) }
    var manualInviteCode by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            HomeTopBar(
                profile = profile,
                unreadCount = unreadCount,
                greeting = greeting,
                onSettingsClick = onSettingsClick,
                onNotificationsClick = onNotificationsClick,
                onCreateHouseClick = onCreateHouseClick,
                onJoinHouseClick = { showJoinDialog = true }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is HouseListUiState.Loading -> {
                `in`.xroden.flockr.ui.components.loading.HomeScreenSkeleton(
                    modifier = Modifier.padding(padding)
                )
            }

            is HouseListUiState.Success -> {
                HomeSuccessContent(
                    houses = state.houses,
                    pendingInvitations = pendingInvitations,
                    isRefreshing = isRefreshing,
                    pullToRefreshState = pullToRefreshState,
                    onRefresh = { viewModel.refresh() },
                    onHouseClick = onHouseClick,
                    onCreateHouseClick = onCreateHouseClick,
                    onAcceptInvitation = { viewModel.acceptInvitation(it) },
                    onRejectInvitation = { viewModel.rejectInvitation(it) },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }

            is HouseListUiState.Error -> {
                HomeErrorState(
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
        }
    }

    if (showJoinDialog) {
        EnterInviteCodeDialog(
            onDismiss = { showJoinDialog = false },
            onJoinHouse = { inviteCode ->
                showJoinDialog = false
                manualInviteCode = inviteCode
            }
        )
    }
    
    if (manualInviteCode != null) {
        // Navigate to full-screen preview
        LaunchedEffect(manualInviteCode) {
            onNavigateToJoinPreview(manualInviteCode!!)
            manualInviteCode = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSuccessContent(
    houses: List<HouseCardData>,
    pendingInvitations: List<`in`.xroden.flockr.features.house.model.InvitationWithHouse>,
    isRefreshing: Boolean,
    pullToRefreshState: PullToRefreshState,
    onRefresh: () -> Unit,
    onHouseClick: (String) -> Unit,
    onCreateHouseClick: () -> Unit,
    onAcceptInvitation: (String) -> Unit,
    onRejectInvitation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(key = "header") {
                Text(
                    text = "Your Households",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (houses.isEmpty()) {
                item {
                    EmptyHouseState(onCreateHouseClick)
                }
            } else {
                if (pendingInvitations.isNotEmpty()) {
                    pendingInvitationsSection(
                        pendingInvitations = pendingInvitations,
                        onAcceptInvitation = onAcceptInvitation,
                        onRejectInvitation = onRejectInvitation
                    )
                }

                housesSection(
                    houses = houses,
                    onHouseClick = onHouseClick
                )
            }

            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

private fun LazyListScope.pendingInvitationsSection(
    pendingInvitations: List<`in`.xroden.flockr.features.house.model.InvitationWithHouse>,
    onAcceptInvitation: (String) -> Unit,
    onRejectInvitation: (String) -> Unit
) {
    item {
        Text(
            text = "Invitations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    items(items = pendingInvitations, key = { it.id }) { invite ->
        InvitationCard(
            invitation = invite,
            onAccept = { onAcceptInvitation(invite.id) },
            onDecline = { onRejectInvitation(invite.id) },
            modifier = Modifier.animateItem()
        )
    }
    item { Spacer(modifier = Modifier.height(16.dp)) }
}

private fun LazyListScope.housesSection(
    houses: List<HouseCardData>,
    onHouseClick: (String) -> Unit
) {
    items(items = houses, key = { it.house.id }) { houseData ->
        HouseCard(
            houseData = houseData,
            onClick = { onHouseClick(houseData.house.id) },
            modifier = Modifier.animateItem()
        )
    }
}

@Composable
private fun HomeErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Could not load households",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun HomeTopBar(
    profile: `in`.xroden.flockr.features.auth.model.Profile?,
    unreadCount: Int,
    greeting: String,
    onSettingsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onCreateHouseClick: () -> Unit,
    onJoinHouseClick: () -> Unit
) {
    val userName = remember(profile?.fullName) {
        profile?.fullName?.split(" ")?.firstOrNull() ?: "there"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                onClick = onSettingsClick,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!profile?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(profile.avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = userName.firstOrNull()?.toString() ?: "U",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var showAddMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { showAddMenu = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            "Add House",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false },
                        offset = androidx.compose.ui.unit.DpOffset(x = 0.dp, y = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        DropdownMenuItem(
                            text = { Text("Join Household", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Home, null) },
                            onClick = {
                                showAddMenu = false
                                onJoinHouseClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Create Household", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Add, null) },
                            onClick = {
                                showAddMenu = false
                                onCreateHouseClick()
                            }
                        )
                    }
                }

                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
                                ) {
                                    Text(unreadCount.toString(), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            "Notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Text(
            text = userName,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun EnterInviteCodeDialog(
    onDismiss: () -> Unit,
    onJoinHouse: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Have an Invite Code?", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Enter the code shared with you to preview and join the household.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Invite Code") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoinHouse(code) },
                enabled = code.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Next")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
fun EmptyHouseState(onCreateHouseClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCreateHouseClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Home,
                        null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                "No households yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Create or join a house to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HouseCard(
    houseData: HouseCardData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberHapticFeedback()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable {
                haptics.performClick()
                onClick()
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!houseData.house.headerImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = remember(houseData.house.headerImageUrl) {
                        ImageRequest.Builder(context)
                            .data(houseData.house.headerImageUrl)
                            .crossfade(150)
                            .memoryCacheKey(houseData.house.id)
                            .diskCacheKey(houseData.house.id)
                            .build()
                    },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(160.dp)
                        .offset(x = 40.dp, y = 40.dp)
                        .alpha(0.15f),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Stats
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    GlassPill {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp))
                        Text(
                            "${houseData.memberCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    GlassPill {
                        Text(
                            "${houseData.currencySymbol}${houseData.monthlyExpense.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Title
                Column {
                    Text(
                        text = houseData.house.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    if (!houseData.house.address.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = houseData.house.address!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassPill(content: @Composable RowScope.() -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = content
        )
    }
}

@Composable
fun InvitationCard(
    invitation: `in`.xroden.flockr.features.house.model.InvitationWithHouse,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberHapticFeedback()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "You have been invited to join",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                invitation.houseName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        haptics.performSuccess()
                        onAccept()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Accept")
                }
                OutlinedButton(
                    onClick = {
                        haptics.performClick()
                        onDecline()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("Decline")
                }
            }
        }
    }
}

package `in`.xroden.flockr.ui.screens.house

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.House
import `in`.xroden.flockr.data.model.HouseConfig
import `in`.xroden.flockr.data.repository.HouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ViewModel for house details
@HiltViewModel
class HouseDetailsViewModel @Inject constructor(
    private val houseRepository: HouseRepository
) : ViewModel() {
    
    private val _house = MutableStateFlow<House?>(null)
    val house: StateFlow<House?> = _house.asStateFlow()
    
    private val _houseConfig = MutableStateFlow<HouseConfig?>(null)
    val houseConfig: StateFlow<HouseConfig?> = _houseConfig.asStateFlow()
    
    private val _currentUserRole = MutableStateFlow<String?>(null)
    val currentUserRole: StateFlow<String?> = _currentUserRole.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadHouse(houseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _house.value = houseRepository.getHouseById(houseId)
            _houseConfig.value = houseRepository.getHouseConfig(houseId)
            
            // Get current user's role in this house
            val members = houseRepository.getHouseMembers(houseId)
            val currentUserId = houseRepository.getCurrentUserId()
            _currentUserRole.value = members.find { it.userId == currentUserId }?.role
            
            _isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseDetailsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToShopping: () -> Unit,
    onNavigateToChores: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToManageMembers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HouseDetailsViewModel = hiltViewModel()
) {
    val house by viewModel.house.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    LaunchedEffect(houseId) {
        viewModel.loadHouse(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        house?.name ?: "Household",
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
                actions = {
                    // Only show settings for Owner/Admin
                    if (currentUserRole == "Owner" || currentUserRole == "Admin") {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Default.Settings,
                                "Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
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
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // House Info Header
                item {
                    house?.let { houseData ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // House Name
                                Text(
                                    text = houseData.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                // Address if exists
                                if (houseData.address?.isNotEmpty() == true) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = houseData.address,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                // Quick Actions Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Members button
                                    OutlinedButton(
                                        onClick = onNavigateToManageMembers,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.People,
                                            null,
                                            Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Members", fontSize = 14.sp)
                                    }

                                    // Invite code button
                                    houseData.inviteCode?.let { code ->
                                        val context = androidx.compose.ui.platform.LocalContext.current
                                        OutlinedButton(
                                            onClick = {
                                                val shareIntent = android.content.Intent().apply {
                                                    action = android.content.Intent.ACTION_SEND
                                                    putExtra(
                                                        android.content.Intent.EXTRA_TEXT,
                                                        "Join \"${houseData.name}\" on Flockr!\n\nInvite code: $code"
                                                    )
                                                    type = "text/plain"
                                                }
                                                context.startActivity(android.content.Intent.createChooser(shareIntent, null))
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                null,
                                                Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text("Invite", fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Modules Section
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        ModuleCard(
                            title = "Expenses",
                            subtitle = "Split bills and track spending",
                            icon = Icons.Default.AccountBalance,
                            onClick = onNavigateToExpenses
                        )

                        ModuleCard(
                            title = "Shopping",
                            subtitle = "Shared shopping lists",
                            icon = Icons.Default.ShoppingCart,
                            onClick = onNavigateToShopping
                        )

                        ModuleCard(
                            title = "Chores",
                            subtitle = "Manage household tasks",
                            icon = Icons.Default.CheckCircle,
                            onClick = onNavigateToChores
                        )

                        ModuleCard(
                            title = "Chat",
                            subtitle = "Group conversations",
                            icon = Icons.Default.Message,
                            onClick = onNavigateToChat
                        )

                        ModuleCard(
                            title = "Documents",
                            subtitle = "Store and share files",
                            icon = Icons.Default.Description,
                            onClick = onNavigateToDocuments
                        )
                    }
                }

                // Bottom spacer
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun HouseHeaderCard(
    house: House,
    currentUserRole: String?,
    onMembersClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showCopiedSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(showCopiedSnackbar) {
        if (showCopiedSnackbar) {
            snackbarHostState.showSnackbar(
                message = "Invite code copied!",
                duration = SnackbarDuration.Short
            )
            showCopiedSnackbar = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // House name and address
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = house.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (house.address?.isNotEmpty() == true) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = house.address ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Divider
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Invite code section
            house.inviteCode?.let { code ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Invite Code",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Invite code display
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(code))
                                showCopiedSnackbar = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = code,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                letterSpacing = 2.sp
                            )
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy code",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Share button
                    OutlinedButton(
                        onClick = {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "Join my household \"${house.name}\" on Flockr!\n\nUse invite code: $code"
                                )
                                type = "text/plain"
                            }
                            val chooser = android.content.Intent.createChooser(shareIntent, "Share invite code")
                            context.startActivity(chooser)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Invite Code", fontWeight = FontWeight.SemiBold)
                    }
                }

                // Divider before members button
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Members button
            FilledTonalButton(
                onClick = onMembersClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Members", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}

@Composable
private fun ModuleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon in a container
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


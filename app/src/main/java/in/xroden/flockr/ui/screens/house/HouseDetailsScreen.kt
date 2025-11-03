package `in`.xroden.flockr.ui.screens.house

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
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
    val houseConfig by viewModel.houseConfig.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    LaunchedEffect(houseId) {
        viewModel.loadHouse(houseId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Map Header (Top 25%)
                house?.let { houseData ->
                    MapHeader(
                        house = houseData,
                        currentUserRole = currentUserRole,
                        onBackClick = onNavigateBack,
                        onMembersClick = onNavigateToManageMembers,
                        onSettingsClick = onNavigateToSettings
                    )
                }

                // Modules Grid Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = "Household Modules",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Grid layout for modules (2 columns)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ModuleCard(
                                title = "Expenses",
                                subtitle = "Split & Track",
                                icon = Icons.Default.Star,
                                onClick = onNavigateToExpenses,
                                modifier = Modifier.weight(1f)
                            )
                            ModuleCard(
                                title = "Shopping",
                                subtitle = "Shared Lists",
                                icon = Icons.Default.ShoppingCart,
                                onClick = onNavigateToShopping,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ModuleCard(
                                title = "Chores",
                                subtitle = "Task Manager",
                                icon = Icons.Default.CheckCircle,
                                onClick = onNavigateToChores,
                                modifier = Modifier.weight(1f)
                            )
                            ModuleCard(
                                title = "Chat",
                                subtitle = "Group Chat",
                                icon = Icons.Default.Email,
                                onClick = onNavigateToChat,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 3
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ModuleCard(
                                title = "Documents",
                                subtitle = "File Storage",
                                icon = Icons.Default.Info,
                                onClick = onNavigateToDocuments,
                                modifier = Modifier.weight(1f)
                            )
                            // Placeholder for future module
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapHeader(
    house: House,
    currentUserRole: String?,
    onBackClick: () -> Unit,
    onMembersClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Map or placeholder
        if (house.latitude != null && house.longitude != null) {
            val location = LatLng(house.latitude!!, house.longitude!!)
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(location, 15f)
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                    myLocationButtonEnabled = false
                )
            ) {
                val markerState = rememberMarkerState(position = location)
                Marker(
                    state = markerState,
                    title = house.name,
                    snippet = house.address
                )
            }
        } else {
            // Placeholder with gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        // Gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // Top bar with back, settings, and members buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilledIconButton(
                onClick = onBackClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Settings button - Only visible to Owners and Admins
                if (currentUserRole == "Owner" || currentUserRole == "Admin") {
                    FilledIconButton(
                        onClick = onSettingsClick,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }

                FilledIconButton(
                    onClick = onMembersClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Person, "Members")
                }
            }
        }

        // House info at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                text = house.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (house.address?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = house.address ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "module_card_scale"
    )
    
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon without gradient background
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )

            // Text content
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}


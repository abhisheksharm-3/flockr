package `in`.xroden.flockr.features.house.ui.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import `in`.xroden.flockr.R
import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.ui.theme.*
import javax.inject.Inject

@HiltViewModel
class HouseDetailsViewModel @Inject constructor(
    private val houseRepository: IHouseRepository
) : ViewModel() {
    
    private val _house = MutableStateFlow<House?>(null)
    val house: StateFlow<House?> = _house.asStateFlow()
    
    private val _houseConfig = MutableStateFlow<HouseConfig?>(null)
    val houseConfig: StateFlow<HouseConfig?> = _houseConfig.asStateFlow()
    
    private val _currentUserRole = MutableStateFlow<String?>(null)
    val currentUserRole: StateFlow<String?> = _currentUserRole.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadHouseDetails(houseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Run safely, although repo returns Result
            runCatching {
                _house.value = houseRepository.getHouseById(houseId).getOrNull()
                _houseConfig.value = houseRepository.getHouseConfig(houseId).getOrNull()

                val members = houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }
                val currentUserId = houseRepository.getCurrentUserId()
                _currentUserRole.value = members.find { it.userId == currentUserId }?.role?.name
            }.onFailure {
            }
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
    onNavigateToHouseSettings: () -> Unit,
    viewModel: HouseDetailsViewModel = hiltViewModel()
) {
    val house by viewModel.house.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    LaunchedEffect(houseId) {
        viewModel.loadHouseDetails(houseId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(house?.name ?: "Household", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    if (currentUserRole == HouseMemberRole.OWNER.name || currentUserRole == HouseMemberRole.ADMIN.name) {
                        IconButton(onClick = onNavigateToHouseSettings) {
                            Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        if (isLoading) {
            `in`.xroden.flockr.ui.components.loading.DetailScreenSkeleton(
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item { HouseInfoCard(house = house, currentUserRole = currentUserRole) }
                
                item {
                    house?.let { houseData ->
                        QuickActionsCard(house = houseData, onNavigateToManageMembers = onNavigateToManageMembers)
                    }
                }

                item {
                    Text("Manage Household", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 8.dp))
                }

                item { FeatureCard("Expenses", "Track spending and split bills", Icons.Default.AccountBalance, CategoryBlue, onNavigateToExpenses) }
                item { FeatureCard("Shopping List", "Shared grocery lists", Icons.Default.ShoppingCart, CategoryGreen, onNavigateToShopping) }
                item { FeatureCard("Chores", "Assign and track tasks", Icons.Default.CheckCircle, CategoryPurple, onNavigateToChores) }
                item { FeatureCard("Chat", "Group conversations", Icons.Default.Email, MaterialTheme.colorScheme.tertiary, onNavigateToChat) }
                item { FeatureCard("Documents", "Store shared files", Icons.Default.Description, CategoryOrange, onNavigateToDocuments) }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun HouseInfoCard(house: House?, currentUserRole: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(Modifier.fillMaxWidth().height(220.dp)) {
            if (house != null) {
                if (house.headerImageUrl != null) {
                    AsyncImage(model = house.headerImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Image(painter = painterResource(id = R.drawable.house), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }

            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.8f)), startY = 0f)))

            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.Start) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    currentUserRole?.let { role ->
                        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f), shape = CircleShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))) {
                            Text(role.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), letterSpacing = 1.sp)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(house?.name ?: "Household", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    house?.address?.takeIf { it.isNotEmpty() }?.let { address ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.8f))
                            Text(address, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsCard(house: House, onNavigateToManageMembers: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("MEMBERS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                TextButton(onClick = onNavigateToManageMembers, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(32.dp), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimary)) {
                    Text("View & Invite", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp))
                }
            }

            house.inviteCode?.let { code ->
                val context = LocalContext.current
                IconButton(
                    onClick = {
                        val shareIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "Hey! Join my household \"${house.name}\" on Flockr so we can manage expenses, chores, and shopping together.\n\nHere is the invite code: $code")
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Invite"))
                    },
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Share, "Share Invite", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String, icon: ImageVector, accentColor: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(MaterialTheme.shapes.medium).background(accentColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(24.dp))
        }
    }
}

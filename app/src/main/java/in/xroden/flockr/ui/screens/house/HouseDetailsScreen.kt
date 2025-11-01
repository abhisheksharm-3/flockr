package `in`.xroden.flockr.ui.screens.house

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.House
import `in`.xroden.flockr.data.repository.HouseRepository
import `in`.xroden.flockr.ui.components.FlockrCard
import `in`.xroden.flockr.ui.components.FlockrSectionHeader
import javax.inject.Inject

// ViewModel for house details
@HiltViewModel
class HouseDetailsViewModel @Inject constructor(
    private val houseRepository: HouseRepository
) : ViewModel() {
    suspend fun getHouseById(houseId: String): House? {
        return houseRepository.getHouseById(houseId)
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
    viewModel: HouseDetailsViewModel = hiltViewModel()
) {
    var house by remember { mutableStateOf<House?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(houseId) {
        isLoading = true
        house = viewModel.getHouseById(houseId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        house?.name ?: "House Details",
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
                    IconButton(onClick = onNavigateToManageMembers) {
                        Icon(
                            Icons.Default.Person,
                            "Manage Members",
                            tint = MaterialTheme.colorScheme.primary
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
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // House Info Card
                if (house != null) {
                    FlockrCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = house?.name ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (house?.address?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = house?.address ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Feature Cards Section
                FlockrSectionHeader(
                    text = "Features",
                    modifier = Modifier.padding(top = 8.dp)
                )

                FeatureCard(
                    title = "Expenses",
                    description = "Track and split household expenses",
                    icon = Icons.Default.Star,
                    onClick = onNavigateToExpenses
                )

                FeatureCard(
                    title = "Shopping List",
                    description = "Shared shopping lists for the house",
                    icon = Icons.Default.ShoppingCart,
                    onClick = onNavigateToShopping
                )

                FeatureCard(
                    title = "Chores",
                    description = "Organize household tasks",
                    icon = Icons.Default.CheckCircle,
                    onClick = onNavigateToChores
                )

                FeatureCard(
                    title = "Chat",
                    description = "House group chat",
                    icon = Icons.Default.Email,
                    onClick = onNavigateToChat
                )

                FeatureCard(
                    title = "Documents",
                    description = "Shared files and documents",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToDocuments
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FlockrCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


package `in`.xroden.flockr.ui.screens.house

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.model.House
import `in`.xroden.flockr.data.repository.HouseRepository
import kotlinx.coroutines.launch

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
    viewModel: `in`.xroden.flockr.ui.viewmodel.HouseManagementViewModel = hiltViewModel()
) {
    var house by remember { mutableStateOf<House?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(houseId) {
        scope.launch {
            viewModel.loadHouse(houseId)
        }
    }

    LaunchedEffect(viewModel.currentHouse.value) {
        house = viewModel.currentHouse.value
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Household Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map section (top 20%)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    // TODO: Add Google Maps Composable here when ready
                    // For now, placeholder
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Map View",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    ),
                                    startY = 0f,
                                    endY = 500f
                                )
                            )
                    )

                    // House name and address overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        house?.let {
                            Text(
                                text = it.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )
                            it.address?.let { addr ->
                                Text(
                                    text = addr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Navigation items
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NavigationCard(
                        title = "Expenses",
                        description = "Track bills, IOUs, and monthly reports",
                        onClick = onNavigateToExpenses
                    )

                    NavigationCard(
                        title = "Chores",
                        description = "Manage household tasks and to-dos",
                        onClick = onNavigateToChores
                    )

                    NavigationCard(
                        title = "Shopping List",
                        description = "Shared grocery and shopping items",
                        onClick = onNavigateToShopping
                    )

                    NavigationCard(
                        title = "Chat",
                        description = "Group messaging for the household",
                        onClick = onNavigateToChat
                    )

                    NavigationCard(
                        title = "Documents",
                        description = "Store and share important files",
                        onClick = onNavigateToDocuments
                    )
                }
            }
        }
    }
}

@Composable
fun NavigationCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

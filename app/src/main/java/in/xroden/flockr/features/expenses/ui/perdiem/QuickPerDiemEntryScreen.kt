package `in`.xroden.flockr.features.expenses.ui.perdiem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig
import `in`.xroden.flockr.features.expenses.domain.PerDiemViewModel
import `in`.xroden.flockr.utils.getCurrencySymbol

/**
 * Quick Per Diem Entry Screen - Select from configured items to add entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPerDiemEntryScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddEntry: (String) -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToTransactions: () -> Unit = {},
    viewModel: PerDiemViewModel = hiltViewModel()
) {
    val configsState by viewModel.configState.collectAsState()
    val configs = when (val state = configsState) {
        is `in`.xroden.flockr.features.expenses.domain.PerDiemConfigUiState.Success -> state.configs
        else -> emptyList()
    }
    val isLoading = configsState is `in`.xroden.flockr.features.expenses.domain.PerDiemConfigUiState.Loading
    val houseConfig by viewModel.houseConfig.collectAsState()
    val currencySymbol = getCurrencySymbol(houseConfig?.currencyCode ?: "$")

    LaunchedEffect(houseId) {
        viewModel.loadConfigs(houseId)
        viewModel.loadHouseConfig(houseId)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Add Per Diem Entry",
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
                CircularProgressIndicator()
            }
        } else if (configs.isEmpty()) {
            EmptyPerDiemConfigState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onSetupConfig = onNavigateToConfig
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Select Item",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Choose which per-diem item you want to log usage for",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onNavigateToTransactions,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View All Transactions",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                items(configs, key = { it.id }) { config ->
                    PerDiemQuickSelectCard(
                        config = config,
                        currencySymbol = currencySymbol,
                        onClick = { onNavigateToAddEntry(config.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PerDiemQuickSelectCard(
    config: PerDiemConfig,
    currencySymbol: String = "$",
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Icon based on category
                Icon(
                    imageVector = when (config.category.lowercase()) {
                        "beverage" -> Icons.Default.LocalCafe
                        "food" -> Icons.Default.Fastfood
                        "grocery" -> Icons.Default.ShoppingBasket
                        else -> Icons.Default.Receipt
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )

                Column {
                    Text(
                        text = config.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$currencySymbol${"%.2f".format(config.rate)} per ${config.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Add entry",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun EmptyPerDiemConfigState(
    modifier: Modifier = Modifier,
    onSetupConfig: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Per-Diem Items",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Set up per-diem items first to track daily usage like milk, bread, etc.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSetupConfig,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Set Up Items")
        }
    }
}



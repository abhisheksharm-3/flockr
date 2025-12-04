package `in`.xroden.flockr.features.expenses.ui.onetime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.expenses.domain.ExpenseViewModel
import `in`.xroden.flockr.features.expenses.model.OneTimeExpense
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.theme.CategoryBlue
import `in`.xroden.flockr.ui.theme.CategoryGreen
import `in`.xroden.flockr.ui.theme.CategoryOrange
import `in`.xroden.flockr.ui.theme.CategoryPurple
import `in`.xroden.flockr.ui.theme.CategoryRed
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    houseId: String,
    expenseId: String,
    onNavigateBack: () -> Unit,
    onEditExpense: (String) -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val expense by viewModel.selectedExpense.collectAsState()
    val houseConfig by viewModel.houseConfig.collectAsState()
    val currencySymbol = houseConfig?.getCurrencySymbol() ?: "$"

    LaunchedEffect(expenseId) {
        viewModel.loadOneTimeExpense(expenseId)
        viewModel.loadHouseConfig(houseId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Expense Details") },
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
                    IconButton(onClick = { onEditExpense(expenseId) }) {
                        Icon(
                            Icons.Default.Edit,
                            "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (expense == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val currentExpense = expense!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(CategoryBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Receipt,
                                null,
                                modifier = Modifier.size(40.dp),
                                tint = CategoryBlue
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentExpense.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$currencySymbol${"%.2f".format(currentExpense.amount)}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = currentExpense.category.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Details Section
                SectionCard(title = "Details") {
                    DetailRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Date",
                        value = currentExpense.date.toString()
                    )
                    
                    // Add Paid By if we can resolve user name (might need more data)
                    // For now, skipping or showing ID if needed, but better to fetch name
                    
                    if (!currentExpense.notes.isNullOrBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        DetailRow(
                            icon = Icons.Default.Description,
                            label = "Notes",
                            value = currentExpense.notes
                        )
                    }
                }

                // Splits Section (Placeholder - needs split data in OneTimeExpense model)
                // If OneTimeExpense doesn't have splits loaded, we might need to fetch them separately
                // or update the model. For now, we'll skip unless we update the model.
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

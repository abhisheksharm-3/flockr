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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.expenses.domain.ExpenseViewModel
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.theme.CategoryBlue
import `in`.xroden.flockr.ui.theme.CategoryGreen
import `in`.xroden.flockr.ui.theme.CategoryRed

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
    val currencySymbol = remember(houseConfig) {
        houseConfig?.getCurrencySymbol() ?: "$"
    }
    
    var houseMembers by remember { mutableStateOf<List<`in`.xroden.flockr.features.house.model.MemberWithProfile>>(emptyList()) }

    LaunchedEffect(houseId, expenseId) {
        viewModel.loadOneTimeExpense(expenseId)
        viewModel.loadHouseConfig(houseId)
        houseMembers = viewModel.getHouseMembers(houseId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Expense Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = { onEditExpense(expenseId) }) {
                        Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (expense == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val currentExpense = expense!!
            val payerName = remember(currentExpense.paidBy, houseMembers) {
                houseMembers.find { it.userId == currentExpense.paidBy }?.fullName ?: "Unknown"
            }
            
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(CategoryBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Receipt, null, Modifier.size(40.dp), tint = CategoryBlue)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(currentExpense.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "$currencySymbol${"%.2f".format(currentExpense.amount)}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
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

                SectionCard(title = "Details") {
                    DetailRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Date",
                        value = formatDate(currentExpense.date, houseConfig?.dateFormat ?: "yyyy-MM-dd")
                    )
                    
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    
                    DetailRow(
                        icon = Icons.Default.Person,
                        label = "Paid By",
                        value = payerName
                    )
                    
                    if (!currentExpense.notes.isNullOrBlank()) {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        DetailRow(
                            icon = Icons.Default.Description,
                            label = "Notes",
                            value = currentExpense.notes!!
                        )
                    }
                }

                if (!currentExpense.splits.isNullOrEmpty()) {
                    val totalSplitAmount = currentExpense.splits!!.sumOf { it.amountOwed }
                    val payerShare = currentExpense.amount - totalSplitAmount

                    SectionCard(title = "Splits") {
                        // Payer's Share
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(payerName.take(1).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Column {
                                    Text("$payerName (Payer)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text("$currencySymbol${"%.2f".format(payerShare)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }

                        if (currentExpense.splits!!.isNotEmpty()) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        }

                        currentExpense.splits!!.forEachIndexed { index, split ->
                            val memberName = remember(houseMembers, split.userId) {
                                houseMembers.find { it.userId == split.userId }?.fullName ?: "Unknown"
                            }
                            val isSettled = split.isSettled
                            
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(memberName.take(1).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                    Column {
                                        Text(memberName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(if (isSettled) "Settled" else "Owes", style = MaterialTheme.typography.labelSmall, color = if (isSettled) CategoryGreen else CategoryRed)
                                    }
                                }
                                Text("$currencySymbol${"%.2f".format(split.amountOwed)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            
                            if (index < currentExpense.splits!!.size - 1) {
                                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(date: kotlinx.datetime.LocalDate, pattern: String): String {
    return runCatching {
        val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
        val javaPattern = pattern.replace("YYYY", "yyyy").replace("DD", "dd")
        javaDate.format(java.time.format.DateTimeFormatter.ofPattern(javaPattern))
    }.getOrDefault(date.toString())
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

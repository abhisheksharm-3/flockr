package `in`.xroden.flockr.features.expenses.ui.onetime

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.expenses.presentation.OneTimeExpenseViewModel
import `in`.xroden.flockr.features.expenses.presentation.OneTimeExpenseUiState
import `in`.xroden.flockr.features.expenses.model.OneTimeExpense
import `in`.xroden.flockr.ui.components.inputs.MonthSelector
import `in`.xroden.flockr.ui.theme.*
import `in`.xroden.flockr.utils.getCurrencySymbol
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import java.util.Locale
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.getTodayInHouseTimezone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneTimeExpensesScreen(
    houseId: String,
    initialCategory: String? = null,
    initialUserId: String? = null,
    onNavigateBack: () -> Unit,
    onAddExpense: () -> Unit,
    onNavigateToExpenseDetail: (String) -> Unit,
    onNavigateToEditExpense: (String) -> Unit,
    viewModel: OneTimeExpenseViewModel = hiltViewModel()
) {
    val expenseState by viewModel.expenseState.collectAsState()
    val houseConfig by viewModel.houseConfig.collectAsState()
    
    val currencySymbol = remember(houseConfig) {
        getCurrencySymbol(houseConfig?.currencyCode ?: "$")
    }
    
    val isRefreshing = expenseState is OneTimeExpenseUiState.Loading
    val pullToRefreshState = rememberPullToRefreshState()

    // State for filtering
    var selectedMonth by remember { mutableStateOf<LocalDate?>(null) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedUserId by remember { mutableStateOf(initialUserId) }

    // Calculate current month start using house timezone
    val currentMonthStart = remember(houseConfig) {
        val now = houseConfig.getTodayInHouseTimezone()
        LocalDate(now.year, now.month, 1)
    }

    LaunchedEffect(houseId) {
        viewModel.loadExpenses(houseId)
        viewModel.loadHouseConfig(houseId)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Expenses", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            `in`.xroden.flockr.ui.components.buttons.FlockrExtendedFab(
                text = "Add Expense",
                icon = Icons.Default.Add,
                onClick = onAddExpense
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val state = expenseState) {
            is OneTimeExpenseUiState.Loading -> {
                `in`.xroden.flockr.ui.components.loading.ListScreenSkeleton(
                    modifier = Modifier.padding(padding)
                )
            }
            is OneTimeExpenseUiState.Success -> {
                // Optimize sorting and filtering with derivedStateOf/remember
                val filteredExpenses = remember(state.expenses, selectedMonth, selectedCategory, selectedUserId) {
                    val sorted = state.expenses.sortedWith(
                        compareByDescending<OneTimeExpense> { it.date }.thenByDescending { it.createdAt }
                    )
                    sorted.filter { expense ->
                        val matchMonth = selectedMonth?.let { filterDate ->
                            expense.date.year == filterDate.year && expense.date.month == filterDate.month
                        } ?: true
                        
                        val matchCategory = selectedCategory?.let { category ->
                            expense.category.equals(category, ignoreCase = true)
                        } ?: true

                        val matchUser = selectedUserId?.let { userId ->
                            expense.paidBy == userId
                        } ?: true

                        matchMonth && matchCategory && matchUser
                    }
                }

                if (state.expenses.isEmpty() && selectedMonth == null && selectedCategory == null && selectedUserId == null) {
                    EmptyExpensesState(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        onAddExpense = onAddExpense
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.loadExpenses(houseId) },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize().padding(padding)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item(key = "header") {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                                    MonthSelector(
                                        selectedMonth = selectedMonth ?: currentMonthStart,
                                        onMonthChange = { selectedMonth = it },
                                        showClearButton = selectedMonth != null,
                                        onClearFilter = { selectedMonth = null },
                                        subtitle = "${filteredExpenses.size} expenses",
                                        timezone = houseConfig?.timezone
                                    )

                                    // Category filter chips
                                    val categories = remember(state.expenses) {
                                        state.expenses.map { it.category }.distinct().sorted()
                                    }
                                    if (categories.isNotEmpty()) {
                                        androidx.compose.foundation.lazy.LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            item {
                                                FilterChip(
                                                    selected = selectedCategory == null,
                                                    onClick = { selectedCategory = null },
                                                    label = { Text("All") }
                                                )
                                            }
                                            items(count = categories.size) { index ->
                                                val cat = categories[index]
                                                FilterChip(
                                                    selected = selectedCategory == cat,
                                                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                                    label = { Text(cat) }
                                                )
                                            }
                                        }
                                    }

                                    if (selectedUserId != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilterChip(
                                                selected = true,
                                                onClick = { selectedUserId = null },
                                                label = { Text("User Filter Active") },
                                                trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                                            )
                                        }
                                    }
                                }
                            }
                        
                            if (filteredExpenses.isEmpty()) {
                                item(key = "empty_filter") {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No expenses match filter", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            } else {
                                items(filteredExpenses, key = { it.id }) { expense ->
                                    ModernExpenseCard(
                                        expense = expense,
                                        houseId = houseId,
                                        currencySymbol = currencySymbol,
                                        houseConfig = houseConfig,
                                        onClick = { onNavigateToExpenseDetail(expense.id) },
                                        onEdit = { onNavigateToEditExpense(expense.id) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }

                            item(key = "spacer") {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
            is OneTimeExpenseUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.Error, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Text("Error loading expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        // Fallback message, avoiding raw error dump if possible, or keeping it but styled
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { viewModel.loadExpenses(houseId) }) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernExpenseCard(
    expense: OneTimeExpense,
    houseId: String,
    currencySymbol: String = "$",
    houseConfig: `in`.xroden.flockr.features.house.model.HouseConfig? = null,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OneTimeExpenseViewModel = hiltViewModel()
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Expense?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${expense.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteOneTimeExpense(houseId, expense.id)
                        scope.launch { snackbarHostState.showSnackbar("Expense deleted") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, shape = MaterialTheme.shapes.medium) { Text("Cancel") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium
        )
    }

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    // Use house config date formatting
                    val formattedDate = remember(expense.date, houseConfig) {
                        expense.date.formatWithHouseConfig(houseConfig)
                    }
                    Text(formattedDate, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {
                        Text(
                            text = "$currencySymbol${"%.2f".format(expense.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp, shadowElevation = 4.dp) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { showMenu = false; onEdit() },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = { showMenu = false; showDeleteDialog = true },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = getCategoryColor(expense.category).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, getCategoryColor(expense.category).copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(getCategoryIcon(expense.category), null, Modifier.size(12.dp), tint = getCategoryColor(expense.category))
                        Text(expense.category.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = getCategoryColor(expense.category))
                    }
                }

                Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Paid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            expense.notes?.let { notes ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Text(notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EmptyExpensesState(modifier: Modifier = Modifier, onAddExpense: () -> Unit) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ShoppingCart, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(24.dp))
        Text("No Expenses Yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Start tracking your household expenses by adding your first one", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onAddExpense, shape = MaterialTheme.shapes.medium, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Icon(Icons.Default.Add, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add First Expense", fontWeight = FontWeight.SemiBold)
        }
    }
}

// Date formatting is now handled by DateFormatUtils.formatWithHouseConfig
// See utils/DateFormatUtils.kt

private fun getCategoryColor(category: String): androidx.compose.ui.graphics.Color {
    return when (category.lowercase()) {
        "groceries", "food" -> CategoryGreen
        "utilities", "services" -> CategoryBlue
        "entertainment" -> CategoryPurple
        "transport" -> CategoryYellow
        "shopping" -> CategoryPink
        "rent", "housing" -> CategoryOrange
        "healthcare" -> CategoryTeal
        "education" -> CategoryIndigo
        else -> CategoryBlue
    }
}

private fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "groceries", "food" -> Icons.Default.ShoppingCart
        "utilities", "services" -> Icons.Default.Build
        "entertainment" -> Icons.Default.Movie
        "transport" -> Icons.Default.DirectionsCar
        "shopping" -> Icons.Default.ShoppingBag
        "rent", "housing" -> Icons.Default.Home
        "healthcare" -> Icons.Default.LocalHospital
        "education" -> Icons.Default.School
        else -> Icons.Default.Receipt
    }
}

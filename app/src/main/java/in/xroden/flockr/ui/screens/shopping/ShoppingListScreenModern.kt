package `in`.xroden.flockr.ui.screens.shopping

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import `in`.xroden.flockr.data.model.ShoppingItem
import `in`.xroden.flockr.ui.theme.PositiveGreen
import `in`.xroden.flockr.ui.viewmodel.ShoppingUiState
import `in`.xroden.flockr.ui.viewmodel.ShoppingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreenModern(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddExpense: () -> Unit = {},
    viewModel: ShoppingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showConvertDialog by remember { mutableStateOf<ShoppingItem?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(houseId) {
        viewModel.loadShoppingItems(houseId)
    }

    // Add Item Dialog
    if (showAddDialog) {
        AddShoppingItemDialog(
            houseId = houseId,
            onDismiss = { showAddDialog = false },
            onAdd = { itemName, quantity ->
                scope.launch {
                    viewModel.addItem(houseId, itemName, quantity)
                    showAddDialog = false
                    snackbarHostState.showSnackbar("Item added to list")
                }
            }
        )
    }

    // Convert to Expense Dialog
    showConvertDialog?.let { item ->
        ConvertToExpenseDialog(
            item = item,
            onDismiss = { showConvertDialog = null },
            onConvert = {
                showConvertDialog = null
                onNavigateToAddExpense()
            },
            onSkip = { showConvertDialog = null }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Shopping List",
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("Add Item") },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is ShoppingUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ShoppingUiState.Success -> {
                if (state.items.isEmpty()) {
                    EmptyShoppingState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        onAddItem = { showAddDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Shopping Items",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${state.items.size} items to buy",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Shopping Items
                        items(state.items, key = { it.id }) { item ->
                            ShoppingItemCard(
                                item = item,
                                onChecked = {
                                    scope.launch {
                                        viewModel.markAsPurchased(item.id, houseId, item.itemName)
                                        showConvertDialog = item
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        viewModel.deleteItem(item.id)
                                        snackbarHostState.showSnackbar("Item removed")
                                    }
                                }
                            )
                        }

                        // Bottom Spacer
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
            is ShoppingUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Error loading shopping list",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { viewModel.loadShoppingItems(houseId) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingItemCard(
    item: ShoppingItem,
    onChecked: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checkbox
            Checkbox(
                checked = false,
                onCheckedChange = { onChecked() }
            )

            // Item Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                item.quantity?.let { qty ->
                    Text(
                        text = "Quantity: $qty",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Delete Button
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, "Delete")
            }
        }
    }
}

@Composable
fun AddShoppingItemDialog(
    houseId: String,
    onDismiss: () -> Unit,
    onAdd: (String, String?) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Item",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name *") },
                    placeholder = { Text("e.g., Milk, Bread") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (Optional)") },
                    placeholder = { Text("e.g., 2L, 1 loaf") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onAdd(itemName, quantity.takeIf { it.isNotBlank() })
                        },
                        modifier = Modifier.weight(1f),
                        enabled = itemName.isNotBlank(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun ConvertToExpenseDialog(
    item: ShoppingItem,
    onDismiss: () -> Unit,
    onConvert: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = PositiveGreen
            )
        },
        title = {
            Text(
                text = "Item Purchased!",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Would you like to convert \"${item.itemName}\" into an expense entry?",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = onConvert,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Convert to Expense")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onSkip,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Skip")
            }
        }
    )
}

@Composable
fun EmptyShoppingState(
    modifier: Modifier = Modifier,
    onAddItem: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Shopping List Empty",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add items you need to buy for the household",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddItem,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add First Item")
        }
    }
}


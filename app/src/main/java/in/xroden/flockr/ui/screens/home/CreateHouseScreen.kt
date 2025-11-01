package `in`.xroden.flockr.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.ui.components.FlockrCard
import `in`.xroden.flockr.ui.components.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.FlockrTextField
import `in`.xroden.flockr.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHouseScreen(
    onHouseCreated: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    var houseName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show errors from view model
    LaunchedEffect(uiState) {
        if (uiState is `in`.xroden.flockr.ui.viewmodel.HomeUiState.Error) {
            val msg = (uiState as `in`.xroden.flockr.ui.viewmodel.HomeUiState.Error).message
            isCreating = false
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Create Household",
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create New Household",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Set up a new household to manage together",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            FlockrTextField(
                value = houseName,
                onValueChange = { houseName = it },
                label = "Household Name",
                placeholder = "e.g., Smith Family",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            FlockrTextField(
                value = address,
                onValueChange = { address = it },
                label = "Address (Optional)",
                placeholder = "123 Main St, City, State",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            FlockrPrimaryButton(
                text = "Create Household",
                onClick = {
                    isCreating = true
                    viewModel.createHouse(
                        name = houseName,
                        address = address.takeIf { it.isNotBlank() },
                        latitude = null,
                        longitude = null,
                        onSuccess = { houseId ->
                            isCreating = false
                            onHouseCreated(houseId)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = houseName.isNotBlank(),
                isLoading = isCreating
            )
        }
    }
}

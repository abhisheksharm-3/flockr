package `in`.xroden.flockr.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
            android.util.Log.d("CreateHouseScreen", "Showing error snackbar: $msg")
            // LaunchedEffect runs in a coroutine scope, so we can call suspend functions directly
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Household") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create a new household",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = houseName,
                onValueChange = { houseName = it },
                label = { Text("Household Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g., Smith Family") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("123 Main St, City, State") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    android.util.Log.d("CreateHouseScreen", "Create button clicked - name='$houseName', address='$address'")
                    isCreating = true
                    viewModel.createHouse(
                        name = houseName,
                        address = address.takeIf { it.isNotBlank() },
                        latitude = null,
                        longitude = null,
                        onSuccess = { houseId ->
                            android.util.Log.d("CreateHouseScreen", "House created successfully - id=$houseId")
                            isCreating = false
                            onHouseCreated(houseId)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = houseName.isNotBlank() && !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Create Household")
                }
            }
        }
    }
}

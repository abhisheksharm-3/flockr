package `in`.xroden.flockr.features.settings.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import `in`.xroden.flockr.features.settings.domain.ProfileViewModel
import `in`.xroden.flockr.features.settings.domain.ProfileUiState
import `in`.xroden.flockr.features.settings.domain.UpdateProfileUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profileUiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    val profile = (profileUiState as? ProfileUiState.Success)?.profile

    var fullName by remember { mutableStateOf(profile?.fullName ?: "") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                val inputStream = context.contentResolver.openInputStream(uri)
                val imageData = inputStream?.readBytes() ?: return@runCatching
                viewModel.uploadProfilePicture(imageData)
            }.onFailure {
                errorMessage = "Failed to load image: ${it.message}"
            }
        }
    }

    LaunchedEffect(profile) {
        profile?.let {
            fullName = it.fullName ?: ""
        }
    }

    // Handle update success to navigate back or show success
    LaunchedEffect(updateState) {
        if (updateState is UpdateProfileUiState.Success) {
            onNavigateBack()
        } else if (updateState is UpdateProfileUiState.Error) {
            errorMessage = (updateState as UpdateProfileUiState.Error).message
        }
    }
    
    val isLoading = updateState is UpdateProfileUiState.Loading

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUrl != null) {
                     // Note: Optimally this should come from ViewModel/Cloud
                    Image(
                        painter = rememberAsyncImagePainter(profileImageUrl),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (profile?.avatarUrl != null) {
                     Image(
                        painter = rememberAsyncImagePainter(profile.avatarUrl),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, "Default Avatar", Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(40.dp)
                        .clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, "Change Photo", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                leadingIcon = { Icon(Icons.Default.Person, null) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                isError = errorMessage != null
            )
            
            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp, start = 4.dp).align(Alignment.Start))
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = profile?.email ?: "",
                onValueChange = { },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = true,
                enabled = false,
                shape = MaterialTheme.shapes.medium,
                leadingIcon = { Icon(Icons.Default.Email, null) },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { viewModel.updateProfile(fullName) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = fullName.isNotBlank() && !isLoading,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Save Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun UserAvatar(
    profileImageUrl: String?,
    userName: String?,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    Surface(
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (profileImageUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(profileImageUrl),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = userName?.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

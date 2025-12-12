package `in`.xroden.flockr.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import `in`.xroden.flockr.features.settings.domain.ProfileViewModel
import `in`.xroden.flockr.features.settings.domain.ProfileUiState
import `in`.xroden.flockr.features.settings.domain.UpdateProfileUiState

// Dracula Palette
private val DraculaBackground = Color(0xFF282A36)
private val DraculaCurrentLine = Color(0xFF44475A)
private val DraculaForeground = Color(0xFFF8F8F2)
private val DraculaComment = Color(0xFF6272A4)
private val DraculaCyan = Color(0xFF8BE9FD)
private val DraculaGreen = Color(0xFF50FA7B)
private val DraculaOrange = Color(0xFFFFB86C)
private val DraculaPink = Color(0xFFFF79C6)
private val DraculaPurple = Color(0xFFBD93F9)
private val DraculaRed = Color(0xFFFF5555)
private val DraculaYellow = Color(0xFFF1FA8C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    
    // Extract profile from state if success
    val currentProfile = (uiState as? ProfileUiState.Success)?.profile
    
    // Update local state when profile loads
    LaunchedEffect(currentProfile) {
        if (currentProfile != null) {
            editedName = currentProfile.fullName ?: ""
        }
    }

    Scaffold(
        containerColor = DraculaBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Your Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DraculaForeground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = DraculaForeground
                        )
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(
                            onClick = {
                                viewModel.updateProfile(editedName)
                                isEditing = false
                            },
                            enabled = updateState !is UpdateProfileUiState.Loading && editedName.isNotBlank()
                        ) {
                            if (updateState is UpdateProfileUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = DraculaGreen
                                )
                            } else {
                                Icon(Icons.Default.Check, "Save", tint = DraculaGreen)
                            }
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, "Edit", tint = DraculaCyan)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DraculaBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(color = DraculaPurple, modifier = Modifier.align(Alignment.Center))
                }
                is ProfileUiState.Error -> {
                     Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(state.message, color = DraculaRed)
                        Button(onClick = { viewModel.loadProfile() }) { Text("Retry") }
                    }
                }
                is ProfileUiState.Success -> {
                    val profile = state.profile
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        // Avatar Section
                        Box(
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            // Avatar Placeholder/Image logic (No avatarUrl in Profile model yet?)
                            // Assuming NO avatarUrl based on Profile.kt check. Using generic Icon.
                            Surface(
                                modifier = Modifier
                                    .size(120.dp)
                                    .border(4.dp, DraculaPurple, CircleShape),
                                shape = CircleShape,
                                color = DraculaCurrentLine
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxSize(),
                                    tint = DraculaComment
                                )
                            }
                            
                            // Edit Icon
                            if (isEditing) {
                                Surface(
                                    shape = CircleShape,
                                    color = DraculaPink,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .offset(x = 4.dp, y = 4.dp)
                                        .border(3.dp, DraculaBackground, CircleShape),
                                    shadowElevation = 4.dp
                                ) {
                                    IconButton(onClick = { /* Check if upload supported in VM */ }) {
                                        Icon(Icons.Default.Edit, null, tint = DraculaBackground, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
        
                        // Info Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = DraculaCurrentLine
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // Name Field
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "Full Name",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DraculaComment
                                    )
                                    if (isEditing) {
                                        OutlinedTextField(
                                            value = editedName,
                                            onValueChange = { editedName = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = DraculaCyan,
                                                unfocusedBorderColor = DraculaComment,
                                                focusedTextColor = DraculaForeground,
                                                unfocusedTextColor = DraculaForeground,
                                                cursorColor = DraculaCyan
                                            ),
                                            shape = MaterialTheme.shapes.medium
                                        )
                                    } else {
                                        Text(
                                            text = profile.fullName ?: "Set your name",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DraculaForeground
                                        )
                                    }
                                }
        
                                HorizontalDivider(color = DraculaBackground)
        
                                // Email Field
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "Email Address",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DraculaComment
                                    )
                                    Text(
                                        text = profile.email,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = DraculaForeground.copy(alpha = 0.8f)
                                    )
                                }
        
                                HorizontalDivider(color = DraculaBackground)
                                
                                // ID Field (for debug/ref)
                                 Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "User ID",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DraculaComment
                                    )
                                    Text(
                                        text = profile.id,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DraculaComment,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

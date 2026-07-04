package `in`.xroden.flockr.features.auth.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.R
import `in`.xroden.flockr.features.auth.presentation.SignInUiState
import `in`.xroden.flockr.features.auth.presentation.SignUpUiState
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.features.auth.presentation.AuthViewModel
import `in`.xroden.flockr.utils.rememberHapticFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val signUpState by viewModel.signUpState.collectAsState()
    val signInState by viewModel.signInState.collectAsState()
    val isBusy = signUpState is SignUpUiState.Loading || signInState is SignInUiState.Loading
    val errorMessage = (signUpState as? SignUpUiState.Error)?.message
        ?: (signInState as? SignInUiState.Error)?.message

    // Get Activity context for Credential Manager
    val context = LocalContext.current
    val activity = context as? Activity
    val haptics = rememberHapticFeedback()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Create Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { haptics.performClick(); onNavigateToLogin() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.onSurface
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Section
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Join Flockr",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Create an account to start managing your households together",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Form Section
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "Full Name *",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlockrTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        placeholder = "John Doe",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person, "Name",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column {
                    Text(
                        text = "Email *",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlockrTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "your.email@example.com",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email, "Email",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column {
                    Text(
                        text = "Password *",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Create a strong password") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock, "Password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                }

                Column {
                    Text(
                        text = "Confirm Password *",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = { Text("Re-enter your password") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock, "Confirm",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = confirmPassword.isNotBlank() && password != confirmPassword,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )
                    if (confirmPassword.isNotBlank() && password != confirmPassword) {
                        Text(
                            text = "Passwords do not match",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            FlockrPrimaryButton(
                text = if (signUpState is SignUpUiState.Loading) "Creating Account..." else "Create Account",
                onClick = {
                    haptics.performClick()
                    if (password == confirmPassword) {
                        viewModel.signUp(email, password, fullName)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy &&
                        fullName.isNotBlank() &&
                        email.isNotBlank() &&
                        password.isNotBlank() &&
                        confirmPassword.isNotBlank() &&
                        password == confirmPassword,
                isLoading = signUpState is SignUpUiState.Loading
            )

            // Divider with text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "OR",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            // Google Sign In Button
            Button(
                onClick = { haptics.performClick(); activity?.let { viewModel.signInWithGoogle(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1F1F1F),
                    disabledContainerColor = Color.White.copy(alpha = 0.6f),
                    disabledContentColor = Color(0xFF1F1F1F).copy(alpha = 0.6f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp,
                    disabledElevation = 0.dp
                ),
                enabled = !isBusy && activity != null
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (signInState is SignInUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Sign in prompt
            OutlinedButton(
                onClick = { haptics.performClick(); onNavigateToLogin() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Sign In Instead", fontWeight = FontWeight.SemiBold)
            }

            // Footer text
            Text(
                text = "Already have an account? Sign in to access your households.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

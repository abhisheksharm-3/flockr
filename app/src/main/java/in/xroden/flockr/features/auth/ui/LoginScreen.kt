package `in`.xroden.flockr.features.auth.ui

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import `in`.xroden.flockr.features.auth.presentation.AuthUiState
import `in`.xroden.flockr.features.auth.presentation.SignInUiState
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.features.auth.presentation.AuthViewModel
import `in`.xroden.flockr.utils.rememberHaptics
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToSignup: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val signInState by viewModel.signInState.collectAsStateWithLifecycle()

    // Get Activity context for Credential Manager
    val context = LocalContext.current
    val activity = context as? Activity
    val haptics = rememberHaptics()

    LaunchedEffect(signInState) {
        when (signInState) {
            is SignInUiState.Success -> haptics.success()
            is SignInUiState.Error -> haptics.error()
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            if (onNavigateBack != null) {
                CenterAlignedTopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
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
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Section
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Sign in to continue managing your households",
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
                        placeholder = { Text("Enter your password") },
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
            }

            if (uiState is AuthUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    val error = (uiState as? AuthUiState.Error)?.message ?: "Unknown error"
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            val isSigningIn = signInState is SignInUiState.Loading

            FlockrPrimaryButton(
                text = if (isSigningIn) "Signing In..." else "Sign In",
                onClick = { viewModel.signIn(email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSigningIn && email.isNotBlank() && password.isNotBlank(),
                isLoading = isSigningIn
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

            // Show sign-in errors
            if (signInState is SignInUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = (signInState as SignInUiState.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Button(
                onClick = { haptics.tap(); activity?.let { viewModel.signInWithGoogle(it) } },
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
                enabled = !isSigningIn && activity != null
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSigningIn) {
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

            // Sign up prompt
            OutlinedButton(
                onClick = { haptics.tap(); onNavigateToSignup() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Create Account", fontWeight = FontWeight.SemiBold)
            }

            // Footer text
            Text(
                text = "Don't have an account? Create one to start managing your household.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

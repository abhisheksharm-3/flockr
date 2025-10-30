package `in`.xroden.flockr.ui.screens.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var showCarousel by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Get the current profile name from the StateFlow
    val profile by viewModel.profile.collectAsState()
    var fullName by remember { mutableStateOf("") }

    // Initialize fullName with profile data when available
    LaunchedEffect(profile) {
        if (fullName.isEmpty() && profile?.fullName?.isNotBlank() == true) {
            fullName = profile?.fullName ?: ""
        }
    }

    if (showCarousel) {
        OnboardingCarousel(
            onComplete = { showCarousel = false }
        )
    } else {
        OnboardingSetup(
            fullName = fullName,
            onFullNameChange = { fullName = it },
            isLoading = isUpdating,
            onComplete = {
                isUpdating = true
                coroutineScope.launch {
                    viewModel.updateProfile(
                        fullName = fullName.ifBlank { null },
                        hasCompletedOnboarding = true
                    )
                    isUpdating = false
                    // The navigation will be handled automatically by state changes
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingCarousel(
    onComplete: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Home,
            title = "Welcome to Flockr",
            description = "The all-in-one app for managing your household. Track expenses, organize chores, and stay connected with your housemates."
        ),
        OnboardingPage(
            icon = Icons.Default.Star,
            title = "Track Finances",
            description = "Split bills easily, track who owes what, and generate automated expense reports. Never lose track of shared expenses again."
        ),
        OnboardingPage(
            icon = Icons.Default.CheckCircle,
            title = "Organize Chores",
            description = "Assign tasks, create shopping lists, and keep everyone on the same page. Household management made simple."
        ),
        OnboardingPage(
            icon = Icons.Default.Notifications,
            title = "Stay Connected",
            description = "Real-time notifications, group chat, and document sharing. Everything your household needs in one place."
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            // Page indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (index == pagerState.currentPage) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.widthIn(min = 120.dp)
                ) {
                    Text(
                        if (pagerState.currentPage < pages.size - 1) "Next"
                        else "Get Started"
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 32.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSetup(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    isLoading: Boolean = false,
    onComplete: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Your Profile") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "What's your name?",
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text("Your Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "This is how you'll appear to other household members",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                enabled = fullName.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Complete Setup")
                }
            }
        }
    }
}


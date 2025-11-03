# Contributing to Flockr

First off, thank you for considering contributing to Flockr! It's people like you that make Flockr such a great tool for household management.

## 🎯 Code of Conduct

By participating in this project, you are expected to uphold our Code of Conduct:
- Be respectful and inclusive
- Welcome newcomers and help them get started
- Accept constructive criticism gracefully
- Focus on what is best for the community

## 🚀 How Can I Contribute?

### Reporting Bugs 🐛

Before creating bug reports, please check the issue list as you might find out that you don't need to create one. When you are creating a bug report, please include as many details as possible:

* **Use a clear and descriptive title**
* **Describe the exact steps to reproduce the problem**
* **Provide specific examples to demonstrate the steps**
* **Describe the behavior you observed after following the steps**
* **Explain which behavior you expected to see instead and why**
* **Include screenshots or animated GIFs** if possible
* **Include your Android version** and device model
* **Include app version** you're using

### Suggesting Enhancements ✨

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, please include:

* **Use a clear and descriptive title**
* **Provide a step-by-step description of the suggested enhancement**
* **Provide specific examples to demonstrate the steps**
* **Describe the current behavior** and **explain which behavior you expected to see instead**
* **Explain why this enhancement would be useful**

### Pull Requests 🔀

* Fill in the required template
* Follow the Kotlin coding conventions
* Include screenshots and animated GIFs in your pull request whenever possible
* End all files with a newline
* Write meaningful commit messages

## 💻 Development Process

### Setting Up Development Environment

1. **Fork and clone the repository**
```bash
git clone https://github.com/YOUR_USERNAME/flockr.git
cd flockr
```

2. **Set up Supabase**
- Follow the setup guide in README.md
- Use a development Supabase project (don't use production)

3. **Configure the app**
```properties
# local.properties
SUPABASE_URL=your_dev_supabase_url
SUPABASE_KEY=your_dev_anon_key
MAPS_API_KEY=your_maps_key (optional)
```

4. **Build and run**
```bash
./gradlew assembleDebug
```

### Kotlin Style Guide

We follow the [official Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html) with these additions:

#### Naming Conventions
```kotlin
// Classes: PascalCase
class HouseRepository

// Functions and variables: camelCase
fun loadHouseDetails()
val userName: String

// Constants: UPPERCASE_SNAKE_CASE
const val MAX_HOUSES = 10

// Compose functions: PascalCase
@Composable
fun HouseDetailsScreen()

// Boolean variables: isX, hasX, canX
val isLoading: Boolean
val hasPermission: Boolean
val canEdit: Boolean
```

#### Code Organization
```kotlin
// 1. Package declaration
package `in`.xroden.flockr.ui.screens.house

// 2. Imports (sorted)
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*

// 3. Top-level declarations
private const val TAG = "HouseDetailsScreen"

// 4. Class/functions
@Composable
fun HouseDetailsScreen() {
    // Implementation
}
```

#### Function Style
```kotlin
// Short functions: single expression
fun calculateTotal(a: Int, b: Int) = a + b

// Complex functions: explicit return type
fun processExpense(expense: Expense): Result<Unit> {
    return try {
        // Logic
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

#### Repository Pattern
```kotlin
@Singleton
class SomeRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    // Use Flow for real-time data
    fun getItemsFlow(id: String): Flow<List<Item>> {
        return flow {
            emit(getItems(id))
            // Setup realtime subscription
        }
    }
    
    // Suspend functions for one-time operations
    suspend fun createItem(item: Item): Result<Unit> {
        return try {
            supabase.from("items").insert(item)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

#### ViewModel Pattern
```kotlin
@HiltViewModel
class SomeViewModel @Inject constructor(
    private val repository: SomeRepository
) : ViewModel() {
    
    // Single StateFlow for UI state
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Public functions for user actions
    fun loadData() {
        viewModelScope.launch {
            // Logic
        }
    }
}

// Sealed class for states
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: List<Item>) : UiState()
    data class Error(val message: String) : UiState()
}
```

#### Composable Functions
```kotlin
@Composable
fun MyScreen(
    onNavigateBack: () -> Unit,
    viewModel: MyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = { /* TopBar */ }
    ) { padding ->
        when (val state = uiState) {
            is UiState.Loading -> LoadingIndicator()
            is UiState.Success -> Content(state.data)
            is UiState.Error -> ErrorMessage(state.message)
        }
    }
}

// Private composables for sub-components
@Composable
private fun Content(data: List<Item>) {
    LazyColumn {
        items(data) { item ->
            ItemCard(item)
        }
    }
}
```

### Git Commit Messages

* Use the present tense ("Add feature" not "Added feature")
* Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
* Limit the first line to 72 characters or less
* Reference issues and pull requests liberally after the first line

Good examples:
```
✨ Add per-household currency configuration
🐛 Fix notification permission request on Android 13+
🎨 Improve HouseDetailsScreen with map header
📝 Update README with comprehensive documentation
♻️ Refactor expense repository to use Flow
🔧 Configure RLS policies for house_config table
```

Emoji prefixes (optional but recommended):
- ✨ `:sparkles:` - New feature
- 🐛 `:bug:` - Bug fix
- 🎨 `:art:` - UI/UX improvements
- ♻️ `:recycle:` - Code refactoring
- 📝 `:memo:` - Documentation
- 🔧 `:wrench:` - Configuration changes
- ✅ `:white_check_mark:` - Tests
- ⚡ `:zap:` - Performance improvements

### Pull Request Process

1. **Create a feature branch**
```bash
git checkout -b feature/amazing-feature
```

2. **Make your changes**
   - Write clean, documented code
   - Follow the style guide
   - Test thoroughly

3. **Commit your changes**
```bash
git add .
git commit -m "✨ Add amazing feature"
```

4. **Push to your fork**
```bash
git push origin feature/amazing-feature
```

5. **Open a Pull Request**
   - Use a clear title and description
   - Reference any related issues
   - Include screenshots for UI changes
   - Wait for review

6. **Address review feedback**
   - Make requested changes
   - Push additional commits
   - Request re-review

7. **Merge**
   - Your PR will be merged once approved
   - Delete your feature branch after merge

### Testing Guidelines

Before submitting a PR, please test:

1. **Manual Testing**
   - Test the feature on multiple devices
   - Test on different API levels (29+)
   - Test in both light and dark themes
   - Test with multiple households
   - Test edge cases

2. **Code Quality**
   - No lint errors
   - No compiler warnings
   - Follows style guide
   - Well-documented

3. **Performance**
   - No memory leaks
   - Smooth animations
   - Fast load times

### Documentation

* Update README.md if you change functionality
* Add KDoc comments for public APIs
* Update CHANGELOG.md with your changes
* Include inline comments for complex logic

## 🏗️ Project Structure Guidelines

### Adding New Features

1. **Create data model** in `data/model/`
2. **Create repository** in `data/repository/`
3. **Create ViewModel** in `ui/viewmodel/`
4. **Create screen** in `ui/screens/`
5. **Update navigation** in `ui/navigation/`
6. **Add to README** feature list

### Adding New Database Tables

1. **Update schema** in `SUPABASE_RPC_FUNCTIONS.sql`
2. **Add RLS policies** for security
3. **Create Kotlin data model**
4. **Update repository**
5. **Document in README**

### Adding New Dependencies

1. **Add to** `gradle/libs.versions.toml`
2. **Add to** `app/build.gradle.kts`
3. **Document why** it's needed
4. **Check license** compatibility

## 🎯 Priority Areas

We especially welcome contributions in these areas:

1. **Testing** - Unit tests, integration tests, UI tests
2. **Documentation** - Improve docs, add examples
3. **Performance** - Optimize queries, reduce latency
4. **Accessibility** - Improve a11y support
5. **Localization** - Add language translations
6. **UI Polish** - Animations, transitions, micro-interactions

## 📜 License

By contributing, you agree that your contributions will be licensed under the same license as the project (MIT License).

## ❓ Questions?

Feel free to:
- Open an issue with the `question` label
- Start a discussion in GitHub Discussions
- Reach out to maintainers

## 🙏 Thank You!

Your contributions make Flockr better for everyone. Thank you for being part of this project!

---

**Happy Coding! 🚀**


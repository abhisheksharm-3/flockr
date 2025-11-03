# Flockr Design System

## Overview
This document describes the consistent UI design system applied across all screens in Flockr, inspired by the Finance Hub's clean, data-rich design philosophy.

## Design Principles

### 1. **Clean & Modern**
- Consistent spacing (24dp horizontal padding, 20dp vertical spacing)
- Rounded corners (12dp for cards, 16dp for FABs)
- Subtle borders (1dp with 0.2-0.3 alpha)
- Minimal elevation (0-2dp)

### 2. **Data-Rich Display**
- Clear visual hierarchy with bold headlines
- Descriptive subtitles for context
- Compact cards for quick stats
- ModernListItem for navigation

### 3. **Interactive Feedback**
- Spring animations on press (scale to 0.97-0.98)
- Smooth transitions
- Clear visual states (pressed, selected, disabled)

## Component Library

### Typography
- **Display Small**: Headlines (displaySmall, Bold)
- **Title Large**: Screen titles (titleLarge, SemiBold)
- **Body Large**: Descriptions and subtitles
- **Label Large**: Form field labels (SemiBold)

### Colors
- **Background**: Material 3 background color
- **Surface**: Cards and elevated elements
- **Primary**: Accent color for CTAs and highlights
- **On Surface Variant**: Secondary text (subtitles, descriptions)

### Components

#### DataCard
```kotlin
DataCard(
    title = "Section Title",
    subtitle = "Optional description",
    onClick = { /* optional */ }
) {
    // Content
}
```

#### CompactDataCard
```kotlin
CompactDataCard(
    label = "This Month",
    value = "$1,234.56",
    modifier = Modifier.weight(1f),
    accentColor = MaterialTheme.colorScheme.primary
)
```

#### ModernListItem
```kotlin
ModernListItem(
    title = "Feature Name",
    subtitle = "Description of feature",
    icon = Icons.Default.Icon,
    onClick = { /* navigate */ },
    showChevron = true
)
```

#### FlockrTextField
```kotlin
FlockrTextField(
    value = value,
    onValueChange = { value = it },
    placeholder = "Hint text",
    leadingIcon = { Icon(...) },
    modifier = Modifier.fillMaxWidth()
)
```

#### FlockrPrimaryButton
```kotlin
FlockrPrimaryButton(
    text = "Action",
    onClick = { /* action */ },
    modifier = Modifier.fillMaxWidth(),
    enabled = true,
    isLoading = false
)
```

## Screen Patterns

### Standard Screen Structure
```kotlin
Scaffold(
    topBar = {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    "Screen Title",
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
    containerColor = MaterialTheme.colorScheme.background
) { padding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Section Header",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Description of this section",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Content items...
    }
}
```

### Form Screen Pattern
```kotlin
Column(
    verticalArrangement = Arrangement.spacedBy(16.dp)
) {
    Column {
        Text(
            text = "Field Label *",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlockrTextField(
            value = value,
            onValueChange = { value = it },
            placeholder = "Hint",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

## Screens Updated

### Authentication
- ✅ **LoginScreen**: Modern form layout with clear sections
- ✅ **SignupScreen**: Consistent with login, password validation

### Main Navigation
- ✅ **HomeScreen**: Modern household cards with animations
- ✅ **CreateHouseScreen**: Clean form with validation
- ✅ **JoinHouseScreen**: Centered invite code entry

### House Features
- ✅ **HouseDetailsScreen**: Feature cards with map header
- ✅ **HouseSettingsScreen**: Form-based settings editing
- ✅ **ManageMembersScreen**: List-based member management

### Feature Modules
- ✅ **ExpenseDashboardScreen**: Finance Hub design pattern
- ✅ **ChoresScreenModern**: Modern task management
- ✅ **DocumentsScreen**: Tabbed document organization
- ✅ **SettingsScreen**: Clean settings organization

## Best Practices

1. **Spacing**: Use consistent 24dp horizontal padding and 20dp item spacing
2. **Headers**: Always include a descriptive subtitle below the main title
3. **Forms**: Label above field, validation messages below
4. **Navigation**: Use ModernListItem for feature navigation
5. **Actions**: Primary actions use FlockrPrimaryButton
6. **Loading States**: Show loading indicator in buttons or screens
7. **Empty States**: Provide helpful empty state messages with actions
8. **Animations**: Use spring animations for interactive elements

## Color Usage

- **Primary**: CTAs, selected states, important highlights
- **Secondary**: Secondary actions, accent elements
- **Surface**: Card backgrounds, elevated elements
- **SurfaceVariant**: Compact cards, subtle backgrounds
- **Error**: Validation errors, destructive actions
- **OnSurfaceVariant**: Secondary text, icons

## Accessibility

- All interactive elements have minimum 48dp touch targets
- Clear color contrast for text
- Semantic icons with content descriptions
- Error messages clearly associated with fields
- Loading states announced for screen readers


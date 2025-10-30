# Flockr - Household Management Super-App

A full-stack, multi-tenant household management application built with Kotlin, Jetpack Compose, and Supabase.

## 🌟 Features

### Core Functionality
- **Multi-Household Support**: Users can be members of multiple households
- **User Authentication**: Secure sign-up/sign-in with persistent sessions
- **Onboarding Flow**: Guided setup for new users

### Finance Suite
- **Expense Tracking**: Track one-time and recurring expenses
- **Bill Splitting (IOUs)**: Split expenses among household members with automatic balance tracking
- **Monthly Reports**: Automated expense summaries by member and category
- **Per-Diem Billing**: Track daily items and generate itemized monthly bills

### Organization Suite
- **Shared Shopping Lists**: Real-time collaborative shopping with purchase notifications
- **Chores & To-Dos**: Assign and track household tasks with due dates
- **Smart Notifications**: Get notified when items are purchased or chores are completed

### Communication Suite
- **House Chat**: Real-time messaging for each household
- **Notification Center**: Centralized inbox with deep-linking to relevant content
- **Push Notifications**: Stay informed even when the app is closed (via Supabase Functions)

### Document Vault
- **Personal Documents**: Store private files securely
- **House Documents**: Share important files with household members

### App Settings
- **Theme Switcher**: Choose between Light, Dark, or System theme
- **Profile Management**: Edit your name and preferences

## 🏗️ Architecture

### Tech Stack
- **Frontend**: Kotlin with Jetpack Compose (Material 3)
- **Backend**: Supabase (Auth, PostgREST, Storage, Realtime, Functions)
- **Maps**: Google Maps SDK (placeholder ready)
- **Architecture**: MVVM + Repository Pattern
- **DI**: Hilt
- **Async**: Kotlin Coroutines & Flow

### Design System
- **Framework**: Material 3
- **Font Pairing**:
  - **Newsreader** (serif) for headings and titles
  - **Inter** (sans-serif) for body text and UI elements
- **Theme**: Full theme switcher with persistent preferences (DataStore)

### Key Architectural Principles
- **Persistent Auth**: Session-based authentication with automatic routing
- **Multi-Household Design**: Built around the concept of multiple household membership
- **Reactive-First (Realtime)**: All list data uses Flows with Supabase realtime subscriptions
- **Server-Side Logic (RPC-First)**: Complex calculations done via Supabase database functions
- **State Management**: ViewModels expose single `StateFlow<UiState>` for each screen

## 📦 Project Structure

```
app/src/main/java/in/xroden/flockr/
├── data/
│   ├── model/                  # Data classes (House, Profile, Expense, etc.)
│   ├── preferences/            # DataStore preferences (Theme)
│   └── repository/             # Data repositories with Flow-based APIs
│       ├── AuthRepository
│       ├── HouseRepository
│       ├── NotificationRepository
│       ├── ExpenseRepository
│       ├── ShoppingRepository
│       ├── ChoreRepository
│       ├── ChatRepository
│       └── DocumentRepository
├── di/                         # Dependency injection (Hilt modules)
├── ui/
│   ├── navigation/             # Navigation graph and routes
│   ├── screens/                # Composable screens
│   │   ├── auth/               # Login, Signup
│   │   ├── onboarding/         # Onboarding flow
│   │   ├── home/               # Multi-household home screen
│   │   ├── house/              # House details with map
│   │   ├── notifications/      # Notification center
│   │   ├── expenses/           # Expense tracking
│   │   ├── shopping/           # Shopping list
│   │   ├── chores/             # Chore management
│   │   ├── chat/               # House chat
│   │   ├── documents/          # Document vault
│   │   └── settings/           # App settings
│   ├── theme/                  # Material 3 theme, colors, typography
│   └── viewmodel/              # ViewModels for all screens
├── FlockrApplication.kt        # Application class
└── MainActivity.kt             # Entry point
```

## 🚀 Setup Instructions

### 1. Prerequisites
- Android Studio (latest stable version)
- JDK 11 or higher
- A Supabase account (free tier available)

### 2. Supabase Setup

#### Step 1: Create a Supabase Project
1. Go to [supabase.com](https://supabase.com) and create a new project
2. Wait for the project to initialize

#### Step 2: Run the Database Schema
1. In your Supabase project, go to the SQL Editor
2. Open the `supabase_schema.sql` file from this repository
3. Copy and paste the entire contents into the SQL editor
4. Click "Run" to execute the schema

This will create:
- All necessary tables (profiles, houses, expenses, chores, etc.)
- Row Level Security (RLS) policies
- Database functions for complex operations
- Triggers for automated workflows

#### Step 3: Configure Storage Buckets
1. Go to Storage in your Supabase dashboard
2. Create two buckets:
   - `personal_documents` (private)
   - `house_documents` (private with RLS)

#### Step 4: Get Your Credentials
1. Go to Project Settings → API
2. Copy your:
   - **Project URL** (e.g., `https://xxxxx.supabase.co`)
   - **Anon/Public Key**

### 3. Android App Setup

#### Step 1: Clone and Open
```bash
git clone <your-repo-url>
cd Flockr
```
Open the project in Android Studio.

#### Step 2: Configure Supabase Credentials
1. Open `app/src/main/java/in/xroden/flockr/di/AppModule.kt`
2. Replace the placeholder values:
```kotlin
private const val SUPABASE_URL = "YOUR_SUPABASE_URL"
private const val SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY"
```

#### Step 3: Install Fonts
The app uses custom fonts for a premium look. Follow the instructions in `FONTS_SETUP.md`:

1. Download **Newsreader** and **Inter** from Google Fonts
2. Rename and place the font files in `app/src/main/res/font/`
3. Or temporarily use fallback fonts (see `FONTS_SETUP.md`)

#### Step 4: Build and Run
1. Sync Gradle files
2. Build the project
3. Run on an emulator or physical device (API 29+)

### 4. Optional: Google Maps Integration

To enable the map view in HouseDetailsScreen:

1. Get a Google Maps API key from [Google Cloud Console](https://console.cloud.google.com/)
2. Add it to `local.properties`:
```
MAPS_API_KEY=YOUR_API_KEY_HERE
```
3. Update `AndroidManifest.xml` with the API key
4. Uncomment the map implementation in `HouseDetailsScreen.kt`

## 🔧 Configuration Files

### Key Files to Update
1. **`di/AppModule.kt`** - Add your Supabase credentials
2. **`local.properties`** (optional) - Add Google Maps API key
3. **Font files** - Download and add custom fonts (see `FONTS_SETUP.md`)

## 📱 App Flow

1. **Authentication**: User signs up or logs in
2. **Onboarding**: New users set their name and preferences
3. **Home Screen**: View all households you're a member of
4. **House Details**: Access all features for a specific household
   - Map view showing house location
   - Navigation to Expenses, Chores, Shopping, Chat, Documents
5. **Feature Screens**: Manage expenses, chores, shopping lists, etc.
6. **Notifications**: Real-time updates for all household activities
7. **Settings**: Change theme, edit profile, logout

## 🔐 Security Features

- **Row Level Security (RLS)**: All database tables are protected
- **User Isolation**: Users can only access their own data and data from houses they're members of
- **Secure Auth**: Supabase handles authentication with JWT tokens
- **Storage Security**: Documents are stored with proper access controls

## 🎨 Design Philosophy

The app follows Material 3 design guidelines with a custom font pairing for elegance:
- **Clean, data-rich interfaces** that prioritize information
- **Real-time updates** to keep all household members in sync
- **Clear notifications** so nothing falls through the cracks
- **Intuitive navigation** with deep-linking from notifications

## 🔄 Real-time Features

The following features update in real-time using Supabase Realtime:
- Shopping lists
- Chores
- Messages
- Notifications
- House membership changes

## 🗄️ Database Functions (RPCs)

Server-side functions handle complex operations:
- `create_notification_for_house` - Notify all house members
- `get_monthly_summary` - Calculate monthly expense totals
- `get_spend_by_member` - Track spending per member
- `get_spend_by_category` - Categorize expenses
- `get_user_balances` - Calculate IOU balances
- `get_per_diem_bill_itemized` - Generate per-diem bills
- `get_per_diem_bill_by_member` - Per-member per-diem costs

## 🚧 Future Enhancements

- [ ] Google Maps integration for house locations
- [ ] Full expense tracking UI with charts
- [ ] Document upload and preview
- [ ] House invitation system
- [ ] Recurring chores
- [ ] Budget planning tools
- [ ] Export reports to PDF

## 📄 License

This project is built as a demonstration of modern Android development with Supabase.

## 🤝 Contributing

This is a comprehensive reference implementation. Feel free to use it as a foundation for your own projects!

## 📞 Support

For issues or questions about:
- **Supabase setup**: Check [Supabase Documentation](https://supabase.com/docs)
- **Android/Compose**: Check [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- **Hilt**: Check [Hilt Documentation](https://dagger.dev/hilt/)

---

**Built with ❤️ using Kotlin, Jetpack Compose, and Supabase**


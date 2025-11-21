<div align="center">

# 🏠 Flockr

**Modern Household Management Platform**

[![Version](https://img.shields.io/badge/version-1.5.0-blue.svg)](https://github.com/abhisheksharm-3/flockr)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com/)
[![License](https://img.shields.io/badge/license-MIT-orange.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9+-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg)](https://developer.android.com/jetpack/compose)

*A full-stack, multi-tenant household management application built with Kotlin, Jetpack Compose, and Supabase.*

[Features](#-features) • [Quick Start](#-quick-start) • [Architecture](#️-architecture) • [Screenshots](#-screenshots) • [Contributing](#-contributing)

</div>

---

---

## 📚 Table of Contents

- [What's New](#-whats-new-in-v150)
- [Features](#-features)
  - [Finance Suite](#-finance-suite)
  - [Organization Suite](#-organization-suite)
  - [Communication Hub](#-communication-hub)
  - [Document Vault](#-document-vault)
  - [Modern UI/UX](#-modern-uiux)
- [Architecture](#️-architecture)
- [Quick Start](#-quick-start)
- [Screenshots](#-screenshots)
- [Configuration](#-configuration)
- [Database Schema](#️-database-schema)
- [Security](#️-security)
- [Testing](#-testing)
- [Contributing](#-contributing)
- [Roadmap](#-roadmap)
- [License](#-license)

---

## 🎉 What's New in v1.5.0

This is our **biggest release yet** - a complete rebuild from the ground up!

### 🎨 Fresh New Look
✨ **Complete UI Redesign** - Every screen has been redesigned with a modern, polished appearance  
🎯 **Better User Experience** - Improved navigation, cleaner layouts, and enhanced readability  
🌈 **Consistent Design** - All screens now follow the same beautiful design language  

### 🏗️ Under the Hood
🔧 **Complete Code Reorganization** - We've rebuilt the entire app structure for better performance and maintainability  
⚡ **Faster & More Stable** - Better organized code means faster load times and fewer bugs  
🛠️ **Future-Ready** - This foundation makes it easier to add exciting new features  

### 💪 What This Means for You
- **Same features you love**, just better organized and more beautiful
- **All your data is safe** - nothing was lost in the transition
- **Better performance** across the entire app
- **More polished experience** from start to finish

[See full changelog →](CHANGELOG.md)

---

## 🌟 Features

### 🎯 Key Highlights

<div align="center">

| 💰 **Complete Finance Management** | 🏠 **Multi-Household Support** | 🔄 **Real-Time Sync** |
|:--:|:--:|:--:|
| Track expenses, split bills, manage recurring payments, and generate detailed reports | Join unlimited households with role-based permissions and isolated data | Everything updates instantly across all devices via WebSocket |

| 📱 **Modern UI** | 🔒 **Secure & Private** | 🌍 **Fully Configurable** |
|:--:|:--:|:--:|
| Material 3 design with dark mode, smooth animations, and intuitive navigation | Row-level security, JWT auth, and encrypted storage | Per-household currency, timezone, and locale settings |

</div>

---

### 💰 Finance Suite
- **Expense Tracking** - Track one-time and recurring expenses with automatic categorization
- **Bill Splitting** - Split expenses among housemates with automated IOU calculations
- **Per-Diem Billing** - Configure daily items (milk, newspapers) with monthly auto-billing
- **Balance Management** - Real-time balance calculation and settlement tracking
- **Monthly Reports** - Automated summaries by member and category with export/share functionality
- **Multi-Currency** - Per-household currency configuration (USD, EUR, INR, etc.)

### 🏠 Organization Suite
- **Shared Shopping Lists** - Real-time collaborative shopping with purchase notifications
- **Smart Chores** - Assign, track, and complete household tasks with due dates
- **Task Assignment** - Assign chores to specific members with automatic notifications
- **Completion Tracking** - Mark tasks complete with timestamps and member attribution

### 💬 Communication Hub
- **House Chat** - Real-time messaging for each household with sender identification
- **Notification Center** - Unified inbox for all household activities
- **Deep Linking** - Tap notifications to jump directly to relevant content
- **Smart Alerts** - Get notified about purchases, bill splits, task completions, and more

### 📁 Document Vault
- **Personal Storage** - Secure private document storage
- **House Documents** - Share important files (leases, receipts) with household members
- **Upload/Download** - Easy file management with automatic notifications

### 🎨 Modern UI/UX
- **Material 3 Design** - Beautiful, modern interface following latest design guidelines
- **Finance Hub Design System** - Clean, data-rich design inspired by modern fintech apps
- **Consistent Components** - Reusable DataCard, ModernListItem, and FlockrTextField components
- **Theme Switcher** - Choose Light, Dark, or System-matched theme
- **Custom Typography** - Clear hierarchy with SemiBold titles and readable body text
- **Responsive Design** - Optimized for all screen sizes
- **Smooth Animations** - Polished spring animations and micro-interactions

### 📐 Design System (NEW)
The app follows a comprehensive design system inspired by modern fintech applications:
- **Clean & Minimal** - Subtle borders (1dp, 0.3 alpha), minimal elevation (0-2dp)
- **Data-Rich Display** - Clear visual hierarchy with compact stats and detailed information
- **Consistent Spacing** - 24dp horizontal padding, 20dp item spacing throughout
- **Component Library** - DataCard, CompactDataCard, ModernListItem, FlockrTextField, FlockrPrimaryButton

**Documentation**:
- See [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) for comprehensive guidelines

### 🔔 Real-Time Updates
Everything syncs instantly across all devices:
- Shopping lists
- Chores & tasks
- Chat messages
- Notifications
- Balance changes
- House membership

---

## 🎯 Who Is Flockr For?

Flockr is perfect for anyone sharing living spaces and expenses:

- **🏘️ Roommates & Flatmates** - Split rent, utilities, and groceries fairly
- **👨‍👩‍👧‍👦 Joint Families** - Track shared household expenses and coordinate tasks
- **🏠 Co-living Spaces** - Manage multiple members with clear financial tracking
- **🎓 Student Housing** - Keep track of who owes what without awkward conversations
- **👥 Shared Vacation Homes** - Coordinate expenses when multiple families share a property

---

## 🏗️ Architecture

### Tech Stack

**Frontend**
- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Repository Pattern
- **Dependency Injection**: Hilt
- **Async**: Kotlin Coroutines & Flow
- **Navigation**: Jetpack Navigation Compose

**Backend**
- **BaaS**: Supabase (PostgreSQL, Auth, Storage, Realtime)
- **Database**: PostgreSQL with Row Level Security
- **Storage**: Supabase Storage with private buckets
- **Realtime**: Supabase Realtime subscriptions
- **Functions**: PostgreSQL RPC functions

**Additional**
- **Maps**: Google Maps SDK (optional)
- **Fonts**: Newsreader (serif), Inter (sans-serif)
- **Theme**: DataStore for persistence

### Architecture Highlights

✅ **Clean Architecture** - Separation of concerns with data, domain, and presentation layers  
✅ **Reactive Programming** - Flow-based data streams with real-time updates  
✅ **Server-Side Logic** - Complex calculations done via PostgreSQL RPC functions  
✅ **Persistent Auth** - Session-based authentication with automatic token refresh  
✅ **Multi-Tenant** - Built from ground-up for multiple household support  
✅ **Offline-First Ready** - Architecture supports future offline capabilities  

---

## 📦 Project Structure

```
app/src/main/java/in/xroden/flockr/
├── data/
│   ├── model/              # Kotlin data classes
│   │   ├── House.kt
│   │   ├── HouseConfig.kt # NEW: Per-household settings
│   │   ├── Expense.kt
│   │   ├── Chore.kt
│   │   └── ...
│   ├── repository/         # Data access layer
│   │   ├── AuthRepository
│   │   ├── HouseRepository
│   │   ├── ExpenseRepository
│   │   ├── NotificationRepository
│   │   └── ...
│   └── preferences/        # DataStore preferences
│       └── ThemePreferences
├── di/                     # Hilt dependency injection
│   └── AppModule
├── ui/
│   ├── components/         # Reusable UI components
│   ├── navigation/         # Navigation graph
│   ├── screens/           # Feature screens
│   │   ├── auth/          # Login, Signup
│   │   ├── home/          # Multi-household home
│   │   ├── house/         # House details with map
│   │   ├── expenses/      # Finance management
│   │   ├── shopping/      # Shopping lists
│   │   ├── chores/        # Task management
│   │   ├── chat/          # Messaging
│   │   ├── documents/     # File storage
│   │   ├── notifications/ # Notification center
│   │   └── settings/      # App settings
│   ├── theme/             # Material 3 theme
│   └── viewmodel/         # ViewModels
├── utils/                 # Utilities
│   └── PermissionHandler  # Runtime permissions
└── MainActivity.kt        # Entry point
```

---

## 🚀 Quick Start

> **Get Flockr running in 4 simple steps!**

### ⚙️ Prerequisites
- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: 11 or higher
- **Android SDK**: API 29+ (Android 10+)
- **Supabase Account**: Free tier available at [supabase.com](https://supabase.com)

### Step 1: Clone the Repository

```bash
git clone https://github.com/abhisheksharm-3/flockr.git
cd flockr
```

### Step 2: Set Up Supabase

1. **Create a Supabase Project**
   - Go to [supabase.com](https://supabase.com)
   - Click "New Project"
   - Wait for initialization (2-3 minutes)

2. **Run Database Setup**
   - Open Supabase Dashboard → SQL Editor
   - Copy contents of `SUPABASE_RPC_FUNCTIONS.sql`
   - Paste and click "Run"
   - Verify all functions were created (check bottom of file)

3. **Create Storage Bucket**
   - Go to Storage → New Bucket
   - Name: `documents`
   - Privacy: Private
   - Click "Create bucket"

4. **Get Your Credentials**
   - Settings → API
   - Copy:
     - **Project URL**: `https://xxxxx.supabase.co`
     - **Anon Key**: `eyJhbG...`

### Step 3: Configure the App

1. **Add Supabase Credentials**

Create/edit `local.properties`:
```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your-anon-key-here
```

2. **(Optional) Add Google Maps**

Get a Maps API key from [Google Cloud Console](https://console.cloud.google.com/):
```properties
MAPS_API_KEY=your-maps-api-key
```

### Step 4: Build & Run

```bash
./gradlew assembleDebug
```

Or in Android Studio:
- Sync Gradle
- Run on emulator (API 29+) or physical device

---

## 📸 Screenshots

> *Coming Soon - Screenshots will be added here*

---

## 🔧 Configuration

### Per-Household Settings

Each household can be configured with:
- **Currency Code** (USD, EUR, GBP, INR, etc.)
- **Currency Symbol** ($, €, £, ₹)
- **Date Format** (YYYY-MM-DD, DD/MM/YYYY, MM/DD/YYYY)
- **First Day of Week** (Sunday=0, Monday=1)
- **Timezone** (UTC, America/New_York, etc.)

Configure via the `house_config` table or add a settings UI.

### Runtime Permissions

The app requests these permissions:
- **POST_NOTIFICATIONS** (Android 13+) - For push notifications
- **READ_MEDIA_IMAGES** (Android 13+) / **READ_EXTERNAL_STORAGE** (Android 12-) - For document uploads
- **ACCESS_FINE_LOCATION** / **ACCESS_COARSE_LOCATION** - For house location (optional)

Permissions are requested at runtime when needed.

---

## 🗄️ Database Schema

### Core Tables
- `profiles` - User profiles synced with Supabase Auth
- `houses` - Household information with location data
- `house_config` - **NEW**: Per-household settings
- `house_members` - Junction table for multi-tenant membership
- `house_invitations` - Invitation system with codes

### Finance Tables
- `one_time_expenses` - Individual purchases
- `recurring_expenses` - Monthly bills (rent, utilities)
- `expense_splits` - Bill splitting with IOU tracking
- `transactions` - Settlement ledger
- `per_diem_config` - Daily item templates
- `per_diem_entries` - Daily usage logs
- `payment_history` - Recurring expense payment tracking

### Organization Tables
- `shopping_items` - Shared shopping lists
- `chores` - Tasks with assignment and recurrence

### Communication Tables
- `messages` - House group chat
- `notifications` - Unified notification inbox

### Storage
- `documents` - File metadata with Supabase Storage integration

### Server-Side Functions (RPC)
- `create_notification_for_house` - Broadcast notifications
- `get_user_balances` - Calculate IOU balances
- `get_monthly_summary` - Monthly expense totals
- `get_spend_by_member` - Member spending breakdown
- `get_spend_by_category` - Category breakdown
- `get_per_diem_bill_itemized` - Itemized per-diem bill
- `get_per_diem_bill_by_member` - Per-member per-diem costs

---

## 🛡️ Security

### Row Level Security (RLS)
Every table has RLS policies ensuring:
- Users can only access houses they're members of
- Personal data is isolated per user
- Documents respect house membership
- Notifications are user-specific

### Authentication
- JWT-based authentication via Supabase Auth
- Automatic token refresh
- Secure password hashing
- Optional email verification

### Storage Security
- Private buckets with RLS policies
- Authenticated uploads/downloads
- Automatic cleanup on document deletion

---

## 🎨 Design System

### Typography
- **Headings**: Newsreader (elegant serif)
- **Body**: Inter (clean sans-serif)

### Color Scheme
- Material 3 dynamic color system
- Supports Light and Dark themes
- Accessible contrast ratios

### Components
Custom reusable components in `ui/components/`:
- `FlockrPrimaryButton` - Main CTA button
- `FlockrCard` - Content card with elevation
- `FlockrSectionHeader` - Section title
- `FlockrTextField` - Styled input field

---

## 📱 Features in Detail

### Multi-Household Management
- Users can join unlimited households
- Each household has unique invite code
- Owner can manage members
- Leave household anytime

### Real-Time Synchronization
- Instant updates via Supabase Realtime
- No manual refresh needed
- WebSocket-based for efficiency
- Automatic reconnection

### Notification System
- Unified notification center
- Deep-linking to content
- Mark as read/unread
- Persistent across sessions

### Expense Management
- Multiple expense types (one-time, recurring, per-diem)
- Automatic IOU calculations
- Split bills equally or by amount
- Settlement tracking
- Export reports to share externally

---

## 🧪 Testing

### Manual Testing Checklist
- [ ] Sign up and complete onboarding
- [ ] Create a household
- [ ] Generate invite code and join via code
- [ ] Add expenses and split bills
- [ ] View and settle balances
- [ ] Create shopping list and mark items purchased
- [ ] Assign and complete chores
- [ ] Send chat messages
- [ ] Upload documents
- [ ] Generate expense report
- [ ] Switch themes
- [ ] Test notifications
- [ ] Test with multiple households

### Future: Automated Testing
- Unit tests for repositories
- ViewModel tests
- UI tests with Compose Testing
- Integration tests for RPC functions

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'Add amazing feature'`)
4. **Push to the branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

### Development Guidelines
- Follow Kotlin coding conventions
- Use Material 3 design principles
- Write clean, documented code
- Test on multiple devices/API levels
- Update README if adding features

---

## 📋 Roadmap

### Planned Features
- [ ] Expense charts and visualizations
- [ ] Recurring chore scheduling
- [ ] Budget planning and alerts
- [ ] Export reports to PDF
- [ ] Push notifications via Supabase Functions
- [ ] Offline mode with local caching
- [ ] House settings UI for currency/timezone
- [ ] Member roles and permissions
- [ ] Photo attachments for expenses
- [ ] Receipt scanning with OCR

### Nice-to-Have
- [ ] Dark mode improvements
- [ ] Tablet-optimized layout
- [ ] Widget support
- [ ] Wear OS companion app
- [ ] Voice commands
- [ ] Calendar integration

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **Supabase** for the amazing backend platform
- **Jetpack Compose** team for modern Android UI
- **Material Design** for design guidelines
- **Google Fonts** for beautiful typography

---

---

## ❓ Frequently Asked Questions

<details>
<summary><b>Is Flockr free to use?</b></summary>

Yes! Flockr is open-source and free to use. You only need a free Supabase account for the backend, which offers generous free tier limits suitable for most households.
</details>

<details>
<summary><b>How many households can I join?</b></summary>

There's no limit! You can join as many households as you want. Each household has its own isolated data, expenses, and settings.
</details>

<details>
<summary><b>Is my financial data secure?</b></summary>

Absolutely! We use:
- Row-Level Security (RLS) to ensure you can only access data from households you're a member of
- JWT-based authentication with Supabase Auth
- Encrypted HTTPS connections for all data transfers
- Private storage buckets for documents
</details>

<details>
<summary><b>Can I use Flockr offline?</b></summary>

Currently, Flockr requires an internet connection. Offline mode with local caching is planned for a future release.
</details>

<details>
<summary><b>What currencies are supported?</b></summary>

Flockr supports all major currencies! Each household can configure its own currency (USD, EUR, GBP, INR, JPY, and more) with the appropriate symbol.
</details>

<details>
<summary><b>Can I export my expense data?</b></summary>

Yes! Monthly reports can be generated and shared. PDF export functionality is planned for future releases.
</details>

<details>
<summary><b>What's the minimum Android version required?</b></summary>

Flockr requires Android 10 (API 29) or higher.
</details>

---

## 📞 Support

### Documentation
- [Supabase Docs](https://supabase.com/docs)
- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose)
- [Hilt Docs](https://dagger.dev/hilt/)
- [Material 3 Guidelines](https://m3.material.io/)

### Issues
Found a bug? [Open an issue](https://github.com/abhisheksharm-3/flockr/issues)

### Questions
Have questions? [Start a discussion](https://github.com/abhisheksharm-3/flockr/discussions)

---

<div align="center">

**Built with ❤️ using Kotlin, Jetpack Compose, and Supabase**

⭐ Star this repo if you find it helpful!

[Report Bug](https://github.com/abhisheksharm-3/flockr/issues) · [Request Feature](https://github.com/abhisheksharm-3/flockr/issues) · [Documentation](https://github.com/abhisheksharm-3/flockr/wiki)

</div>

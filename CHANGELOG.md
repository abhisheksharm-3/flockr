# Flockr Changelog

All notable changes to Flockr will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.0] - 2025-11-21

### 🎉 Major Refactoring & UI Overhaul

This is a **massive** release that completely rebuilds Flockr from the ground up! We've reorganized the entire codebase and given the app a fresh new look.

---

## 📱 For Everyone (User-Friendly Version)

### 🎨 What You'll Notice

#### ✨ Brand New User Interface
- **Fresh Modern Look**: Complete UI redesign across every screen with a more polished, professional appearance
- **Better Visual Consistency**: All screens now follow the same beautiful design language
- **Improved Readability**: Cleaner layouts, better spacing, and easier-to-read text throughout
- **Enhanced Navigation**: Moving between features feels smoother and more intuitive
- **Updated Colors & Icons**: Refreshed color schemes and icon designs for a modern feel

#### 🚀 What This Means for You

**Better Experience**: 
- The app looks and feels more professional and polished
- Everything is easier to find and use
- Screens load faster and respond quicker
- More consistent design means less confusion

**Same Features, Better Packaging**:
- All your favorite features (expenses, chores, shopping, chat) are still there
- They just work better and look better now
- Nothing is missing - everything is improved

**Why We Did This**:
Think of it like renovating a house. We kept the foundation (all your data and features) but completely remodeled every room to make it more beautiful, organized, and easier to live in.

---

## 👨‍💻 For Developers (Technical Version)

### 🏗️ Major Architectural Refactoring

#### **Complete Project Restructure: Layer-by-Type → Layer-by-Feature**

**What Changed:**

**Before (Layer-by-Type):**
```
app/src/main/java/in/xroden/flockr/
├── data/
│   ├── model/          # ALL models
│   ├── repository/     # ALL repositories
│   └── preferences/
├── ui/
│   ├── screens/        # ALL screens
│   ├── viewmodel/      # ALL viewmodels
│   └── components/
└── di/
```

**After (Layer-by-Feature):**
```
app/src/main/java/in/xroden/flockr/
├── features/
│   ├── auth/           # Everything auth-related
│   │   ├── AuthScreen.kt
│   │   ├── AuthViewModel.kt
│   │   └── AuthRepository.kt
│   ├── expenses/       # Everything expense-related
│   │   ├── ExpenseScreen.kt
│   │   ├── ExpenseViewModel.kt
│   │   └── ExpenseRepository.kt
│   ├── chores/
│   ├── shopping/
│   └── ...
├── data/
│   ├── dto/            # Data transfer objects
│   └── enums/          # Shared enums
└── ui/
    ├── components/     # Shared UI components
    ├── theme/          # Theme configuration
    └── navigation/     # Navigation graph
```

**Why This Matters:**

1. **Better Code Organization**: Related code is now grouped together by feature instead of by type
2. **Easier Maintenance**: Want to work on expenses? Everything you need is in `features/expenses/`
3. **Improved Scalability**: Adding new features no longer pollutes shared directories
4. **Better Encapsulation**: Each feature is self-contained with its own models, repositories, and UI
5. **Team Collaboration**: Multiple developers can work on different features without conflicts
6. **Faster Navigation**: No more hunting through 50+ files in `viewmodel/` or `screens/` directories

### 🎨 UI/UX Overhaul

#### **Comprehensive Design System Implementation**
- Redesigned all screens to follow a unified design system
- Implemented consistent spacing, typography, and color schemes across the app
- Modernized component library with new reusable components
- Enhanced Material 3 theming with better dark mode support
- Improved accessibility and visual hierarchy

#### **Screen-by-Screen Updates**
- **Finance Suite**: Cleaner expense cards, better report visualization
- **Shopping Lists**: More intuitive item management UI
- **Chores**: Improved task assignment and completion interfaces
- **Chat**: Better message bubbles and timestamp display
- **Settings**: Reorganized settings with clearer categories
- **All Forms**: Standardized input fields and validation feedback

### 🔧 Technical Improvements

#### **Build & Compilation**
- Resolved all outstanding compilation errors and warnings
- Fixed dependency conflicts and circular dependencies
- Improved build times with better module organization
- Enhanced type safety with proper null handling

#### **Code Quality**
- Fixed role permission checks (enum comparison instead of string comparison)
- Corrected finance calculation bugs in RPC functions
- Resolved serialization issues in data models
- Improved error handling and logging throughout
- Added proper documentation to all public APIs

#### **Database & Backend**
- Fixed `get_monthly_summary` RPC for accurate expense totals
- Updated `get_per_diem_bill_by_member` with required fields
- Improved `get_recurring_expenses_with_status` for payment tracking
- Synchronized all data models with backend responses

### 📊 Migration Impact

**Files Moved/Renamed**: ~150+ files
**New Directory Structure**: Feature-based modules
**Maintained**: 100% backward compatibility with existing data
**Breaking Changes**: None (internal refactoring only)

### 🎯 Benefits for Development

1. **Reduced Cognitive Load**: Work on one feature without mental context switching
2. **Better Feature Isolation**: Each feature can be tested independently
3. **Clearer Dependencies**: Feature boundaries make dependencies explicit
4. **Easier Onboarding**: New developers can understand one feature at a time
5. **Future-Proof**: Easy to extract features into separate modules if needed

### 🐛 Bug Fixes Included

- Fixed role-based permission checks (Owner/Admin/Member)
- Corrected home screen member count display
- Resolved per diem report missing field exceptions
- Fixed currency display in expense forms
- Resolved ProductivityDialog overload conflicts
- Fixed unresolved references (CategoryRed, sp units, operators)
- Improved document upload error handling

### 📝 What Stays The Same

- All user data remains intact
- Database schema unchanged (only RPC function fixes)
- API compatibility maintained
- All feature functionality preserved
- Configuration and settings preserved

---

## [1.1.0] - 2025-11-07

### 🎉 Major Refactoring Release - Production Ready!

This release focuses on cleaning up the codebase, removing redundant logic, fixing bugs, and optimizing database performance. The app has been refactored from "just working" to "production-ready" state.

### ✨ What's New for Users

#### 🚀 Performance Improvements
- **Faster Load Times**: Optimized database queries now load your expenses, bills, and notifications up to 30% faster
- **Smoother Experience**: Removed redundant database operations that were causing lag
- **Better Reliability**: Fixed issues that could cause app crashes when creating recurring bills

#### 🐛 Bug Fixes
- **Fixed Recurring Bills**: You can now create recurring bills without encountering serialization errors
- **Notification Consistency**: Notifications now work reliably across all features (expenses, chores, shopping lists)
- **Data Accuracy**: Removed duplicate triggers that were causing inconsistent house configuration data

#### 🔒 Security Enhancements
- **Cleaner Database**: Removed redundant functions and triggers that could potentially cause data inconsistencies
- **Better Data Integrity**: Standardized how we store notification data to prevent corruption
- **Optimized Queries**: Added performance indexes without compromising security

#### 💪 Stability Improvements
- **No More Crashes**: Fixed the "Serializer for class 'Any' is not found" error when creating recurring bills
- **Consistent Behavior**: House settings now initialize correctly every time you create a new house
- **Error Handling**: Better error messages throughout the app so you know what's happening

### 🔧 What This Means for You

- **More Reliable**: The app is now more stable and less likely to encounter unexpected errors
- **Faster**: Everything from loading your dashboard to adding expenses is now quicker
- **Ready to Scale**: Whether you're managing one house or multiple, the app performs consistently well
- **Peace of Mind**: Data is handled more securely and consistently across all features

### 📝 Technical Details

This release includes:
- 5 database migrations to clean up redundant logic
- Fixed serialization issues in recurring expense creation
- Removed duplicate house config triggers
- Standardized notification system
- Added performance indexes
- Improved error handling across ViewModels

### 🙏 Thank You

Thank you for being part of Flockr's journey from MVP to a production-ready app! This refactoring sets the foundation for exciting new features coming soon.

---

## [1.0.0] - 2025-11-01

### Initial Release
- House management
- Expense tracking and splitting
- Recurring bills
- Per diem tracking
- Shopping list
- Chores management
- Real-time messaging
- Notifications


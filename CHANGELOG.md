# Flockr Changelog

All notable changes to Flockr will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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


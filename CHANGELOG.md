# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.6.5] - 2025-12-20

### New Features\
- **Monthly Productivity**: Productivity screen now shows monthly stats with month selector. See who completed the most chores each month!\
- **Yearly Top 3**: Added yearly top performers section showing the top 3 contributors for the current year.\
\
### UI Improvements\
- **House Settings**: Redesigned with FormSectionCard pattern for consistent styling.\
- **Edit Recurring Bill**: Cleaned up Save button styling to match app theme.\
- **Invite Member Dialog**: Simplified layout with cleaner informational card.

---

## [1.6.5] - 2025-12-20

### ✨ New Features
- **Monthly Productivity**: Productivity screen now shows monthly stats with a month selector. See who completed the most chores each month!
- **Yearly Top 3**: Added a yearly top performers section showing the top 3 contributors for the currently selected year with medal colors.

### 🎨 UI Improvements
- **House Settings**: Redesigned with `FormSectionCard` pattern for consistent styling with icon headers.
- **Edit Recurring Bill**: Cleaned up Save button styling by removing the Surface wrapper.
- **Invite Member Dialog**: Simplified layout by moving Send button to TopAppBar and adding an informational card.

---

## [1.6.4] - 2025-12-19

### Improvements
- **Code Quality**: Refactored database queries to use the latest type-safe filter syntax.
- **UI Updates**: Updated deprecated icons for better Right-to-Left (RTL) support.
### Bug Fixes
- **Join House**: Fixed an issue where the preview screen would not appear after entering a code.
- **Startup**: Resolved a resource reference error in the splash loader.

---

## [1.6.3] - 2025-12-19

### 🐛 Bug Fixes
- **Join House Preview**: Fixed an issue where the Join House Preview screen was not appearing after entering an invite code.
- **Deep Linking**: Fixed deep links (`flockr://invite/CODE`) not opening the preview screen correctly.
- **Empty State**: Fixed a blank screen issue when validating invite codes.

---

## [1.6.2] - 2025-12-19

### 🐛 Bug Fixes
- **Realtime Updates**: Fixed notifications, chores, and per-diem screens not updating immediately after changes.
- **Compiler Warnings**: Cleaned up 80+ unused imports/parameters across the codebase.

### ✨ Improvements
- **Skeleton Loaders**: Replaced circular loading with shimmer skeletons across all screens.
- **Optimistic UI**: Instant feedback when marking notifications read or completing chores.

### 🧹 Code Cleanup
- Removed unused ProductivityDialog.kt
- Fixed empty if blocks and redundant qualifiers.

---

## [1.6.0] - 2025-12-13

### 🐛 Bug Fixes
- **Deep Links**: Fixed crash when clicking invitation links (added missing intent filter).
- **Chores**: 'Complete Chore' now correctly records who completed it and when.
- **Documents**: Fixed key bug where Personal and House documents were not properly isolated.
- **Stability**: Resolved coroutine suspension errors in real-time updates and fixed all build compilation issues.

### 🧹 Maintenance
- **Cleanup**: Removed unused UI components (`FlockrCard`, `FlockrButton`) and redundant screens.
- **Refactoring**: Consolidated utility functions and standardized codebase imports.

---

## [1.5.6] - 2025-12-13

### Bug Fixes
- **Monthly Reports**: Fixed an issue where 'Settlement' transactions were incorrectly shown in the 'Spend by Category' pie chart.
- **Balances**: Resolved a critical bug where settling a balance created a recursive debt cycle.
- **Settlements**: Fixed logic to ensure settled debts are correctly marked as resolved.

### UI Improvements
- **Debt Breakdown**: Completely overhauled the breakdown UI with a cleaner design, including date context and better typography.

---

## [1.5.5] - 2025-12-13

### 🚀 CI/CD & DevOps
- **New Pipeline**: Implemented a production-grade CI/CD pipeline using GitHub Actions.
- **Automation**: Added `release_utils.py` for robust, safe version management and changelog generation.
- **Releases**: Automated GitHub Releases with APK artifacts attached.
- **Single Source of Truth**: All versioning is now driven by `version.properties`.

### ⚡ Optimization
- Enabled resource shrinking for release builds to reduce APK size.
- Optimized Gradle build process with caching.

### 🏗️ Architecture
- **Refactoring**: Massive codebase reorganization for better maintainability (Feature-based structure).
- **Cleanup**: Removed legacy `ci.yml` and redundant configuration files.

---

## [1.5.0] - 2025-11-21
### ✨ Features
- **UI Redesign**: Complete overhaul of the user interface with Material 3.
- **Design System**: Introduced a comprehensive design system for consistency.
- **Multi-Tenancy**: Enhanced support for multiple households.

### 🐛 Fixes
- Fixed notification serialization issues.
- Resolved build errors in `NotificationSerializer`.
- Corrected balance calculation logic in `HouseAuditLog`.

---

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

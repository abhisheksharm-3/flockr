### What's new in 1.9.0

#### Code Quality Refactor
- Extracted `AddExpenseViewModel` — all form state (fields, split logic, timezone-aware date) moved out of the Composable
- Removed all `delay()` anti-patterns from ViewModels; screens now call `reset*State()` after consuming Success events
- Fixed blocking file I/O in `DocumentViewModel` and `HouseSettingsViewModel` — reads now run on `Dispatchers.IO`
- Replaced concrete repository injection with interfaces in `ChatViewModel`, `DocumentViewModel`, and `PerDiemViewModel`
- Removed duplicate state flows (`_spendByMemberState`, `_spendByCategoryState`) from `MonthlySummaryViewModel` — data lives in `MonthlySummaryUiState.Success`
- Fixed all inline backtick-escaped package paths across ViewModels — proper imports throughout
- Converted `object ExpenseCreated` to `data object` and removed dead `fetchHouseByInviteCode` / `loadHouse` wrappers
- Removed AI-generated KDoc and boilerplate inline comments project-wide

#### Theme
- Added M3 surface container hierarchy tokens (`surfaceContainerLowest` → `surfaceContainerHighest`) to both light and dark color schemes
- Added inverse surface tokens (`inverseSurface`, `inverseOnSurface`, `inversePrimary`) to both schemes
- Fixed dark theme surface container values to use the correct layered progression

#### Architecture
- Relocated `HouseSettingsUiState` / `UpdateHouseSettingsUiState` from `settings` package to `house` package where they belong

#### Security
- Standardized role casing across all Supabase RPC functions (Owner/Admin/Member)
- Added `SET search_path TO 'public'` to all `SECURITY DEFINER` functions
- Removed redundant admin-check functions (`is_house_admin_or_owner`, `user_is_admin_in_house`)

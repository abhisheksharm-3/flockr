package `in`.xroden.flockr.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// FOLD.MONEY INSPIRED COLOR PALETTE
// Sophisticated, data-rich design with professional tones
// =============================================================================

// Primary Colors - Deep Blue/Indigo (Professional, trustworthy)
val FoldBlue = Color(0xFF0F172A)               // Deep slate - Primary brand color
val FoldBlueMedium = Color(0xFF1E293B)         // Medium slate for surfaces
val FoldBlueLight = Color(0xFF334155)          // Light slate for elevated surfaces
val FoldAccent = Color(0xFF3B82F6)             // Bright blue for accents and CTAs

// Secondary Colors - Sophisticated Purple
val FoldPurple = Color(0xFF6366F1)             // Indigo for secondary actions
val FoldPurpleLight = Color(0xFF818CF8)        // Light purple for hover states
val FoldPurpleDark = Color(0xFF4F46E5)         // Dark purple for pressed states

// Neutral Colors - Light Mode (Clean, minimal)
val NeutralWhite = Color(0xFFFFFFFF)           // Pure white
val NeutralGray50 = Color(0xFFF8FAFC)          // Off-white background
val NeutralGray100 = Color(0xFFF1F5F9)         // Light gray background
val NeutralGray200 = Color(0xFFE2E8F0)         // Subtle borders
val NeutralGray300 = Color(0xFFCBD5E1)         // Dividers
val NeutralGray400 = Color(0xFF94A3B8)         // Placeholder text
val NeutralGray500 = Color(0xFF64748B)         // Secondary text
val NeutralGray600 = Color(0xFF475569)         // Body text
val NeutralGray700 = Color(0xFF334155)         // Strong text
val NeutralGray800 = Color(0xFF1E293B)         // Heading text
val NeutralGray900 = Color(0xFF0F172A)         // Primary text

// Neutral Colors - Dark Mode (Rich, deep)
val DarkGray950 = Color(0xFF020617)            // Deepest background
val DarkGray900 = Color(0xFF0F172A)            // Dark background
val DarkGray800 = Color(0xFF1E293B)            // Dark surface
val DarkGray700 = Color(0xFF334155)            // Dark elevated surface
val DarkGray600 = Color(0xFF475569)            // Dark borders
val DarkGray500 = Color(0xFF64748B)            // Dark secondary text
val DarkGray400 = Color(0xFF94A3B8)            // Dark body text
val DarkGray300 = Color(0xFFCBD5E1)            // Dark emphasized text
val DarkGray200 = Color(0xFFE2E8F0)            // Dark primary text

// Financial Semantic Colors (Critical for money apps)
val PositiveGreen = Color(0xFF10B981)          // Positive balances, profit
val PositiveGreenLight = Color(0xFFD1FAE5)     // Light green backgrounds
val NegativeRed = Color(0xFFEF4444)            // Negative balances, losses
val NegativeRedLight = Color(0xFFFEE2E2)       // Light red backgrounds
val NeutralBalance = Color(0xFF94A3B8)         // Zero/neutral state

// Semantic Colors
val SuccessGreen = Color(0xFF10B981)           // Success states
val SuccessGreenDark = Color(0xFF059669)       // Dark success
val WarningAmber = Color(0xFFF59E0B)           // Warning states
val WarningAmberDark = Color(0xFFD97706)       // Dark warning
val ErrorRed = Color(0xFFEF4444)               // Error states
val ErrorRedDark = Color(0xFFDC2626)           // Dark error
val InfoBlue = Color(0xFF3B82F6)               // Information states
val InfoBlueDark = Color(0xFF2563EB)           // Dark info

// Category Colors (For expense categories, charts)
val CategoryBlue = Color(0xFF3B82F6)           // Utilities, Services
val CategoryPurple = Color(0xFF8B5CF6)         // Entertainment
val CategoryGreen = Color(0xFF10B981)          // Food, Groceries
val CategoryYellow = Color(0xFFF59E0B)         // Transport
val CategoryPink = Color(0xFFEC4899)           // Shopping
val CategoryOrange = Color(0xFFF97316)         // Rent, Housing
val CategoryTeal = Color(0xFF14B8A6)           // Healthcare
val CategoryIndigo = Color(0xFF6366F1)         // Education

// =============================================================================
// LIGHT THEME - Clean, Data-Rich (fold.money inspired)
// =============================================================================

val Primary = FoldAccent                       // Bright blue for CTAs
val OnPrimary = NeutralWhite
val PrimaryContainer = Color(0xFFDCEAFB)       // Very light blue
val OnPrimaryContainer = FoldBlue              // Deep slate

val Secondary = FoldPurple                     // Sophisticated purple
val OnSecondary = NeutralWhite
val SecondaryContainer = Color(0xFFE0E7FF)     // Very light indigo
val OnSecondaryContainer = Color(0xFF312E81)   // Deep indigo

val Tertiary = CategoryPink                    // Accent pink
val OnTertiary = NeutralWhite
val TertiaryContainer = Color(0xFFFCE7F3)      // Very light pink
val OnTertiaryContainer = Color(0xFF831843)    // Deep pink

val Error = ErrorRed
val OnError = NeutralWhite
val ErrorContainer = NegativeRedLight
val OnErrorContainer = ErrorRedDark

val Background = NeutralGray50                 // Off-white
val OnBackground = NeutralGray900              // Deep slate
val Surface = NeutralWhite
val OnSurface = NeutralGray900
val SurfaceVariant = NeutralGray100            // Light gray
val OnSurfaceVariant = NeutralGray600          // Body text
val Outline = NeutralGray300                   // Subtle dividers

// =============================================================================
// DARK THEME - Rich, Professional (fold.money inspired)
// =============================================================================

val DarkPrimary = FoldAccent                   // Bright blue for visibility
val DarkOnPrimary = DarkGray950
val DarkPrimaryContainer = Color(0xFF1E40AF)   // Deep blue
val DarkOnPrimaryContainer = Color(0xFFDCEAFB) // Light blue

val DarkSecondary = FoldPurpleLight            // Lighter purple for dark mode
val DarkOnSecondary = DarkGray950
val DarkSecondaryContainer = FoldPurpleDark
val DarkOnSecondaryContainer = Color(0xFFE0E7FF)

val DarkTertiary = Color(0xFFF472B6)          // Light pink for visibility
val DarkOnTertiary = DarkGray950
val DarkTertiaryContainer = Color(0xFF9F1239)
val DarkOnTertiaryContainer = Color(0xFFFCE7F3)

val DarkError = Color(0xFFFCA5A5)             // Light red for visibility
val DarkOnError = DarkGray950
val DarkErrorContainer = ErrorRedDark
val DarkOnErrorContainer = NegativeRedLight

val DarkBackground = DarkGray950               // Deepest background
val DarkOnBackground = DarkGray200             // Light text
val DarkSurface = DarkGray900                  // Rich dark surface
val DarkOnSurface = DarkGray200
val DarkSurfaceVariant = DarkGray800           // Elevated surfaces
val DarkOnSurfaceVariant = DarkGray400         // Secondary text
val DarkOutline = DarkGray600                  // Borders

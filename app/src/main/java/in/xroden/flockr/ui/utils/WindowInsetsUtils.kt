package `in`.xroden.flockr.ui.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Utility functions for handling WindowInsets in EdgeToEdge mode
 */
object WindowInsetsUtils {
    
    /**
     * Get system bars top padding
     */
    @Composable
    fun getStatusBarHeight(): Dp {
        return WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    }
    
    /**
     * Get system bars bottom padding (navigation bar)
     */
    @Composable
    fun getNavigationBarHeight(): Dp {
        return WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    }
}


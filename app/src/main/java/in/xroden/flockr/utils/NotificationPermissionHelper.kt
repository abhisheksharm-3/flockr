package `in`.xroden.flockr.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Helper class to handle notification permissions for Android 13+ (API 33+)
 */
object NotificationPermissionHelper {
    
    /**
     * Check if notification permission is granted
     * Always returns true for Android 12 and below
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // No permission needed for Android 12 and below
            true
        }
    }
    
    /**
     * Check if we should show rationale for notification permission
     * Only relevant for Android 13+
     */
    fun shouldShowRationale(activity: ComponentActivity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            false
        }
    }
    
    /**
     * Create a permission launcher for notification permission
     * Call this in onCreate() of your Activity
     * 
     * Example:
     * ```
     * val permissionLauncher = NotificationPermissionHelper.createPermissionLauncher(this) { granted ->
     *     if (granted) {
     *         // Permission granted
     *     } else {
     *         // Permission denied
     *     }
     * }
     * ```
     */
    fun createPermissionLauncher(
        activity: ComponentActivity,
        onResult: (Boolean) -> Unit
    ): ActivityResultLauncher<String> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            onResult(isGranted)
        }
    }
    
    /**
     * Request notification permission
     * Only needed for Android 13+
     */
    fun requestPermission(launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}


/** Asks for the one runtime permission Flockr uses: posting notifications. */
package `in`.xroden.flockr.utils

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/** Must be created before the activity starts, because it registers an activity result launcher. */
class PermissionManager(activity: ComponentActivity) {

    private var onNotificationPermissionResult: ((Boolean) -> Unit)? = null

    private val notificationPermissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        onNotificationPermissionResult?.invoke(isGranted)
    }

    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        onNotificationPermissionResult = onResult
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

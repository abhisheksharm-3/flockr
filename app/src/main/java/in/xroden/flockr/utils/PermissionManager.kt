package `in`.xroden.flockr.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionManager(private val activity: ComponentActivity) {

    private var onNotificationPermissionResult: ((Boolean) -> Unit)? = null
    private var onLocationPermissionResult: ((Boolean) -> Unit)? = null
    private var onStoragePermissionResult: ((Boolean) -> Unit)? = null

    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onNotificationPermissionResult?.invoke(isGranted)
    }

    private val locationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        onLocationPermissionResult?.invoke(granted)
    }

    private val storagePermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onStoragePermissionResult?.invoke(isGranted)
    }

    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onNotificationPermissionResult = onResult
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onResult(true)
        }
    }

    fun requestLocationPermission(onResult: (Boolean) -> Unit) {
        onLocationPermissionResult = onResult
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun requestStoragePermission(onResult: (Boolean) -> Unit) {
        onStoragePermissionResult = onResult
        storagePermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
    }

    companion object {
        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        fun hasStoragePermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

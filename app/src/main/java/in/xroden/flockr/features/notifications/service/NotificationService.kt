package `in`.xroden.flockr.features.notifications.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import `in`.xroden.flockr.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to handle device notifications with channel management
 */
@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NotificationService"
        
        // Notification Channels
        private const val CHANNEL_EXPENSES = "expenses"
        private const val CHANNEL_CHORES = "chores"
        private const val CHANNEL_SHOPPING = "shopping"
        private const val CHANNEL_BILLS = "bills"
        private const val CHANNEL_MEMBERS = "members"
        private const val CHANNEL_DOCUMENTS = "documents"
        private const val CHANNEL_CHAT = "chat"
        private const val CHANNEL_GENERAL = "general"
        
        // Notification IDs (use hash of title+timestamp to ensure uniqueness)
        private const val NOTIFICATION_ID_BASE = 1000
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create all notification channels
     * Must be called before showing any notifications on Android O+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_EXPENSES,
                    "Expenses",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for expense additions and settlements"
                    enableVibration(true)
                },
                
                NotificationChannel(
                    CHANNEL_CHORES,
                    "Chores",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for chore assignments and completions"
                    enableVibration(true)
                },
                
                NotificationChannel(
                    CHANNEL_SHOPPING,
                    "Shopping",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for shopping list updates"
                    enableVibration(false)
                },
                
                NotificationChannel(
                    CHANNEL_BILLS,
                    "Bills",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for recurring bills and payments"
                    enableVibration(true)
                },
                
                NotificationChannel(
                    CHANNEL_MEMBERS,
                    "Members",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifications for member joins and leaves"
                    enableVibration(false)
                },
                
                NotificationChannel(
                    CHANNEL_DOCUMENTS,
                    "Documents",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifications for document uploads"
                    enableVibration(false)
                },
                
                NotificationChannel(
                    CHANNEL_CHAT,
                    "Messages",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for new messages"
                    enableVibration(true)
                },
                
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General notifications"
                    enableVibration(false)
                }
            )
            
            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    /**
     * Show a device notification
     * @param id Unique notification ID
     * @param title Notification title
     * @param message Notification message
     * @param type Notification type (expense, chore, shopping, etc.)
     * @param houseId Optional house ID for navigation
     * @param data Optional extra data for specific actions
     */
    fun showNotification(
        id: String,
        title: String,
        message: String,
        type: String,
        houseId: String? = null,
        data: Map<String, String>? = null
    ) {
        // Check if notifications are enabled at system level
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }

        // Check for runtime permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                 Log.w(TAG, "Notification permission not granted, skipping notification: $title")
                 return
             }
        }
        
        try {
            // Determine channel based on type
            val channelId = when (type.lowercase()) {
                "expense", "settlement", "per_diem" -> CHANNEL_EXPENSES
                "chore", "chore_assigned", "chore_completed" -> CHANNEL_CHORES
                "shopping", "shopping_item" -> CHANNEL_SHOPPING
                "bill", "recurring_bill", "bill_payment" -> CHANNEL_BILLS
                "member", "member_joined", "member_left" -> CHANNEL_MEMBERS
                "document" -> CHANNEL_DOCUMENTS
                "message", "chat" -> CHANNEL_CHAT
                else -> CHANNEL_GENERAL
            }
            
            // Create intent for notification tap
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                
                // Add house ID if available
                houseId?.let {
                    putExtra("house_id", it)
                    putExtra("notification_type", type)
                }
                
                // Add extra data
                data?.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build notification
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(getNotificationIcon(type))
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(getNotificationPriority(type))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            
            // Show notification with unique ID
            val notificationId = NOTIFICATION_ID_BASE + id.hashCode()

            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show notification: ${e.message}", e)
        }
    }

    /**
     * Get notification icon based on type
     * Using R.drawable icons when available, fallback to android.R
     */
    private fun getNotificationIcon(type: String): Int {
        // You can add custom drawable resources here
        // For now, use a system icon that's guaranteed to exist
        return android.R.drawable.ic_dialog_info
    }
    
    /**
     * Get notification priority based on type
     */
    private fun getNotificationPriority(type: String): Int {
        return when (type.lowercase()) {
            "bill", "recurring_bill", "bill_payment" -> NotificationCompat.PRIORITY_HIGH
            "expense", "settlement", "chore_assigned" -> NotificationCompat.PRIORITY_DEFAULT
            else -> NotificationCompat.PRIORITY_LOW
        }
    }
    
    /**
     * Cancel a specific notification
     */
    fun cancelNotification(id: String) {
        runCatching {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = NOTIFICATION_ID_BASE + id.hashCode()
            notificationManager.cancel(notificationId)
        }
    }
    
    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        runCatching {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancelAll()
        }
    }
    
    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        val notificationManager = NotificationManagerCompat.from(context)
        return notificationManager.areNotificationsEnabled()
    }
    
    /**
     * Check if a specific channel is enabled
     */
    fun isChannelEnabled(channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = notificationManager.getNotificationChannel(channelId)
            return channel?.importance != NotificationManager.IMPORTANCE_NONE
        }
        return areNotificationsEnabled()
    }
}

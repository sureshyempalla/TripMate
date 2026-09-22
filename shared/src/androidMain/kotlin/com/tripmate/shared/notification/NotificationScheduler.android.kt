package com.tripmate.shared.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.tripmate.shared.model.Activity
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import kotlinx.coroutines.suspendCancellableCoroutine

private const val CHANNEL_ID = "trip_reminders"
private const val CHANNEL_NAME = "Trip reminders"

class AndroidNotificationScheduler(private val context: Context) : NotificationScheduler {

    init {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        )
        manager?.createNotificationChannel(channel)
    }

    override suspend fun requestPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        // Actual runtime request (ActivityResultContracts.RequestPermission)
        // is triggered from the composable that owns an Activity context —
        // see androidApp/.../MainActivity.kt.
    }

    override suspend fun currentDeviceToken(): String? =
        Firebase.messaging.getToken()

    override fun showLocalReminder(activity: Activity) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Next up: ${activity.title}")
            .setContentText(activity.place?.name ?: activity.notes ?: "")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(activity.id.hashCode(), notification)
    }
}

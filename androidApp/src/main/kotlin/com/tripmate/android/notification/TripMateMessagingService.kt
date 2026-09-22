package com.tripmate.android.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives the "next activity" push sent by the Cloud Function scheduler
 * (see /firebase/functions/index.js). The function sends a data + notification
 * payload with keys: activityId, activityTitle, placeName, deepLink.
 */
class TripMateMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // TODO: persist token to Firestore under users/{uid}/deviceTokens
        // so the Cloud Function scheduler knows where to send pushes.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["activityTitle"] ?: "Next activity"
        val body = message.notification?.body ?: message.data["placeName"] ?: ""

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "trip_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(message.data["activityId"]?.hashCode() ?: 0, notification)
    }
}

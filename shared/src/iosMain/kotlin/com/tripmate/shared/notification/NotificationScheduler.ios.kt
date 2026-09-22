package com.tripmate.shared.notification

import com.tripmate.shared.model.Activity
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UserNotifications.*
import kotlinx.coroutines.suspendCancellableCoroutine

class IosNotificationScheduler : NotificationScheduler {

    override suspend fun requestPermission(): Boolean =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { granted, _ ->
                continuation.resumeWith(Result.success(granted))
            }
        }

    override suspend fun currentDeviceToken(): String? =
        Firebase.messaging.getToken()

    @OptIn(ExperimentalForeignApi::class)
    override fun showLocalReminder(activity: Activity) {
        val content = UNMutableNotificationContent().apply {
            setTitle("Next up: ${activity.title}")
            setBody(activity.place?.name ?: activity.notes ?: "")
            setSound(UNNotificationSound.defaultSound)
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = activity.id,
            content = content,
            trigger = null, // fire immediately; used only as an on-open fallback
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request, withCompletionHandler = null)
    }
}

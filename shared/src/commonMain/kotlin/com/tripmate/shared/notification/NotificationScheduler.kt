package com.tripmate.shared.notification

import com.tripmate.shared.model.Activity

/**
 * Local-device notification scheduling is intentionally NOT what drives the
 * "next activity" push in this app (a server-side Cloud Function scheduler
 * does that, since it has to work even if the app isn't running — see
 * /firebase/functions). This interface is kept for the pieces that *are*
 * legitimately on-device:
 *  - requesting notification permission
 *  - registering the FCM/APNs device token with the backend
 *  - showing a local fallback notification if a push is ever missed
 *    (e.g. device was offline) by checking the next activity on app open
 */
interface NotificationScheduler {
    suspend fun requestPermission(): Boolean
    suspend fun currentDeviceToken(): String?
    fun showLocalReminder(activity: Activity)
}

import UIKit
import FirebaseCore
import FirebaseFirestore
import FirebaseMessaging
import UserNotifications

/// Wires up Firebase + APNs/FCM token registration. Kept as a UIKit
/// AppDelegate (via UIApplicationDelegateAdaptor) rather than SwiftUI's
/// newer App lifecycle alone, since FCM's push-registration callbacks are
/// still delivered through UIApplicationDelegate/UNUserNotificationCenter.
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        configureFirestoreOfflinePersistence()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        application.registerForRemoteNotifications()
        return true
    }

    /// iOS's Firestore SDK also persists to disk by default but caps the
    /// cache at 100MB — matches the Android side's configuration in
    /// TripMateApplication.kt so the whole itinerary survives offline
    /// rather than being evicted under storage pressure. Must run before
    /// any Firestore call anywhere in the app (including the shared KMM
    /// module's listeners, started later from `doInitKoin()`), so this
    /// happens immediately after `FirebaseApp.configure()`.
    private func configureFirestoreOfflinePersistence() {
        let settings = Firestore.firestore().settings
        settings.cacheSettings = PersistentCacheSettings(sizeBytes: NSNumber(value: kFIRFirestoreCacheSizeUnlimited))
        Firestore.firestore().settings = settings
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    // Called whenever FCM (re)issues a token for this device. This is what
    // needs to be written to users/{uid}.deviceTokens in Firestore so the
    // Cloud Function scheduler (firebase/functions/index.js) knows where to
    // send the "next activity" push.
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        // TODO: DeviceTokenRegistrar.shared.register(token: token)
        print("FCM token: \(token)")
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }
}

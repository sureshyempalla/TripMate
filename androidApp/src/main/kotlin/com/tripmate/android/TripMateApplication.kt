package com.tripmate.android

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.tripmate.shared.data.DatabaseDriverFactory
import com.tripmate.shared.di.sharedModule
import com.tripmate.shared.notification.AndroidNotificationScheduler
import com.tripmate.shared.notification.NotificationScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class TripMateApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        configureFirestoreOfflinePersistence()

        val androidModule = module {
            single { DatabaseDriverFactory(get()) }
            single<NotificationScheduler> { AndroidNotificationScheduler(get()) }
        }

        startKoin {
            androidContext(this@TripMateApplication)
            modules(sharedModule, androidModule)
        }
    }

    /**
     * Firestore's Android SDK persists reads/writes to disk by default, but
     * caps the cache at 100MB and can evict older data under pressure. A
     * whole trip's photos-free itinerary data is tiny, so there's no reason
     * to risk eviction mid-trip — set the cache unlimited instead. This must
     * run before the first Firestore call anywhere in the app (including
     * from the shared module's snapshot listeners), so it happens here in
     * Application.onCreate, ahead of startKoin.
     */
    private fun configureFirestoreOfflinePersistence() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
            )
            .build()
    }
}

package com.tripmate.shared.di

import com.tripmate.shared.data.DatabaseDriverFactory
import com.tripmate.shared.notification.IosNotificationScheduler
import com.tripmate.shared.notification.NotificationScheduler
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Called once from Swift (`KoinBootstrapKt.doInitKoin()`) at app launch —
 * see iosApp/TripMate/TripMateApp.swift. Kotlin's `object`/top-level `fun`
 * on iOS is exposed to Swift as `<FileName>Kt.functionName()`.
 */
fun doInitKoin() {
    val iosModule = module {
        single { DatabaseDriverFactory() }
        single<NotificationScheduler> { IosNotificationScheduler() }
    }
    startKoin {
        modules(sharedModule, iosModule)
    }
}

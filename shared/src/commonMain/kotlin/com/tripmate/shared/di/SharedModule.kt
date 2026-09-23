package com.tripmate.shared.di

import app.cash.sqldelight.db.SqlDriver
import com.tripmate.db.TripMateDatabase
import com.tripmate.shared.auth.AuthRepository
import com.tripmate.shared.data.DatabaseDriverFactory
import com.tripmate.shared.data.FirestoreTripRepository
import com.tripmate.shared.data.TripRepository
import com.tripmate.shared.notification.NotificationScheduler
import com.tripmate.shared.viewmodel.AddActivityViewModel
import com.tripmate.shared.viewmodel.BudgetViewModel
import com.tripmate.shared.viewmodel.CreateTripViewModel
import com.tripmate.shared.viewmodel.DocumentWalletViewModel
import com.tripmate.shared.viewmodel.PackingListViewModel
import com.tripmate.shared.viewmodel.TripDetailViewModel
import com.tripmate.shared.viewmodel.TripListViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Wiring shared by both platforms. Each platform supplies [platformModule]
 * with its own [DatabaseDriverFactory] and [NotificationScheduler] `actual`,
 * then calls `startKoin { modules(sharedModule, platformModule) }`.
 */
val sharedModule: Module = module {
    single<SqlDriver> { get<DatabaseDriverFactory>().createDriver() }
    single { TripMateDatabase(get()) }
    single<TripRepository> { FirestoreTripRepository(get()) }
    single { AuthRepository() }

    factory { (userId: String) -> TripListViewModel(get(), userId) }
    factory { (tripId: String) -> TripDetailViewModel(get(), tripId) }
    factory { (tripId: String, activityId: String?) -> AddActivityViewModel(get(), tripId, activityId) }
    factory { (ownerId: String) -> CreateTripViewModel(get(), ownerId) }
    factory { (tripId: String) -> PackingListViewModel(get(), tripId) }
    factory { (tripId: String) -> BudgetViewModel(get(), tripId) }
    factory { (tripId: String) -> DocumentWalletViewModel(get(), tripId) }
}

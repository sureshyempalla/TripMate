package com.tripmate.shared.di

import app.cash.sqldelight.db.SqlDriver
import com.tripmate.db.TripMateDatabase
import com.tripmate.shared.auth.AuthRepository
import com.tripmate.shared.data.ActivityRepository
import com.tripmate.shared.data.BudgetRepository
import com.tripmate.shared.data.DatabaseDriverFactory
import com.tripmate.shared.data.DocumentRepository
import com.tripmate.shared.data.FirestoreActivityRepository
import com.tripmate.shared.data.FirestoreBudgetRepository
import com.tripmate.shared.data.FirestoreDocumentRepository
import com.tripmate.shared.data.FirestorePackingRepository
import com.tripmate.shared.data.FirestoreSyncCoordinator
import com.tripmate.shared.data.FirestoreTripRepository
import com.tripmate.shared.data.PackingRepository
import com.tripmate.shared.data.SyncCoordinator
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
 *
 * One repository interface per feature (see CLAUDE.md's "Adding a feature"
 * recipe) — [ActivityRepository] additionally depends on [TripRepository]
 * for [ActivityRepository.observeNextActivity]'s trip lookup, and
 * [SyncCoordinator] is the one cross-cutting piece every feature's local
 * cache relies on.
 */
val sharedModule: Module = module {
    single<SqlDriver> { get<DatabaseDriverFactory>().createDriver() }
    single { TripMateDatabase(get()) }
    single<TripRepository> { FirestoreTripRepository(get()) }
    single<ActivityRepository> { FirestoreActivityRepository(get(), get()) }
    single<PackingRepository> { FirestorePackingRepository(get()) }
    single<BudgetRepository> { FirestoreBudgetRepository(get()) }
    single<DocumentRepository> { FirestoreDocumentRepository(get()) }
    single<SyncCoordinator> { FirestoreSyncCoordinator(get()) }
    single { AuthRepository() }

    factory { (userId: String) -> TripListViewModel(get(), get(), get(), userId) }
    factory { (tripId: String) -> TripDetailViewModel(get(), get(), get(), tripId) }
    factory { (tripId: String, activityId: String?) -> AddActivityViewModel(get(), tripId, activityId) }
    factory { (ownerId: String) -> CreateTripViewModel(get(), ownerId) }
    factory { (tripId: String) -> PackingListViewModel(get(), tripId) }
    factory { (tripId: String) -> BudgetViewModel(get(), tripId) }
    factory { (tripId: String) -> DocumentWalletViewModel(get(), tripId) }
}

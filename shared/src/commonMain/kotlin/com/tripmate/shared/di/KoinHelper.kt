package com.tripmate.shared.di

import com.tripmate.shared.viewmodel.AddActivityViewModel
import com.tripmate.shared.viewmodel.CreateTripViewModel
import com.tripmate.shared.viewmodel.TripDetailViewModel
import com.tripmate.shared.viewmodel.TripListViewModel
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatformTools

/**
 * Swift can't call Koin's `by inject()`/generic `get()` directly (Kotlin
 * generics don't bridge to Obj-C), so these thin, concretely-typed
 * factory functions are what iosApp calls instead
 * (`KoinHelperKt.tripListViewModel(userId:)` etc. from Swift).
 */
object KoinHelper {
    fun tripListViewModel(userId: String): TripListViewModel =
        KoinPlatformTools.defaultContext().get().get { parametersOf(userId) }

    fun tripDetailViewModel(tripId: String): TripDetailViewModel =
        KoinPlatformTools.defaultContext().get().get { parametersOf(tripId) }

    fun addActivityViewModel(tripId: String): AddActivityViewModel =
        KoinPlatformTools.defaultContext().get().get { parametersOf(tripId) }

    fun createTripViewModel(ownerId: String): CreateTripViewModel =
        KoinPlatformTools.defaultContext().get().get { parametersOf(ownerId) }
}

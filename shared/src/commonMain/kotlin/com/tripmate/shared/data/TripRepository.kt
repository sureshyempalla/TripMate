package com.tripmate.shared.data

import com.tripmate.shared.model.Trip
import kotlinx.coroutines.flow.Flow

/**
 * CRUD + observe for [Trip] itself. Reads are Flows backed by the local DB
 * so the UI updates immediately from cache and again when a Firestore
 * snapshot listener (see [SyncCoordinator]) writes through it. Writes go to
 * Firestore first (source of truth) and rely on the snapshot listener to
 * update the local cache — this avoids a class of bugs where local and
 * remote quietly disagree after a failed write.
 */
interface TripRepository {
    fun observeTrips(userId: String): Flow<List<Trip>>
    fun observeTrip(tripId: String): Flow<Trip?>

    suspend fun createTrip(trip: Trip)
    suspend fun updateTrip(trip: Trip)
    suspend fun deleteTrip(tripId: String)
}

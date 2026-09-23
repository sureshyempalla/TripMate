package com.tripmate.shared.data

import com.tripmate.db.TripEntity
import com.tripmate.db.TripMateDatabase
import com.tripmate.shared.model.Trip
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class FirestoreTripRepository(
    private val db: TripMateDatabase,
) : TripRepository {

    private val firestore get() = Firebase.firestore

    override fun observeTrips(userId: String): Flow<List<Trip>> =
        db.tripQueries.selectTripsByOwner(userId)
            .asFlowList { it.toDomain() }

    override fun observeTrip(tripId: String): Flow<Trip?> =
        db.tripQueries.selectTripById(tripId)
            .asFlowOneOrNull { it.toDomain() }

    override suspend fun createTrip(trip: Trip) {
        firestore.collection(FirestoreCollections.TRIPS).document(trip.id).set(trip)
    }

    override suspend fun updateTrip(trip: Trip) {
        firestore.collection(FirestoreCollections.TRIPS).document(trip.id).set(trip)
    }

    override suspend fun deleteTrip(tripId: String) {
        firestore.collection(FirestoreCollections.TRIPS).document(tripId).delete()
        db.tripQueries.deleteTrip(tripId)
    }
}

fun TripEntity.toDomain() = Trip(
    id = id,
    ownerId = ownerId,
    title = title,
    destination = destination,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    coverImageUrl = coverImageUrl,
    createdAtEpochMillis = createdAtEpochMillis,
)

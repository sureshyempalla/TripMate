package com.tripmate.shared.data

import com.tripmate.db.ActivityEntity
import com.tripmate.db.TripMateDatabase
import com.tripmate.shared.model.Activity
import com.tripmate.shared.model.ActivityCategory
import com.tripmate.shared.model.Place
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [tripRepository] is only used for [observeNextActivity], which needs the
 * signed-in user's trip ids before it can look up their activities — this is
 * the one place activity and trip data genuinely cross, so it's composed in
 * rather than duplicating trip-ownership logic here.
 */
class FirestoreActivityRepository(
    private val db: TripMateDatabase,
    private val tripRepository: TripRepository,
) : ActivityRepository {

    private val firestore get() = Firebase.firestore

    override fun observeActivities(tripId: String): Flow<List<Activity>> =
        db.activityQueries.selectActivitiesByTrip(tripId)
            .asFlowList { it.toDomain() }

    override fun observeNextActivity(userId: String): Flow<Activity?> =
        tripRepository.observeTrips(userId).map { trips ->
            val now = com.tripmate.shared.util.currentEpochMillis()
            trips.flatMap { trip ->
                db.activityQueries.selectActivitiesByTrip(trip.id).executeAsList()
            }
                .filter { it.startAtEpochMillis >= now }
                .minByOrNull { it.startAtEpochMillis }
                ?.toDomain()
        }

    override suspend fun createActivity(activity: Activity) {
        firestore.collection(FirestoreCollections.ACTIVITIES).document(activity.id).set(activity)
    }

    override suspend fun updateActivity(activity: Activity) {
        firestore.collection(FirestoreCollections.ACTIVITIES).document(activity.id).set(activity)
    }

    override suspend fun deleteActivity(activityId: String) {
        firestore.collection(FirestoreCollections.ACTIVITIES).document(activityId).delete()
        db.activityQueries.deleteActivity(activityId)
    }
}

fun ActivityEntity.toDomain() = Activity(
    id = id,
    tripId = tripId,
    title = title,
    category = ActivityCategory.valueOf(category),
    place = placeName?.let {
        Place(
            name = it,
            address = placeAddress,
            latitude = placeLatitude,
            longitude = placeLongitude,
            providerPlaceId = placeProviderPlaceId,
        )
    },
    startAtEpochMillis = startAtEpochMillis,
    endAtEpochMillis = endAtEpochMillis,
    notes = notes,
    reminderLeadMinutes = reminderLeadMinutes.toInt(),
)

internal fun upsertActivityEntity(db: TripMateDatabase, activity: Activity) {
    db.activityQueries.upsertActivity(
        activity.id, activity.tripId, activity.title, activity.category.name,
        activity.place?.name, activity.place?.address,
        activity.place?.latitude, activity.place?.longitude, activity.place?.providerPlaceId,
        activity.startAtEpochMillis, activity.endAtEpochMillis, activity.notes,
        activity.reminderLeadMinutes.toLong(),
    )
}

package com.tripmate.shared.data

import com.tripmate.db.ActivityEntity
import com.tripmate.db.TripEntity
import com.tripmate.shared.model.Activity
import com.tripmate.shared.model.ActivityCategory
import com.tripmate.shared.model.Place
import com.tripmate.shared.model.Trip
import kotlinx.datetime.LocalDate

// Mapping between the generated SQLDelight row classes and the plain
// domain models the rest of the app (ViewModels, UI, Firestore layer) uses.
// Keeping this in one file means a column rename only breaks compilation
// here, not scattered across call sites.

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

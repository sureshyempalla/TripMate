package com.tripmate.shared.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * A single trip, e.g. "Japan 2026". Owns a flat list of [Activity] items;
 * the day-by-day grouping shown in the UI (Timeline) is derived from each
 * activity's date rather than stored as a separate hierarchy, so moving an
 * activity to another day is just an edit, not a re-parent.
 */
@Serializable
data class Trip(
    val id: String,
    val ownerId: String,
    val title: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val coverImageUrl: String? = null,
    val createdAtEpochMillis: Long = 0L,
)

@Serializable
data class Place(
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    // Opaque id from whichever places-search provider is wired up
    // (e.g. Google Places). Kept optional so activities can be created
    // with a free-text location before that integration exists.
    val providerPlaceId: String? = null,
)

enum class ActivityCategory {
    FLIGHT, LODGING, FOOD, SIGHTSEEING, TRANSPORT, ACTIVITY, OTHER
}

@Serializable
data class Activity(
    val id: String,
    val tripId: String,
    val title: String,
    val category: ActivityCategory,
    val place: Place? = null,
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long? = null,
    val notes: String? = null,
    // Minutes before startAtEpochMillis that the "next activity" push
    // should fire. Defaults to 30; user-adjustable per activity.
    val reminderLeadMinutes: Int = 30,
)

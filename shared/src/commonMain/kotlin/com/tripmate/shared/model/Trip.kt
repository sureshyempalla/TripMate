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

enum class PackingCategory { DOCUMENTS, CLOTHING, TOILETRIES, ELECTRONICS, HEALTH, OTHER }

@Serializable
data class PackingItem(
    val id: String,
    val tripId: String,
    val name: String,
    val category: PackingCategory = PackingCategory.OTHER,
    val isPacked: Boolean = false,
)

enum class ExpenseCategory { LODGING, FOOD, TRANSPORT, ACTIVITIES, SHOPPING, OTHER }

/**
 * A single spend logged against a trip. Amounts are stored in minor units
 * (cents) as a Long to avoid floating-point rounding when summing totals —
 * the UI divides by 100 only at display time.
 */
@Serializable
data class Expense(
    val id: String,
    val tripId: String,
    val title: String,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val spentAtEpochMillis: Long,
    val notes: String? = null,
)

enum class DocumentType { PASSPORT, BOARDING_PASS, CONFIRMATION, INSURANCE, OTHER }

/**
 * A trip document (passport photo, boarding pass, hotel confirmation...).
 * [localUri] points at a copy in this device's app-private storage (the
 * same local-first pattern as [Trip.coverImageUrl]) — it is not a
 * cross-device file store, just an offline-accessible wallet per device.
 */
@Serializable
data class TripDocument(
    val id: String,
    val tripId: String,
    val title: String,
    val type: DocumentType = DocumentType.OTHER,
    val localUri: String,
    val addedAtEpochMillis: Long = 0L,
)

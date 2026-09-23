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

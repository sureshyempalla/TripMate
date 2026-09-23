package com.tripmate.shared.model

import kotlinx.serialization.Serializable

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

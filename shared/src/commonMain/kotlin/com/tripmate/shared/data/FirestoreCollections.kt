package com.tripmate.shared.data

/**
 * Firestore collection names, shared by every per-feature repository and by
 * [FirestoreSyncCoordinator]. Kept in one place so a rename (or a new
 * feature's collection) only needs updating here — and so it's the one file
 * to cross-check against `firebase/firestore.rules` when adding a feature.
 */
internal object FirestoreCollections {
    const val TRIPS = "trips"
    const val ACTIVITIES = "activities"
    const val PACKING_ITEMS = "packing_items"
    const val EXPENSES = "expenses"
    const val DOCUMENTS = "documents"
}

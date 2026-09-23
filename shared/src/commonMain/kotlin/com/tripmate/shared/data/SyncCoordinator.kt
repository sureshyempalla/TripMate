package com.tripmate.shared.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Whether the data the UI is showing came from a live Firestore listener or
 * from the on-device cache while there's no connection. Firestore's mobile
 * SDKs queue writes locally and sync them once connectivity returns
 * regardless of this value — [SyncStatus] exists purely so the UI can be
 * honest with the person about which one is happening, since neither major
 * competitor (TripIt, Wanderlog) surfaces this clearly, and Wanderlog gates
 * full offline behavior behind a paid tier.
 */
enum class SyncStatus { SYNCING, SYNCED, OFFLINE }

/**
 * Owns the Firestore snapshot listeners that feed every feature's SQLDelight
 * cache for the signed-in user. This is deliberately separate from the
 * per-feature repositories (which only do that feature's CRUD + observe):
 * sync is a single cross-cutting session lifecycle, not something that
 * belongs to any one feature, so it gets its own small interface instead of
 * bloating [TripRepository] or living awkwardly on one arbitrary feature.
 */
interface SyncCoordinator {
    /** Reflects whether the last Firestore snapshot came from the server or the local cache. */
    val syncStatus: StateFlow<SyncStatus>

    /** Starts/refreshes every collection's Firestore listener for this user. */
    suspend fun startSync(userId: String)
    suspend fun stopSync()
}

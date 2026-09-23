package com.tripmate.shared.data

import com.tripmate.db.TripMateDatabase
import com.tripmate.shared.model.Activity
import com.tripmate.shared.model.Expense
import com.tripmate.shared.model.PackingItem
import com.tripmate.shared.model.Trip
import com.tripmate.shared.model.TripDocument
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Attaches Firestore snapshot listeners for the given user's trips (and
 * their activities, packing items, expenses and documents) and writes every
 * snapshot straight into the local DB — every feature repository then
 * observes *only* the DB, never Firestore directly. See [SyncCoordinator]
 * for why this lives separately from the per-feature repositories.
 */
class FirestoreSyncCoordinator(
    private val db: TripMateDatabase,
) : SyncCoordinator {

    private val firestore get() = Firebase.firestore
    private var syncScope: CoroutineScope? = null

    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCING)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    override suspend fun startSync(userId: String) {
        stopSync()
        val scope = CoroutineScope(SupervisorJob())
        syncScope = scope

        scope.launch {
            // includeMetadataChanges: true so the cache-to-server transition
            // (isFromCache flips false with no data change) re-emits and
            // syncStatus updates to SYNCED — without it this listener only
            // fires on actual document changes and the UI stays stuck
            // showing "offline" after the very first (cache) snapshot.
            firestore.collection(FirestoreCollections.TRIPS)
                .where { "ownerId" equalTo userId }
                .snapshots(includeMetadataChanges = true)
                .catch { _syncStatus.value = SyncStatus.OFFLINE }
                .collect { snapshot ->
                    snapshot.documents.forEach { doc ->
                        val trip = doc.data<Trip>()
                        db.tripQueries.upsertTrip(
                            id = trip.id,
                            ownerId = trip.ownerId,
                            title = trip.title,
                            destination = trip.destination,
                            startDate = trip.startDate.toString(),
                            endDate = trip.endDate.toString(),
                            coverImageUrl = trip.coverImageUrl,
                            createdAtEpochMillis = trip.createdAtEpochMillis,
                        )
                    }
                    _syncStatus.value = if (snapshot.metadata.isFromCache) {
                        SyncStatus.OFFLINE
                    } else {
                        SyncStatus.SYNCED
                    }
                }
        }

        scope.launch {
            // Re-subscribes whenever the user's trip ids change, rather than
            // snapshotting them once at startSync time — the trips listener
            // above hasn't necessarily populated the local DB yet when this
            // runs, so freezing the id list here would silently sync zero
            // activities on a fresh sign-in.
            var activitiesJob: Job? = null
            db.tripQueries.selectTripsByOwner(userId).asFlowList { it.id }.collect { tripIds ->
                activitiesJob?.cancel()
                if (tripIds.isEmpty()) return@collect
                activitiesJob = launch {
                    firestore.collection(FirestoreCollections.ACTIVITIES)
                        .where { "tripId" inArray tripIds }
                        .snapshots
                        // A denied/failed listen here shouldn't take down the
                        // whole sync — trips still sync fine without it.
                        .catch { }
                        .collect { snapshot ->
                            snapshot.documents.forEach { doc ->
                                upsertActivityEntity(db, doc.data<Activity>())
                            }
                        }
                }
            }
        }

        scope.launch {
            var packingJob: Job? = null
            db.tripQueries.selectTripsByOwner(userId).asFlowList { it.id }.collect { tripIds ->
                packingJob?.cancel()
                if (tripIds.isEmpty()) return@collect
                packingJob = launch {
                    firestore.collection(FirestoreCollections.PACKING_ITEMS)
                        .where { "tripId" inArray tripIds }
                        .snapshots
                        .catch { }
                        .collect { snapshot ->
                            snapshot.documents.forEach { doc ->
                                upsertPackingItemEntity(db, doc.data<PackingItem>())
                            }
                        }
                }
            }
        }

        scope.launch {
            var expensesJob: Job? = null
            db.tripQueries.selectTripsByOwner(userId).asFlowList { it.id }.collect { tripIds ->
                expensesJob?.cancel()
                if (tripIds.isEmpty()) return@collect
                expensesJob = launch {
                    firestore.collection(FirestoreCollections.EXPENSES)
                        .where { "tripId" inArray tripIds }
                        .snapshots
                        .catch { }
                        .collect { snapshot ->
                            snapshot.documents.forEach { doc ->
                                upsertExpenseEntity(db, doc.data<Expense>())
                            }
                        }
                }
            }
        }

        scope.launch {
            var documentsJob: Job? = null
            db.tripQueries.selectTripsByOwner(userId).asFlowList { it.id }.collect { tripIds ->
                documentsJob?.cancel()
                if (tripIds.isEmpty()) return@collect
                documentsJob = launch {
                    firestore.collection(FirestoreCollections.DOCUMENTS)
                        .where { "tripId" inArray tripIds }
                        .snapshots
                        .catch { }
                        .collect { snapshot ->
                            snapshot.documents.forEach { doc ->
                                upsertDocumentEntity(db, doc.data<TripDocument>())
                            }
                        }
                }
            }
        }
    }

    override suspend fun stopSync() {
        syncScope?.cancel()
        syncScope = null
    }
}

private fun CoroutineScope.cancel() {
    (coroutineContext[Job])?.cancel()
}

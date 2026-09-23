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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TRIPS_COLLECTION = "trips"
private const val ACTIVITIES_COLLECTION = "activities"
private const val PACKING_ITEMS_COLLECTION = "packing_items"
private const val EXPENSES_COLLECTION = "expenses"
private const val DOCUMENTS_COLLECTION = "documents"

/**
 * Firestore is the source of truth; SQLDelight is a read cache so the UI has
 * something to show offline and on cold start before the first snapshot
 * arrives. [startSync] attaches Firestore snapshot listeners for the given
 * user's trips (and their activities) and writes every snapshot straight
 * into the local DB — screens then observe *only* the DB via
 * [observeTrips]/[observeActivities], never Firestore directly.
 */
class FirestoreTripRepository(
    private val db: TripMateDatabase,
) : TripRepository {

    private val firestore get() = Firebase.firestore
    private var syncScope: CoroutineScope? = null

    private val _syncStatus = MutableStateFlow(SyncStatus.SYNCING)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    override fun observeTrips(userId: String): Flow<List<Trip>> =
        db.tripQueries.selectTripsByOwner(userId)
            .asFlowList { it.toDomain() }

    override fun observeTrip(tripId: String): Flow<Trip?> =
        db.tripQueries.selectTripById(tripId)
            .asFlowOneOrNull { it.toDomain() }

    override fun observeActivities(tripId: String): Flow<List<Activity>> =
        db.activityQueries.selectActivitiesByTrip(tripId)
            .asFlowList { it.toDomain() }

    override fun observeNextActivity(userId: String): Flow<Activity?> =
        observeTrips(userId).map { trips ->
            val now = com.tripmate.shared.util.currentEpochMillis()
            trips.flatMap { trip ->
                db.activityQueries.selectActivitiesByTrip(trip.id).executeAsList()
            }
                .filter { it.startAtEpochMillis >= now }
                .minByOrNull { it.startAtEpochMillis }
                ?.toDomain()
        }

    override suspend fun createTrip(trip: Trip) {
        firestore.collection(TRIPS_COLLECTION).document(trip.id).set(trip)
    }

    override suspend fun updateTrip(trip: Trip) {
        firestore.collection(TRIPS_COLLECTION).document(trip.id).set(trip)
    }

    override suspend fun deleteTrip(tripId: String) {
        firestore.collection(TRIPS_COLLECTION).document(tripId).delete()
        db.tripQueries.deleteTrip(tripId)
    }

    override suspend fun createActivity(activity: Activity) {
        firestore.collection(ACTIVITIES_COLLECTION).document(activity.id).set(activity)
    }

    override suspend fun updateActivity(activity: Activity) {
        firestore.collection(ACTIVITIES_COLLECTION).document(activity.id).set(activity)
    }

    override suspend fun deleteActivity(activityId: String) {
        firestore.collection(ACTIVITIES_COLLECTION).document(activityId).delete()
        db.activityQueries.deleteActivity(activityId)
    }

    override fun observePackingItems(tripId: String): Flow<List<PackingItem>> =
        db.packingItemQueries.selectPackingItemsByTrip(tripId).asFlowList { it.toDomain() }

    override suspend fun createPackingItem(item: PackingItem) {
        firestore.collection(PACKING_ITEMS_COLLECTION).document(item.id).set(item)
    }

    override suspend fun updatePackingItem(item: PackingItem) {
        firestore.collection(PACKING_ITEMS_COLLECTION).document(item.id).set(item)
    }

    override suspend fun deletePackingItem(itemId: String) {
        firestore.collection(PACKING_ITEMS_COLLECTION).document(itemId).delete()
        db.packingItemQueries.deletePackingItem(itemId)
    }

    override fun observeExpenses(tripId: String): Flow<List<Expense>> =
        db.expenseQueries.selectExpensesByTrip(tripId).asFlowList { it.toDomain() }

    override suspend fun createExpense(expense: Expense) {
        firestore.collection(EXPENSES_COLLECTION).document(expense.id).set(expense)
    }

    override suspend fun updateExpense(expense: Expense) {
        firestore.collection(EXPENSES_COLLECTION).document(expense.id).set(expense)
    }

    override suspend fun deleteExpense(expenseId: String) {
        firestore.collection(EXPENSES_COLLECTION).document(expenseId).delete()
        db.expenseQueries.deleteExpense(expenseId)
    }

    override fun observeDocuments(tripId: String): Flow<List<TripDocument>> =
        db.tripDocumentQueries.selectDocumentsByTrip(tripId).asFlowList { it.toDomain() }

    override suspend fun createDocument(document: TripDocument) {
        firestore.collection(DOCUMENTS_COLLECTION).document(document.id).set(document)
    }

    override suspend fun deleteDocument(documentId: String) {
        firestore.collection(DOCUMENTS_COLLECTION).document(documentId).delete()
        db.tripDocumentQueries.deleteDocument(documentId)
    }

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
            firestore.collection(TRIPS_COLLECTION)
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
                    firestore.collection(ACTIVITIES_COLLECTION)
                        .where { "tripId" inArray tripIds }
                        .snapshots
                        // A denied/failed listen here shouldn't take down the
                        // whole sync — trips still sync fine without it.
                        .catch { }
                        .collect { snapshot ->
                            snapshot.documents.forEach { doc ->
                                val activity = doc.data<Activity>()
                                upsertActivityEntity(activity)
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
                    firestore.collection(PACKING_ITEMS_COLLECTION)
                        .where { "tripId" inArray tripIds }
                        .snapshots
                        .catch { }
                        .collect { snapshot ->
                            snapshot.documents.forEach { doc ->
                                val item = doc.data<PackingItem>()
                                db.packingItemQueries.upsertPackingItem(
                                    item.id, item.tripId, item.name, item.category.name,
                                    if (item.isPacked) 1L else 0L,
                                )
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
                    firestore.collection(EXPENSES_COLLECTION)
                        .where { "tripId" inArray tripIds }
                        .snapshots
                        .catch { }
                        .collect { snapshot ->
                            snapshot.documents.forEach { doc ->
                                val expense = doc.data<Expense>()
                                db.expenseQueries.upsertExpense(
                                    expense.id, expense.tripId, expense.title, expense.amountMinorUnits,
                                    expense.currencyCode, expense.category.name, expense.spentAtEpochMillis, expense.notes,
                                )
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
                    firestore.collection(DOCUMENTS_COLLECTION)
                        .where { "tripId" inArray tripIds }
                        .snapshots
                        .catch { }
                        .collect { snapshot ->
                            snapshot.documents.forEach { doc ->
                                val document = doc.data<TripDocument>()
                                db.tripDocumentQueries.upsertDocument(
                                    document.id, document.tripId, document.title,
                                    document.type.name, document.localUri, document.addedAtEpochMillis,
                                )
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

    private fun upsertActivityEntity(activity: Activity) {
        db.activityQueries.upsertActivity(
            activity.id, activity.tripId, activity.title, activity.category.name,
            activity.place?.name, activity.place?.address,
            activity.place?.latitude, activity.place?.longitude, activity.place?.providerPlaceId,
            activity.startAtEpochMillis, activity.endAtEpochMillis, activity.notes,
            activity.reminderLeadMinutes.toLong(),
        )
    }
}

private fun CoroutineScope.cancel() {
    (coroutineContext[Job])?.cancel()
}

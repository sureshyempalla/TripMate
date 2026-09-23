package com.tripmate.shared.data

import com.tripmate.shared.model.Activity
import com.tripmate.shared.model.Expense
import com.tripmate.shared.model.PackingItem
import com.tripmate.shared.model.Trip
import com.tripmate.shared.model.TripDocument
import kotlinx.coroutines.flow.Flow
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
 * Single source of truth the ViewModels talk to. Implementations decide how
 * local cache (SQLDelight) and remote (Firestore) reconcile; callers never
 * touch either directly.
 *
 * Reads are Flows backed by the local DB so the UI updates immediately from
 * cache and again when a Firestore snapshot listener writes through it.
 * Writes go to Firestore first (source of truth) and rely on the snapshot
 * listener to update the local cache — this avoids a class of bugs where
 * local and remote quietly disagree after a failed write.
 */
interface TripRepository {
    /** Reflects whether the last Firestore snapshot came from the server or the local cache. */
    val syncStatus: StateFlow<SyncStatus>

    fun observeTrips(userId: String): Flow<List<Trip>>
    fun observeTrip(tripId: String): Flow<Trip?>
    fun observeActivities(tripId: String): Flow<List<Activity>>

    /** The single next-upcoming activity across all of the user's active trips. */
    fun observeNextActivity(userId: String): Flow<Activity?>

    suspend fun createTrip(trip: Trip)
    suspend fun updateTrip(trip: Trip)
    suspend fun deleteTrip(tripId: String)

    suspend fun createActivity(activity: Activity)
    suspend fun updateActivity(activity: Activity)
    suspend fun deleteActivity(activityId: String)

    fun observePackingItems(tripId: String): Flow<List<PackingItem>>
    suspend fun createPackingItem(item: PackingItem)
    suspend fun updatePackingItem(item: PackingItem)
    suspend fun deletePackingItem(itemId: String)

    fun observeExpenses(tripId: String): Flow<List<Expense>>
    suspend fun createExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expenseId: String)

    fun observeDocuments(tripId: String): Flow<List<TripDocument>>
    suspend fun createDocument(document: TripDocument)
    suspend fun deleteDocument(documentId: String)

    /** Starts/refreshes the Firestore listeners that feed the local cache for this user. */
    suspend fun startSync(userId: String)
    suspend fun stopSync()
}

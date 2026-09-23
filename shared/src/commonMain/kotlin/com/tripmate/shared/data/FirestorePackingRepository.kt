package com.tripmate.shared.data

import com.tripmate.db.PackingItemEntity
import com.tripmate.db.TripMateDatabase
import com.tripmate.shared.model.PackingCategory
import com.tripmate.shared.model.PackingItem
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow

class FirestorePackingRepository(
    private val db: TripMateDatabase,
) : PackingRepository {

    private val firestore get() = Firebase.firestore

    override fun observePackingItems(tripId: String): Flow<List<PackingItem>> =
        db.packingItemQueries.selectPackingItemsByTrip(tripId).asFlowList { it.toDomain() }

    override suspend fun createPackingItem(item: PackingItem) {
        firestore.collection(FirestoreCollections.PACKING_ITEMS).document(item.id).set(item)
    }

    override suspend fun updatePackingItem(item: PackingItem) {
        firestore.collection(FirestoreCollections.PACKING_ITEMS).document(item.id).set(item)
    }

    override suspend fun deletePackingItem(itemId: String) {
        firestore.collection(FirestoreCollections.PACKING_ITEMS).document(itemId).delete()
        db.packingItemQueries.deletePackingItem(itemId)
    }
}

fun PackingItemEntity.toDomain() = PackingItem(
    id = id,
    tripId = tripId,
    name = name,
    category = PackingCategory.valueOf(category),
    isPacked = isPacked == 1L,
)

internal fun upsertPackingItemEntity(db: TripMateDatabase, item: PackingItem) {
    db.packingItemQueries.upsertPackingItem(
        item.id, item.tripId, item.name, item.category.name,
        if (item.isPacked) 1L else 0L,
    )
}

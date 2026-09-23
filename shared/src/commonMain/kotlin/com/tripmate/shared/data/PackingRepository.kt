package com.tripmate.shared.data

import com.tripmate.shared.model.PackingItem
import kotlinx.coroutines.flow.Flow

/** CRUD + observe for a trip's packing checklist. */
interface PackingRepository {
    fun observePackingItems(tripId: String): Flow<List<PackingItem>>
    suspend fun createPackingItem(item: PackingItem)
    suspend fun updatePackingItem(item: PackingItem)
    suspend fun deletePackingItem(itemId: String)
}

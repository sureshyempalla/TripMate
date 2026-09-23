package com.tripmate.shared.data

import com.tripmate.shared.model.TripDocument
import kotlinx.coroutines.flow.Flow

/** CRUD + observe for a trip's offline document wallet. */
interface DocumentRepository {
    fun observeDocuments(tripId: String): Flow<List<TripDocument>>
    suspend fun createDocument(document: TripDocument)
    suspend fun deleteDocument(documentId: String)
}

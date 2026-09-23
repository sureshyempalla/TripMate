package com.tripmate.shared.data

import com.tripmate.db.TripDocumentEntity
import com.tripmate.db.TripMateDatabase
import com.tripmate.shared.model.DocumentType
import com.tripmate.shared.model.TripDocument
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow

class FirestoreDocumentRepository(
    private val db: TripMateDatabase,
) : DocumentRepository {

    private val firestore get() = Firebase.firestore

    override fun observeDocuments(tripId: String): Flow<List<TripDocument>> =
        db.tripDocumentQueries.selectDocumentsByTrip(tripId).asFlowList { it.toDomain() }

    override suspend fun createDocument(document: TripDocument) {
        firestore.collection(FirestoreCollections.DOCUMENTS).document(document.id).set(document)
    }

    override suspend fun deleteDocument(documentId: String) {
        firestore.collection(FirestoreCollections.DOCUMENTS).document(documentId).delete()
        db.tripDocumentQueries.deleteDocument(documentId)
    }
}

fun TripDocumentEntity.toDomain() = TripDocument(
    id = id,
    tripId = tripId,
    title = title,
    type = DocumentType.valueOf(type),
    localUri = localUri,
    addedAtEpochMillis = addedAtEpochMillis,
)

internal fun upsertDocumentEntity(db: TripMateDatabase, document: TripDocument) {
    db.tripDocumentQueries.upsertDocument(
        document.id, document.tripId, document.title,
        document.type.name, document.localUri, document.addedAtEpochMillis,
    )
}

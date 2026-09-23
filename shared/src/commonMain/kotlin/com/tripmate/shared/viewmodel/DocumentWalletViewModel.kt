package com.tripmate.shared.viewmodel

import com.tripmate.shared.data.DocumentRepository
import com.tripmate.shared.model.DocumentType
import com.tripmate.shared.model.TripDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class DocumentWalletUiState(
    val isLoading: Boolean = true,
    val documents: List<TripDocument> = emptyList(),
)

/**
 * Screen state for a trip's offline document wallet. [localUri] values are
 * device-local (the UI layer copies the picked file into app-private
 * storage before calling [addDocument], the same pattern as the trip
 * cover-photo picker) — this view model just threads the resulting path
 * through, it doesn't touch the filesystem itself.
 */
class DocumentWalletViewModel(
    private val repository: DocumentRepository,
    private val tripId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _uiState = MutableStateFlow(DocumentWalletUiState())
    val uiState: StateFlow<DocumentWalletUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            repository.observeDocuments(tripId).collect { documents ->
                _uiState.value = DocumentWalletUiState(
                    isLoading = false,
                    documents = documents.sortedByDescending { it.addedAtEpochMillis },
                )
            }
        }
    }

    fun addDocument(title: String, type: DocumentType, localUri: String) {
        if (title.isBlank() || localUri.isBlank()) return
        scope.launch {
            repository.createDocument(
                TripDocument(
                    id = "document_${currentEpochMillisSafe()}_${Random.nextInt(1000, 9999)}",
                    tripId = tripId,
                    title = title.trim(),
                    type = type,
                    localUri = localUri,
                    addedAtEpochMillis = currentEpochMillisSafe(),
                )
            )
        }
    }

    fun deleteDocument(documentId: String) = scope.launch {
        repository.deleteDocument(documentId)
    }
}

private fun currentEpochMillisSafe() = com.tripmate.shared.util.currentEpochMillis()

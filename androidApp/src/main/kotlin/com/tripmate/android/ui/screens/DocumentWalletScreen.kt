package com.tripmate.android.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tripmate.shared.model.DocumentType
import com.tripmate.shared.model.TripDocument
import com.tripmate.shared.viewmodel.DocumentWalletViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import java.io.File

/**
 * A trip's offline document wallet: passports, boarding passes, hotel
 * confirmations and the like, each copied into app-private storage on pick
 * (the same local-first pattern as the trip cover photo) so they stay
 * accessible without a connection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentWalletScreen(tripId: String, onBack: () -> Unit) {
    val viewModel = koinInject<DocumentWalletViewModel>(parameters = { parametersOf(tripId) })
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingUri = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Documents") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { pickerLauncher.launch(arrayOf("image/*", "application/pdf")) }) {
                Icon(Icons.Filled.UploadFile, contentDescription = "Add document")
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.documents.isEmpty()) {
            EmptyDocumentsState(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                items(state.documents, key = { it.id }) { document ->
                    DocumentCard(document = document, onDelete = { viewModel.deleteDocument(document.id) })
                }
            }
        }

        val uri = pendingUri
        if (uri != null) {
            AddDocumentDialog(
                onDismiss = { pendingUri = null },
                onSave = { title, type ->
                    coroutineScope.launch {
                        val localUri = withContext(Dispatchers.IO) { copyDocumentToAppStorage(context, uri) }
                        if (localUri != null) viewModel.addDocument(title, type, localUri)
                        pendingUri = null
                    }
                },
            )
        }
    }
}

@Composable
private fun DocumentCard(document: TripDocument, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                iconForDocumentType(document.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 14.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(document.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    document.type.name.lowercase().replaceFirstChar { it.uppercase() }.replace('_', ' '),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete ${document.title}",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

private fun iconForDocumentType(type: DocumentType): ImageVector = when (type) {
    DocumentType.PASSPORT -> Icons.Filled.FlightTakeoff
    DocumentType.BOARDING_PASS -> Icons.Filled.ConfirmationNumber
    DocumentType.CONFIRMATION -> Icons.Filled.Description
    DocumentType.INSURANCE -> Icons.Filled.HealthAndSafety
    DocumentType.OTHER -> Icons.Filled.InsertDriveFile
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDocumentDialog(onDismiss: () -> Unit, onSave: (title: String, type: DocumentType) -> Unit) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(DocumentType.OTHER) }
    var menuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add document") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(expanded = menuExpanded, onExpandedChange = { menuExpanded = it }) {
                    OutlinedTextField(
                        value = type.name.lowercase().replaceFirstChar { it.uppercase() }.replace('_', ' '),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DocumentType.entries.forEach { entry ->
                            DropdownMenuItem(
                                text = { Text(entry.name.lowercase().replaceFirstChar { it.uppercase() }.replace('_', ' ')) },
                                onClick = {
                                    type = entry
                                    menuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, type) }, enabled = title.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun EmptyDocumentsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.InsertDriveFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(40.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("No documents yet", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Add your passport, boarding passes, or hotel confirmations to access them offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

/**
 * [ActivityResultContracts.OpenDocument] only grants a transient read
 * permission on its returned [uri], so we copy the bytes into app-private
 * storage right away — the same local-first pattern as the trip cover photo.
 */
private fun copyDocumentToAppStorage(context: Context, uri: Uri): String? {
    return try {
        val docsDir = File(context.filesDir, "documents").apply { mkdirs() }
        val extension = context.contentResolver.getType(uri)?.let {
            when {
                it.contains("pdf") -> "pdf"
                else -> "jpg"
            }
        } ?: "jpg"
        val destFile = File(docsDir, "doc_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        if (destFile.exists() && destFile.length() > 0) Uri.fromFile(destFile).toString() else null
    } catch (t: Throwable) {
        null
    }
}

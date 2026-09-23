package com.tripmate.android.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripmate.shared.viewmodel.CreateTripViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import java.io.File

/**
 * Create Trip flow. Name, destination, a date range and an optional cover
 * photo are all a person needs to start planning — everything else
 * (activities) is added later from the trip's Timeline screen.
 */
@Composable
fun CreateTripScreen(
    ownerId: String,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val viewModel = koinInject<CreateTripViewModel>(parameters = { parametersOf(ownerId) })
    val state by viewModel.formState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onSaved()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val savedPath = withContext(Dispatchers.IO) { copyImageToAppStorage(context, uri) }
                if (savedPath != null) viewModel.onCoverImageSelected(savedPath)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New trip") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CoverPhotoPicker(
                coverImageUrl = state.coverImageUrl,
                onPick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text("Trip name") },
                placeholder = { Text("e.g. Japan 2026") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.destination,
                onValueChange = viewModel::onDestinationChanged,
                label = { Text("Destination") },
                placeholder = { Text("e.g. Tokyo, Japan") },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DatePickerField(
                    label = "Start date",
                    date = state.startDate,
                    onDatePicked = { newStart ->
                        val newEnd = state.endDate?.takeIf { it >= newStart } ?: newStart
                        viewModel.onDatesChanged(newStart, newEnd)
                    },
                    modifier = Modifier.weight(1f),
                )
                DatePickerField(
                    label = "End date",
                    date = state.endDate,
                    onDatePicked = { newEnd ->
                        val newStart = state.startDate?.takeIf { it <= newEnd } ?: newEnd
                        viewModel.onDatesChanged(newStart, newEnd)
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Create trip")
                }
            }
        }
    }
}

/**
 * Tappable cover-photo well. Shows the Android Photo Picker's selection once
 * copied into app-private storage, or an inviting placeholder before that.
 */
@Composable
private fun CoverPhotoPicker(coverImageUrl: String?, onPick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (coverImageUrl != null) {
            AsyncImage(
                model = coverImageUrl,
                contentDescription = "Trip cover photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change photo", style = MaterialTheme.typography.labelMedium)
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ),
                    ),
            ) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Add a cover photo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

/**
 * The Photo Picker only grants a transient read permission on its returned
 * [uri], so we copy the bytes into app-private storage right away and hand
 * back a stable file:// URI that survives app restarts and syncs cleanly as
 * [com.tripmate.shared.model.Trip.coverImageUrl].
 */
private fun copyImageToAppStorage(context: Context, uri: Uri): String? {
    return try {
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
        val destFile = File(coversDir, "cover_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        if (destFile.exists() && destFile.length() > 0) Uri.fromFile(destFile).toString() else null
    } catch (t: Throwable) {
        null
    }
}

/** A button that opens a calendar dialog and reports back the picked day. */
@Composable
private fun DatePickerField(
    label: String,
    date: LocalDate?,
    onDatePicked: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedButton(onClick = { showDialog = true }, modifier = modifier) {
        Text(date?.toString() ?: label)
    }

    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: Clock.System.todayIn(TimeZone.currentSystemDefault())).toUtcMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDatePicked(it.toLocalDateFromUtc()) }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toLocalDateFromUtc(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

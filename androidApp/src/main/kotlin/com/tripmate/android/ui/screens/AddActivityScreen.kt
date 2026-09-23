package com.tripmate.android.ui.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripmate.shared.model.ActivityCategory
import com.tripmate.shared.model.Place
import com.tripmate.shared.viewmodel.AddActivityViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Add Activity flow. Kept as a single screen (not a multi-step wizard) —
 * name, place, time and notes are all short fields, and a wizard would just
 * add taps for something a person often fills in while standing in line at
 * a ticket counter.
 */
@Composable
fun AddActivityScreen(
    tripId: String,
    activityId: String?,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val viewModel = koinInject<AddActivityViewModel>(parameters = { parametersOf(tripId, activityId) })
    val state by viewModel.formState.collectAsState()
    val isEditing = activityId != null
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedSuccessfully, state.deletedSuccessfully) {
        if (state.savedSuccessfully || state.deletedSuccessfully) onSaved()
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this activity?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit activity" else "Add activity") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete activity",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoadingExisting) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text("What's the activity?") },
                placeholder = { Text("e.g. Ghibli Museum tour") },
                modifier = Modifier.fillMaxWidth(),
            )

            CategoryPicker(selected = state.category, onSelect = viewModel::onCategoryChanged)

            OutlinedTextField(
                value = state.place?.name.orEmpty(),
                onValueChange = { viewModel.onPlaceChanged(if (it.isBlank()) null else Place(name = it)) },
                label = { Text("Place") },
                placeholder = { Text("Search or type a location") },
                modifier = Modifier.fillMaxWidth(),
                // TODO: replace with a real places-autocomplete field once a
                // provider (e.g. Google Places) is wired up.
            )

            DateTimePickerRow(
                startAtEpochMillis = state.startAtEpochMillis,
                onPicked = { start, end -> viewModel.onTimeChanged(start, end) },
            )

            ReminderLeadPicker(
                minutes = state.reminderLeadMinutes,
                onChange = viewModel::onReminderLeadChanged,
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

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
                    Text(if (isEditing) "Save changes" else "Save activity")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPicker(selected: ActivityCategory, onSelect: (ActivityCategory) -> Unit) {
    Column {
        Text("Category", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        // FlowRow (rather than a plain Row) wraps chips onto a second line
        // once they run out of horizontal space, matching the design mockup's
        // flex-wrap layout instead of squeezing/clipping the trailing chips.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActivityCategory.entries.forEach { category ->
                FilterChip(
                    selected = category == selected,
                    onClick = { onSelect(category) },
                    label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
    }
}

@Composable
private fun DateTimePickerRow(
    startAtEpochMillis: Long?,
    onPicked: (Long, Long?) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    // Holds the date chosen in step 1 (UTC-midnight millis, as DatePicker
    // reports them) while step 2 collects the time to combine it with.
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    Column {
        Text("Date & time", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(startAtEpochMillis?.toDateTimeLabel() ?: "Pick date & time")
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startAtEpochMillis ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = datePickerState.selectedDateMillis != null,
                    onClick = {
                        pendingDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                        showTimePicker = true
                    },
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val referenceMillis = startAtEpochMillis ?: pendingDateMillis ?: System.currentTimeMillis()
        val referenceLocal = Instant.fromEpochMilliseconds(referenceMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        val timePickerState = rememberTimePickerState(
            initialHour = referenceLocal.hour,
            initialMinute = referenceLocal.minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val combined = combineDateAndTime(
                        dateMillisUtc = pendingDateMillis ?: referenceMillis,
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                    )
                    onPicked(combined, null)
                    pendingDateMillis = null
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingDateMillis = null
                    showTimePicker = false
                }) { Text("Cancel") }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
        )
    }
}

/**
 * [DatePicker]'s selectedDateMillis is the UTC-midnight instant of the picked
 * calendar day, independent of the device's timezone; this combines that day
 * with a wall-clock hour/minute in the device's own timezone to get the
 * activity's real start instant.
 */
private fun combineDateAndTime(dateMillisUtc: Long, hour: Int, minute: Int): Long {
    val utcDate = Instant.fromEpochMilliseconds(dateMillisUtc).toLocalDateTime(TimeZone.UTC).date
    return utcDate.atTime(hour, minute).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}

private fun Long.toDateTimeLabel(): String {
    val local = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val dayName = local.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    val monthName = local.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    val hour = if (local.hour % 12 == 0) 12 else local.hour % 12
    val minute = local.minute.toString().padStart(2, '0')
    val suffix = if (local.hour < 12) "AM" else "PM"
    return "$dayName, $monthName ${local.dayOfMonth} · $hour:$minute $suffix"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderLeadPicker(minutes: Int, onChange: (Int) -> Unit) {
    Column {
        Text("Remind me", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(15, 30, 60, 120).forEach { option ->
                FilterChip(
                    selected = minutes == option,
                    onClick = { onChange(option) },
                    label = { Text(if (option < 60) "${option}m before" else "${option / 60}h before") },
                )
            }
        }
    }
}

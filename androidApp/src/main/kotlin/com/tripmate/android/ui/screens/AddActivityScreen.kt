package com.tripmate.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripmate.shared.model.ActivityCategory
import com.tripmate.shared.model.Place
import com.tripmate.shared.viewmodel.AddActivityViewModel
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
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val viewModel = koinInject<AddActivityViewModel>(parameters = { parametersOf(tripId) })
    val state by viewModel.formState.collectAsState()

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add activity") },
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
                    Text("Save activity")
                }
            }
        }
    }
}

@Composable
private fun CategoryPicker(selected: ActivityCategory, onSelect: (ActivityCategory) -> Unit) {
    Column {
        Text("Category", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    // A real implementation wires up Android's DatePickerDialog /
    // TimePickerDialog (or Compose's DatePicker) here. Left as a clearly
    // marked stub button so the screen is runnable and the wiring point is
    // obvious rather than hidden behind a fully "working" fake picker.
    OutlinedButton(onClick = { onPicked(startAtEpochMillis ?: System.currentTimeMillis(), null) }) {
        Text(
            if (startAtEpochMillis == null) "Pick date & time" else "Change date & time"
        )
    }
}

@Composable
private fun ReminderLeadPicker(minutes: Int, onChange: (Int) -> Unit) {
    Column {
        Text("Remind me", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

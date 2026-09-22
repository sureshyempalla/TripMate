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
import com.tripmate.shared.viewmodel.CreateTripViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Create Trip flow. Name, destination and a date range are all a person
 * needs to start planning — everything else (activities, cover image) is
 * added later from the trip's Timeline screen.
 */
@Composable
fun CreateTripScreen(
    ownerId: String,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val viewModel = koinInject<CreateTripViewModel>(parameters = { parametersOf(ownerId) })
    val state by viewModel.formState.collectAsState()

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onSaved()
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

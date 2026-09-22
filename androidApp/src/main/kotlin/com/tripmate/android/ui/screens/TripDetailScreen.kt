package com.tripmate.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripmate.android.ui.components.OfflineBanner
import com.tripmate.shared.model.Activity
import com.tripmate.shared.viewmodel.DayGroup
import com.tripmate.shared.viewmodel.TripDetailViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * The Timeline: the trip's itinerary as chronological day sections rather
 * than a calendar grid, so "what's happening today / next" reads at a
 * glance without needing to parse a grid of dates.
 */
@Composable
fun TripDetailScreen(
    tripId: String,
    onAddActivity: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinInject<TripDetailViewModel>(parameters = { parametersOf(tripId) })
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.trip?.title ?: "Trip") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddActivity) {
                Icon(Icons.Filled.Add, contentDescription = "Add activity")
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.days.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                OfflineBanner(syncStatus = state.syncStatus)
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No activities yet. Tap + to add the first one.")
                }
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OfflineBanner(syncStatus = state.syncStatus)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.days.forEach { day ->
                    item { DaySectionHeader(day) }
                    items(day.activities, key = { it.id }) { activity ->
                        ActivityRow(activity)
                    }
                }
            }
        }
    }
}

@Composable
private fun DaySectionHeader(day: DayGroup) {
    Text(
        text = day.date.toString(),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ActivityRow(activity: Activity) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.title, style = MaterialTheme.typography.titleMedium)
                activity.place?.name?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Text(
                text = activity.startAtEpochMillis.toTimeLabel(),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun Long.toTimeLabel(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = if (local.hour % 12 == 0) 12 else local.hour % 12
    val minute = local.minute.toString().padStart(2, '0')
    val suffix = if (local.hour < 12) "AM" else "PM"
    return "$hour:$minute $suffix"
}

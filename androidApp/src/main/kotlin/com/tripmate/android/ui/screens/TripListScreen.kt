package com.tripmate.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tripmate.android.ui.components.NextUpBanner
import com.tripmate.android.ui.components.OfflineBanner
import com.tripmate.shared.model.Trip
import com.tripmate.shared.viewmodel.TripListViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Home screen. Trips are grouped Upcoming / Past — a person planning a trip
 * wants to see it at the top; a person reminiscing wants past trips out of
 * the way but not gone.
 */
@Composable
fun TripListScreen(
    userId: String,
    onOpenTrip: (String) -> Unit,
    onCreateTrip: () -> Unit,
) {
    val viewModel = koinInject<TripListViewModel>(parameters = { parametersOf(userId) })
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("TripMate") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTrip) {
                Icon(Icons.Filled.Add, contentDescription = "New trip")
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.upcomingTrips.isEmpty() && state.pastTrips.isEmpty()) {
            Column(modifier = Modifier.padding(padding)) {
                OfflineBanner(syncStatus = state.syncStatus)
                EmptyTripsState(modifier = Modifier.weight(1f), onCreateTrip = onCreateTrip)
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        OfflineBanner(syncStatus = state.syncStatus)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.nextActivityBanner?.let { banner ->
                item {
                    NextUpBanner(
                        banner = banner,
                        onClick = { state.upcomingTrips.firstOrNull()?.let { onOpenTrip(it.id) } },
                    )
                }
            }

            if (state.upcomingTrips.isNotEmpty()) {
                item { SectionHeader("Upcoming") }
                items(state.upcomingTrips, key = { it.id }) { trip ->
                    TripCard(trip = trip, onClick = { onOpenTrip(trip.id) })
                }
            }

            if (state.pastTrips.isNotEmpty()) {
                item { SectionHeader("Past") }
                items(state.pastTrips, key = { it.id }) { trip ->
                    TripCard(trip = trip, onClick = { onOpenTrip(trip.id) })
                }
            }
        }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun TripCard(trip: Trip, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(trip.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(trip.destination, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${trip.startDate} – ${trip.endDate}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun EmptyTripsState(modifier: Modifier = Modifier, onCreateTrip: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("No trips yet", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Plan your first trip — add dates, places, and activities, and TripMate will remind you what's next.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onCreateTrip) { Text("Create a trip") }
    }
}

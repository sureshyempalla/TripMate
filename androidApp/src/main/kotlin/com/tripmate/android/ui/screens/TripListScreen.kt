package com.tripmate.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripmate.android.ui.components.NextUpBanner
import com.tripmate.android.ui.components.OfflineBanner
import com.tripmate.shared.model.Trip
import com.tripmate.shared.viewmodel.TripListViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Home screen. Trips are grouped Upcoming / Past — a person planning a trip
 * wants to see it at the top; a person reminiscing wants past trips out of
 * the way but not gone.
 *
 * The top bar (including the search toggle) and bottom nav bar are owned by
 * MainActivity's outer Scaffold (this is one of the four tab destinations);
 * this screen only supplies the body and its own FAB, and filters by
 * [searchQuery] when the person is searching.
 */
@Composable
fun TripListScreen(
    userId: String,
    searchQuery: String,
    onOpenTrip: (String) -> Unit,
    onCreateTrip: () -> Unit,
) {
    val viewModel = koinInject<TripListViewModel>(parameters = { parametersOf(userId) })
    val state by viewModel.uiState.collectAsState()
    var tripPendingDelete by remember { mutableStateOf<Trip?>(null) }

    val isSearching = searchQuery.isNotBlank()
    val filteredUpcoming = if (isSearching) state.upcomingTrips.filter { it.matches(searchQuery) } else state.upcomingTrips
    val filteredPast = if (isSearching) state.pastTrips.filter { it.matches(searchQuery) } else state.pastTrips

    tripPendingDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { tripPendingDelete = null },
            title = { Text("Delete \"${trip.title}\"?") },
            text = { Text("This trip and its dates will be permanently deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTrip(trip.id)
                    tripPendingDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripPendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateTrip) {
                Icon(Icons.Filled.Add, contentDescription = "New trip")
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
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

        if (isSearching && filteredUpcoming.isEmpty() && filteredPast.isEmpty()) {
            Column(modifier = Modifier.padding(padding)) {
                OfflineBanner(syncStatus = state.syncStatus)
                NoSearchResultsState(query = searchQuery, modifier = Modifier.weight(1f))
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OfflineBanner(syncStatus = state.syncStatus)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!isSearching) {
                    state.nextActivityBanner?.let { banner ->
                        item {
                            val bannerTripCover = state.upcomingTrips.firstOrNull { it.id == banner.tripId }?.coverImageUrl
                            NextUpBanner(
                                banner = banner,
                                coverImageUrl = bannerTripCover,
                                onClick = { onOpenTrip(banner.tripId) },
                            )
                        }
                    }
                }

                if (filteredUpcoming.isNotEmpty()) {
                    item { SectionHeader("Upcoming") }
                    items(filteredUpcoming, key = { it.id }) { trip ->
                        TripCard(
                            trip = trip,
                            faded = false,
                            onClick = { onOpenTrip(trip.id) },
                            onDelete = { tripPendingDelete = trip },
                        )
                    }
                }

                if (filteredPast.isNotEmpty()) {
                    item { SectionHeader("Past") }
                    items(filteredPast, key = { it.id }) { trip ->
                        TripCard(
                            trip = trip,
                            faded = true,
                            onClick = { onOpenTrip(trip.id) },
                            onDelete = { tripPendingDelete = trip },
                        )
                    }
                }
            }
        }
    }
}

private fun Trip.matches(query: String): Boolean {
    val q = query.trim()
    return title.contains(q, ignoreCase = true) || destination.contains(q, ignoreCase = true)
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun NoSearchResultsState(query: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("No trips match \"$query\"", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Try a different trip name or destination.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Photo-forward trip card: a cover image (or a warm gradient fallback while
 * no photo has been set) with the title and destination overlaid on a scrim,
 * and the date range + a relative-time chip below (e.g. "In 3 weeks") — this
 * is the single biggest look-and-feel lever discussed in the design review:
 * real imagery reads as "travel app" where a flat icon tile read as "utility
 * tracker".
 */
@Composable
private fun TripCard(trip: Trip, faded: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().let { if (faded) it.alpha(0.85f) else it },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (faded) 110.dp else 132.dp),
        ) {
            if (trip.coverImageUrl != null) {
                AsyncImage(
                    model = trip.coverImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.CardTravel,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.68f),
                        ),
                    ),
            )
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(
                    trip.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    trip.destination,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${trip.startDate} – ${trip.endDate}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (faded) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.secondary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!faded) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    ) {
                        Text(
                            trip.countdownLabel(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete trip",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/**
 * "Today" / "Tomorrow" / "In N days" / "In N weeks" — the relative countdown
 * chip design mockups show on upcoming trip cards, in place of a flat static
 * "Upcoming" label that gives no sense of how soon a trip actually is.
 */
private fun Trip.countdownLabel(): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    if (today in startDate..endDate) return "Ongoing"
    val daysUntil = today.daysUntil(startDate)
    return when {
        daysUntil <= 0 -> "Today"
        daysUntil == 1 -> "Tomorrow"
        daysUntil < 7 -> "In $daysUntil days"
        daysUntil < 60 -> "In ${daysUntil / 7} week${if (daysUntil / 7 == 1) "" else "s"}"
        else -> "In ${daysUntil / 30} month${if (daysUntil / 30 == 1) "" else "s"}"
    }
}


@Composable
private fun EmptyTripsState(modifier: Modifier = Modifier, onCreateTrip: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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

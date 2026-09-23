package com.tripmate.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Home screen. Trips are grouped Upcoming / Past — a person planning a trip
 * wants to see it at the top; a person reminiscing wants past trips out of
 * the way but not gone.
 *
 * The top bar and bottom nav bar are owned by MainActivity's outer Scaffold
 * (this is one of the four tab destinations); this screen only supplies the
 * body and its own FAB.
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

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OfflineBanner(syncStatus = state.syncStatus)
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
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
                        TripCard(trip = trip, faded = false, onClick = { onOpenTrip(trip.id) })
                    }
                }

                if (state.pastTrips.isNotEmpty()) {
                    item { SectionHeader("Past") }
                    items(state.pastTrips, key = { it.id }) { trip ->
                        TripCard(trip = trip, faded = true, onClick = { onOpenTrip(trip.id) })
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
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
}

/**
 * Photo-forward trip card: a cover image (or a warm gradient fallback while
 * no photo has been set — there's no cover-image picker yet) with the title
 * and destination overlaid on a scrim, and the date range + a status chip
 * below. This is the single biggest look-and-feel lever discussed in the
 * design review: real imagery reads as "travel app" where a flat icon tile
 * read as "utility tracker".
 */
@Composable
private fun TripCard(trip: Trip, faded: Boolean, onClick: () -> Unit) {
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${trip.startDate} – ${trip.endDate}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (faded) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.secondary,
            )
            if (!faded) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                ) {
                    Text(
                        "Upcoming",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
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

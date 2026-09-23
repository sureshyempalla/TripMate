package com.tripmate.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tripmate.android.ui.components.OfflineBanner
import com.tripmate.shared.model.Activity
import com.tripmate.shared.model.ActivityCategory
import com.tripmate.shared.model.Trip
import com.tripmate.shared.viewmodel.TripDetailViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * The Timeline: a photo hero header (matching the trip's cover on the Trips
 * list), one day-tab per calendar day of the trip, and that day's activities
 * as a vertical dot-and-line timeline — mirroring the "what's happening
 * today / next" read of the design mockups rather than one long stacked list.
 */
@Composable
fun TripDetailScreen(
    tripId: String,
    onAddActivity: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onOpenPacking: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenDocuments: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel = koinInject<TripDetailViewModel>(parameters = { parametersOf(tripId) })
    val state by viewModel.uiState.collectAsState()
    val trip = state.trip

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddActivity) {
                Icon(Icons.Filled.Add, contentDescription = "Add activity")
            }
        },
    ) { padding ->
        if (state.isLoading || trip == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val allDays = remember(trip.startDate, trip.endDate) { trip.startDate.datesUntilInclusive(trip.endDate) }
        val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
        var selectedDate by remember(tripId) {
            mutableStateOf(if (today in allDays) today else allDays.firstOrNull() ?: trip.startDate)
        }
        val nextActivity = remember(state.days) { state.days.flatMap { it.activities }.nextUpcoming() }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TripHeroHeader(trip = trip, onBack = onBack, onOpenPacking = onOpenPacking, onOpenBudget = onOpenBudget, onOpenDocuments = onOpenDocuments)
            OfflineBanner(syncStatus = state.syncStatus)
            DayTabsRow(
                days = allDays,
                selectedDate = selectedDate,
                onSelect = { selectedDate = it },
            )

            val activitiesForDay = state.days.firstOrNull { it.date == selectedDate }?.activities.orEmpty()
            if (activitiesForDay.isEmpty()) {
                EmptyDayState(date = selectedDate, modifier = Modifier.weight(1f))
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    activitiesForDay.forEachIndexed { index, activity ->
                        ActivityTimelineRow(
                            activity = activity,
                            isLast = index == activitiesForDay.lastIndex,
                            isNextUp = activity.id == nextActivity?.id,
                            onClick = { onOpenActivity(activity.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripHeroHeader(
    trip: Trip,
    onBack: () -> Unit,
    onOpenPacking: () -> Unit,
    onOpenBudget: () -> Unit,
    onOpenDocuments: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().height(196.dp)) {
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
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.25f),
                        0.35f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.78f),
                    ),
                ),
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(14.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.85f)),
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
        }
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onOpenDocuments,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f)),
            ) {
                Icon(Icons.Filled.Folder, contentDescription = "Documents", tint = Color.Black)
            }
            IconButton(
                onClick = onOpenBudget,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f)),
            ) {
                Icon(Icons.Filled.Receipt, contentDescription = "Budget", tint = Color.Black)
            }
            IconButton(
                onClick = onOpenPacking,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f)),
            ) {
                Icon(Icons.Filled.Luggage, contentDescription = "Packing list", tint = Color.Black)
            }
        }
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                trip.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                "${trip.startDate} – ${trip.endDate} · ${trip.destination}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun DayTabsRow(days: List<LocalDate>, selectedDate: LocalDate, onSelect: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        days.forEachIndexed { index, date ->
            val selected = date == selectedDate
            Surface(
                onClick = { onSelect(date) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
                border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Day ${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        date.shortLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDayState(date: LocalDate, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("No activities yet for ${date.shortLabel()}.", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Tap + to add the first one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

/**
 * One stop on the day's timeline: a time column, a dot-and-connecting-line
 * rail, and a card. The single next-upcoming activity across the whole trip
 * is called out with a highlighted card and a "NEXT UP" badge, same as the
 * Trips-list banner it corresponds to.
 */
@Composable
private fun ActivityTimelineRow(activity: Activity, isLast: Boolean, isNextUp: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = activity.startAtEpochMillis.toTimeLabel(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.width(56.dp).padding(top = 14.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 18.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            color = if (isNextUp) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            border = if (isNextUp) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        activity.category.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        activity.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (isNextUp) {
                        Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary) {
                            Text(
                                "NEXT UP",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                activity.place?.name?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

private fun ActivityCategory.icon(): ImageVector = when (this) {
    ActivityCategory.FLIGHT -> Icons.Filled.Flight
    ActivityCategory.LODGING -> Icons.Filled.Hotel
    ActivityCategory.FOOD -> Icons.Filled.Restaurant
    ActivityCategory.SIGHTSEEING -> Icons.Filled.CameraAlt
    ActivityCategory.TRANSPORT -> Icons.Filled.DirectionsCar
    ActivityCategory.ACTIVITY -> Icons.Filled.LocalActivity
    ActivityCategory.OTHER -> Icons.Filled.Circle
}

private fun List<Activity>.nextUpcoming(): Activity? {
    val now = Clock.System.now().toEpochMilliseconds()
    return filter { it.startAtEpochMillis >= now }.minByOrNull { it.startAtEpochMillis }
}

private fun LocalDate.datesUntilInclusive(end: LocalDate): List<LocalDate> {
    val result = mutableListOf<LocalDate>()
    var cursor = this
    while (cursor <= end) {
        result.add(cursor)
        cursor = cursor.plus(1, DateTimeUnit.DAY)
    }
    return result
}

private fun LocalDate.shortLabel(): String {
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$monthName $dayOfMonth"
}

private fun Long.toTimeLabel(): String {
    val instant = Instant.fromEpochMilliseconds(this)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = if (local.hour % 12 == 0) 12 else local.hour % 12
    val minute = local.minute.toString().padStart(2, '0')
    val suffix = if (local.hour < 12) "AM" else "PM"
    return "$hour:$minute $suffix"
}

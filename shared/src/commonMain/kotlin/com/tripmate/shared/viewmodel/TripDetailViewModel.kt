package com.tripmate.shared.viewmodel

import com.tripmate.shared.data.ActivityRepository
import com.tripmate.shared.data.SyncCoordinator
import com.tripmate.shared.data.SyncStatus
import com.tripmate.shared.data.TripRepository
import com.tripmate.shared.model.Activity
import com.tripmate.shared.model.Trip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** One day's worth of activities, as shown as a tab/section in the Timeline screen. */
data class DayGroup(
    val date: kotlinx.datetime.LocalDate,
    val activities: List<Activity>,
)

data class TripDetailUiState(
    val isLoading: Boolean = true,
    val trip: Trip? = null,
    val days: List<DayGroup> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.SYNCING,
)

/** Screen state for the Timeline (day-by-day itinerary) view of a single trip. */
class TripDetailViewModel(
    private val tripRepository: TripRepository,
    private val activityRepository: ActivityRepository,
    private val syncCoordinator: SyncCoordinator,
    private val tripId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _uiState = MutableStateFlow(TripDetailUiState())
    val uiState: StateFlow<TripDetailUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            combine(
                tripRepository.observeTrip(tripId),
                activityRepository.observeActivities(tripId),
                syncCoordinator.syncStatus,
            ) { trip, activities, syncStatus -> Triple(trip, activities, syncStatus) }
                .collect { (trip, activities, syncStatus) ->
                    _uiState.value = TripDetailUiState(
                        isLoading = false,
                        trip = trip,
                        days = activities
                            .groupBy { it.startAtEpochMillis.toLocalDate() }
                            .entries
                            .sortedBy { it.key }
                            .map { (date, items) -> DayGroup(date, items.sortedBy { it.startAtEpochMillis }) },
                        syncStatus = syncStatus,
                    )
                }
        }
    }

    fun deleteActivity(activityId: String) = scope.launch {
        activityRepository.deleteActivity(activityId)
    }
}

private fun Long.toLocalDate(): kotlinx.datetime.LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).date

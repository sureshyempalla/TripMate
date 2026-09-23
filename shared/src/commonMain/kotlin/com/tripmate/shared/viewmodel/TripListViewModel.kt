package com.tripmate.shared.viewmodel

import com.tripmate.shared.data.SyncStatus
import com.tripmate.shared.data.TripRepository
import com.tripmate.shared.model.Trip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

data class TripListUiState(
    val isLoading: Boolean = true,
    val upcomingTrips: List<Trip> = emptyList(),
    val pastTrips: List<Trip> = emptyList(),
    val nextActivityBanner: NextActivityBanner? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCING,
)

data class NextActivityBanner(
    val tripId: String,
    val activityTitle: String,
    val placeName: String?,
    val startAtEpochMillis: Long,
)

/**
 * Platform-agnostic screen state for the Trips list (the app's home screen).
 * Both androidApp and iosApp collect [uiState] and render it with their own
 * native widgets — this class holds no UI framework types.
 */
class TripListViewModel(
    private val repository: TripRepository,
    private val userId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _uiState = MutableStateFlow(TripListUiState())
    val uiState: StateFlow<TripListUiState> = _uiState.asStateFlow()

    init {
        observeTrips()
        observeNextActivity()
        observeSyncStatus()
        refresh()
    }

    private fun observeTrips() = scope.launch {
        repository.observeTrips(userId).collect { trips ->
            val now = com.tripmate.shared.util.currentEpochMillis()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                upcomingTrips = trips.filter {
                    it.endDate.toEpochMillisEndOfDay() >= now
                }.sortedBy { it.startDate },
                pastTrips = trips.filter {
                    it.endDate.toEpochMillisEndOfDay() < now
                }.sortedByDescending { it.startDate },
            )
        }
    }

    private fun observeNextActivity() = scope.launch {
        repository.observeNextActivity(userId).collect { activity ->
            _uiState.value = _uiState.value.copy(
                nextActivityBanner = activity?.let {
                    NextActivityBanner(it.tripId, it.title, it.place?.name, it.startAtEpochMillis)
                }
            )
        }
    }

    private fun observeSyncStatus() = scope.launch {
        repository.syncStatus.collect { status ->
            _uiState.value = _uiState.value.copy(syncStatus = status)
        }
    }

    fun refresh() = scope.launch {
        repository.startSync(userId)
    }

    /** Deletes a trip from Firestore and the local cache. Note: this does not
     * currently cascade-delete the trip's activities in Firestore — pre-existing
     * behavior of [TripRepository.deleteTrip], not new here. */
    fun deleteTrip(tripId: String) = scope.launch {
        repository.deleteTrip(tripId)
    }
}

private fun kotlinx.datetime.LocalDate.toEpochMillisEndOfDay(): Long =
    this.atTime(23, 59)
        .toInstant(kotlinx.datetime.TimeZone.currentSystemDefault())
        .toEpochMilliseconds()

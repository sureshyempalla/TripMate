package com.tripmate.shared.viewmodel

import com.tripmate.shared.data.TripRepository
import com.tripmate.shared.model.Trip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.random.Random

data class CreateTripFormState(
    val title: String = "",
    val destination: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedSuccessfully: Boolean = false,
)

/** Screen state + validation for the Create Trip flow. */
class CreateTripViewModel(
    private val repository: TripRepository,
    private val ownerId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _formState = MutableStateFlow(CreateTripFormState())
    val formState: StateFlow<CreateTripFormState> = _formState.asStateFlow()

    fun onTitleChanged(title: String) {
        _formState.value = _formState.value.copy(title = title)
    }

    fun onDestinationChanged(destination: String) {
        _formState.value = _formState.value.copy(destination = destination)
    }

    fun onDatesChanged(startDate: LocalDate, endDate: LocalDate) {
        _formState.value = _formState.value.copy(startDate = startDate, endDate = endDate)
    }

    fun save() {
        val state = _formState.value
        if (state.title.isBlank()) {
            _formState.value = state.copy(error = "Give this trip a name.")
            return
        }
        if (state.destination.isBlank()) {
            _formState.value = state.copy(error = "Where are you going?")
            return
        }
        val startDate = state.startDate
        val endDate = state.endDate
        if (startDate == null || endDate == null) {
            _formState.value = state.copy(error = "Pick start and end dates.")
            return
        }
        if (endDate < startDate) {
            _formState.value = state.copy(error = "End date can't be before the start date.")
            return
        }

        _formState.value = state.copy(isSaving = true, error = null)
        scope.launch {
            try {
                repository.createTrip(
                    Trip(
                        id = "trip_${currentEpochMillisSafe()}_${Random.nextInt(1000, 9999)}",
                        ownerId = ownerId,
                        title = state.title.trim(),
                        destination = state.destination.trim(),
                        startDate = startDate,
                        endDate = endDate,
                        createdAtEpochMillis = currentEpochMillisSafe(),
                    )
                )
                _formState.value = _formState.value.copy(isSaving = false, savedSuccessfully = true)
            } catch (t: Throwable) {
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    error = t.message ?: "Couldn't create this trip. Try again.",
                )
            }
        }
    }
}

private fun currentEpochMillisSafe() = com.tripmate.shared.util.currentEpochMillis()

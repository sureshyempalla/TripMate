package com.tripmate.shared.viewmodel

import com.tripmate.shared.data.TripRepository
import com.tripmate.shared.model.Activity
import com.tripmate.shared.model.ActivityCategory
import com.tripmate.shared.model.Place
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class AddActivityFormState(
    val title: String = "",
    val category: ActivityCategory = ActivityCategory.ACTIVITY,
    val place: Place? = null,
    val startAtEpochMillis: Long? = null,
    val endAtEpochMillis: Long? = null,
    val notes: String = "",
    val reminderLeadMinutes: Int = 30,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedSuccessfully: Boolean = false,
)

/** Screen state + validation for the Add/Edit Activity flow. */
class AddActivityViewModel(
    private val repository: TripRepository,
    private val tripId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _formState = MutableStateFlow(AddActivityFormState())
    val formState: StateFlow<AddActivityFormState> = _formState.asStateFlow()

    fun onTitleChanged(title: String) {
        _formState.value = _formState.value.copy(title = title)
    }

    fun onCategoryChanged(category: ActivityCategory) {
        _formState.value = _formState.value.copy(category = category)
    }

    fun onPlaceChanged(place: Place?) {
        _formState.value = _formState.value.copy(place = place)
    }

    fun onTimeChanged(startAtEpochMillis: Long, endAtEpochMillis: Long?) {
        _formState.value = _formState.value.copy(
            startAtEpochMillis = startAtEpochMillis,
            endAtEpochMillis = endAtEpochMillis,
        )
    }

    fun onNotesChanged(notes: String) {
        _formState.value = _formState.value.copy(notes = notes)
    }

    fun onReminderLeadChanged(minutes: Int) {
        _formState.value = _formState.value.copy(reminderLeadMinutes = minutes)
    }

    fun save() {
        val state = _formState.value
        val startAt = state.startAtEpochMillis
        if (state.title.isBlank()) {
            _formState.value = state.copy(error = "Give this activity a name.")
            return
        }
        if (startAt == null) {
            _formState.value = state.copy(error = "Pick a date and time.")
            return
        }

        _formState.value = state.copy(isSaving = true, error = null)
        scope.launch {
            try {
                repository.createActivity(
                    Activity(
                        id = "activity_${currentEpochMillisSafe()}_${Random.nextInt(1000, 9999)}",
                        tripId = tripId,
                        title = state.title.trim(),
                        category = state.category,
                        place = state.place,
                        startAtEpochMillis = startAt,
                        endAtEpochMillis = state.endAtEpochMillis,
                        notes = state.notes.ifBlank { null },
                        reminderLeadMinutes = state.reminderLeadMinutes,
                    )
                )
                _formState.value = _formState.value.copy(isSaving = false, savedSuccessfully = true)
            } catch (t: Throwable) {
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    error = t.message ?: "Couldn't save this activity. Try again.",
                )
            }
        }
    }
}

private fun currentEpochMillisSafe() = com.tripmate.shared.util.currentEpochMillis()

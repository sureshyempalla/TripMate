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
    val isDeleting: Boolean = false,
    val error: String? = null,
    val savedSuccessfully: Boolean = false,
    val deletedSuccessfully: Boolean = false,
    /** True once an existing activity's fields have been loaded into the form, in edit mode. */
    val isLoadingExisting: Boolean = false,
)

/**
 * Screen state + validation for the Add/Edit Activity flow. When [activityId]
 * is null this is a fresh Add; when it's set, the form loads that activity's
 * current fields (from the same [TripRepository.observeActivities] stream the
 * Timeline already uses) and [save] updates it in place instead of creating
 * a new one. [delete] is only meaningful in edit mode.
 */
class AddActivityViewModel(
    private val repository: TripRepository,
    private val tripId: String,
    private val activityId: String? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _formState = MutableStateFlow(AddActivityFormState(isLoadingExisting = activityId != null))
    val formState: StateFlow<AddActivityFormState> = _formState.asStateFlow()

    private var hasLoadedExisting = false

    init {
        if (activityId != null) {
            scope.launch {
                repository.observeActivities(tripId).collect { activities ->
                    if (hasLoadedExisting) return@collect
                    val existing = activities.find { it.id == activityId } ?: return@collect
                    hasLoadedExisting = true
                    _formState.value = _formState.value.copy(
                        title = existing.title,
                        category = existing.category,
                        place = existing.place,
                        startAtEpochMillis = existing.startAtEpochMillis,
                        endAtEpochMillis = existing.endAtEpochMillis,
                        notes = existing.notes.orEmpty(),
                        reminderLeadMinutes = existing.reminderLeadMinutes,
                        isLoadingExisting = false,
                    )
                }
            }
        }
    }

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
                val activity = Activity(
                    id = activityId ?: "activity_${currentEpochMillisSafe()}_${Random.nextInt(1000, 9999)}",
                    tripId = tripId,
                    title = state.title.trim(),
                    category = state.category,
                    place = state.place,
                    startAtEpochMillis = startAt,
                    endAtEpochMillis = state.endAtEpochMillis,
                    notes = state.notes.ifBlank { null },
                    reminderLeadMinutes = state.reminderLeadMinutes,
                )
                if (activityId != null) {
                    repository.updateActivity(activity)
                } else {
                    repository.createActivity(activity)
                }
                _formState.value = _formState.value.copy(isSaving = false, savedSuccessfully = true)
            } catch (t: Throwable) {
                _formState.value = _formState.value.copy(
                    isSaving = false,
                    error = t.message ?: "Couldn't save this activity. Try again.",
                )
            }
        }
    }

    /** Deletes the activity being edited. No-op (should not be called) when adding new. */
    fun delete() {
        val id = activityId ?: return
        _formState.value = _formState.value.copy(isDeleting = true, error = null)
        scope.launch {
            try {
                repository.deleteActivity(id)
                _formState.value = _formState.value.copy(isDeleting = false, deletedSuccessfully = true)
            } catch (t: Throwable) {
                _formState.value = _formState.value.copy(
                    isDeleting = false,
                    error = t.message ?: "Couldn't delete this activity. Try again.",
                )
            }
        }
    }
}

private fun currentEpochMillisSafe() = com.tripmate.shared.util.currentEpochMillis()

package com.tripmate.shared.data

import com.tripmate.shared.model.Activity
import kotlinx.coroutines.flow.Flow

/** CRUD + observe for a trip's [Activity] items. */
interface ActivityRepository {
    fun observeActivities(tripId: String): Flow<List<Activity>>

    /** The single next-upcoming activity across all of the user's active trips. */
    fun observeNextActivity(userId: String): Flow<Activity?>

    suspend fun createActivity(activity: Activity)
    suspend fun updateActivity(activity: Activity)
    suspend fun deleteActivity(activityId: String)
}

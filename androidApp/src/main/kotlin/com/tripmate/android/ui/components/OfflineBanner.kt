package com.tripmate.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tripmate.shared.data.SyncStatus

/**
 * A quiet, honest status strip — not a red error banner, since being
 * offline isn't a failure state in this app (the itinerary is fully usable
 * offline by design). This is deliberately more visible than TripIt/Wanderlog
 * make their offline state, and unlike Wanderlog it isn't a paywalled
 * capability being called out — it's just telling the truth about the sync.
 */
@Composable
fun OfflineBanner(syncStatus: SyncStatus, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = syncStatus == SyncStatus.OFFLINE,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondary)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "You're offline — showing your saved itinerary. Changes sync when you're back.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

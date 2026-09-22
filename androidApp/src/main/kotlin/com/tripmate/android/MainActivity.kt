package com.tripmate.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tripmate.android.ui.screens.AddActivityScreen
import com.tripmate.android.ui.screens.CreateTripScreen
import com.tripmate.android.ui.screens.TripDetailScreen
import com.tripmate.android.ui.screens.TripListScreen
import com.tripmate.android.ui.theme.TripMateTheme
import com.tripmate.shared.auth.AuthRepository
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op: banner just won't show local fallback */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            TripMateTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SignedInGate { userId -> TripMateNavHost(userId) }
                }
            }
        }
    }
}

/**
 * Firestore's security rules require a real Firebase Auth uid on every call,
 * so nothing below this can render until [AuthRepository.ensureSignedIn]
 * resolves (there's no login UI yet, so that's an anonymous sign-in). Sign-in
 * only fails when the project isn't configured for it (e.g. the Anonymous
 * provider isn't enabled in the Firebase console), so that's surfaced as a
 * retryable error instead of crashing the app.
 */
@Composable
private fun SignedInGate(content: @Composable (userId: String) -> Unit) {
    val authRepository = koinInject<AuthRepository>()
    var retryToken by remember { mutableIntStateOf(0) }
    val result by produceState<Result<String>?>(initialValue = null, retryToken) {
        value = runCatching { authRepository.ensureSignedIn() }
    }

    when (val current = result) {
        null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> current.fold(
            onSuccess = { userId -> content(userId) },
            onFailure = { error ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Couldn't sign in",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        error.message ?: "Something went wrong. Check your connection and try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { retryToken++ }) { Text("Retry") }
                }
            },
        )
    }
}

@Composable
private fun TripMateNavHost(userId: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "trips") {
        composable("trips") {
            TripListScreen(
                userId = userId,
                onOpenTrip = { tripId -> navController.navigate("trip/$tripId") },
                onCreateTrip = { navController.navigate("trip/new") },
            )
        }
        composable("trip/new") {
            CreateTripScreen(
                ownerId = userId,
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(
            route = "trip/{tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId").orEmpty()
            TripDetailScreen(
                tripId = tripId,
                onAddActivity = { navController.navigate("trip/$tripId/add") },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "trip/{tripId}/add",
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId").orEmpty()
            AddActivityScreen(
                tripId = tripId,
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }
    }
}

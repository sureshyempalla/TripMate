package com.tripmate.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardTravel
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tripmate.android.ui.screens.AddActivityScreen
import com.tripmate.android.ui.screens.CreateTripScreen
import com.tripmate.android.ui.screens.ExploreScreen
import com.tripmate.android.ui.screens.NotificationsScreen
import com.tripmate.android.ui.screens.PackingListScreen
import com.tripmate.android.ui.screens.BudgetScreen
import com.tripmate.android.ui.screens.DocumentWalletScreen
import com.tripmate.android.ui.screens.ProfileScreen
import com.tripmate.android.ui.screens.TripDetailScreen
import com.tripmate.android.ui.screens.TripListScreen
import com.tripmate.android.ui.theme.TripMateTheme
import com.tripmate.shared.auth.AuthRepository
import kotlinx.coroutines.launch
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

/** The four destinations reachable from the bottom nav bar / drawer. */
private data class TopLevelDestination(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination("trips", "Trips", Icons.Filled.CardTravel),
    TopLevelDestination("explore", "Explore", Icons.Filled.Explore),
    TopLevelDestination("notifications", "Notifications", Icons.Filled.Notifications),
    TopLevelDestination("profile", "Profile", Icons.Filled.Person),
)

@Composable
private fun TripMateNavHost(userId: String) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    var tripSearchActive by remember { mutableStateOf(false) }
    var tripSearchQuery by remember { mutableStateOf("") }
    val tripSearchFocusRequester = remember { FocusRequester() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTopLevel = topLevelDestinations.firstOrNull { it.route == currentRoute }
    val isTopLevel = currentTopLevel != null

    // Closing search when navigating away from the Trips tab avoids landing
    // back on it later with a stale query silently filtering the list.
    LaunchedEffect(currentRoute) {
        if (currentRoute != "trips" && tripSearchActive) {
            tripSearchActive = false
            tripSearchQuery = ""
        }
    }
    LaunchedEffect(tripSearchActive) {
        if (tripSearchActive) tripSearchFocusRequester.requestFocus()
    }

    DismissibleNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DismissibleDrawerSheet {
                AppDrawerContent(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        scope.launch { drawerState.close() }
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                popUpTo("trips") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        },
        // The drawer only makes sense over the four tab destinations — a
        // trip's Timeline or a form screen opens it as a dead end since
        // there's nothing there to switch to.
        gesturesEnabled = isTopLevel,
    ) {
        Scaffold(
            topBar = {
                if (currentTopLevel != null) {
                    val isTripsSearch = currentTopLevel.route == "trips" && tripSearchActive
                    TopAppBar(
                        title = {
                            if (isTripsSearch) {
                                OutlinedTextField(
                                    value = tripSearchQuery,
                                    onValueChange = { tripSearchQuery = it },
                                    placeholder = { Text("Search trips") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().focusRequester(tripSearchFocusRequester),
                                )
                            } else {
                                Text(if (currentTopLevel.route == "trips") "TripMate" else currentTopLevel.label)
                            }
                        },
                        navigationIcon = {
                            if (isTripsSearch) {
                                IconButton(onClick = {
                                    tripSearchActive = false
                                    tripSearchQuery = ""
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close search")
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                                }
                            }
                        },
                        actions = {
                            if (isTripsSearch) {
                                if (tripSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { tripSearchQuery = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                    }
                                }
                            } else {
                                if (currentTopLevel.route == "trips") {
                                    IconButton(onClick = { tripSearchActive = true }) {
                                        Icon(Icons.Filled.Search, contentDescription = "Search trips")
                                    }
                                }
                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                                    }
                                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                        DropdownMenuItem(text = { Text("Sort trips") }, onClick = { menuExpanded = false })
                                        DropdownMenuItem(text = { Text("Sync now") }, onClick = { menuExpanded = false })
                                        Divider()
                                        DropdownMenuItem(text = { Text("Sign out") }, onClick = { menuExpanded = false })
                                    }
                                }
                            }
                        },
                    )
                }
                // Detail/form screens (Timeline, Add activity, Create trip) draw
                // their own TopAppBar with a back/close icon instead.
            },
            bottomBar = {
                if (isTopLevel) {
                    NavigationBar {
                        topLevelDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = destination.route == currentRoute,
                                onClick = {
                                    if (destination.route != currentRoute) {
                                        navController.navigate(destination.route) {
                                            popUpTo("trips") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "trips",
                modifier = Modifier.padding(padding),
            ) {
                composable("trips") {
                    TripListScreen(
                        userId = userId,
                        searchQuery = tripSearchQuery,
                        onOpenTrip = { tripId -> navController.navigate("trip/$tripId") },
                        onCreateTrip = { navController.navigate("trip/new") },
                    )
                }
                composable("explore") { ExploreScreen() }
                composable("notifications") { NotificationsScreen() }
                composable("profile") { ProfileScreen() }
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
                ) { entry ->
                    val tripId = entry.arguments?.getString("tripId").orEmpty()
                    TripDetailScreen(
                        tripId = tripId,
                        onAddActivity = { navController.navigate("trip/$tripId/add") },
                        onOpenActivity = { activityId -> navController.navigate("trip/$tripId/activity/$activityId") },
                        onOpenPacking = { navController.navigate("trip/$tripId/packing") },
                        onOpenBudget = { navController.navigate("trip/$tripId/budget") },
                        onOpenDocuments = { navController.navigate("trip/$tripId/documents") },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = "trip/{tripId}/packing",
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
                ) { entry ->
                    val tripId = entry.arguments?.getString("tripId").orEmpty()
                    PackingListScreen(
                        tripId = tripId,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = "trip/{tripId}/budget",
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
                ) { entry ->
                    val tripId = entry.arguments?.getString("tripId").orEmpty()
                    BudgetScreen(
                        tripId = tripId,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = "trip/{tripId}/documents",
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
                ) { entry ->
                    val tripId = entry.arguments?.getString("tripId").orEmpty()
                    DocumentWalletScreen(
                        tripId = tripId,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = "trip/{tripId}/add",
                    arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
                ) { entry ->
                    val tripId = entry.arguments?.getString("tripId").orEmpty()
                    AddActivityScreen(
                        tripId = tripId,
                        activityId = null,
                        onSaved = { navController.popBackStack() },
                        onCancel = { navController.popBackStack() },
                    )
                }
                composable(
                    route = "trip/{tripId}/activity/{activityId}",
                    arguments = listOf(
                        navArgument("tripId") { type = NavType.StringType },
                        navArgument("activityId") { type = NavType.StringType },
                    ),
                ) { entry ->
                    val tripId = entry.arguments?.getString("tripId").orEmpty()
                    val activityId = entry.arguments?.getString("activityId").orEmpty()
                    AddActivityScreen(
                        tripId = tripId,
                        activityId = activityId,
                        onSaved = { navController.popBackStack() },
                        onCancel = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDrawerContent(currentRoute: String?, onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.width(280.dp).fillMaxSize().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("G", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("Guest traveler", style = MaterialTheme.typography.titleMedium)
            Text(
                "Signed in anonymously · this device only",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Divider(modifier = Modifier.padding(horizontal = 12.dp))
        Spacer(modifier = Modifier.height(8.dp))

        DrawerRow(icon = Icons.Filled.CardTravel, label = "Trips", selected = currentRoute == "trips", onClick = { onNavigate("trips") })
        DrawerRow(icon = Icons.Filled.Explore, label = "Explore", selected = currentRoute == "explore", onClick = { onNavigate("explore") })
        DrawerRow(icon = Icons.Filled.Notifications, label = "Notifications", selected = currentRoute == "notifications", onClick = { onNavigate("notifications") })
        DrawerRow(icon = Icons.Filled.Person, label = "Profile", selected = currentRoute == "profile", onClick = { onNavigate("profile") })

        Spacer(modifier = Modifier.weight(1f))
        Divider(modifier = Modifier.padding(horizontal = 12.dp))
        // Settings/About are placeholders until those screens exist; Sign out
        // is inert since an anonymous-only account has nothing to sign out of
        // yet (see AuthRepository).
        DrawerRow(icon = Icons.Filled.Settings, label = "Settings", selected = false, onClick = {})
        DrawerRow(icon = Icons.Filled.Info, label = "About", selected = false, onClick = {})
        DrawerRow(icon = Icons.Filled.Logout, label = "Sign out", selected = false, onClick = {})
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DrawerRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.material3.NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

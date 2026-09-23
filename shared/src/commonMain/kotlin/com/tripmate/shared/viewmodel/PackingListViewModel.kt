package com.tripmate.shared.viewmodel

import com.tripmate.shared.data.PackingRepository
import com.tripmate.shared.model.PackingCategory
import com.tripmate.shared.model.PackingItem
import com.tripmate.shared.model.Trip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.daysUntil
import kotlin.random.Random

data class PackingListUiState(
    val isLoading: Boolean = true,
    val items: List<PackingItem> = emptyList(),
    val packedCount: Int = 0,
    val totalCount: Int = 0,
)

/**
 * Screen state for a trip's packing checklist. Items are grouped by
 * [PackingCategory] in the UI; this class just exposes the flat, sorted
 * list plus the packed/total counts the progress indicator needs.
 */
class PackingListViewModel(
    private val repository: PackingRepository,
    private val tripId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _uiState = MutableStateFlow(PackingListUiState())
    val uiState: StateFlow<PackingListUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            repository.observePackingItems(tripId).collect { items ->
                _uiState.value = PackingListUiState(
                    isLoading = false,
                    items = items.sortedWith(compareBy({ it.category.ordinal }, { it.name })),
                    packedCount = items.count { it.isPacked },
                    totalCount = items.size,
                )
            }
        }
    }

    fun addItem(name: String, category: PackingCategory) {
        if (name.isBlank()) return
        scope.launch {
            repository.createPackingItem(
                PackingItem(
                    id = "packing_${currentEpochMillisSafe()}_${Random.nextInt(1000, 9999)}",
                    tripId = tripId,
                    name = name.trim(),
                    category = category,
                )
            )
        }
    }

    fun togglePacked(item: PackingItem) = scope.launch {
        repository.updatePackingItem(item.copy(isPacked = !item.isPacked))
    }

    fun deleteItem(itemId: String) = scope.launch {
        repository.deletePackingItem(itemId)
    }

    /** Seeds the list with a starter checklist scaled to trip length. A no-op
     * per call site guard (only offered in the UI while the list is empty),
     * not enforced here, so re-tapping isn't silently blocked if that ever
     * changes. */
    fun addSuggestedItems(trip: Trip) {
        scope.launch {
            buildSuggestedPackingList(trip).forEach { repository.createPackingItem(it) }
        }
    }
}

private fun currentEpochMillisSafe() = com.tripmate.shared.util.currentEpochMillis()

/**
 * A generic starter checklist scaled only by trip length (nights), not by
 * destination climate or activities — TripMate doesn't have a weather or
 * itinerary-aware packing engine yet. Good enough as a "don't forget the
 * basics" starting point that the person edits from there.
 */
fun buildSuggestedPackingList(trip: Trip): List<PackingItem> {
    val nights = maxOf(1, trip.startDate.daysUntil(trip.endDate))
    val clothingCount = (nights + 1).coerceAtMost(10)

    fun item(name: String, category: PackingCategory) = PackingItem(
        id = "packing_${com.tripmate.shared.util.currentEpochMillis()}_${Random.nextInt(100000, 999999)}",
        tripId = trip.id,
        name = name,
        category = category,
    )

    return listOf(
        item("Passport / ID", PackingCategory.DOCUMENTS),
        item("Boarding passes", PackingCategory.DOCUMENTS),
        item("Travel insurance info", PackingCategory.DOCUMENTS),
        item("Wallet & cards", PackingCategory.DOCUMENTS),
        item("$clothingCount t-shirts / tops", PackingCategory.CLOTHING),
        item("$clothingCount underwear & socks", PackingCategory.CLOTHING),
        item("Comfortable walking shoes", PackingCategory.CLOTHING),
        item("Light jacket / layer", PackingCategory.CLOTHING),
        item("Sleepwear", PackingCategory.CLOTHING),
        item("Toothbrush & toothpaste", PackingCategory.TOILETRIES),
        item("Deodorant", PackingCategory.TOILETRIES),
        item("Sunscreen", PackingCategory.TOILETRIES),
        item("Phone charger", PackingCategory.ELECTRONICS),
        item("Headphones", PackingCategory.ELECTRONICS),
        item("Power bank / adapter", PackingCategory.ELECTRONICS),
        item("Any daily medications", PackingCategory.HEALTH),
        item("Pain reliever & band-aids", PackingCategory.HEALTH),
        item("Reusable water bottle", PackingCategory.OTHER),
    )
}

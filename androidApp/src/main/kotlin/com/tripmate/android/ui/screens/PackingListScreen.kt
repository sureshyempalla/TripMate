package com.tripmate.android.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.tripmate.shared.data.TripRepository
import com.tripmate.shared.model.PackingCategory
import com.tripmate.shared.model.PackingItem
import com.tripmate.shared.viewmodel.PackingListViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * A trip's packing checklist: items grouped by [PackingCategory], each with
 * a checkbox toggling packed state, a small add-item row, and — while the
 * list is still empty — a "Suggest a packing list" action that seeds a
 * starter checklist scaled to the trip's length.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingListScreen(tripId: String, onBack: () -> Unit) {
    val viewModel = koinInject<PackingListViewModel>(parameters = { parametersOf(tripId) })
    val tripRepository = koinInject<TripRepository>()
    val state by viewModel.uiState.collectAsState()
    val trip by remember(tripId) { tripRepository.observeTrip(tripId) }.collectAsState(initial = null)

    var newItemName by remember { mutableStateOf("") }
    var newItemCategory by remember { mutableStateOf(PackingCategory.OTHER) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Packing list") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.totalCount > 0) {
                PackingProgressBar(packed = state.packedCount, total = state.totalCount)
            }

            AddPackingItemRow(
                name = newItemName,
                onNameChange = { newItemName = it },
                category = newItemCategory,
                onCategoryChange = { newItemCategory = it },
                onAdd = {
                    viewModel.addItem(newItemName, newItemCategory)
                    newItemName = ""
                },
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.items.isEmpty()) {
                EmptyPackingState(
                    enabled = trip != null,
                    onSuggest = { trip?.let(viewModel::addSuggestedItems) },
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    PackingCategory.entries.forEach { category ->
                        val itemsInCategory = state.items.filter { it.category == category }
                        if (itemsInCategory.isNotEmpty()) {
                            item(key = "header_${category.name}") {
                                Text(
                                    category.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                                )
                            }
                            items(itemsInCategory, key = { it.id }) { packingItem ->
                                PackingItemRow(
                                    item = packingItem,
                                    onToggle = { viewModel.togglePacked(packingItem) },
                                    onDelete = { viewModel.deleteItem(packingItem.id) },
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PackingProgressBar(packed: Int, total: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Packed", style = MaterialTheme.typography.labelMedium)
            Text("$packed / $total", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else packed.toFloat() / total },
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
    }
}

@Composable
private fun PackingItemRow(item: PackingItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.isPacked, onCheckedChange = { onToggle() })
        Text(
            item.name,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (item.isPacked) TextDecoration.LineThrough else TextDecoration.None,
            color = if (item.isPacked) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete ${item.name}",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddPackingItemRow(
    name: String,
    onNameChange: (String) -> Unit,
    category: PackingCategory,
    onCategoryChange: (PackingCategory) -> Unit,
    onAdd: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("Add an item") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.height(0.dp))
            OutlinedButton(onClick = onAdd, enabled = name.isNotBlank(), modifier = Modifier.padding(start = 8.dp)) {
                Text("Add")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PackingCategory.entries.forEach { entry ->
                FilterChip(
                    selected = entry == category,
                    onClick = { onCategoryChange(entry) },
                    label = { Text(entry.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
    }
}

@Composable
private fun EmptyPackingState(enabled: Boolean, onSuggest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(40.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("Nothing on your list yet", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Add items above, or let us suggest a starter checklist for this trip.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onSuggest, enabled = enabled) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.height(18.dp))
            Spacer(modifier = Modifier.height(0.dp))
            Text("  Suggest a packing list")
        }
    }
}

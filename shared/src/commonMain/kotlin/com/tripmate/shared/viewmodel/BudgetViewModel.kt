package com.tripmate.shared.viewmodel

import com.tripmate.shared.data.TripRepository
import com.tripmate.shared.model.Expense
import com.tripmate.shared.model.ExpenseCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/** One currency's running total — kept separate from other currencies rather
 * than converted, since TripMate has no live FX rate source. Most trips only
 * ever log expenses in one currency, so this collapses to a single row in
 * the common case. */
data class CurrencyTotal(val currencyCode: String, val totalMinorUnits: Long)

data class CategoryTotal(val category: ExpenseCategory, val currencyCode: String, val totalMinorUnits: Long)

data class BudgetUiState(
    val isLoading: Boolean = true,
    val expenses: List<Expense> = emptyList(),
    val totalsByCurrency: List<CurrencyTotal> = emptyList(),
    val totalsByCategory: List<CategoryTotal> = emptyList(),
)

/** Screen state for a trip's expense log. There is no live currency
 * conversion — amounts are totaled within each currency separately. */
class BudgetViewModel(
    private val repository: TripRepository,
    private val tripId: String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            repository.observeExpenses(tripId).collect { expenses ->
                _uiState.value = BudgetUiState(
                    isLoading = false,
                    expenses = expenses.sortedByDescending { it.spentAtEpochMillis },
                    totalsByCurrency = expenses
                        .groupBy { it.currencyCode }
                        .map { (currency, items) -> CurrencyTotal(currency, items.sumOf { it.amountMinorUnits }) }
                        .sortedByDescending { it.totalMinorUnits },
                    totalsByCategory = expenses
                        .groupBy { it.category to it.currencyCode }
                        .map { (key, items) -> CategoryTotal(key.first, key.second, items.sumOf { it.amountMinorUnits }) }
                        .sortedByDescending { it.totalMinorUnits },
                )
            }
        }
    }

    fun addExpense(title: String, amountMinorUnits: Long, currencyCode: String, category: ExpenseCategory, notes: String?) {
        if (title.isBlank() || amountMinorUnits <= 0 || currencyCode.isBlank()) return
        scope.launch {
            repository.createExpense(
                Expense(
                    id = "expense_${currentEpochMillisSafe()}_${Random.nextInt(1000, 9999)}",
                    tripId = tripId,
                    title = title.trim(),
                    amountMinorUnits = amountMinorUnits,
                    currencyCode = currencyCode.trim().uppercase(),
                    category = category,
                    spentAtEpochMillis = currentEpochMillisSafe(),
                    notes = notes?.ifBlank { null },
                )
            )
        }
    }

    fun deleteExpense(expenseId: String) = scope.launch {
        repository.deleteExpense(expenseId)
    }
}

private fun currentEpochMillisSafe() = com.tripmate.shared.util.currentEpochMillis()

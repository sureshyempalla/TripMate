package com.tripmate.shared.data

import com.tripmate.shared.model.Expense
import kotlinx.coroutines.flow.Flow

/** CRUD + observe for a trip's logged expenses. */
interface BudgetRepository {
    fun observeExpenses(tripId: String): Flow<List<Expense>>
    suspend fun createExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expenseId: String)
}

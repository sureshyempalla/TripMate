package com.tripmate.shared.data

import com.tripmate.db.ExpenseEntity
import com.tripmate.db.TripMateDatabase
import com.tripmate.shared.model.Expense
import com.tripmate.shared.model.ExpenseCategory
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow

class FirestoreBudgetRepository(
    private val db: TripMateDatabase,
) : BudgetRepository {

    private val firestore get() = Firebase.firestore

    override fun observeExpenses(tripId: String): Flow<List<Expense>> =
        db.expenseQueries.selectExpensesByTrip(tripId).asFlowList { it.toDomain() }

    override suspend fun createExpense(expense: Expense) {
        firestore.collection(FirestoreCollections.EXPENSES).document(expense.id).set(expense)
    }

    override suspend fun updateExpense(expense: Expense) {
        firestore.collection(FirestoreCollections.EXPENSES).document(expense.id).set(expense)
    }

    override suspend fun deleteExpense(expenseId: String) {
        firestore.collection(FirestoreCollections.EXPENSES).document(expenseId).delete()
        db.expenseQueries.deleteExpense(expenseId)
    }
}

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    tripId = tripId,
    title = title,
    amountMinorUnits = amountMinorUnits,
    currencyCode = currencyCode,
    category = ExpenseCategory.valueOf(category),
    spentAtEpochMillis = spentAtEpochMillis,
    notes = notes,
)

internal fun upsertExpenseEntity(db: TripMateDatabase, expense: Expense) {
    db.expenseQueries.upsertExpense(
        expense.id, expense.tripId, expense.title, expense.amountMinorUnits,
        expense.currencyCode, expense.category.name, expense.spentAtEpochMillis, expense.notes,
    )
}

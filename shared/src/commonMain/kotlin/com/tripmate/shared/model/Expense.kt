package com.tripmate.shared.model

import kotlinx.serialization.Serializable

enum class ExpenseCategory { LODGING, FOOD, TRANSPORT, ACTIVITIES, SHOPPING, OTHER }

/**
 * A single spend logged against a trip. Amounts are stored in minor units
 * (cents) as a Long to avoid floating-point rounding when summing totals —
 * the UI divides by 100 only at display time.
 */
@Serializable
data class Expense(
    val id: String,
    val tripId: String,
    val title: String,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val spentAtEpochMillis: Long,
    val notes: String? = null,
)

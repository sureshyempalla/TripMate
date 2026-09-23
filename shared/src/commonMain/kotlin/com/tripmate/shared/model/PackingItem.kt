package com.tripmate.shared.model

import kotlinx.serialization.Serializable

enum class PackingCategory { DOCUMENTS, CLOTHING, TOILETRIES, ELECTRONICS, HEALTH, OTHER }

@Serializable
data class PackingItem(
    val id: String,
    val tripId: String,
    val name: String,
    val category: PackingCategory = PackingCategory.OTHER,
    val isPacked: Boolean = false,
)

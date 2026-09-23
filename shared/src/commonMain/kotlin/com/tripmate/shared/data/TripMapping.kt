package com.tripmate.shared.data

import com.tripmate.db.ActivityEntity
import com.tripmate.db.ExpenseEntity
import com.tripmate.db.PackingItemEntity
import com.tripmate.db.TripDocumentEntity
import com.tripmate.db.TripEntity
import com.tripmate.shared.model.Activity
import com.tripmate.shared.model.ActivityCategory
import com.tripmate.shared.model.DocumentType
import com.tripmate.shared.model.Expense
import com.tripmate.shared.model.ExpenseCategory
import com.tripmate.shared.model.PackingCategory
import com.tripmate.shared.model.PackingItem
import com.tripmate.shared.model.Place
import com.tripmate.shared.model.Trip
import com.tripmate.shared.model.TripDocument
import kotlinx.datetime.LocalDate

// Mapping between the generated SQLDelight row classes and the plain
// domain models the rest of the app (ViewModels, UI, Firestore layer) uses.
// Keeping this in one file means a column rename only breaks compilation
// here, not scattered across call sites.

fun TripEntity.toDomain() = Trip(
    id = id,
    ownerId = ownerId,
    title = title,
    destination = destination,
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    coverImageUrl = coverImageUrl,
    createdAtEpochMillis = createdAtEpochMillis,
)

fun ActivityEntity.toDomain() = Activity(
    id = id,
    tripId = tripId,
    title = title,
    category = ActivityCategory.valueOf(category),
    place = placeName?.let {
        Place(
            name = it,
            address = placeAddress,
            latitude = placeLatitude,
            longitude = placeLongitude,
            providerPlaceId = placeProviderPlaceId,
        )
    },
    startAtEpochMillis = startAtEpochMillis,
    endAtEpochMillis = endAtEpochMillis,
    notes = notes,
    reminderLeadMinutes = reminderLeadMinutes.toInt(),
)


fun PackingItemEntity.toDomain() = PackingItem(
    id = id,
    tripId = tripId,
    name = name,
    category = PackingCategory.valueOf(category),
    isPacked = isPacked == 1L,
)

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

fun TripDocumentEntity.toDomain() = TripDocument(
    id = id,
    tripId = tripId,
    title = title,
    type = DocumentType.valueOf(type),
    localUri = localUri,
    addedAtEpochMillis = addedAtEpochMillis,
)

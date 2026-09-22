package com.tripmate.shared.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <T : Any, R> Query<T>.asFlowList(mapper: (T) -> R): Flow<List<R>> =
    asFlow().mapToList(Dispatchers.Default).map { rows -> rows.map(mapper) }

fun <T : Any, R> Query<T>.asFlowOneOrNull(mapper: (T) -> R): Flow<R?> =
    asFlow().mapToOneOrNull(Dispatchers.Default).map { row -> row?.let(mapper) }

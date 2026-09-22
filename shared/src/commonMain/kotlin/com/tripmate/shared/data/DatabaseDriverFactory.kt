package com.tripmate.shared.data

import app.cash.sqldelight.db.SqlDriver

/** Platform-specific SQLDelight driver creation (Android SQLite vs iOS NativeSqlite). */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

package com.tripmate.shared.util

import kotlinx.datetime.Clock

fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

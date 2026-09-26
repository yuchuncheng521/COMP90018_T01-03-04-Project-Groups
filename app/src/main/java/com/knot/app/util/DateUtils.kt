package com.knot.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Generates the list of months a group has existed for, newest first --
 * e.g. group created Jan 2025, today is Sep 2026 -> ["September 2026",
 * "August 2026", ..., "January 2025"]. This replaces a hardcoded month list:
 * it always matches how long the group has actually been running.
 *
 * Pure function (no Firebase) -- easy to unit test on its own.
 */
fun monthsSince(createdAtMillis: Long, nowMillis: Long = System.currentTimeMillis()): List<String> {
    if (createdAtMillis <= 0L) return emptyList()

    val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val cursor = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val start = Calendar.getInstance().apply {
        timeInMillis = createdAtMillis
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val months = mutableListOf<String>()
    while (!cursor.before(start)) {
        months.add(formatter.format(cursor.time))
        cursor.add(Calendar.MONTH, -1)
    }
    return months
}

package com.knot.app.notifications

import android.content.Context

object NotificationPreferences {

    private const val PREFS_NAME = "knot_notification_preferences"
    private const val KEY_PUSH_ENABLED = "push_notifications_enabled"

    fun isPushEnabled(context: Context): Boolean =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(KEY_PUSH_ENABLED, true)

    fun setPushEnabled(
        context: Context,
        enabled: Boolean
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putBoolean(KEY_PUSH_ENABLED, enabled)
            .apply()
    }
}

package com.knot.app.notifications

import android.content.Context

object NotificationPreferences {

    private const val PREFS_NAME = "knot_notification_preferences"
    private const val KEY_PUSH_ENABLED = "push_notifications_enabled"
    private const val KEY_WEEKLY_PROMPT_REMINDERS_ENABLED = "weekly_prompt_reminders_enabled"

    fun isPushEnabled(context: Context): Boolean =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(KEY_PUSH_ENABLED, false)

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

    fun areWeeklyPromptRemindersEnabled(context: Context): Boolean =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(KEY_WEEKLY_PROMPT_REMINDERS_ENABLED, false)

    fun setWeeklyPromptRemindersEnabled(
        context: Context,
        enabled: Boolean
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putBoolean(KEY_WEEKLY_PROMPT_REMINDERS_ENABLED, enabled)
            .apply()
    }
}

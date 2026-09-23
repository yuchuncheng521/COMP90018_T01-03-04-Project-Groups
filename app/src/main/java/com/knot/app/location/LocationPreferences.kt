package com.knot.app.location

import android.content.Context

object LocationPreferences {

    private const val PREFS_NAME = "knot_location_preferences"
    private const val KEY_ATTACH_LOCATION = "attach_location_to_memories"

    fun isAttachLocationEnabled(context: Context): Boolean =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        ).getBoolean(KEY_ATTACH_LOCATION, false)

    fun setAttachLocationEnabled(
        context: Context,
        enabled: Boolean
    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putBoolean(KEY_ATTACH_LOCATION, enabled)
            .apply()
    }
}

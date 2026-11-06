package com.example.liftlogic.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.liftlogic.R

/** Small SharedPreferences wrapper for appearance settings and the profile picture. */
object AppPrefs {

    private const val FILE = "liftlogic_prefs"
    private const val KEY_NIGHT_MODE = "night_mode"
    private const val KEY_ACCENT = "accent"
    private const val KEY_AVATAR_PREFIX = "avatar_uri_"

    const val ACCENT_GREEN = "green"
    const val ACCENT_BLUE = "blue"
    const val ACCENT_PURPLE = "purple"
    const val ACCENT_ORANGE = "orange"
    const val ACCENT_RED = "red"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Dark by default, like the original LiftLogic design. */
    fun getNightMode(context: Context): Int =
        prefs(context).getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_YES)

    fun setNightMode(context: Context, mode: Int) {
        prefs(context).edit().putInt(KEY_NIGHT_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getAccent(context: Context): String =
        prefs(context).getString(KEY_ACCENT, ACCENT_GREEN) ?: ACCENT_GREEN

    fun setAccent(context: Context, accent: String) {
        prefs(context).edit().putString(KEY_ACCENT, accent).apply()
    }

    fun accentOverlay(accent: String): Int = when (accent) {
        ACCENT_BLUE -> R.style.ThemeOverlay_LiftLogic_Accent_Blue
        ACCENT_PURPLE -> R.style.ThemeOverlay_LiftLogic_Accent_Purple
        ACCENT_ORANGE -> R.style.ThemeOverlay_LiftLogic_Accent_Orange
        ACCENT_RED -> R.style.ThemeOverlay_LiftLogic_Accent_Red
        else -> R.style.ThemeOverlay_LiftLogic_Accent_Green
    }

    // ----- Daily workout reminder -----
    private const val KEY_REMINDER_ON = "reminder_on"
    private const val KEY_REMINDER_HOUR = "reminder_hour"
    private const val KEY_REMINDER_MINUTE = "reminder_minute"
    private const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"

    fun isReminderOn(context: Context): Boolean = prefs(context).getBoolean(KEY_REMINDER_ON, false)
    fun reminderHour(context: Context): Int = prefs(context).getInt(KEY_REMINDER_HOUR, 18)
    fun reminderMinute(context: Context): Int = prefs(context).getInt(KEY_REMINDER_MINUTE, 0)

    fun setReminder(context: Context, on: Boolean, hour: Int, minute: Int) {
        prefs(context).edit()
            .putBoolean(KEY_REMINDER_ON, on)
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
    }

    /** True the first time it is called, so we only ask for notification permission once. */
    fun shouldAskNotifications(context: Context): Boolean {
        val asked = prefs(context).getBoolean(KEY_ASKED_NOTIFICATIONS, false)
        if (!asked) prefs(context).edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply()
        return !asked
    }

    fun getAvatarUri(context: Context, userId: String): String? =
        prefs(context).getString(KEY_AVATAR_PREFIX + userId, null)

    fun setAvatarUri(context: Context, userId: String, uri: String?) {
        prefs(context).edit().apply {
            if (uri == null) remove(KEY_AVATAR_PREFIX + userId) else putString(KEY_AVATAR_PREFIX + userId, uri)
        }.apply()
    }
}

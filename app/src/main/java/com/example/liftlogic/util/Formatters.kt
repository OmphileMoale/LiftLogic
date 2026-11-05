package com.example.liftlogic.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    /** "1h 05m" or "45m" or "0m" */
    fun duration(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return if (hours > 0) "%dh %02dm".format(hours, minutes) else "%dm".format(minutes)
    }

    /** "05:07" or "1:05:07" for live timers */
    fun clock(totalSeconds: Int): String {
        val s = totalSeconds.coerceAtLeast(0)
        val hours = s / 3600
        val minutes = (s % 3600) / 60
        val seconds = s % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
    }

    fun date(millis: Long): String = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(millis))

    fun shortDate(millis: Long): String = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))

    fun dateTime(millis: Long): String = SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault()).format(Date(millis))

    fun monthYear(millis: Long): String = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(millis))

    fun time(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)
}

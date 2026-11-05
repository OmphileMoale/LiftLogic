package com.example.liftlogic.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.liftlogic.LiftLogicApp
import com.example.liftlogic.util.AppPrefs
import com.example.liftlogic.util.Stats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

/** Schedules the optional daily "time to train" reminder with AlarmManager. */
object ReminderScheduler {

    fun apply(context: Context) {
        if (AppPrefs.isReminderOn(context)) {
            schedule(context, AppPrefs.reminderHour(context), AppPrefs.reminderMinute(context))
        } else {
            cancel(context)
        }
    }

    private fun schedule(context: Context, hour: Int, minute: Int) {
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        // Inexact repeating alarms need no special permission and are battery friendly
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent(context))
    }

    private fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            42,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
}

/** Fires once a day; shows the reminder only if the user hasn't trained today. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as LiftLogicApp).container
        val userId = container.session.currentUserId ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val trainedToday = container.db.workoutDao().getFinishedWorkouts(userId)
                    .any { w -> w.finishedAt?.let { Stats.toDate(it) == LocalDate.now() } == true }
                if (!trainedToday) container.notifications.workoutReminder()
            } finally {
                pending.finish()
            }
        }
    }
}

/** Alarms are cleared on reboot, so schedule the reminder again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) ReminderScheduler.apply(context)
    }
}

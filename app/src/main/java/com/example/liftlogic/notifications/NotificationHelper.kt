package com.example.liftlogic.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.liftlogic.MainActivity
import com.example.liftlogic.R

/** Creates notification channels and posts the rest-timer and reminder notifications. */
class NotificationHelper(private val context: Context) {

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REST, context.getString(R.string.channel_rest_timer), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.channel_rest_timer_desc) }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.channel_reminders_desc) }
        )
    }

    fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun restFinished() {
        vibrate()
        post(
            REST_ID,
            NotificationCompat.Builder(context, CHANNEL_REST)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle(context.getString(R.string.notif_rest_over_title))
                .setContentText(context.getString(R.string.notif_rest_over_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setTimeoutAfter(60_000)
                .setContentIntent(openAppIntent())
        )
    }

    fun cancelRest() {
        NotificationManagerCompat.from(context).cancel(REST_ID)
    }

    fun workoutReminder() {
        post(
            REMINDER_ID,
            NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_fitness_center)
                .setContentTitle(context.getString(R.string.notif_reminder_title))
                .setContentText(context.getString(R.string.notif_reminder_text))
                .setAutoCancel(true)
                .setContentIntent(openAppIntent())
        )
    }

    private fun post(id: Int, builder: NotificationCompat.Builder) {
        if (!canPostNotifications()) return
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (e: SecurityException) {
            // Permission was revoked while the app was running
        }
    }

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun vibrate() {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
    }

    companion object {
        const val CHANNEL_REST = "rest_timer"
        const val CHANNEL_REMINDERS = "reminders"
        private const val REST_ID = 1001
        private const val REMINDER_ID = 1002
    }
}

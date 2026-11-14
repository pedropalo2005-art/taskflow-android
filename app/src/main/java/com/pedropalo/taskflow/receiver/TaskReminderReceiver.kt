package com.pedropalo.taskflow.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pedropalo.taskflow.MainActivity

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Tarea Pendiente"
        val taskCategory = intent.getStringExtra(EXTRA_TASK_CATEGORY) ?: "General"

        showNotification(context, taskId, taskTitle, taskCategory)
    }

    private fun showNotification(
        context: Context,
        taskId: String,
        title: String,
        category: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de Tareas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarmas de alta precisión para tareas y compromisos"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(alarmSound, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ Recordatorio: $category")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Tienes pendiente realizar ahora:\n$title"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "task_reminders_channel_exact"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_CATEGORY = "extra_task_category"

        /**
         * Programa una alarma con máxima precisión horaria.
         * En Android moderno, setAlarmClock() garantiza que la alarma NO sea aplazada
         * ni retrasada por el modo Doze de ahorro de batería.
         */
        fun scheduleAlarm(
            context: Context,
            taskId: String,
            title: String,
            category: String,
            timestamp: Long
        ) {
            if (timestamp <= System.currentTimeMillis()) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val intent = Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TITLE, title)
                putExtra(EXTRA_TASK_CATEGORY, category)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val showAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                taskId.hashCode() + 100,
                showAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                // Estándar de oro: se ejecuta al segundo exacto sin retrasos del sistema
                val alarmClockInfo = AlarmManager.AlarmClockInfo(timestamp, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } catch (e: Exception) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            timestamp,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            timestamp,
                            pendingIntent
                        )
                    }
                } catch (e2: Exception) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, timestamp, pendingIntent)
                }
            }
        }

        fun cancelAlarm(context: Context, taskId: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, TaskReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }
}

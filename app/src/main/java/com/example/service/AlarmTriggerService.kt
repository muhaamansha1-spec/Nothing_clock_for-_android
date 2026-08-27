package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.model.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object AlarmStateHolder {
    val ringingAlarm = MutableStateFlow<Alarm?>(null)
    val isTimerRinging = MutableStateFlow(false)
}

class AlarmTriggerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private const val TAG = "AlarmTriggerService"
        const val CHANNEL_ID = "alarm_timer_channel"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START_ALARM = "com.example.service.ACTION_START_ALARM"
        const val ACTION_START_TIMER = "com.example.service.ACTION_START_TIMER"
        const val ACTION_STOP_ALARM = "com.example.service.ACTION_STOP_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.service.ACTION_SNOOZE_ALARM"
        const val ACTION_STOP_TIMER = "com.example.service.ACTION_STOP_TIMER"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action: $action")
        when (action) {
            ACTION_START_ALARM -> {
                val id = intent.getIntExtra("alarm_id", -1)
                val label = intent.getStringExtra("alarm_label") ?: ""
                val hour = intent.getIntExtra("alarm_hour", 0)
                val minute = intent.getIntExtra("alarm_minute", 0)
                val ringtone = intent.getStringExtra("alarm_ringtone") ?: "GLYPH RAPID"

                val alarm = Alarm(
                    id = id,
                    hour = hour,
                    minute = minute,
                    daysOfWeek = "",
                    label = label,
                    isEnabled = true,
                    ringtone = ringtone
                )

                AlarmStateHolder.ringingAlarm.value = alarm
                AudioSynthPlayer.play(ringtone)

                startForeground(NOTIFICATION_ID, buildAlarmNotification(alarm))
            }
            ACTION_START_TIMER -> {
                val ringtone = intent.getStringExtra("timer_ringtone") ?: "GLYPH RAPID"
                AlarmStateHolder.isTimerRinging.value = true
                AudioSynthPlayer.play(ringtone)

                startForeground(NOTIFICATION_ID, buildTimerNotification())
            }
            ACTION_STOP_ALARM -> {
                stopAlarmAndService()
            }
            ACTION_SNOOZE_ALARM -> {
                snoozeAlarmAndService()
            }
            ACTION_STOP_TIMER -> {
                stopTimerAndService()
            }
        }
        return START_NOT_STICKY
    }

    private fun stopAlarmAndService() {
        val alarm = AlarmStateHolder.ringingAlarm.value
        AlarmStateHolder.ringingAlarm.value = null
        AudioSynthPlayer.stop()
        
        if (alarm != null && alarm.id != -1) {
            serviceScope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val dao = db.alarmDao()
                val existing = dao.getAlarmById(alarm.id)
                if (existing != null) {
                    if (existing.isRecurring) {
                        SystemAlarmScheduler.scheduleAlarm(applicationContext, existing)
                    } else {
                        val updated = existing.copy(isEnabled = false)
                        dao.updateAlarm(updated)
                        SystemAlarmScheduler.cancelAlarm(applicationContext, updated)
                    }
                }
            }
        }
        stopForeground(true)
        stopSelf()
    }

    private fun snoozeAlarmAndService() {
        val alarm = AlarmStateHolder.ringingAlarm.value
        AlarmStateHolder.ringingAlarm.value = null
        AudioSynthPlayer.stop()

        if (alarm != null) {
            serviceScope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val dao = db.alarmDao()
                val existing = if (alarm.id != -1) dao.getAlarmById(alarm.id) else null

                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.MINUTE, 5)
                val snoozeHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val snoozeMin = cal.get(java.util.Calendar.MINUTE)
                val snoozeLabel = if (alarm.label.isEmpty()) "SNOOZE" else "${alarm.label} (SNOOZE)"

                val snoozeAlarm = Alarm(
                    hour = snoozeHour,
                    minute = snoozeMin,
                    daysOfWeek = "",
                    label = snoozeLabel,
                    isEnabled = true,
                    ringtone = alarm.ringtone
                )
                
                val insertedId = dao.insertAlarm(snoozeAlarm).toInt()
                val scheduledSnooze = snoozeAlarm.copy(id = insertedId)
                SystemAlarmScheduler.scheduleAlarm(applicationContext, scheduledSnooze)

                if (existing != null && existing.isRecurring) {
                    SystemAlarmScheduler.scheduleAlarm(applicationContext, existing)
                } else if (existing != null) {
                    dao.updateAlarm(existing.copy(isEnabled = false))
                }
            }
        }
        stopForeground(true)
        stopSelf()
    }

    private fun stopTimerAndService() {
        AlarmStateHolder.isTimerRinging.value = false
        AudioSynthPlayer.stop()
        stopForeground(true)
        stopSelf()
    }

    private fun buildAlarmNotification(alarm: Alarm): Notification {
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmNotificationReceiver::class.java).apply {
            action = AlarmNotificationReceiver.ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmNotificationReceiver::class.java).apply {
            action = AlarmNotificationReceiver.ACTION_SNOOZE_ALARM
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (alarm.label.isEmpty()) "ALARM" else alarm.label.uppercase()
        val timeStr = String.format("%02d:%02d", alarm.hour, alarm.minute)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Ringing at $timeStr")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DISMISS", dismissPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "SNOOZE", snoozePendingIntent)
            .build()
    }

    private fun buildTimerNotification(): Notification {
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmNotificationReceiver::class.java).apply {
            action = AlarmNotificationReceiver.ACTION_DISMISS_TIMER
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            3,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("TIMER FINISHED")
            .setContentText("Your timer has completed!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "DISMISS", dismissPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarms & Timers Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification channel for alarms and timer events"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

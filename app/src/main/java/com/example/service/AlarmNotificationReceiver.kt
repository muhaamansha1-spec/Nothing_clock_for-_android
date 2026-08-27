package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.launch

class AlarmNotificationReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmNotificationReceiver"
        const val ACTION_ALARM_TRIGGER = "com.example.ACTION_ALARM_TRIGGER"
        const val ACTION_TIMER_TRIGGER = "com.example.ACTION_TIMER_TRIGGER"

        const val ACTION_DISMISS_ALARM = "com.example.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.ACTION_SNOOZE_ALARM"
        const val ACTION_DISMISS_TIMER = "com.example.ACTION_DISMISS_TIMER"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "onReceive action: $action")
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val db = com.example.data.local.AppDatabase.getDatabase(context)
                val pendingResult = goAsync()
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val alarms = db.alarmDao().getAllAlarmsList()
                        for (alarm in alarms) {
                            if (alarm.isEnabled) {
                                SystemAlarmScheduler.scheduleAlarm(context, alarm)
                            }
                        }
                        pendingResult.finish()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error rescheduling alarms on boot", e)
                        pendingResult.finish()
                    }
                }
            }
            ACTION_ALARM_TRIGGER -> {
                val serviceIntent = Intent(context, AlarmTriggerService::class.java).apply {
                    setAction(AlarmTriggerService.ACTION_START_ALARM)
                    putExtra("alarm_id", intent.getIntExtra("alarm_id", -1))
                    putExtra("alarm_label", intent.getStringExtra("alarm_label") ?: "")
                    putExtra("alarm_hour", intent.getIntExtra("alarm_hour", 0))
                    putExtra("alarm_minute", intent.getIntExtra("alarm_minute", 0))
                    putExtra("alarm_ringtone", intent.getStringExtra("alarm_ringtone") ?: "GLYPH RAPID")
                }
                startServiceHelper(context, serviceIntent)
            }
            ACTION_TIMER_TRIGGER -> {
                val serviceIntent = Intent(context, AlarmTriggerService::class.java).apply {
                    setAction(AlarmTriggerService.ACTION_START_TIMER)
                    putExtra("timer_ringtone", intent.getStringExtra("timer_ringtone") ?: "GLYPH RAPID")
                }
                startServiceHelper(context, serviceIntent)
            }
            ACTION_DISMISS_ALARM -> {
                val serviceIntent = Intent(context, AlarmTriggerService::class.java).apply {
                    setAction(AlarmTriggerService.ACTION_STOP_ALARM)
                }
                startServiceHelper(context, serviceIntent)
            }
            ACTION_SNOOZE_ALARM -> {
                val serviceIntent = Intent(context, AlarmTriggerService::class.java).apply {
                    setAction(AlarmTriggerService.ACTION_SNOOZE_ALARM)
                }
                startServiceHelper(context, serviceIntent)
            }
            ACTION_DISMISS_TIMER -> {
                val serviceIntent = Intent(context, AlarmTriggerService::class.java).apply {
                    setAction(AlarmTriggerService.ACTION_STOP_TIMER)
                }
                startServiceHelper(context, serviceIntent)
            }
        }
    }

    private fun startServiceHelper(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}

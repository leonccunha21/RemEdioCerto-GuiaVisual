package com.zmstore.projectr.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zmstore.projectr.data.model.Medication
import com.zmstore.projectr.receiver.AlarmReceiver
import java.util.*
import java.util.concurrent.TimeUnit

object MedicationAlarmHelper {

    fun scheduleAlarm(context: Context, medication: Medication) {
        if (!medication.isActive) {
            cancelAlarm(context, medication)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_MEDICATION_ID, medication.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medication.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = calculateNextTriggerTime(medication)

        if (triggerTime > 0) {
            setAlarm(alarmManager, triggerTime, pendingIntent)
        }
    }

    fun scheduleSnooze(context: Context, medication: Medication) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_MEDICATION_ID, medication.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medication.id + 99999, // Unique ID for snooze
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10)
        setAlarm(alarmManager, snoozeTime, pendingIntent)
    }

    private fun setAlarm(alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, medication: Medication) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medication.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun calculateNextTriggerTime(medication: Medication): Long {
        val currentTime = System.currentTimeMillis()
        
        if (!medication.customTimes.isNullOrBlank()) {
            val times = medication.customTimes.split(",").map { it.trim() }
            var nextDoseTimeInMillis = Long.MAX_VALUE
            var timeFound = false

            for (timeStr in times) {
                val parts = timeStr.split(":")
                if (parts.size == 2) {
                    val h = parts[0].toIntOrNull() ?: 0
                    val m = parts[1].toIntOrNull() ?: 0
                    
                    val testCal = Calendar.getInstance()
                    testCal.timeInMillis = currentTime
                    testCal.set(Calendar.HOUR_OF_DAY, h)
                    testCal.set(Calendar.MINUTE, m)
                    testCal.set(Calendar.SECOND, 0)
                    
                    var testTime = testCal.timeInMillis
                    if (testTime <= currentTime + 30000) { 
                        testTime += TimeUnit.DAYS.toMillis(1)
                    }

                    if (testTime < nextDoseTimeInMillis) {
                        nextDoseTimeInMillis = testTime
                        timeFound = true
                    }
                }
            }
            return if (timeFound) nextDoseTimeInMillis else 0L
        } else if (medication.intervalHours > 0) {
            val baseTime = if (medication.lastTakenTimestamp > 0) medication.lastTakenTimestamp else currentTime
            var nextTime = baseTime + TimeUnit.HOURS.toMillis(medication.intervalHours.toLong())
            
            while (nextTime <= currentTime) {
                nextTime += TimeUnit.HOURS.toMillis(medication.intervalHours.toLong())
            }
            return nextTime
        }
        
        return 0L
    }
}

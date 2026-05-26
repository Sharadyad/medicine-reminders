package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.Medicine
import java.util.Calendar

class MedicineAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(medicine: Medicine) {
        if (!medicine.isAlarmEnabled) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MEDICINE_ID", medicine.id)
            putExtra("MEDICINE_NAME", medicine.name)
            putExtra("MEDICINE_DOSAGE", medicine.dosage)
            putExtra("MEDICINE_HOUR", medicine.timeHour)
            putExtra("MEDICINE_MINUTE", medicine.timeMinute)
            putExtra("MEDICINE_YEAR", medicine.year ?: -1)
            putExtra("MEDICINE_MONTH", medicine.month ?: -1)
            putExtra("MEDICINE_DAY", medicine.day ?: -1)
            putExtra("MEDICINE_RINGTONE_URI", medicine.ringtoneUri)
            putExtra("MEDICINE_RINGTONE_NAME", medicine.ringtoneName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, medicine.timeHour)
            set(Calendar.MINUTE, medicine.timeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (medicine.isOneTime) {
                set(Calendar.YEAR, medicine.year!!)
                set(Calendar.MONTH, medicine.month!!)
                set(Calendar.DAY_OF_MONTH, medicine.day!!)
            } else {
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        // Only schedule if the alarm time is in the future
        if (calendar.timeInMillis <= System.currentTimeMillis() && medicine.isOneTime) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancel(medicine: Medicine) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

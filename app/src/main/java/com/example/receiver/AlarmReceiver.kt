package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Medicine
import com.example.data.MedicineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getLongExtra("MEDICINE_ID", -1L)
        val medicineName = intent.getStringExtra("MEDICINE_NAME") ?: "Medicine"
        val medicineDosage = intent.getStringExtra("MEDICINE_DOSAGE") ?: "1 tab"
        val timeHour = intent.getIntExtra("MEDICINE_HOUR", 0)
        val timeMinute = intent.getIntExtra("MEDICINE_MINUTE", 0)
        val year = intent.getIntExtra("MEDICINE_YEAR", -1)
        val month = intent.getIntExtra("MEDICINE_MONTH", -1)
        val day = intent.getIntExtra("MEDICINE_DAY", -1)
        val ringtoneUriString = intent.getStringExtra("MEDICINE_RINGTONE_URI")
        val ringtoneName = intent.getStringExtra("MEDICINE_RINGTONE_NAME")

        if (medicineId == -1L) return

        val isOneTime = year != -1 && month != -1 && day != -1

        // 1. Play custom audio and display notification status
        playAlarmSound(context, ringtoneUriString)
        showNotification(context, medicineId, medicineName, medicineDosage, ringtoneUriString)

        // 2. Coroutine block for Room DB manipulation & reschedule logic
        val database = AppDatabase.getDatabase(context)
        val repository = MedicineRepository(database.medicineDao())
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (isOneTime) {
                    val medicine = repository.getMedicineById(medicineId)
                    if (medicine != null) {
                        repository.update(medicine.copy(isAlarmEnabled = false))
                    }
                } else {
                    val alarmScheduler = MedicineAlarmScheduler(context)
                    val rescheduleMed = Medicine(
                        id = medicineId,
                        name = medicineName,
                        dosage = medicineDosage,
                        timeHour = timeHour,
                        timeMinute = timeMinute,
                        isAlarmEnabled = true,
                        year = if (year != -1) year else null,
                        month = if (month != -1) month else null,
                        day = if (day != -1) day else null,
                        ringtoneUri = ringtoneUriString,
                        ringtoneName = ringtoneName
                    )
                    alarmScheduler.schedule(rescheduleMed)
                }
            } catch (e: Exception) {
                // Graceful fallback
            }
        }
    }

    private fun playAlarmSound(context: Context, uriString: String?) {
        val uri: Uri = if (!uriString.isNullOrEmpty()) {
            try {
                Uri.parse(uriString)
            } catch (e: Exception) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        try {
            val ringtone = RingtoneManager.getRingtone(context, uri)
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ringtone.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtone.play()
                // Play for 15 seconds then stop gracefully
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        if (ringtone.isPlaying) {
                            ringtone.stop()
                        }
                    } catch (e: Exception) {}
                }, 15000)
            }
        } catch (e: Exception) {
            try {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                RingtoneManager.getRingtone(context, fallbackUri)?.play()
            } catch (ex: Exception) {}
        }
    }

    private fun showNotification(context: Context, id: Long, name: String, dosage: String, uriString: String?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "medicine_reminders_channel_v1"

        val alarmSound: Uri = if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled daily medicine"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(alarmSound, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Take Your Medicine")
            .setContentText("Reminder to take $name ($dosage)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setContentIntent(pendingIntent)

        notificationManager.notify(id.toInt(), builder.build())
    }
}

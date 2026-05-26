package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.MedicineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val database = AppDatabase.getDatabase(context)
            val repository = MedicineRepository(database.medicineDao())
            val alarmScheduler = MedicineAlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val medicines = repository.allMedicines.first()
                    for (medicine in medicines) {
                        if (medicine.isAlarmEnabled) {
                            alarmScheduler.schedule(medicine)
                        }
                    }
                } catch (e: Exception) {
                    // Safe silent fail during background restoration
                }
            }
        }
    }
}

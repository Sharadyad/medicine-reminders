package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Medicine
import com.example.data.MedicineRepository
import com.example.receiver.MedicineAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedicineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MedicineRepository
    private val scheduler: MedicineAlarmScheduler
    private val sharedPrefs = application.getSharedPreferences("med_reminders_prefs", Context.MODE_PRIVATE)

    val uiState: StateFlow<List<Medicine>>
    
    private val _isDarkThemeEnabled = MutableStateFlow(
        sharedPrefs.getBoolean("dark_theme_enabled", false)
    )
    val isDarkThemeEnabled: StateFlow<Boolean> = _isDarkThemeEnabled.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MedicineRepository(database.medicineDao())
        scheduler = MedicineAlarmScheduler(application)

        uiState = repository.allMedicines.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun toggleTheme(customDarkEnabled: Boolean) {
        _isDarkThemeEnabled.value = customDarkEnabled
        sharedPrefs.edit().putBoolean("dark_theme_enabled", customDarkEnabled).apply()
    }

    fun addMedicine(
        name: String, 
        dosage: String, 
        hour: Int, 
        minute: Int,
        year: Int? = null,
        month: Int? = null,
        day: Int? = null,
        ringtoneUri: String? = null,
        ringtoneName: String? = null
    ) {
        viewModelScope.launch {
            val medicine = Medicine(
                name = name,
                dosage = dosage,
                timeHour = hour,
                timeMinute = minute,
                isAlarmEnabled = true,
                year = year,
                month = month,
                day = day,
                ringtoneUri = ringtoneUri,
                ringtoneName = ringtoneName
            )
            val newId = repository.insert(medicine)
            val savedMedicine = medicine.copy(id = newId)
            scheduler.schedule(savedMedicine)
        }
    }

    fun toggleAlarm(medicine: Medicine) {
        viewModelScope.launch {
            val updated = medicine.copy(isAlarmEnabled = !medicine.isAlarmEnabled)
            repository.update(updated)
            if (updated.isAlarmEnabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated)
            }
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            scheduler.cancel(medicine)
            repository.delete(medicine)
        }
    }
}

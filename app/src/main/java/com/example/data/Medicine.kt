package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,
    val timeHour: Int,
    val timeMinute: Int,
    val isAlarmEnabled: Boolean = true,
    val year: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val ringtoneUri: String? = null,
    val ringtoneName: String? = null
) {
    val isOneTime: Boolean
        get() = year != null && month != null && day != null

    val formattedDate: String
        get() {
            if (isOneTime) {
                // month from database is 0-indexed or 1-indexed? We can use 0-indexed to match Calendar (0 = Jan, 11 = Dec)
                val monthsName = arrayOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                val m = month ?: 0
                val monthStr = if (m in 0..11) monthsName[m] else (m + 1).toString()
                return "$monthStr $day, $year"
            }
            return "Daily"
        }

    val formattedTime: String
        get() {
            val hourFormatted = if (timeHour % 12 == 0) 12 else timeHour % 12
            val minuteFormatted = String.format(Locale.US, "%02d", timeMinute)
            val amPm = if (timeHour >= 12) "PM" else "AM"
            return "$hourFormatted:$minuteFormatted $amPm"
        }
}

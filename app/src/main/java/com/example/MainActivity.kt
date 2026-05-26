package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Medicine
import com.example.ui.MedicineViewModel
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.runtime.DisposableEffect
import com.example.ui.components.AdmobBannerField
import java.util.Calendar
import android.net.Uri
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.compose.foundation.layout.widthIn

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Google Mobile Ads SDK
        try {
            com.google.android.gms.ads.MobileAds.initialize(this) {}
        } catch (e: Exception) {
            // Safe fallback
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: MedicineViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkThemeEnabled.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    MedicineReminderApp(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MedicineReminderApp(
    modifier: Modifier = Modifier,
    viewModel: MedicineViewModel = viewModel()
) {
    val medicines by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Content with instant Light/Dark Theme toggle
            val isDarkTheme by viewModel.isDarkThemeEnabled.collectAsState()
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Medicine Reminders",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.toggleTheme(!isDarkTheme) },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                            contentDescription = "Toggle Theme Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val activeCount = medicines.count { it.isAlarmEnabled }
                Text(
                    text = if (medicines.isEmpty()) {
                        "Your daily list is empty"
                    } else {
                        "You have ${medicines.size} scheduled daily task${if (medicines.size > 1) "s" else ""} ($activeCount active reminder${if (activeCount != 1) "s" else ""})"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Notification permission warning banner
            AnimatedVisibility(
                visible = !hasNotificationPermission,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Notification alert",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Permission Needed",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Alarms or notifications require authorization. Tap to authorize.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Grant", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Reminders list
            if (medicines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Daily checklist empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create medication slots to trigger automatic, offline local reminders for dosage schedules.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("medicines_list"),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(medicines, key = { it.id }) { medicine ->
                        MedicineItem(
                            medicine = medicine,
                            onToggleAlarm = { viewModel.toggleAlarm(medicine) },
                            onDelete = { viewModel.deleteMedicine(medicine) }
                        )
                    }
                }
            }
            
            // Bottom non-intrusive ad placement
            AdmobBannerField()
        }

        // Add Floating Action Button on Bottom Corner
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_medicine_fab"),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add medicine slot")
        }

        // Scheduled Dialog overlay
        if (showAddDialog) {
            AddMedicineDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, dosage, hour, minute, year, month, day, uri, title ->
                    viewModel.addMedicine(name, dosage, hour, minute, year, month, day, uri, title)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun MedicineItem(
    medicine: Medicine,
    onToggleAlarm: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("medicine_card_${medicine.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (medicine.isAlarmEnabled)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (medicine.isAlarmEnabled) 2.dp else 0.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill icon circle badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (medicine.isAlarmEnabled)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (medicine.isAlarmEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text detail column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (medicine.isAlarmEnabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = medicine.dosage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (medicine.isAlarmEnabled)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = medicine.formattedDate,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (medicine.isOneTime) Icons.Default.CalendarToday else Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (medicine.isAlarmEnabled) 1f else 0.5f)
                        ),
                        border = null
                    )

                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = medicine.ringtoneName ?: "Default Alarm",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 85.dp)
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (medicine.isAlarmEnabled) 1f else 0.5f)
                        ),
                        border = null
                    )
                }
            }

            // Alarm formatted time display
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = medicine.formattedTime,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (medicine.isAlarmEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Toggle Switch for Alarm
                Switch(
                    checked = medicine.isAlarmEnabled,
                    onCheckedChange = { onToggleAlarm() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.testTag("alarm_switch_${medicine.id}")
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete action button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("delete_button_${medicine.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Reminder",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = if (medicine.isAlarmEnabled) 1f else 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AddMedicineDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, Int, Int, Int?, Int?, Int?, String?, String?) -> Unit
) {
    var medName by remember { mutableStateOf("") }
    var medDosage by remember { mutableStateOf("") }
    var displayHour by remember { mutableStateOf(8) }
    var displayMinute by remember { mutableStateOf(0) }
    var isAm by remember { mutableStateOf(true) }

    // Date-scheduling options
    var isOneTimeReminder by remember { mutableStateOf(false) }
    val calendar = remember { Calendar.getInstance() }
    var targetYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var targetMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var targetDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    // Custom Ringtones
    var ringtoneUri by remember { mutableStateOf<String?>(null) }
    var ringtoneName by remember { mutableStateOf<String?>(null) }

    var isPlayingPreview by remember { mutableStateOf(false) }
    var activePreviewPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var activePreviewRingtone by remember { mutableStateOf<android.media.Ringtone?>(null) }

    var showError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun stopPreview() {
        isPlayingPreview = false
        try {
            activePreviewPlayer?.stop()
            activePreviewPlayer?.release()
            activePreviewPlayer = null
        } catch (e: Exception) {}
        try {
            activePreviewRingtone?.stop()
            activePreviewRingtone = null
        } catch (e: Exception) {}
    }

    fun playPreview(uriString: String?) {
        stopPreview()
        if (uriString.isNullOrEmpty()) return
        isPlayingPreview = true
        try {
            val uri = Uri.parse(uriString)
            if (uriString.startsWith("content://settings/system") || uriString.startsWith("content://media/internal") || uriString.contains("ringtones") || uriString.contains("notifications")) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                if (ringtone != null) {
                    activePreviewRingtone = ringtone
                    ringtone.play()
                } else {
                    isPlayingPreview = false
                }
            } else {
                val player = android.media.MediaPlayer().apply {
                    setDataSource(context, uri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    prepare()
                    start()
                }
                activePreviewPlayer = player
            }
        } catch (e: Exception) {
            isPlayingPreview = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPreview()
        }
    }

    // Launchers for choosing sounds
    val customAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {}
            ringtoneUri = uri.toString()
            var filename = "Custom Audio Track"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && cursor.moveToFirst()) {
                        filename = cursor.getString(nameIdx)
                    }
                }
            } catch (e: Exception) {}
            ringtoneName = filename
        }
    }

    val systemRingtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) as? Uri
            }
            if (uri != null) {
                ringtoneUri = uri.toString()
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtoneName = try {
                    ringtone?.getTitle(context) ?: "System Tone"
                } catch (e: Exception) {
                    "System Tone"
                }
            }
        }
    }

    val launchSystemRingtonePicker = {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Reminder Sound")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        }
        systemRingtonePickerLauncher.launch(intent)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 4.dp)
                .testTag("add_medicine_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Add Medicine Plan",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Input fields
                item {
                    OutlinedTextField(
                        value = medName,
                        onValueChange = {
                            medName = it
                            if (it.isNotBlank()) showError = false
                        },
                        label = { Text("Medicine Name") },
                        placeholder = { Text("e.g. Aspirin") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("med_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = medDosage,
                        onValueChange = { medDosage = it },
                        label = { Text("Dosage / Instructions") },
                        placeholder = { Text("e.g. 1 Tablet after breakfast") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("med_dosage_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Time picker title
                item {
                    Text(
                        text = "Reminder Time",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Custom Time picker
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeAdjusterBlock(
                            value = displayHour,
                            label = "Hour",
                            onIncrement = {
                                displayHour = if (displayHour == 12) 1 else displayHour + 1
                            },
                            onDecrement = {
                                displayHour = if (displayHour == 1) 12 else displayHour - 1
                            }
                        )

                        Text(
                            ":",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )

                        TimeAdjusterBlock(
                            value = displayMinute,
                            label = "Minute",
                            onIncrement = {
                                displayMinute = (displayMinute + 5) % 60
                            },
                            onDecrement = {
                                displayMinute = if (displayMinute - 5 < 0) 55 else displayMinute - 5
                            },
                            formatString = "%02d"
                        )

                        // AM/PM segments
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .clickable { isAm = true }
                                    .padding(2.dp)
                                    .testTag("am_pill"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAm) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "AM",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAm) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                modifier = Modifier
                                    .clickable { isAm = false }
                                    .padding(2.dp)
                                    .testTag("pm_pill"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!isAm) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "PM",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isAm) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 🗓️ Date picking section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "One-time Date Alarm",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = isOneTimeReminder,
                                onCheckedChange = { isOneTimeReminder = it }
                            )
                        }

                        if (isOneTimeReminder) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val monthStr = arrayOf(
                                    "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                                )[targetMonth]
                                Text(
                                    text = "Selected Date: $monthStr $targetDay, $targetYear",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                targetYear = y
                                                targetMonth = m
                                                targetDay = d
                                            },
                                            targetYear,
                                            targetMonth,
                                            targetDay
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Change", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Alarm repeat pattern is Daily.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 26.dp)
                            )
                        }
                    }
                }

                // 🎵 Ringtone selecting section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Custom Notification Sound",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (ringtoneUri != null) "Selected: ${ringtoneName ?: "Custom File"}" else "Selected: Standard default alarm",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (ringtoneUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pick built-in tones
                            OutlinedButton(
                                onClick = launchSystemRingtonePicker,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("System Sound", style = MaterialTheme.typography.labelSmall)
                            }

                            // Pick files from device storage
                            OutlinedButton(
                                onClick = { customAudioPickerLauncher.launch(arrayOf("audio/*")) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text("Pick Audio File", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        if (ringtoneUri != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (isPlayingPreview) {
                                        stopPreview()
                                    } else {
                                        playPreview(ringtoneUri)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlayingPreview) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPlayingPreview) "Stop Preview" else "Test Custom Sound",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }

                if (showError) {
                    item {
                        Text(
                            text = "Medicine name cannot be empty",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Action buttons inside Dialog
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("dialog_cancel")
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (medName.isNotBlank()) {
                                    val finalHour = when {
                                        isAm -> if (displayHour == 12) 0 else displayHour
                                        else -> if (displayHour == 12) 12 else displayHour + 12
                                    }
                                    val resolvedDosage = medDosage.ifBlank { "1 unit" }
                                    
                                    val y = if (isOneTimeReminder) targetYear else null
                                    val m = if (isOneTimeReminder) targetMonth else null
                                    val d = if (isOneTimeReminder) targetDay else null

                                    onAdd(
                                        medName.trim(), 
                                        resolvedDosage.trim(), 
                                        finalHour, 
                                        displayMinute,
                                        y,
                                        m,
                                        d,
                                        ringtoneUri,
                                        ringtoneName
                                    )
                                } else {
                                    showError = true
                                }
                            },
                            modifier = Modifier.testTag("dialog_save"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Plan")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimeAdjusterBlock(
    value: Int,
    label: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    formatString: String = "%d"
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        FilledIconButton(
            onClick = onIncrement,
            modifier = Modifier.size(40.dp)
        ) {
            Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = String.format(formatString, value),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        FilledIconButton(
            onClick = onDecrement,
            modifier = Modifier.size(40.dp)
        ) {
            Text("-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

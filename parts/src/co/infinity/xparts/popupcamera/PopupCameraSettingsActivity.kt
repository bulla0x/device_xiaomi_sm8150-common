/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: Project Infinity X
 * SPDX-License-Identifier: Apache-2.0
 */

package co.infinity.xparts.popupcamera

import android.os.Bundle
import android.os.RemoteException
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.infinity.xparts.R
import co.infinity.xparts.theme.XPartsTheme
import co.infinity.xparts.ui.components.MainSwitchBar
import co.infinity.xparts.ui.components.SectionHeader
import vendor.xiaomi.hardware.motor.V1_0.IMotor

class PopupCameraSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XPartsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PopupCameraScreen(onBackPressed = { finish() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopupCameraScreen(onBackPressed: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PopupCameraPreferences(context) }

    var isLedAllowed by remember { mutableStateOf(prefs.isLedAllowed) }
    var soundEffect by remember { mutableStateOf(prefs.soundEffect) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var showCalibrateDialog by remember { mutableStateOf(false) }

    val entries = stringArrayResource(R.array.popupcamera_effects_entries)
    val values = stringArrayResource(R.array.popupcamera_effects_values)
    val selectedIndex = values.indexOf(soundEffect).coerceAtLeast(0)
    val currentEffectName = if (selectedIndex in entries.indices) entries[selectedIndex] else entries[0]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.popup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                MainSwitchBar(
                    title = stringResource(R.string.popup_led_title),
                    summary = stringResource(R.string.popup_led_summary),
                    checked = isLedAllowed,
                    onCheckedChange = { checked ->
                        isLedAllowed = checked
                        prefs.setLedAllowed(checked)
                    }
                )
            }

            item {
                SectionHeader(stringResource(R.string.popup_title))
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { showSoundDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.popup_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentEffectName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            item {
                SectionHeader(stringResource(R.string.popup_camera_calibrate_now))
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { showCalibrateDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.popup_camera_calibrate_now),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.popup_camera_popup_failed_times_calibrate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSoundDialog) {
        AlertDialog(
            onDismissRequest = { showSoundDialog = false },
            title = { Text(stringResource(R.string.popup_title)) },
            text = {
                LazyColumn {
                    itemsIndexed(entries) { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSound = values[index]
                                    soundEffect = newSound
                                    prefs.setSoundEffect(newSound)
                                    showSoundDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = values[index] == soundEffect,
                                onClick = {
                                    val newSound = values[index]
                                    soundEffect = newSound
                                    prefs.setSoundEffect(newSound)
                                    showSoundDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(entry, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSoundDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showCalibrateDialog) {
        AlertDialog(
            onDismissRequest = { showCalibrateDialog = false },
            title = { Text(stringResource(R.string.popup_camera_tip)) },
            text = { Text(stringResource(R.string.popup_camera_popup_failed_times_calibrate)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCalibrateDialog = false
                        try {
                            val motor: IMotor? = PopupCameraService.getMotorService()
                            if (motor != null) {
                                motor.popupMotorCalibrate()
                                Toast.makeText(
                                    context,
                                    R.string.popup_camera_calibrate_success,
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    R.string.popup_camera_calibrate_failed,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: RemoteException) {
                            Toast.makeText(
                                context,
                                R.string.popup_camera_calibrate_failed,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.popup_camera_calibrate_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalibrateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

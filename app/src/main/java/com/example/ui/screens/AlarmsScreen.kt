package com.example.ui.screens

import com.example.ui.theme.LocalCustomFont


import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Alarm

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmsScreen(
    alarms: List<Alarm>,
    is24Hour: Boolean = true,
    onToggleAlarm: (Alarm) -> Unit,
    onAddAlarm: (hour: Int, minute: Int, daysOfWeek: String, label: String, vibrate: Boolean, ringtone: String) -> Unit,
    onUpdateAlarm: (Alarm) -> Unit = {},
    onDeleteAlarm: (Alarm) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLandscape) {
            if (alarms.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "No Alarms",
                            tint = Color(0x33FFFFFF),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO ALARMS",
                            color = Color(0xFF666666),
                            fontSize = 13.sp,
                            fontFamily = LocalCustomFont.current,
                            letterSpacing = 2.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .testTag("add_alarm_button")
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color.White)
                                .clickable { showAddDialog = true }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Alarm",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NEW ALARM",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = LocalCustomFont.current,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left pane: Controls and Summary
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ALARM HUB",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${alarms.size} SCHEDULED",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 11.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .testTag("add_alarm_button")
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color.White)
                                .clickable { showAddDialog = true }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Alarm",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NEW ALARM",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = LocalCustomFont.current,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Right pane: Scrollable Alarm List
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(alarms, key = { it.id }) { alarm ->
                            AlarmItemRow(
                                alarm = alarm,
                                is24Hour = is24Hour,
                                onToggle = { onToggleAlarm(alarm) },
                                onEdit = { editingAlarm = alarm },
                                onDelete = { onDeleteAlarm(alarm) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        } else {
            if (alarms.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "No Alarms",
                        tint = Color(0x33FFFFFF),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NO ALARMS",
                        color = Color(0xFF666666),
                        fontSize = 14.sp,
                        fontFamily = LocalCustomFont.current,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TAP THE '+' PILL TO ADD ONE",
                        color = Color(0xFF444444),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 1.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmItemRow(
                            alarm = alarm,
                            is24Hour = is24Hour,
                            onToggle = { onToggleAlarm(alarm) },
                            onEdit = { editingAlarm = alarm },
                            onDelete = { onDeleteAlarm(alarm) },
                            modifier = Modifier.animateItem()
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(100.dp)) // Padding to not conflict with FAB
                    }
                }
            }

            // Pill-shaped floating add button (only in portrait)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .testTag("add_alarm_button")
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White)
                        .clickable { showAddDialog = true }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Alarm",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NEW ALARM",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = LocalCustomFont.current,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        if (showAddDialog) {
            AlarmFormDialog(
                alarmToEdit = null,
                onDismiss = {
                    com.example.service.AudioSynthPlayer.stop()
                    showAddDialog = false
                },
                onSave = { hour, minute, days, label, vibrate, ringtone ->
                    com.example.service.AudioSynthPlayer.stop()
                    onAddAlarm(hour, minute, days, label, vibrate, ringtone)
                    showAddDialog = false
                }
            )
        }

        if (editingAlarm != null) {
            val currentAlarm = editingAlarm!!
            AlarmFormDialog(
                alarmToEdit = currentAlarm,
                onDismiss = {
                    com.example.service.AudioSynthPlayer.stop()
                    editingAlarm = null
                },
                onSave = { hour, minute, days, label, vibrate, ringtone ->
                    com.example.service.AudioSynthPlayer.stop()
                    onUpdateAlarm(
                        currentAlarm.copy(
                            hour = hour,
                            minute = minute,
                            daysOfWeek = days,
                            label = label,
                            isVibrateEnabled = vibrate,
                            ringtone = ringtone,
                            isEnabled = true
                        )
                    )
                    editingAlarm = null
                },
                onDelete = {
                    com.example.service.AudioSynthPlayer.stop()
                    onDeleteAlarm(currentAlarm)
                    editingAlarm = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmItemRow(
    alarm: Alarm,
    is24Hour: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayHour = when {
        alarm.hour == 0 -> 12
        alarm.hour > 12 -> alarm.hour - 12
        else -> alarm.hour
    }
    val timeStr = String.format("%02d:%02d", displayHour, alarm.minute)
    val period = if (alarm.hour >= 12) "PM" else "AM"

    val cardBg by animateColorAsState(
        targetValue = if (alarm.isEnabled) Color(0x2B18181B) else Color(0x1918181B),
        animationSpec = tween(250),
        label = "alarm_card_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (alarm.isEnabled) Color.White else Color(0xFF71717A),
        animationSpec = tween(250),
        label = "alarm_text_color"
    )

    val opacity by animateFloatAsState(
        targetValue = if (alarm.isEnabled) 1.0f else 0.6f,
        animationSpec = tween(250),
        label = "alarm_opacity"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(32.dp))
            .clickable { onEdit() }
            .graphicsLayer(alpha = opacity),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeStr,
                            color = textColor,
                            fontSize = 38.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold
                        )
                        if (period.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = period,
                                color = if (alarm.isEnabled) MaterialTheme.colorScheme.tertiary else Color(0xFF555555),
                                fontSize = 14.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                    if (alarm.label.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = alarm.label.uppercase(),
                            color = if (alarm.isEnabled) Color(0xFF888888) else Color(0xFF444444),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (alarm.isEnabled) Color(0x1AFFFFFF) else Color(0x0AFFFFFF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Ringtone",
                            tint = if (alarm.isEnabled) MaterialTheme.colorScheme.tertiary else Color(0xFF555555),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = alarm.ringtone,
                            color = if (alarm.isEnabled) Color(0xB3FFFFFF) else Color(0xFF555555),
                            fontSize = 9.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }

                // Custom Nothing-Style Pill Toggle Switch
                Row(
                    modifier = Modifier
                        .testTag("toggle_alarm_switch_${alarm.id}")
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (alarm.isEnabled) Color.White else Color(0xFF1E1E1E))
                        .clickable { onToggle() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (alarm.isEnabled) "ON" else "OFF",
                        color = if (alarm.isEnabled) Color.Black else Color(0xFF666666),
                        fontSize = 11.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Days row or RING ONCE indicator
                if (alarm.daysOfWeek.isEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (alarm.isEnabled) Color(0x1FFFFFFF) else Color(0x0DFFFFFF))
                            .border(1.dp, if (alarm.isEnabled) Color(0x33FFFFFF) else Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (alarm.isEnabled) MaterialTheme.colorScheme.tertiary else Color(0xFF666666))
                        )
                        Text(
                            text = "RING ONCE",
                            color = if (alarm.isEnabled) Color.White else Color(0xFF71717A),
                            fontSize = 10.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
                        val dayAbbreviations = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        
                        weekdays.forEachIndexed { i, letter ->
                            val abbrev = dayAbbreviations[i]
                            val isActive = alarm.isScheduledForDay(abbrev)
                            val circleColor = when {
                                isActive && alarm.isEnabled -> Color.White
                                isActive -> Color(0xFF444444)
                                else -> Color.Transparent
                            }
                            val textCol = when {
                                isActive && alarm.isEnabled -> Color.Black
                                isActive -> Color.Black
                                else -> Color(0xFF444444)
                            }

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(circleColor)
                                    .border(1.dp, if (isActive) Color.Transparent else Color(0xFF2C2C2C), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    color = textCol,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LocalCustomFont.current
                                )
                            }
                        }
                    }
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .testTag("delete_alarm_button_${alarm.id}")
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF161616))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Alarm",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmFormDialog(
    alarmToEdit: Alarm? = null,
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, daysOfWeek: String, label: String, vibrate: Boolean, ringtone: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val isEditMode = alarmToEdit != null

    val initialRawHour = alarmToEdit?.hour ?: 7
    val initialDisplayHour = when {
        initialRawHour == 0 -> 12
        initialRawHour > 12 -> initialRawHour - 12
        else -> initialRawHour
    }
    val initialIsAm = initialRawHour < 12

    var sHour by remember { mutableStateOf(initialDisplayHour) }
    var sMinute by remember { mutableStateOf(alarmToEdit?.minute ?: 0) }
    var isAm by remember { mutableStateOf(initialIsAm) }
    var label by remember { mutableStateOf(alarmToEdit?.label ?: "") }
    var isVibrate by remember { mutableStateOf(alarmToEdit?.isVibrateEnabled ?: true) }
    var selectedRingtone by remember { mutableStateOf(alarmToEdit?.ringtone ?: "GLYPH RAPID") }
    val coroutineScope = rememberCoroutineScope()
    
    // Day Selection State
    val initialDays = if (alarmToEdit != null) {
        if (alarmToEdit.daysOfWeek.isNotEmpty()) {
            alarmToEdit.daysOfWeek.split(",").filter { it.isNotBlank() }.toSet()
        } else {
            emptySet()
        }
    } else {
        emptySet() // Ring once by default for new alarms, with option to select repeat
    }

    var selectedDays by remember { mutableStateOf(initialDays) }
    var isRingOnce by remember { mutableStateOf(alarmToEdit?.daysOfWeek?.isEmpty() ?: true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEditMode) "EDIT ALARM" else "CREATE ALARM",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Time picker inputs using modern tactile arrows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlarmTimeSpinnerColumn(
                        label = "HOUR",
                        value = String.format("%02d", sHour),
                        onIncrement = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            sHour = if (sHour == 12) 1 else sHour + 1
                        },
                        onDecrement = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            sHour = if (sHour == 1) 12 else sHour - 1
                        }
                    )

                    Text(
                        text = ":",
                        color = Color(0x33FFFFFF),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalCustomFont.current,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    AlarmTimeSpinnerColumn(
                        label = "MIN",
                        value = String.format("%02d", sMinute),
                        onIncrement = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            sMinute = if (sMinute == 59) 0 else sMinute + 1
                        },
                        onDecrement = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            sMinute = if (sMinute == 0) 59 else sMinute - 1
                        }
                    )

                    Text(
                        text = ":",
                        color = Color(0x33FFFFFF),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalCustomFont.current,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    AlarmTimeSpinnerColumn(
                        label = "AM/PM",
                        value = if (isAm) "AM" else "PM",
                        onIncrement = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            isAm = !isAm
                        },
                        onDecrement = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            isAm = !isAm
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Custom Label input
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text("LABEL (OPTIONAL)", color = Color(0xFF444444), fontSize = 11.sp, fontFamily = LocalCustomFont.current) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0xFF2C2C2C),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Schedule & Ring Once Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCHEDULE",
                        color = Color(0xFF888888),
                        fontSize = 11.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = when {
                            isRingOnce || selectedDays.isEmpty() -> "RING ONCE"
                            selectedDays.size == 7 -> "EVERY DAY"
                            selectedDays == setOf("Mon", "Tue", "Wed", "Thu", "Fri") -> "WEEKDAYS"
                            selectedDays == setOf("Sat", "Sun") -> "WEEKENDS"
                            else -> "${selectedDays.size} DAYS"
                        },
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 10.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Preset options: [ RING ONCE ] [ WEEKDAYS ] [ EVERY DAY ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isOnceActive = isRingOnce || selectedDays.isEmpty()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isOnceActive) Color.White else Color(0xFF141414))
                            .border(1.dp, if (isOnceActive) Color.White else Color(0xFF2C2C2C), RoundedCornerShape(18.dp))
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                isRingOnce = true
                                selectedDays = emptySet()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnceActive) MaterialTheme.colorScheme.tertiary else Color(0xFF666666))
                            )
                            Text(
                                text = "RING ONCE",
                                color = if (isOnceActive) Color.Black else Color(0xFF888888),
                                fontSize = 10.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val isWeekdaysActive = !isRingOnce && selectedDays == setOf("Mon", "Tue", "Wed", "Thu", "Fri")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isWeekdaysActive) Color.White else Color(0xFF141414))
                            .border(1.dp, if (isWeekdaysActive) Color.White else Color(0xFF2C2C2C), RoundedCornerShape(18.dp))
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                isRingOnce = false
                                selectedDays = setOf("Mon", "Tue", "Wed", "Thu", "Fri")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "WEEKDAYS",
                            color = if (isWeekdaysActive) Color.Black else Color(0xFF888888),
                            fontSize = 10.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val isEveryDayActive = !isRingOnce && selectedDays.size == 7
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isEveryDayActive) Color.White else Color(0xFF141414))
                            .border(1.dp, if (isEveryDayActive) Color.White else Color(0xFF2C2C2C), RoundedCornerShape(18.dp))
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                isRingOnce = false
                                selectedDays = setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "EVERY DAY",
                            color = if (isEveryDayActive) Color.Black else Color(0xFF888888),
                            fontSize = 10.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Individual 7 Day Circles (M, T, W, T, F, S, S)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val daysFull = listOf("M", "T", "W", "T", "F", "S", "S")
                    val dayAbbreviations = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    dayAbbreviations.forEachIndexed { i, abbrev ->
                        val letter = daysFull[i]
                        val isSel = !isRingOnce && selectedDays.contains(abbrev)
                        val bg = if (isSel) Color.White else Color(0xFF0E0E0E)
                        val tc = if (isSel) Color.Black else Color(0xFF666666)
                        
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(bg)
                                .border(1.dp, if (isSel) Color.Transparent else Color(0xFF2C2C2C), CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    if (isRingOnce) {
                                        isRingOnce = false
                                        selectedDays = setOf(abbrev)
                                    } else {
                                        val next = if (isSel) selectedDays - abbrev else selectedDays + abbrev
                                        if (next.isEmpty()) {
                                            isRingOnce = true
                                            selectedDays = emptySet()
                                        } else {
                                            selectedDays = next
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter,
                                color = tc,
                                fontSize = 11.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Vibrate on ring switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0E0E0E))
                        .border(1.dp, Color(0xFF222222), RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            isVibrate = !isVibrate
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VIBRATION",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isVibrate) Color.White else Color(0xFF222222))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isVibrate) "ON" else "OFF",
                            color = if (isVibrate) Color.Black else Color(0xFF777777),
                            fontSize = 10.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "SELECT RINGTONE",
                    color = Color(0xFF888888),
                    fontSize = 11.sp,
                    fontFamily = LocalCustomFont.current,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(10.dp))

                val ringtones = com.example.service.CustomRingtoneManager.getAllRingtones()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ringtones.chunked(2).forEach { rowList ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowList.forEach { rt ->
                                val isSelected = rt == selectedRingtone
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFF141414) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color(0x1AFFFFFF),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            selectedRingtone = rt
                                            com.example.service.AudioSynthPlayer.play(rt)
                                            coroutineScope.launch {
                                                delay(1500)
                                                if (selectedRingtone == rt) {
                                                    com.example.service.AudioSynthPlayer.stop()
                                                }
                                            }
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rt,
                                        color = if (isSelected) Color.White else Color(0x99FFFFFF),
                                        fontSize = 11.sp,
                                        fontFamily = LocalCustomFont.current,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action buttons in clean pills
                if (isEditMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onDelete != null) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .testTag("delete_alarm_in_dialog")
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22EF4444))
                                    .border(1.dp, Color(0x66EF4444), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Alarm",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151515))
                        ) {
                            Text(
                                text = "CANCEL",
                                color = Color(0xFF888888),
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                val finalHour = when {
                                    isAm && sHour == 12 -> 0
                                    isAm -> sHour
                                    !isAm && sHour == 12 -> 12
                                    else -> sHour + 12
                                }
                                val daysString = if (isRingOnce) "" else selectedDays.joinToString(",")
                                onSave(finalHour, sMinute, daysString, label, isVibrate, selectedRingtone)
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = "SAVE CHANGES",
                                color = Color.Black,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF151515))
                        ) {
                            Text(
                                text = "CANCEL",
                                color = Color(0xFF888888),
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                val finalHour = when {
                                    isAm && sHour == 12 -> 0
                                    isAm -> sHour
                                    !isAm && sHour == 12 -> 12
                                    else -> sHour + 12
                                }
                                val daysString = if (isRingOnce) "" else selectedDays.joinToString(",")
                                onSave(finalHour, sMinute, daysString, label, isVibrate, selectedRingtone)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = "SAVE",
                                color = Color.Black,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmTimeSpinnerColumn(
    label: String,
    value: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = Color(0xFF444444),
            fontSize = 10.sp,
            fontFamily = LocalCustomFont.current,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Increment box
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x1F27272A))
                .clickable { onIncrement() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(text = "▲", color = Color(0xFF888888), fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Readout display
        Text(
            text = value,
            color = Color.White,
            fontSize = 28.sp,
            fontFamily = LocalCustomFont.current,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Decrement box
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x1F27272A))
                .clickable { onDecrement() }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(text = "▼", color = Color(0xFF888888), fontSize = 10.sp)
        }
    }
}
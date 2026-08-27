package com.example.ui.screens

import com.example.ui.theme.LocalCustomFont


import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DotMatrixString
import com.example.ui.viewmodel.TimerState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimerScreen(
    durationInput: Long,
    secondsRemaining: Long,
    timerState: TimerState,
    percentage: Float,
    timerRingtone: String,
    onSetTimerRingtone: (String) -> Unit,
    onSetDuration: (Long) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onClear: () -> Unit
) {
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val animatedPercentage by androidx.compose.animation.core.animateFloatAsState(
        targetValue = percentage,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "timer_arc_percentage"
    )
    // Current input configuration states for HOURS, MINUTES, SECONDS
    var sHours by remember { mutableStateOf(0) }
    var sMinutes by remember { mutableStateOf(5) }
    var sSeconds by remember { mutableStateOf(0) }

    val formattedRemaining = String.format(
        "%02d:%02d:%02d",
        secondsRemaining / 3600,
        (secondsRemaining % 3600) / 60,
        secondsRemaining % 60
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (timerState == TimerState.IDLE) {
                // Configuration mode: Left column is spinners, Right column is Start Button
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
                        .background(Color(0x2B18181B))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SET COUNTDOWN",
                        color = Color(0x88FFFFFF),
                        fontSize = 10.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeSpinnerColumn(label = "HR", value = sHours, onValueChange = { sHours = it.coerceIn(0, 23) })
                        Text(text = ":", color = Color(0x33FFFFFF), fontSize = 20.sp, fontFamily = LocalCustomFont.current)
                        TimeSpinnerColumn(label = "MIN", value = sMinutes, onValueChange = { sMinutes = it.coerceIn(0, 59) })
                        Text(text = ":", color = Color(0x33FFFFFF), fontSize = 20.sp, fontFamily = LocalCustomFont.current)
                        TimeSpinnerColumn(label = "SEC", value = sSeconds, onValueChange = { sSeconds = it.coerceIn(0, 59) })
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 5, 10, 15).forEach { mins ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x1F27272A))
                                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                                    .clickable {
                                        val total = sMinutes + mins
                                        if (total >= 60) {
                                            sHours += total / 60
                                            sMinutes = total % 60
                                        } else {
                                            sMinutes = total
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${mins}m",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = LocalCustomFont.current,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("apply_timer_btn")
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.tertiary) // High-visibility red
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                            .clickable {
                                val totalSeconds = (sHours * 3600) + (sMinutes * 60) + sSeconds
                                if (totalSeconds > 0) {
                                    onSetDuration(totalSeconds.toLong())
                                    onStart()
                                }
                            }
                            .padding(horizontal = 32.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "START TIMER",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalCustomFont.current,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            } else {
                // Running countdown screen
                // Left Column: The progress circle arc
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(170.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color(0xFF151515),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = tertiaryColor,
                                startAngle = -90f,
                                sweepAngle = animatedPercentage * 360f,
                                useCenter = false,
                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DotMatrixString(
                                text = formattedRemaining.take(5), // HH:mm
                                activeColor = Color.White,
                                inactiveColor = Color.Transparent,
                                charSpacing = 3.dp,
                                dotSize = 4.dp,
                                dotSpacing = 1.dp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formattedRemaining.takeLast(2), // ss
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = 16.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Right Column: Controls vertical stack
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onClear,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(40.dp)),
                            shape = RoundedCornerShape(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x2B18181B))
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
                                if (timerState == TimerState.RUNNING) {
                                    onPause()
                                } else {
                                    onStart()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("toggle_timer_running_btn"),
                            shape = RoundedCornerShape(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (timerState == TimerState.RUNNING) MaterialTheme.colorScheme.tertiary else Color.White
                            )
                        ) {
                            Text(
                                text = if (timerState == TimerState.RUNNING) "PAUSE" else "RESUME",
                                color = if (timerState == TimerState.RUNNING) Color.White else Color.Black,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    } else {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (timerState == TimerState.IDLE) {
                // Configuration mode: custom slider columns / quick set increments inside a card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(40.dp))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(40.dp))
                        .background(Color(0x2B18181B))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SET COUNTDOWN",
                        color = Color(0x88FFFFFF),
                        fontSize = 11.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Scroll column incrementers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeSpinnerColumn(label = "HR", value = sHours, onValueChange = { sHours = it.coerceIn(0, 23) })
                        Text(text = ":", color = Color(0x33FFFFFF), fontSize = 24.sp, fontFamily = LocalCustomFont.current)
                        TimeSpinnerColumn(label = "MIN", value = sMinutes, onValueChange = { sMinutes = it.coerceIn(0, 59) })
                        Text(text = ":", color = Color(0x33FFFFFF), fontSize = 24.sp, fontFamily = LocalCustomFont.current)
                        TimeSpinnerColumn(label = "SEC", value = sSeconds, onValueChange = { sSeconds = it.coerceIn(0, 59) })
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Tactile quick set pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 5, 10, 15).forEach { mins ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0x1F27272A))
                                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(24.dp))
                                    .clickable {
                                        val total = sMinutes + mins
                                        if (total >= 60) {
                                            sHours += total / 60
                                            sMinutes = total % 60
                                        } else {
                                            sMinutes = total
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${mins}m",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = LocalCustomFont.current,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Pill start button
                // Timer Ringtone Section inside a clean card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(24.dp))
                        .background(Color(0xFF0C0C0F))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "TIMER RINGTONE",
                        color = Color(0xFF888888),
                        fontSize = 11.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val ringtones = com.example.service.CustomRingtoneManager.getAllRingtones()
                    val coroutineScope = rememberCoroutineScope()
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ringtones.chunked(3).forEach { rowList ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowList.forEach { rt ->
                                    val isSelected = rt == timerRingtone
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0xFF1E1E24) else Color(0x0FFFFFFF))
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color(0x1AFFFFFF),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                onSetTimerRingtone(rt)
                                                com.example.service.AudioSynthPlayer.play(rt)
                                                coroutineScope.launch {
                                                    delay(1500)
                                                    // Stop preview after 1.5s
                                                    if (timerRingtone == rt) {
                                                        com.example.service.AudioSynthPlayer.stop()
                                                    }
                                                }
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = rt,
                                            color = if (isSelected) Color.White else Color(0x99FFFFFF),
                                            fontSize = 9.sp,
                                            fontFamily = LocalCustomFont.current,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            modifier = Modifier.basicMarquee()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .testTag("apply_timer_btn")
                        .fillMaxWidth() // Span fully across for ultimate visibility and touch-target size
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.tertiary) // High-visibility red
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                        .clickable {
                            val totalSeconds = (sHours * 3600) + (sMinutes * 60) + sSeconds
                            if (totalSeconds > 0) {
                                onSetDuration(totalSeconds.toLong())
                                onStart()
                            }
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "START TIMER",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalCustomFont.current,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp
                    )
                }
            } else {
                // Running countdown view (with giant circular arc and dot time)
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    // Circular timeline track Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Track
                        drawCircle(
                            color = Color(0xFF151515),
                            radius = size.minDimension / 2,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Glow line remaining
                        drawArc(
                            color = tertiaryColor, // Nothing signature red indicator
                            startAngle = -90f,
                            sweepAngle = animatedPercentage * 360f,
                            useCenter = false,
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Time readable inside core circle
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DotMatrixString(
                            text = formattedRemaining.take(5), // HH:mm
                            activeColor = Color.White,
                            inactiveColor = Color.Transparent,
                            charSpacing = 4.dp,
                            dotSize = 5.dp,
                            dotSpacing = 1.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formattedRemaining.takeLast(2), // ss
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 18.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Timer options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cancel / Clear Pill
                    Button(
                        onClick = onClear,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(40.dp)),
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x2B18181B))
                    ) {
                        Text(
                            text = "CANCEL",
                            color = Color(0xFF888888),
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Play / Pause Pill
                    Button(
                        onClick = {
                            if (timerState == TimerState.RUNNING) {
                                onPause()
                            } else {
                                onStart()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("toggle_timer_running_btn"),
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (timerState == TimerState.RUNNING) MaterialTheme.colorScheme.tertiary else Color.White
                        )
                    ) {
                        Text(
                            text = if (timerState == TimerState.RUNNING) "PAUSE" else "RESUME",
                            color = if (timerState == TimerState.RUNNING) Color.White else Color.Black,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TimeSpinnerColumn(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
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

        // Custom Increment box
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x1F27272A))
                .clickable { onValueChange(value + 1) }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(text = "▲", color = Color(0xFF888888), fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Readout display
        Text(
            text = String.format("%02d", value),
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
                .clickable { onValueChange(value - 1) }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(text = "▼", color = Color(0xFF888888), fontSize = 10.sp)
        }
    }
}
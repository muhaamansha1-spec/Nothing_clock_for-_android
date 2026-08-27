package com.example.ui.screens

import com.example.ui.theme.LocalCustomFont


import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DotMatrixString
import com.example.ui.components.WeatherDotMatrixIcon
import com.example.data.model.WeatherCondition
import com.example.data.repository.WeatherRepository
import com.example.service.MediaPlaybackManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class AudioTrack(
    val title: String,
    val artist: String,
    val durationSeconds: Int
)

/**
 * Checks if the notification access permission is granted for this application.
 */
fun isNotificationServiceEnabled(context: Context): Boolean {
    return try {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == pkgName) {
                    return true
                }
            }
        }
        false
    } catch (e: Throwable) {
        Log.e("LandscapeDockScreen", "Failed to check notification service status", e)
        false
    }
}

/**
 * Highly aesthetic, full screen landscape dock clock designed for AMOLED displays.
 * Incorporates a subtle shift animation that slowly orbits the time based on minutes
 * to prevent pixel burn-in.
 * Features an integrated physical-mode Music Control Module styled in Nothing's monochromatic
 * design, with live support for streaming apps (Spotify, YT Music, etc.) via Notification Access.
 */
@Composable
fun LandscapeDockScreen(
    currentTimestamp: Long,
    is24Hour: Boolean = true,
    onDismiss: () -> Unit
) {
    val date = Date(currentTimestamp)
    val timeFormat = if (is24Hour) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    } else {
        SimpleDateFormat("hh:mm:ss", Locale.getDefault())
    }
    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
    
    val timeStr = timeFormat.format(date)
    val dateStr = dateFormat.format(date).uppercase()
    val period = if (is24Hour) "" else SimpleDateFormat("a", Locale.getDefault()).format(date).uppercase()

    // Calculate shift offset to prevent Amoled burn-in (max 15 pixels)
    val calendar = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)
    val angle = (minute + second / 60.0) * (2 * Math.PI / 60.0)
    
    val offsetX = (15 * cos(angle)).toInt()
    val offsetY = (15 * sin(angle)).toInt()

    val context = LocalContext.current
    var isNotificationAccessGranted by remember { mutableStateOf(false) }

    // Weather repository state
    val weatherRepo = remember { WeatherRepository.getInstance(context) }
    val weatherState by weatherRepo.weatherState.collectAsState()
    val sharedPrefs = remember { context.getSharedPreferences("nothing_clock_settings", Context.MODE_PRIVATE) }
    val isFahrenheit = remember(sharedPrefs) { sharedPrefs.getBoolean("is_fahrenheit", false) }

    // Periodically fetch weather every 15 minutes while docked
    LaunchedEffect(Unit) {
        weatherRepo.fetchCurrentWeather()
        while (true) {
            kotlinx.coroutines.delay(15 * 60 * 1000L)
            weatherRepo.fetchCurrentWeather()
        }
    }

    // Periodically poll notification access to react instantly when the user grants it
    LaunchedEffect(Unit) {
        while (true) {
            isNotificationAccessGranted = isNotificationServiceEnabled(context)
            kotlinx.coroutines.delay(1000L)
        }
    }

    // Keep screen awake while in dock mode
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Collect real-time active system media session flows
    val realIsPlaying by MediaPlaybackManager.isPlaying.collectAsState()
    val realTitle by MediaPlaybackManager.trackTitle.collectAsState()
    val realArtist by MediaPlaybackManager.trackArtist.collectAsState()
    val realDuration by MediaPlaybackManager.trackDuration.collectAsState()
    val realPosition by MediaPlaybackManager.trackPosition.collectAsState()
    val realAlbumArt by MediaPlaybackManager.albumArt.collectAsState()
    val hasActiveSession by MediaPlaybackManager.hasActiveSession.collectAsState()

    // Live ticking simulation for the real media player position
    var simulatedProgressSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(realIsPlaying, realPosition, hasActiveSession) {
        if (hasActiveSession) {
            simulatedProgressSeconds = realPosition
            if (realIsPlaying) {
                while (true) {
                    kotlinx.coroutines.delay(1000L)
                    simulatedProgressSeconds++
                    if (realDuration > 0 && simulatedProgressSeconds >= realDuration) {
                        simulatedProgressSeconds = realDuration
                    }
                }
            }
        }
    }

    // Local Playlist Fallback (Demo Mode) when system notification is connected but no active session or access is missing
    var isLocalPlaying by remember { mutableStateOf(false) }
    var localTrackIdx by remember { mutableIntStateOf(0) }
    var localProgressSeconds by remember { mutableIntStateOf(0) }

    val playlist = remember {
        listOf(
            AudioTrack("GLYPH RAPID", "NOTHING BEAT", 145),
            AudioTrack("VOX UNISON", "SYNTH(0)", 210),
            AudioTrack("TEENAGE AMBIENT", "TE-1 SYSTEM", 180),
            AudioTrack("SILENT STATE", "WHITE LENS", 320)
        )
    }

    val currentLocalTrack = playlist[localTrackIdx]

    // Local ticking track progress
    LaunchedEffect(isLocalPlaying, localTrackIdx) {
        if (isLocalPlaying) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                localProgressSeconds = (localProgressSeconds + 1) % currentLocalTrack.durationSeconds
            }
        }
    }

    // Battery State listener
    var batteryLevelPct by remember { mutableIntStateOf(100) }
    var batteryIsCharging by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                    val status = it.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                    batteryIsCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
                    batteryLevelPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
                }
            }
        }
        val filter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = context.registerReceiver(receiver, filter)
        stickyIntent?.let { intent ->
            val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
            batteryIsCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
            batteryLevelPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    // Bind UI states dynamically based on status
    val isUsingSystemMedia = isNotificationAccessGranted && hasActiveSession
    val activePlaying = if (isUsingSystemMedia) realIsPlaying else isLocalPlaying
    val displayTitle = if (isUsingSystemMedia) realTitle else currentLocalTrack.title
    val displayArtist = if (isUsingSystemMedia) realArtist else currentLocalTrack.artist
    val displayDuration = if (isUsingSystemMedia) realDuration else currentLocalTrack.durationSeconds
    val displayProgress = if (isUsingSystemMedia) simulatedProgressSeconds else localProgressSeconds

    // Formatting helper
    fun formatTrackTime(secs: Int): String {
        val m = secs / 60
        val s = secs % 60
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    // Interactive spectra looping animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_transit")
    val animatedAmplitudes = (0..7).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 280 + (i * 90),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "amp_$i"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Top-left Indicators: Battery & Weather
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 16.dp, start = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Battery Indicator Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .background(Color(0xCC141414))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val accentColor = MaterialTheme.colorScheme.tertiary
                    val isLow = batteryLevelPct <= 20 && !batteryIsCharging
                    val barColor = when {
                        batteryIsCharging -> accentColor
                        isLow -> accentColor
                        else -> Color.White
                    }

                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.size(width = 18.dp, height = 10.dp)
                    ) {
                        val strokeWidth = 1.dp.toPx()
                        val w = size.width
                        val h = size.height
                        val bodyWidth = w - 2.5.dp.toPx()

                        drawRoundRect(
                            color = barColor.copy(alpha = 0.8f),
                            size = androidx.compose.ui.geometry.Size(bodyWidth, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )

                        drawRoundRect(
                            color = barColor.copy(alpha = 0.8f),
                            topLeft = androidx.compose.ui.geometry.Offset(bodyWidth + 0.5.dp.toPx(), h * 0.25f),
                            size = androidx.compose.ui.geometry.Size(2.dp.toPx(), h * 0.5f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                        )

                        val fillMargin = 2.dp.toPx()
                        val maxFillWidth = bodyWidth - (fillMargin * 2)
                        val fillWidth = (maxFillWidth * (batteryLevelPct / 100f)).coerceAtLeast(1f)

                        drawRoundRect(
                            color = barColor,
                            topLeft = androidx.compose.ui.geometry.Offset(fillMargin, fillMargin),
                            size = androidx.compose.ui.geometry.Size(fillWidth, h - (fillMargin * 2)),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx())
                        )
                    }

                    Text(
                        text = if (batteryIsCharging) "⚡ $batteryLevelPct%" else "$batteryLevelPct%",
                        color = if (batteryIsCharging || isLow) accentColor else Color.White,
                        fontSize = 9.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Weather Indicator Pill (Dot Matrix Weather Icon + Live Temp + Location)
            val weather = weatherState
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .background(Color(0xCC141414))
                    .clickable {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        coroutineScope.launch {
                            weatherRepo.fetchCurrentWeather()
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val condition = weather?.condition ?: WeatherCondition.CLEAR_DAY
                    WeatherDotMatrixIcon(
                        condition = condition,
                        dotSize = 2.2.dp,
                        dotSpacing = 1.2.dp,
                        activeColor = Color.White,
                        inactiveColor = Color(0x14FFFFFF)
                    )

                    val tempValue = if (weather != null) {
                        if (isFahrenheit) weather.temperatureFahrenheit.toInt() else weather.temperatureCelsius.toInt()
                    } else 21
                    val unit = if (isFahrenheit) "°F" else "°C"

                    Text(
                        text = "$tempValue$unit",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    val locName = weather?.locationName ?: "LONDON"
                    Text(
                        text = "• $locName",
                        color = Color(0x99FFFFFF),
                        fontSize = 8.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Top-right exit button to switch back to World Clock from Dock Mode
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .background(Color(0xCC141414))
                .clickable { onDismiss() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Text(
                    text = "WORLD CLOCK ↙",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .offset { IntOffset(offsetX, offsetY) },
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Dot Matrix visual clock
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                DotMatrixString(
                    text = timeStr.take(5), // HH:mm
                    activeColor = Color.White,
                    inactiveColor = Color(0x11FFFFFF),
                    charSpacing = 12.dp,
                    dotSize = 8.dp,
                    dotSpacing = 2.dp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = ".${timeStr.takeLast(2)}",
                        color = MaterialTheme.colorScheme.tertiary, // Distinct physical style red second dot
                        fontSize = 28.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (period.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = period,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 18.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = dateStr,
                        color = Color(0xFF888888),
                        fontSize = 11.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "TAP BACKYARD TO EXIT",
                    color = Color(0x28FFFFFF),
                    fontSize = 8.sp,
                    fontFamily = LocalCustomFont.current,
                    letterSpacing = 3.sp
                )
            }

            // Vertical partition line
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.65f)
                    .width(1.dp)
                    .background(Color(0x1AFFFFFF))
            )

            // Right Column: Physical-style Audio Controller widget
            Column(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
                    .padding(start = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Capture clicks inside this column to prevent backdrop dismiss
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                // If notification access is missing, show high-fidelity call-to-action
                if (!isNotificationAccessGranted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .clickable {
                                try {
                                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("LandscapeDock", "Could not open settings", e)
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LINK REAL PHONE PLAYER",
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontFamily = LocalCustomFont.current,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "GRANT ↗",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = 8.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Top tag: audio framework metadata
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (activePlaying) MaterialTheme.colorScheme.tertiary else Color(0x33FFFFFF))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUsingSystemMedia) "PHONE LINK [SPOTIFY/MEDIA]" else "LOCAL DECK [SIMULATED]",
                        color = Color(0x55FFFFFF),
                        fontSize = 8.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Small bouncing sound graphs
                    Row(
                        modifier = Modifier.height(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        for (i in 0..7) {
                            val targetAmp = if (activePlaying) animatedAmplitudes[i].value else 0.15f
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight(targetAmp)
                                    .background(if (activePlaying) MaterialTheme.colorScheme.tertiary else Color(0x40FFFFFF))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Track Album Cover & Metadata Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album Cover Box with 16dp rounded corners matching Nothing design
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF161616))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUsingSystemMedia && realAlbumArt != null) {
                            Image(
                                bitmap = realAlbumArt!!.asImageBitmap(),
                                contentDescription = "Album Cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Placeholder artwork with Nothing dot/icon styling
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (activePlaying) MaterialTheme.colorScheme.tertiary else Color(0x55FFFFFF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = displayTitle,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = displayArtist,
                            color = Color(0xFF888888),
                            fontSize = 11.sp,
                            fontFamily = LocalCustomFont.current,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress timeline meter
                val progressRatio = if (displayDuration > 0) displayProgress.toFloat() / displayDuration.toFloat() else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0x18FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressRatio)
                            .fillMaxHeight()
                            .background(Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Time indicators of timeline
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTrackTime(displayProgress),
                        color = Color(0x66FFFFFF),
                        fontSize = 9.sp,
                        fontFamily = LocalCustomFont.current
                    )
                    Text(
                        text = formatTrackTime(displayDuration),
                        color = Color(0x66FFFFFF),
                        fontSize = 9.sp,
                        fontFamily = LocalCustomFont.current
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hardware-style player keys
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Key
                    IconButton(
                        onClick = {
                            if (isUsingSystemMedia) {
                                MediaPlaybackManager.skipToPrevious()
                            } else {
                                val newIdx = (localTrackIdx - 1 + playlist.size) % playlist.size
                                localTrackIdx = newIdx
                                localProgressSeconds = 0
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, Color(0x1EFFFFFF), CircleShape)
                    ) {
                        Text(
                            text = "◀",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Play/Pause circular highlight Key
                    IconButton(
                        onClick = {
                            if (isUsingSystemMedia) {
                                if (realIsPlaying) MediaPlaybackManager.pause() else MediaPlaybackManager.play()
                            } else {
                                isLocalPlaying = !isLocalPlaying
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(if (activePlaying) MaterialTheme.colorScheme.tertiary else Color.White, CircleShape)
                    ) {
                        Text(
                            text = if (activePlaying) "▮▮" else "▶",
                            color = if (activePlaying) Color.White else Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalCustomFont.current
                        )
                    }

                    // Next Key
                    IconButton(
                        onClick = {
                            if (isUsingSystemMedia) {
                                MediaPlaybackManager.skipToNext()
                            } else {
                                val newIdx = (localTrackIdx + 1) % playlist.size
                                localTrackIdx = newIdx
                                localProgressSeconds = 0
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, Color(0x1EFFFFFF), CircleShape)
                    ) {
                        Text(
                            text = "▶",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
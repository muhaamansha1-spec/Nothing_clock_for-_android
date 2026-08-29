package com.example

import com.example.ui.theme.LocalCustomFont


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AlarmsScreen
import com.example.ui.screens.LandscapeDockScreen
import com.example.ui.screens.StopwatchScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.screens.WorldClockScreen
import com.example.ui.theme.NothingClockTheme
import com.example.ui.components.DotMatrixString
import com.example.ui.viewmodel.ClockSection
import com.example.ui.viewmodel.ClockViewModel
import com.example.ui.viewmodel.CameraCutout
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.withFrameMillis
import androidx.compose.foundation.pager.PagerState

class MainActivity : ComponentActivity() {
    private val viewModel: ClockViewModel by viewModels()

    companion object {
        var isDockModeActiveState = mutableStateOf(false)
        var dockVolumeTrigger = mutableStateOf(0L)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isDockModeActiveState.value) {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                when (keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            0 // flag 0 suppresses default system volume dialog
                        )
                        dockVolumeTrigger.value = System.currentTimeMillis()
                        return true
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            0 // flag 0 suppresses default system volume dialog
                        )
                        dockVolumeTrigger.value = System.currentTimeMillis()
                        return true
                    }
                    KeyEvent.KEYCODE_VOLUME_MUTE -> {
                        audioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_TOGGLE_MUTE,
                            0 // flag 0 suppresses default system volume dialog
                        )
                        dockVolumeTrigger.value = System.currentTimeMillis()
                        return true
                    }
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (isDockModeActiveState.value) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE -> return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        try {
            // Initialize custom ringtone manager
            com.example.service.CustomRingtoneManager.init(this)
        } catch (_: Throwable) {}
        
        try {
            // Handle direct navigation from widgets (e.g. Alarm widget)
            handleNavigationIntent(intent)
        } catch (_: Throwable) {}
        
        // Request notification permission if Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            } catch (_: Throwable) {}
        }
        
        try {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } catch (_: Throwable) {}
        setContent {
            val context = LocalContext.current
            var fontName by remember { mutableStateOf(com.example.service.CustomFontManager.getSelectedFontName(context)) }
            var customFontFamily by remember(fontName) {
                mutableStateOf(com.example.service.CustomFontManager.loadFontFamily(context, fontName))
            }

            NothingClockTheme(customFontFamily = customFontFamily) {
                MainClockApp(
                    viewModel = viewModel,
                    activeFontName = fontName,
                    onFontChanged = { newFontName ->
                        fontName = newFontName
                        com.example.service.CustomFontManager.setSelectedFontName(context, newFontName)
                        customFontFamily = com.example.service.CustomFontManager.loadFontFamily(context, newFontName)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        val sectionName = intent?.getStringExtra("EXTRA_SECTION")
        if (sectionName != null) {
            try {
                viewModel.selectSection(com.example.ui.viewmodel.ClockSection.valueOf(sectionName))
            } catch (_: Exception) {}
        }
    }

}

@Composable
fun MainClockApp(
    viewModel: ClockViewModel,
    activeFontName: String = "DEFAULT_MONO",
    onFontChanged: (String) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val currentSection by viewModel.currentSection.collectAsState()
    val alarms by viewModel.alarms.collectAsState()
    val worldClocks by viewModel.worldClocks.collectAsState()
    val ticker by viewModel.ticker.collectAsState()

    // Camera Cutout state
    val cutout by viewModel.selectedCutout.collectAsState()
    val is24Hour by viewModel.is24Hour.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // Stopwatch States
    val stopwatchTime by viewModel.stopwatchTime.collectAsState()
    val isStopwatchRunning by viewModel.isStopwatchRunning.collectAsState()
    val laps by viewModel.laps.collectAsState()

    // Timer States
    val timerDurationInput by viewModel.timerDurationInput.collectAsState()
    val timerSecondsRemaining by viewModel.timerSecondsRemaining.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val timerRingtone by viewModel.timerRingtone.collectAsState()

    // Ringing alert states
    val ringingAlarm by viewModel.ringingAlarm.collectAsState()
    val isTimerRinging by viewModel.isTimerRinging.collectAsState()

    // Configuration for landscape dock checking
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isDockModeActive by remember { mutableStateOf(false) }

    DisposableEffect(isDockModeActive) {
        val activity = context as? Activity
        if (isDockModeActive) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    var fontUploadStatus by remember { mutableStateOf<String?>(null) }
    var uploadedFilesList by remember {
        mutableStateOf(com.example.service.CustomFontManager.getUploadedFontFiles(context))
    }

    val fontPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val (success, resultMsg) = com.example.service.CustomFontManager.saveFontFromUri(context, it)
            if (success) {
                fontUploadStatus = "SUCCESS: Font '$resultMsg' installed!"
                uploadedFilesList = com.example.service.CustomFontManager.getUploadedFontFiles(context)
                onFontChanged(resultMsg)
            } else {
                fontUploadStatus = "ERROR: $resultMsg"
            }
        }
    }

    LaunchedEffect(Unit) {
        com.example.data.repository.WeatherRepository.getInstance(context).fetchCurrentWeather()
    }

    var lastOrientationWasLandscape by remember { mutableStateOf(false) }
    LaunchedEffect(isLandscape, currentSection) {
        if (isLandscape && !lastOrientationWasLandscape && currentSection == ClockSection.WORLD_CLOCK) {
            isDockModeActive = true
        }
        lastOrientationWasLandscape = isLandscape
    }
    
    LaunchedEffect(isDockModeActive, currentSection) {
        MainActivity.isDockModeActiveState.value = isDockModeActive && currentSection == ClockSection.WORLD_CLOCK
    }
    
    // If Dock mode is active and in World Clock section, render the Dock overlay
    if (isDockModeActive && currentSection == ClockSection.WORLD_CLOCK) {
        LandscapeDockScreen(
            currentTimestamp = ticker,
            is24Hour = is24Hour,
            onDismiss = {
                isDockModeActive = false
            }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                bottomBar = {
                    // High fidelity segmented pill nav bar with flowing indicator
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(WindowInsets.navigationBars.asPaddingValues())
                            .padding(
                                horizontal = if (isLandscape) 48.dp else 24.dp,
                                vertical = if (isLandscape) 8.dp else 20.dp
                            )
                            .clip(RoundedCornerShape(28.dp))
                            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(28.dp))
                            .background(Color(0x2B18181B))
                            .padding(4.dp)
                    ) {
                        val tabs = listOf(
                            ClockSection.ALARMS to "ALARMS",
                            ClockSection.WORLD_CLOCK to "WORLD",
                            ClockSection.STOPWATCH to "STOPWATCH",
                            ClockSection.TIMER to "TIMER"
                        )

                        val density = LocalDensity.current
                        var barHeight by remember { mutableStateOf(if (isLandscape) 36.dp else 44.dp) }
                        val selectedIdx = tabs.indexOfFirst { it.first == currentSection }.coerceAtLeast(0)
                        val tabWidth = maxWidth / tabs.size

                        // The flowing fluid background pill
                        val animatedOffset by animateDpAsState(
                            targetValue = tabWidth * selectedIdx,
                            animationSpec = spring(
                                dampingRatio = 0.78f, // Slightly bouncy, fluid, and premium natural feel
                                stiffness = 380f // Smooth snapping glide, organic and highly responsive
                            ),
                            label = "pill_offset_anim"
                        )

                        Box(
                            modifier = Modifier
                                .width(tabWidth)
                                .height(barHeight)
                                .graphicsLayer {
                                    translationX = animatedOffset.toPx()
                                }
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color.White)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    barHeight = with(density) { coordinates.size.height.toDp() }
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabs.forEach { (section, label) ->
                                val isSelected = currentSection == section
                                val tabText by animateColorAsState(
                                    targetValue = if (isSelected) Color.Black else Color(0xFF71717A),
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "tab_label_color"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(24.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.selectSection(section)
                                        }
                                        .padding(vertical = if (isLandscape) 8.dp else 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = tabText,
                                        fontSize = 10.sp,
                                        fontFamily = LocalCustomFont.current,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                },
                contentWindowInsets = WindowInsets(0.dp)
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(innerPadding)
                        .statusBarsPadding()
                ) {
                    // App Logo Title bar (hidden in landscape to optimize space)
                    if (!isLandscape) {
                        val headerPaddingStart by animateDpAsState(
                            targetValue = when (cutout) {
                                CameraCutout.LEFT_CORNER -> 50.dp
                                else -> 24.dp
                            },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "header_start_padding"
                        )

                        val headerPaddingEnd by animateDpAsState(
                            targetValue = when (cutout) {
                                CameraCutout.RIGHT_CORNER -> 54.dp
                                else -> 24.dp
                            },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "header_end_padding"
                        )

                        val headerPaddingTop by animateDpAsState(
                            targetValue = when (cutout) {
                                CameraCutout.LEFT_CORNER, CameraCutout.RIGHT_CORNER -> 6.dp
                                else -> 16.dp
                            },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "header_top_padding"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = headerPaddingStart,
                                    end = headerPaddingEnd,
                                    top = headerPaddingTop,
                                    bottom = 16.dp
                               ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NOTHING",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )

                            // Minimalist signature + settings button
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(MaterialTheme.colorScheme.tertiary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "CLOCK(R)",
                                        color = Color(0x77FFFFFF),
                                        fontSize = 11.sp,
                                        fontFamily = LocalCustomFont.current,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 1.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(
                                    onClick = { showSettingsDialog = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
                                        .testTag("settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Divider line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF141414))
                        )
                    }

                    // Render Active Content Screen with a HorizontalPager supporting latency-free sliding and swipe-to-switch
                    val sectionList = listOf(
                        ClockSection.ALARMS,
                        ClockSection.WORLD_CLOCK,
                        ClockSection.STOPWATCH,
                        ClockSection.TIMER
                    )

                    val pagerState = rememberPagerState(initialPage = sectionList.indexOf(currentSection).coerceAtLeast(0)) {
                        sectionList.size
                    }

                    // Sync page swipe to viewModel
                    LaunchedEffect(pagerState.currentPage) {
                        val targetSection = sectionList[pagerState.currentPage]
                        if (targetSection != viewModel.currentSection.value) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        viewModel.selectSection(targetSection)
                    }

                    // Sync click/navigation actions to page
                    LaunchedEffect(currentSection) {
                        val targetPage = sectionList.indexOf(currentSection).coerceAtLeast(0)
                        if (pagerState.currentPage != targetPage) {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { page ->
                        when (sectionList[page]) {
                            ClockSection.ALARMS -> {
                                AlarmsScreen(
                                    alarms = alarms,
                                    is24Hour = is24Hour,
                                    onToggleAlarm = { viewModel.toggleAlarm(it) },
                                    onAddAlarm = { h, m, days, label, vib, ring ->
                                        viewModel.addAlarm(h, m, days, label, vib, ring)
                                    },
                                    onUpdateAlarm = { viewModel.updateAlarm(it) },
                                    onDeleteAlarm = { viewModel.deleteAlarm(it) }
                                )
                            }
                            ClockSection.WORLD_CLOCK -> {
                                WorldClockScreen(
                                    currentTimestamp = ticker,
                                    clocks = worldClocks,
                                    is24Hour = is24Hour,
                                    onAddClock = { name, tz, country ->
                                        viewModel.addWorldClock(name, tz, country)
                                    },
                                    onDeleteClock = { viewModel.deleteWorldClock(it) },
                                    onOpenDockMode = { isDockModeActive = true }
                                )
                            }
                            ClockSection.STOPWATCH -> {
                                StopwatchScreen(
                                    timeMs = stopwatchTime,
                                    isRunning = isStopwatchRunning,
                                    laps = laps,
                                    onStart = { viewModel.startStopwatch() },
                                    onPause = { viewModel.pauseStopwatch() },
                                    onReset = { viewModel.resetStopwatch() },
                                    onLap = { viewModel.addLap() }
                                )
                            }
                            ClockSection.TIMER -> {
                                TimerScreen(
                                    durationInput = timerDurationInput,
                                    secondsRemaining = timerSecondsRemaining,
                                    timerState = timerState,
                                    percentage = viewModel.timerPercentage,
                                    timerRingtone = timerRingtone,
                                    onSetTimerRingtone = { viewModel.setTimerRingtone(it) },
                                    onSetDuration = { viewModel.setTimerDuration(it) },
                                    onStart = { viewModel.startTimer() },
                                    onPause = { viewModel.pauseTimer() },
                                    onReset = { viewModel.resetTimer() },
                                    onClear = { viewModel.clearTimer() }
                                )
                            }
                        }
                    }
                }
            }

            // Visual overlay for simulated hardware/software bezel camera cutout
            if (!isLandscape) {
                CameraCutoutVisualizer(cutout = cutout)
            }

            // Overlay settings dialog when active
            if (showSettingsDialog) {
                CameraCutoutsSettingsDialog(
                    currentCutout = cutout,
                    onCutoutSelected = { viewModel.setCameraCutout(it) },
                    is24Hour = is24Hour,
                    on24HourToggled = { viewModel.setIs24Hour(it) },
                    activeFontName = activeFontName,
                    onFontChanged = onFontChanged,
                    uploadedFonts = uploadedFilesList,
                    fontUploadStatus = fontUploadStatus,
                    onUploadFontClick = {
                        try {
                            fontPickerLauncher.launch(arrayOf("*/*"))
                        } catch (e: Throwable) {
                            fontUploadStatus = "ERROR: File picker not available on this device"
                        }
                    },
                    onDeleteFont = { fontKey ->
                        com.example.service.CustomFontManager.deleteFont(context, fontKey)
                        uploadedFilesList = com.example.service.CustomFontManager.getUploadedFontFiles(context)
                        if (activeFontName == fontKey) {
                            onFontChanged("NDOT57")
                        }
                    },
                    onDismiss = { showSettingsDialog = false }
                )
            }

            // Alarm ringing overlay
            ringingAlarm?.let { alarm ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(24.dp)
                        .clickable(enabled = true) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Alarm Active",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = if (alarm.label.isEmpty()) "ALARM TRIGGERED" else alarm.label.uppercase(),
                                color = MaterialTheme.colorScheme.tertiary,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 2.sp
                            )
                        }

                        val displayHour = when {
                            alarm.hour == 0 -> 12
                            alarm.hour > 12 -> alarm.hour - 12
                            else -> alarm.hour
                        }
                        val period = if (alarm.hour >= 12) "PM" else "AM"
                        val timeStr = String.format("%02d:%02d", displayHour, alarm.minute)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DotMatrixString(
                                text = timeStr,
                                activeColor = Color.White,
                                inactiveColor = Color(0x06FFFFFF),
                                dotSize = 7.dp,
                                dotSpacing = 2.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = period,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { viewModel.snoozeAlarm() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16161C))
                            ) {
                                Text(
                                    text = "SNOOZE (5M)",
                                    color = Color.White,
                                    fontFamily = LocalCustomFont.current,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = { viewModel.dismissAlarm() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Text(
                                    text = "DISMISS",
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

            // Timer ringing overlay
            if (isTimerRinging) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(24.dp)
                        .clickable(enabled = true) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(32.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x26FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Timer Active",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "TIMER FINISHED",
                                color = Color.White,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 2.sp
                            )
                        }

                        DotMatrixString(
                            text = "00:00",
                            activeColor = MaterialTheme.colorScheme.tertiary,
                            inactiveColor = Color(0x06FFFFFF),
                            dotSize = 7.dp,
                            dotSpacing = 2.dp
                        )

                        Button(
                            onClick = { viewModel.dismissTimerRingtone() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text(
                                text = "DISMISS TIMER",
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
fun CameraCutoutVisualizer(cutout: CameraCutout) {
    // Eliminated visual cutout circle to keep screen immersive while preserving layout shift offset logic
}

@Composable
fun CameraCutoutsSettingsDialog(
    currentCutout: CameraCutout,
    onCutoutSelected: (CameraCutout) -> Unit,
    is24Hour: Boolean,
    on24HourToggled: (Boolean) -> Unit,
    activeFontName: String = "NDOT57",
    onFontChanged: (String) -> Unit = {},
    uploadedFonts: List<java.io.File> = emptyList(),
    fontUploadStatus: String? = null,
    onUploadFontClick: () -> Unit = {},
    onDeleteFont: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    androidx.activity.compose.BackHandler {
        onDismiss()
    }

    var selectedMusicPackage by remember {
        mutableStateOf(
            try {
                context.getSharedPreferences("widget_settings", android.content.Context.MODE_PRIVATE)
                    .getString("widget_music_app_package", "") ?: ""
            } catch (e: Throwable) { "" }
        )
    }

    val musicApps = remember {
        listOf(
            "Default System Player" to "",
            "Spotify" to "com.spotify.music",
            "YouTube Music" to "com.google.android.apps.youtube.music",
            "Apple Music" to "com.apple.android.music",
            "Amazon Music" to "com.amazon.mp3",
            "SoundCloud" to "com.soundcloud.android",
            "Pandora" to "com.pandora.android",
            "TIDAL" to "com.aspiro.tidal",
            "Deezer" to "deezer.android.app",
            "VLC" to "org.videolan.vlc"
        )
    }

    var drawingDots by remember { 
        mutableStateOf(
            try {
                ClockWidgetProvider.loadDrawingDots(context)
            } catch (e: Throwable) {
                BooleanArray(35)
            }
        ) 
    }
    var customRingtoneName by remember { mutableStateOf("") }
    var synthFreq by remember { mutableStateOf(440f) }
    var synthLfo by remember { mutableStateOf(6f) }
    var synthWave by remember { mutableStateOf("SINE") }

    var allRingtonesList by remember {
        mutableStateOf(
            try {
                com.example.service.CustomRingtoneManager.getAllRingtones()
            } catch (e: Throwable) {
                listOf("GLYPH RAPID", "VOX UNISON", "TEENAGE AMBIENT", "SILENT STATE", "RETRO BEATS", "DIGITAL CHIRP")
            }
        )
    }

    var uploadingFileName by remember { mutableStateOf<String?>(null) }
    var uploadProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(uploadingFileName) {
        if (uploadingFileName != null) {
            uploadProgress = 0f
            while (uploadProgress < 1.0f) {
                kotlinx.coroutines.delay(100)
                uploadProgress += 0.1f
            }
            val mockRing = when (uploadingFileName) {
                "GLYPH_CHATTER.wav" -> com.example.service.CustomRingtone("GLYPH CHATTER", 750f, 12f, "CHIRP")
                "KINETIC_PULSE.mp3" -> com.example.service.CustomRingtone("KINETIC PULSE", 320f, 6f, "SQUARE")
                "SYNAPSE_SHOCK.wav" -> com.example.service.CustomRingtone("SYNAPSE SHOCK", 950f, 4f, "TRIANGLE")
                "AMBER_COAL.ogg" -> com.example.service.CustomRingtone("AMBER COAL", 220f, 3f, "SINE")
                else -> com.example.service.CustomRingtone("CUSTOM FILE", 440f, 5f, "SINE")
            }
            try {
                com.example.service.CustomRingtoneManager.addCustomRingtone(context, mockRing)
                allRingtonesList = com.example.service.CustomRingtoneManager.getAllRingtones()
                com.example.service.AudioSynthPlayer.play(mockRing.name)
                kotlinx.coroutines.delay(1500)
                com.example.service.AudioSynthPlayer.stop()
            } catch (_: Throwable) {}
            uploadingFileName = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 620.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0E0E0E))
                .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(24.dp))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { /* Consume click inside modal */ }
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "SETTINGS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "SELECT SCREEN CAMERA CUTOUT",
                    color = Color(0x88FFFFFF),
                    fontSize = 10.sp,
                    fontFamily = LocalCustomFont.current,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Cutout Selector Options
                val options = listOf(
                    CameraCutout.NONE to "NONE",
                    CameraCutout.LEFT_CORNER to "LEFT CORNER",
                    CameraCutout.CENTER to "CENTER CLOUD",
                    CameraCutout.RIGHT_CORNER to "RIGHT CORNER"
                )
                
                options.forEach { (option, label) ->
                    val isSelected = option == currentCutout
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF141414) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color(0x1AFFFFFF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onCutoutSelected(option) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0x99FFFFFF),
                            fontSize = 11.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.tertiary else Color(0x40FFFFFF), CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "TIME DISPLAY FORMAT",
                    color = Color(0x88FFFFFF),
                    fontSize = 10.sp,
                    fontFamily = LocalCustomFont.current,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val timeOptions = listOf(
                        false to "12-HOUR",
                        true to "24-HOUR"
                    )
                    timeOptions.forEach { (option, label) ->
                        val isSelected = option == is24Hour
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
                                .clickable { on24HourToggled(option) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0x99FFFFFF),
                                fontSize = 11.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2C)))
                Spacer(modifier = Modifier.height(24.dp))

                // Section: App Typography & Custom Fonts (.otf / .ttf)
                Text(
                    text = "APP TYPOGRAPHY & FONTS",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "UPLOAD CUSTOM .OTF / .TTF FONTS OR CHOOSE FROM BUILT-IN TYPOGRAPHY.",
                    color = Color(0x88FFFFFF),
                    fontSize = 9.sp,
                    fontFamily = LocalCustomFont.current
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Upload Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141414))
                        .border(1.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp))
                        .clickable {
                            onUploadFontClick()
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "+ UPLOAD CUSTOM FONT (.OTF / .TTF)",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontSize = 11.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                fontUploadStatus?.let { status ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = status,
                        color = if (status.startsWith("SUCCESS")) Color(0xFF4CAF50) else MaterialTheme.colorScheme.tertiary,
                        fontSize = 9.sp,
                        fontFamily = LocalCustomFont.current
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Font Options List (Built-ins + Uploaded)
                val builtInFonts = listOf(
                    "NDOT57" to "NDOT 57 (DEFAULT)",
                    "DEFAULT_MONO" to "NOTHING MONO",
                    "DEFAULT_SANS" to "SANS-SERIF",
                    "DEFAULT_SERIF" to "SERIF"
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Built-in Font Options
                    builtInFonts.forEach { (fontKey, label) ->
                        val isSelected = activeFontName == fontKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF141414) else Color(0xFF080808))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color(0x1AFFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onFontChanged(fontKey)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0x99FFFFFF),
                                fontSize = 11.sp,
                                fontFamily = com.example.service.CustomFontManager.loadFontFamily(context, fontKey) ?: FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.tertiary else Color(0x40FFFFFF), CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent)
                            )
                        }
                    }

                    // User Uploaded Custom Fonts
                    uploadedFonts.forEach { file ->
                        val fontKey = file.name
                        val isSelected = activeFontName == fontKey
                        val customFamily = remember(fontKey) {
                            com.example.service.CustomFontManager.loadFontFamily(context, fontKey)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF141414) else Color(0xFF080808))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color(0x1AFFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onFontChanged(fontKey)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.nameWithoutExtension.uppercase(),
                                    color = if (isSelected) Color.White else Color(0x99FFFFFF),
                                    fontSize = 11.sp,
                                    fontFamily = customFamily ?: FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = file.name,
                                    color = Color(0xFF555555),
                                    fontSize = 8.sp,
                                    fontFamily = LocalCustomFont.current
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Delete font option
                                Text(
                                    text = "DELETE",
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontSize = 8.sp,
                                    fontFamily = LocalCustomFont.current,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x22FF0000))
                                        .clickable {
                                            onDeleteFont(fontKey)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.tertiary else Color(0x40FFFFFF), CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent)
                                )
                            }
                        }
                    }
                }

                // Live Preview Card
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF000000))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "FONT PREVIEW:",
                            color = Color(0x66FFFFFF),
                            fontSize = 8.sp,
                            fontFamily = LocalCustomFont.current
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "12:34:56 - NOTHING CLOCK",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = com.example.service.CustomFontManager.loadFontFamily(context, activeFontName) ?: FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2C)))
                Spacer(modifier = Modifier.height(24.dp))

                // Section 2B: Music Widget App Selector
                Text(
                    text = "MUSIC WIDGET LAUNCHER APP",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CHOOSE WHICH MUSIC APP OPENS WHEN CLICKING THE MUSIC WIDGET.",
                    color = Color(0x88FFFFFF),
                    fontSize = 9.sp,
                    fontFamily = LocalCustomFont.current
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    musicApps.forEach { (name, pkg) ->
                        val isSelected = selectedMusicPackage == pkg
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF141414) else Color(0xFF080808))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.tertiary else Color(0x1AFFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedMusicPackage = pkg
                                    context.getSharedPreferences("widget_settings", android.content.Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("widget_music_app_package", pkg)
                                        .putString("widget_music_app_name", name)
                                        .apply()
                                    // Trigger immediate widget update to bind the new click intent
                                    com.example.MusicWidgetProvider.updateAllWidgets(context)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name.uppercase(),
                                    color = if (isSelected) Color.White else Color(0x99FFFFFF),
                                    fontSize = 11.sp,
                                    fontFamily = LocalCustomFont.current,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (pkg.isNotEmpty()) {
                                    Text(
                                        text = pkg,
                                        color = Color(0xFF555555),
                                        fontSize = 8.sp,
                                        fontFamily = LocalCustomFont.current
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.tertiary else Color(0x40FFFFFF), CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.tertiary else Color.Transparent)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2C)))
                Spacer(modifier = Modifier.height(24.dp))

                // Section 3: Dot Matrix Drawing Widget
                Text(
                    text = "DOT MATRIX WIDGET DESIGNER",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TOUCH INDIVIDUAL DOTS TO DRAW GLYPHS. VIBRATES ON TOGGLE.",
                    color = Color(0x88FFFFFF),
                    fontSize = 9.sp,
                    fontFamily = LocalCustomFont.current
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 5x7 Dot Grid Widget
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(Color(0xFF070707))
                            .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        for (r in 0 until 7) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (c in 0 until 5) {
                                    val index = r * 5 + c
                                    val isActive = drawingDots.getOrElse(index) { false }
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(if (isActive) MaterialTheme.colorScheme.tertiary else Color(0xFF141414))
                                            .border(1.dp, if (isActive) Color.Transparent else Color(0x20FFFFFF), CircleShape)
                                            .clickable {
                                                try {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    val newDots = if (drawingDots.size == 35) drawingDots.copyOf() else BooleanArray(35)
                                                    newDots[index] = !newDots[index]
                                                    drawingDots = newDots
                                                    ClockWidgetProvider.saveDrawingDots(context, newDots)
                                                } catch (_: Throwable) {}
                                            }
                                    )
                                }
                            }
                        }
                    }

                    // Presets
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "GLYPH PRESETS",
                            color = Color(0xAAFFFFFF),
                            fontSize = 9.sp,
                            fontFamily = LocalCustomFont.current
                        )

                        val presets = listOf(
                            "HEART" to booleanArrayOf(
                                false, true,  false, true,  false,
                                true,  true,  true,  true,  true,
                                true,  true,  true,  true,  true,
                                false, true,  true,  true,  false,
                                false, false, true,  false, false,
                                false, false, false, false, false,
                                false, false, false, false, false
                            ),
                            "SMILE" to booleanArrayOf(
                                false, false, false, false, false,
                                false, true,  false, true,  false,
                                false, false, false, false, false,
                                true,  false, false, false, true,
                                false, true,  true,  true,  false,
                                false, false, false, false, false,
                                false, false, false, false, false
                            ),
                            "ARROW" to booleanArrayOf(
                                false, false, true,  false, false,
                                false, true,  true,  true,  false,
                                true,  false, true,  false, true,
                                false, false, true,  false, false,
                                false, false, true,  false, false,
                                false, false, true,  false, false,
                                false, false, true,  false, false
                            ),
                            "STAR" to booleanArrayOf(
                                false, false, true,  false, false,
                                true,  true,  true,  true,  true,
                                false, true,  true,  true,  false,
                                false, true,  false, true,  false,
                                true,  false, false, false, true,
                                false, false, false, false, false,
                                false, false, false, false, false
                            )
                        )

                        presets.forEach { (name, preset) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val newPreset = preset.copyOf()
                                        drawingDots = newPreset
                                        ClockWidgetProvider.saveDrawingDots(context, newPreset)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = LocalCustomFont.current,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Clear Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141414))
                                .border(1.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val emptyDots = BooleanArray(35)
                                    drawingDots = emptyDots
                                    ClockWidgetProvider.saveDrawingDots(context, emptyDots)
                                }
                                .padding(vertical = 6.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CLEAR ALL",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = 10.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Add Widget to Home Screen Buttons
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Clock Widget
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(Color(0xFF141414))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(23.dp))
                                .clickable {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                                        val myProvider = android.content.ComponentName(context, ClockWidgetProvider::class.java)
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                                val successCallback = android.app.PendingIntent.getBroadcast(
                                                    context,
                                                    0,
                                                    android.content.Intent(context, ClockWidgetProvider::class.java).apply {
                                                        action = ClockWidgetProvider.ACTION_UPDATE_CLOCK
                                                    },
                                                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                                                )
                                                appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
                                            } else {
                                                android.widget.Toast.makeText(context, "Launcher does not support widget pinning", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            android.widget.Toast.makeText(context, "Android 8.0+ is required", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (_: Throwable) {
                                        android.widget.Toast.makeText(context, "Could not pin widget on this device", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ PIN CLOCK",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Alarm Widget
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(Color(0xFF141414))
                                .border(1.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(23.dp))
                                .clickable {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                                        val alarmProvider = android.content.ComponentName(context, AlarmWidgetProvider::class.java)
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                                val successCallback = android.app.PendingIntent.getBroadcast(
                                                    context,
                                                    0,
                                                    android.content.Intent(context, AlarmWidgetProvider::class.java).apply {
                                                        action = AlarmWidgetProvider.ACTION_UPDATE_ALARM_WIDGET
                                                    },
                                                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                                                )
                                                appWidgetManager.requestPinAppWidget(alarmProvider, null, successCallback)
                                            } else {
                                                android.widget.Toast.makeText(context, "Launcher does not support widget pinning", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            android.widget.Toast.makeText(context, "Android 8.0+ is required", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (_: Throwable) {
                                        android.widget.Toast.makeText(context, "Could not pin widget on this device", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ PIN ALARM",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontSize = 10.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Weather Widget
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(Color(0xFF141414))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(23.dp))
                                .clickable {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                                        val weatherProvider = android.content.ComponentName(context, WeatherWidgetProvider::class.java)
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                                val successCallback = android.app.PendingIntent.getBroadcast(
                                                    context,
                                                    0,
                                                    android.content.Intent(context, WeatherWidgetProvider::class.java).apply {
                                                        action = WeatherWidgetProvider.ACTION_UPDATE_WEATHER
                                                    },
                                                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                                                )
                                                appWidgetManager.requestPinAppWidget(weatherProvider, null, successCallback)
                                            } else {
                                                android.widget.Toast.makeText(context, "Launcher does not support widget pinning", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            android.widget.Toast.makeText(context, "Android 8.0+ is required", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (_: Throwable) {
                                        android.widget.Toast.makeText(context, "Could not pin widget on this device", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ PIN WEATHER",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Music Widget
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(Color(0xFF141414))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(23.dp))
                                .clickable {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                                        val musicProvider = android.content.ComponentName(context, MusicWidgetProvider::class.java)
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                                val successCallback = android.app.PendingIntent.getBroadcast(
                                                    context,
                                                    0,
                                                    android.content.Intent(context, MusicWidgetProvider::class.java).apply {
                                                        action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                                                    },
                                                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                                                )
                                                appWidgetManager.requestPinAppWidget(musicProvider, null, successCallback)
                                            } else {
                                                android.widget.Toast.makeText(context, "Launcher does not support widget pinning", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            android.widget.Toast.makeText(context, "Android 8.0+ is required", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (_: Throwable) {
                                        android.widget.Toast.makeText(context, "Could not pin widget on this device", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+ PIN MUSIC",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2C)))
                Spacer(modifier = Modifier.height(24.dp))

                // Section 4: Upload Custom Audio Files Simulator
                Text(
                    text = "IMPORT AUDIO LIBRARY",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SELECT AN AUDIO FILE ON STORAGE TO IMPORT INTO CLOCK MEMORY.",
                    color = Color(0x88FFFFFF),
                    fontSize = 9.sp,
                    fontFamily = LocalCustomFont.current
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (uploadingFileName != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F0F0F))
                            .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "IMPORTING: ${uploadingFileName}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontFamily = LocalCustomFont.current,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // Progress bar container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF1A1A1A))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(uploadProgress)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${(uploadProgress * 100).toInt()}% READY",
                            color = Color(0xAAFFFFFF),
                            fontSize = 9.sp,
                            fontFamily = LocalCustomFont.current
                        )
                    }
                } else {
                    val availableFilesToImport = listOf(
                        "GLYPH_CHATTER.wav" to "320 KB • Clean Pulse",
                        "KINETIC_PULSE.mp3" to "1.2 MB • Techno beat",
                        "SYNAPSE_SHOCK.wav" to "480 KB • Synth Alarm",
                        "AMBER_COAL.ogg" to "890 KB • Warm Sine wave"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableFilesToImport.forEach { (filename, details) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF161616), RoundedCornerShape(12.dp))
                                    .background(Color(0xFF080808))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = filename,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = LocalCustomFont.current,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = details,
                                        color = Color(0xFF666666),
                                        fontSize = 9.sp,
                                        fontFamily = LocalCustomFont.current
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            uploadingFileName = filename
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "IMPORT",
                                        color = Color.Black,
                                        fontSize = 9.sp,
                                        fontFamily = LocalCustomFont.current,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2C)))
                Spacer(modifier = Modifier.height(24.dp))

                // Section 5: Custom Synth Tone Creator
                Text(
                    text = "REAL-TIME SYNTH GENERATOR",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = LocalCustomFont.current,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DESIGN A SYNTHETIC SOUND PATTERN IN MEMORY.",
                    color = Color(0x88FFFFFF),
                    fontSize = 9.sp,
                    fontFamily = LocalCustomFont.current
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Name field
                Text(
                    text = "RINGTONE NAME",
                    color = Color(0x88FFFFFF),
                    fontSize = 9.sp,
                    fontFamily = LocalCustomFont.current
                )
                Spacer(modifier = Modifier.height(4.dp))

                androidx.compose.foundation.text.BasicTextField(
                    value = customRingtoneName,
                    onValueChange = { customRingtoneName = it.take(15).uppercase() },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF141414))
                                .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            if (customRingtoneName.isEmpty()) {
                                Text(
                                    text = "E.G. CHIRP BEAT",
                                    color = Color(0xFF444444),
                                    fontSize = 12.sp,
                                    fontFamily = LocalCustomFont.current
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pitch/Freq slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "BASE FREQUENCY",
                        color = Color(0x88FFFFFF),
                        fontSize = 9.sp,
                        fontFamily = LocalCustomFont.current
                    )
                    Text(
                        text = "${synthFreq.toInt()} HZ",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = synthFreq,
                    onValueChange = { synthFreq = it },
                    valueRange = 200f..1500f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0xFF2C2C2C)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Modulation Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MODULATION SPEED",
                        color = Color(0x88FFFFFF),
                        fontSize = 9.sp,
                        fontFamily = LocalCustomFont.current
                    )
                    Text(
                        text = "${synthLfo.toInt()} HZ",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = synthLfo,
                    onValueChange = { synthLfo = it },
                    valueRange = 1f..15f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0xFF2C2C2C)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Waveform type
                Text(
                    text = "WAVEFORM SELECT",
                    color = Color(0x88FFFFFF),
                    fontSize = 9.sp,
                    fontFamily = LocalCustomFont.current
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val waves = listOf("SINE", "SQUARE", "TRIANGLE", "CHIRP")
                    waves.forEach { w ->
                        val isSelected = synthWave == w
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White else Color(0xFF141414))
                                .border(1.dp, if (isSelected) Color.Transparent else Color(0x15FFFFFF), RoundedCornerShape(8.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    synthWave = w
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = w,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 9.sp,
                                fontFamily = LocalCustomFont.current,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Generate & preview button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (customRingtoneName.isNotEmpty()) Color.White else Color(0xFF141414))
                        .border(
                            width = 1.dp,
                            color = if (customRingtoneName.isNotEmpty()) Color.Transparent else Color(0x20FFFFFF),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable(enabled = customRingtoneName.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val cleanName = customRingtoneName.trim()
                            val customTone = com.example.service.CustomRingtone(
                                name = cleanName,
                                frequency = synthFreq,
                                lfoSpeed = synthLfo,
                                waveform = synthWave
                            )
                            com.example.service.CustomRingtoneManager.addCustomRingtone(context, customTone)
                            allRingtonesList = com.example.service.CustomRingtoneManager.getAllRingtones()
                            com.example.service.AudioSynthPlayer.play(cleanName)
                            coroutineScope.launch {
                                delay(1500)
                                com.example.service.AudioSynthPlayer.stop()
                            }
                            customRingtoneName = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SYNTHESIZE & IMPORT TONE",
                        color = if (customRingtoneName.isNotEmpty()) Color.Black else Color(0x44FFFFFF),
                        fontSize = 11.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of current custom ringtones with deletion option
                val userCreatedRingtones = allRingtonesList.filter {
                    !listOf("GLYPH RAPID", "VOX UNISON", "TEENAGE AMBIENT", "SILENT STATE", "RETRO BEATS", "DIGITAL CHIRP").contains(it)
                }

                if (userCreatedRingtones.isNotEmpty()) {
                    Text(
                        text = "USER IMPORTED TONES",
                        color = Color(0x88FFFFFF),
                        fontSize = 9.sp,
                        fontFamily = LocalCustomFont.current
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        userCreatedRingtones.forEach { rName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F0F0F))
                                    .border(1.dp, Color(0xFF1F1F1F), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = rName,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = LocalCustomFont.current,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Play Preview
                                    Text(
                                        text = "PLAY",
                                        color = Color(0xFF888888),
                                        fontSize = 10.sp,
                                        fontFamily = LocalCustomFont.current,
                                        modifier = Modifier.clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            com.example.service.AudioSynthPlayer.play(rName)
                                            coroutineScope.launch {
                                                delay(1500)
                                                com.example.service.AudioSynthPlayer.stop()
                                            }
                                        }
                                    )

                                    // Delete
                                    Text(
                                        text = "DELETE",
                                        color = Color(0xFFFF4D4D),
                                        fontSize = 10.sp,
                                        fontFamily = LocalCustomFont.current,
                                        modifier = Modifier.clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            com.example.service.CustomRingtoneManager.removeCustomRingtone(context, rName)
                                            allRingtonesList = com.example.service.CustomRingtoneManager.getAllRingtones()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2C2C2C)))
                Spacer(modifier = Modifier.height(24.dp))
                
                // Finish / Save button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White, RoundedCornerShape(24.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DONE",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = LocalCustomFont.current,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}


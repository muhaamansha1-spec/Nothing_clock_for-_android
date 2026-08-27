package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Alarm
import com.example.data.model.WorldClock
import com.example.data.repository.ClockRepository
import com.example.service.AudioSynthPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.TimeZone
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import android.content.Intent

enum class ClockSection {
    ALARMS, WORLD_CLOCK, STOPWATCH, TIMER
}

enum class TimerState {
    IDLE, RUNNING, PAUSED
}

enum class CameraCutout {
    NONE, LEFT_CORNER, CENTER, RIGHT_CORNER
}

class ClockViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ClockRepository(db.alarmDao(), db.worldClockDao())

    private val sharedPrefs = application.getSharedPreferences("nothing_clock_settings", android.content.Context.MODE_PRIVATE)

    // Camera Cutout custom state
    private val _selectedCutout = MutableStateFlow(
        CameraCutout.valueOf(sharedPrefs.getString("camera_cutout", CameraCutout.NONE.name) ?: CameraCutout.NONE.name)
    )
    val selectedCutout: StateFlow<CameraCutout> = _selectedCutout.asStateFlow()

    fun setCameraCutout(cutout: CameraCutout) {
        _selectedCutout.value = cutout
        sharedPrefs.edit().putString("camera_cutout", cutout.name).apply()
    }

    // 12-hour vs 24-hour setting state
    private val _is24Hour = MutableStateFlow(
        sharedPrefs.getBoolean("is_24_hour", true)
    )
    val is24Hour: StateFlow<Boolean> = _is24Hour.asStateFlow()

    fun setIs24Hour(enabled: Boolean) {
        _is24Hour.value = enabled
        sharedPrefs.edit().putBoolean("is_24_hour", enabled).apply()
        com.example.AlarmWidgetProvider.updateAllWidgets(getApplication())
    }

    // Ringtone alert states
    private val _ringingAlarm = MutableStateFlow<Alarm?>(null)
    val ringingAlarm: StateFlow<Alarm?> = _ringingAlarm.asStateFlow()

    private val _isTimerRinging = MutableStateFlow(false)
    val isTimerRinging: StateFlow<Boolean> = _isTimerRinging.asStateFlow()

    private val _timerRingtone = MutableStateFlow(
        sharedPrefs.getString("timer_ringtone", "TEENAGE AMBIENT") ?: "TEENAGE AMBIENT"
    )
    val timerRingtone: StateFlow<String> = _timerRingtone.asStateFlow()

    fun setTimerRingtone(ringtone: String) {
        _timerRingtone.value = ringtone
        sharedPrefs.edit().putString("timer_ringtone", ringtone).apply()
    }

    // Section state
    private val _currentSection = MutableStateFlow(ClockSection.ALARMS)
    val currentSection: StateFlow<ClockSection> = _currentSection.asStateFlow()

    fun selectSection(section: ClockSection) {
        _currentSection.value = section
    }

    // Database flows
    val alarms: StateFlow<List<Alarm>> = repository.allAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val worldClocks: StateFlow<List<WorldClock>> = repository.allWorldClocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Time ticker for ticking clocks (updates every second)
    private val _ticker = MutableStateFlow(System.currentTimeMillis())
    val ticker: StateFlow<Long> = _ticker.asStateFlow()

    private var lastTriggeredMinute = -1

    init {
        // Synchronize with background AlarmStateHolder
        viewModelScope.launch {
            com.example.service.AlarmStateHolder.ringingAlarm.collect { alarm ->
                _ringingAlarm.value = alarm
            }
        }
        viewModelScope.launch {
            com.example.service.AlarmStateHolder.isTimerRinging.collect { ringing ->
                _isTimerRinging.value = ringing
            }
        }

        // Start ticker loop for local UI updates
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
            while (true) {
                val now = System.currentTimeMillis()
                _ticker.value = now
                delay(1000)
            }
        }
    }

    fun dismissAlarm() {
        val intent = Intent(getApplication(), com.example.service.AlarmTriggerService::class.java).apply {
            action = com.example.service.AlarmTriggerService.ACTION_STOP_ALARM
        }
        getApplication<Application>().startService(intent)
    }

    fun snoozeAlarm() {
        val intent = Intent(getApplication(), com.example.service.AlarmTriggerService::class.java).apply {
            action = com.example.service.AlarmTriggerService.ACTION_SNOOZE_ALARM
        }
        getApplication<Application>().startService(intent)
    }

    // Alarm Actions
    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.updateAlarm(updated)
            if (updated.isEnabled) {
                com.example.service.SystemAlarmScheduler.scheduleAlarm(getApplication(), updated)
            } else {
                com.example.service.SystemAlarmScheduler.cancelAlarm(getApplication(), updated)
            }
            com.example.AlarmWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun addAlarm(hour: Int, minute: Int, daysOfWeek: String, label: String, vibrate: Boolean, ringtone: String = "GLYPH RAPID") {
        viewModelScope.launch {
            val alarm = Alarm(
                hour = hour,
                minute = minute,
                daysOfWeek = daysOfWeek,
                label = label,
                isEnabled = true,
                isVibrateEnabled = vibrate,
                ringtone = ringtone
            )
            val insertedId = repository.insertAlarm(alarm).toInt()
            val scheduledAlarm = alarm.copy(id = insertedId)
            com.example.service.SystemAlarmScheduler.scheduleAlarm(getApplication(), scheduledAlarm)
            com.example.AlarmWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
            if (alarm.isEnabled) {
                com.example.service.SystemAlarmScheduler.scheduleAlarm(getApplication(), alarm)
            } else {
                com.example.service.SystemAlarmScheduler.cancelAlarm(getApplication(), alarm)
            }
            com.example.AlarmWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
            com.example.service.SystemAlarmScheduler.cancelAlarm(getApplication(), alarm)
            com.example.AlarmWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    // World Clock Actions
    fun addWorldClock(cityName: String, timezoneId: String, country: String) {
        viewModelScope.launch {
            repository.insertWorldClock(
                WorldClock(
                    cityName = cityName,
                    timezoneId = timezoneId,
                    country = country,
                    isFavorite = true
                )
            )
        }
    }

    fun deleteWorldClock(clock: WorldClock) {
        viewModelScope.launch {
            repository.deleteWorldClock(clock)
        }
    }

    // --- Stopwatch Logic ---
    private val _stopwatchTime = MutableStateFlow(0L)
    val stopwatchTime: StateFlow<Long> = _stopwatchTime.asStateFlow()

    private val _isStopwatchRunning = MutableStateFlow(false)
    val isStopwatchRunning: StateFlow<Boolean> = _isStopwatchRunning.asStateFlow()

    private val _laps = MutableStateFlow<List<Long>>(emptyList())
    val laps: StateFlow<List<Long>> = _laps.asStateFlow()

    private var stopwatchJob: Job? = null
    private var stopwatchStartTime = 0L
    private var accumulatedTime = 0L

    fun startStopwatch() {
        if (_isStopwatchRunning.value) return
        _isStopwatchRunning.value = true
        stopwatchStartTime = System.currentTimeMillis()
        stopwatchJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - stopwatchStartTime
                _stopwatchTime.value = accumulatedTime + elapsed
                delay(16) // ~60fps refresh rate
            }
        }
    }

    fun pauseStopwatch() {
        if (!_isStopwatchRunning.value) return
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
        accumulatedTime += System.currentTimeMillis() - stopwatchStartTime
        _stopwatchTime.value = accumulatedTime
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        _isStopwatchRunning.value = false
        accumulatedTime = 0L
        _stopwatchTime.value = 0L
        _laps.value = emptyList()
    }

    fun addLap() {
        val totalCurrent = _stopwatchTime.value
        val lastLapSum = _laps.value.sum()
        val currentLapTime = totalCurrent - lastLapSum
        _laps.value = listOf(currentLapTime) + _laps.value
    }

    // --- Timer Logic ---
    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _timerDurationInput = MutableStateFlow(0L) // Total seconds set by user
    val timerDurationInput: StateFlow<Long> = _timerDurationInput.asStateFlow()

    private val _timerSecondsRemaining = MutableStateFlow(0L)
    val timerSecondsRemaining: StateFlow<Long> = _timerSecondsRemaining.asStateFlow()

    private var timerJob: Job? = null

    // For circular indicators
    val timerPercentage: Float
        get() {
            val total = _timerDurationInput.value
            val remaining = _timerSecondsRemaining.value
            return if (total > 0) remaining.toFloat() / total.toFloat() else 0f
        }

    fun setTimerDuration(seconds: Long) {
        if (_timerState.value == TimerState.IDLE) {
            _timerDurationInput.value = seconds
            _timerSecondsRemaining.value = seconds
        }
    }

    fun startTimer() {
        if (_timerSecondsRemaining.value <= 0) return
        timerJob?.cancel()
        _isTimerRinging.value = false
        AudioSynthPlayer.stop()
        _timerState.value = TimerState.RUNNING
        
        // Schedule system AlarmManager timer
        com.example.service.SystemAlarmScheduler.scheduleTimer(
            getApplication(),
            _timerSecondsRemaining.value,
            _timerRingtone.value
        )

        timerJob = viewModelScope.launch {
            while (_timerSecondsRemaining.value > 0) {
                delay(1000)
                _timerSecondsRemaining.value -= 1
            }
            _timerState.value = TimerState.IDLE
            _timerDurationInput.value = 0
            _timerSecondsRemaining.value = 0
            // When finished, the AlarmManager triggers the BroadcastReceiver which starts AlarmTriggerService,
            // which sets AlarmStateHolder.isTimerRinging to true, which we collect reactively in _isTimerRinging!
        }
    }

    fun dismissTimerRingtone() {
        _isTimerRinging.value = false
        AudioSynthPlayer.stop()
        val intent = Intent(getApplication(), com.example.service.AlarmTriggerService::class.java).apply {
            action = com.example.service.AlarmTriggerService.ACTION_STOP_TIMER
        }
        getApplication<Application>().startService(intent)
    }

    fun pauseTimer() {
        if (_timerState.value != TimerState.RUNNING) return
        _timerState.value = TimerState.PAUSED
        timerJob?.cancel()
        com.example.service.SystemAlarmScheduler.cancelTimer(getApplication())
    }

    fun resetTimer() {
        timerJob?.cancel()
        com.example.service.SystemAlarmScheduler.cancelTimer(getApplication())
        _isTimerRinging.value = false
        AudioSynthPlayer.stop()
        val intent = Intent(getApplication(), com.example.service.AlarmTriggerService::class.java).apply {
            action = com.example.service.AlarmTriggerService.ACTION_STOP_TIMER
        }
        getApplication<Application>().startService(intent)
        _timerState.value = TimerState.IDLE
        _timerSecondsRemaining.value = _timerDurationInput.value
    }

    fun clearTimer() {
        timerJob?.cancel()
        com.example.service.SystemAlarmScheduler.cancelTimer(getApplication())
        _isTimerRinging.value = false
        AudioSynthPlayer.stop()
        val intent = Intent(getApplication(), com.example.service.AlarmTriggerService::class.java).apply {
            action = com.example.service.AlarmTriggerService.ACTION_STOP_TIMER
        }
        getApplication<Application>().startService(intent)
        _timerState.value = TimerState.IDLE
        _timerDurationInput.value = 0
        _timerSecondsRemaining.value = 0
    }
}

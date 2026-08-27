package com.example.service

import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MediaPlaybackManager {
    private val _trackTitle = MutableStateFlow("")
    val trackTitle: StateFlow<String> = _trackTitle.asStateFlow()

    private val _trackArtist = MutableStateFlow("")
    val trackArtist: StateFlow<String> = _trackArtist.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _trackDuration = MutableStateFlow(0) // in seconds
    val trackDuration: StateFlow<Int> = _trackDuration.asStateFlow()

    private val _trackPosition = MutableStateFlow(0) // in seconds
    val trackPosition: StateFlow<Int> = _trackPosition.asStateFlow()

    private val _hasActiveSession = MutableStateFlow(false)
    val hasActiveSession: StateFlow<Boolean> = _hasActiveSession.asStateFlow()

    private val _albumArt = MutableStateFlow<Bitmap?>(null)
    val albumArt: StateFlow<Bitmap?> = _albumArt.asStateFlow()

    @Volatile
    private var activeController: MediaController? = null

    fun updateController(controller: MediaController?) {
        activeController = controller
        if (controller == null) {
            _hasActiveSession.value = false
            _trackTitle.value = ""
            _trackArtist.value = ""
            _albumArt.value = null
            _isPlaying.value = false
            return
        }
        _hasActiveSession.value = true
        updateMetadata(controller.metadata)
        updatePlaybackState(controller.playbackState)
    }

    fun updateMetadata(metadata: MediaMetadata?) {
        if (metadata != null) {
            // Retrieve track info, handle fallbacks cleanly
            _trackTitle.value = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Track"
            _trackArtist.value = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
            _trackDuration.value = if (durationMs > 0) (durationMs / 1000).toInt() else 0

            var art: Bitmap? = null
            try {
                art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                if (art == null) {
                    art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                }
                if (art == null) {
                    art = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
                }
            } catch (e: Exception) {
                Log.e("MediaPlaybackManager", "Error getting metadata artwork", e)
            }
            _albumArt.value = art
        } else {
            _trackTitle.value = ""
            _trackArtist.value = ""
            _trackDuration.value = 0
            _albumArt.value = null
        }
    }

    fun updatePlaybackState(state: PlaybackState?) {
        if (state != null) {
            _isPlaying.value = state.state == PlaybackState.STATE_PLAYING
            _trackPosition.value = (state.position / 1000).toInt()
        } else {
            _isPlaying.value = false
            _trackPosition.value = 0
        }
    }

    fun play() {
        try {
            activeController?.transportControls?.play()
        } catch (e: Exception) {
            Log.e("MediaPlaybackManager", "Error calling play()", e)
        }
    }

    fun pause() {
        try {
            activeController?.transportControls?.pause()
        } catch (e: Exception) {
            Log.e("MediaPlaybackManager", "Error calling pause()", e)
        }
    }

    fun skipToNext() {
        try {
            activeController?.transportControls?.skipToNext()
        } catch (e: Exception) {
            Log.e("MediaPlaybackManager", "Error calling skipToNext()", e)
        }
    }

    fun skipToPrevious() {
        try {
            activeController?.transportControls?.skipToPrevious()
        } catch (e: Exception) {
            Log.e("MediaPlaybackManager", "Error calling skipToPrevious()", e)
        }
    }
}

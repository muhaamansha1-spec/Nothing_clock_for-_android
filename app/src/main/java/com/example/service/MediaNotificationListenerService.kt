package com.example.service

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MediaNotificationListenerService : NotificationListenerService() {

    private lateinit var sessionManager: MediaSessionManager
    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            MediaPlaybackManager.updatePlaybackState(state)
            com.example.MusicWidgetProvider.updateAllWidgets(this@MediaNotificationListenerService)
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            MediaPlaybackManager.updateMetadata(metadata)
            com.example.MusicWidgetProvider.updateAllWidgets(this@MediaNotificationListenerService)
        }
    }

    private var registeredController: MediaController? = null

    override fun onCreate() {
        super.onCreate()
        sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        tryToBindActiveSession()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        tryToBindActiveSession()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        tryToBindActiveSession()
    }

    private fun tryToBindActiveSession() {
        try {
            val component = ComponentName(this, MediaNotificationListenerService::class.java)
            val controllers = sessionManager.getActiveSessions(component)
            
            // Prioritize controller that is currently playing, fallback to first available
            val active = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: controllers.firstOrNull()

            if (active != registeredController) {
                registeredController?.unregisterCallback(controllerCallback)
                registeredController = active
                try {
                    registeredController?.registerCallback(controllerCallback)
                } catch (e: Exception) {
                    Log.e("MediaNotificationListener", "Failed to register controller callback", e)
                }
            }
            MediaPlaybackManager.updateController(active)
            com.example.MusicWidgetProvider.updateAllWidgets(this)
        } catch (e: SecurityException) {
            // Notification Listener access is not granted yet
            MediaPlaybackManager.updateController(null)
            com.example.MusicWidgetProvider.updateAllWidgets(this)
        } catch (e: Exception) {
            Log.e("MediaNotificationListener", "Error binding active media session", e)
        }
    }

    override fun onDestroy() {
        try {
            registeredController?.unregisterCallback(controllerCallback)
        } catch (e: Exception) {
            Log.e("MediaNotificationListener", "Failed on onDestroy unregister", e)
        }
        super.onDestroy()
    }
}

package com.heimdall.gjallaraudio.feature.night

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveMediaSessionInfo(
    val packageName: String,
    val appName: String,
    val title: String?,
    val artist: String?,
    val isPlaying: Boolean
)

class MediaNotificationListener : NotificationListenerService() {

    companion object {
        var isListenerConnected: Boolean = false
            private set

        private val _currentSession = MutableStateFlow<ActiveMediaSessionInfo?>(null)
        val currentSession: StateFlow<ActiveMediaSessionInfo?> = _currentSession.asStateFlow()

        fun pauseAll(context: Context) {
            try {
                val mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                val componentName = ComponentName(context, MediaNotificationListener::class.java)
                val controllers = mediaSessionManager?.getActiveSessions(componentName)
                controllers?.forEach { controller ->
                    try {
                        controller.transportControls.pause()
                        controller.transportControls.stop()
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isListenerConnected = true
        refreshActiveSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isListenerConnected = false
        _currentSession.value = null
    }

    fun refreshActiveSessions() {
        try {
            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
            val componentName = ComponentName(this, MediaNotificationListener::class.java)
            val controllers = mediaSessionManager.getActiveSessions(componentName)
            val active = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: controllers.firstOrNull()

            if (active != null) {
                val pm = packageManager
                val appLabel = try {
                    val appInfo = pm.getApplicationInfo(active.packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    active.packageName
                }
                val meta = active.metadata
                val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
                val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                val isPlaying = active.playbackState?.state == PlaybackState.STATE_PLAYING

                _currentSession.value = ActiveMediaSessionInfo(
                    packageName = active.packageName,
                    appName = appLabel,
                    title = title,
                    artist = artist,
                    isPlaying = isPlaying
                )
            }
        } catch (_: Exception) {}
    }
}

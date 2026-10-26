package com.heimdall.gjallaraudio.feature.night

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.widget.Toast

data class InstalledStreamingApp(
    val packageName: String,
    val name: String
)

data class StreamingPlaybackState(
    val isMusicActive: Boolean,
    val detectedAppPackage: String?,
    val detectedAppName: String?,
    val trackTitle: String?,
    val trackArtist: String?,
    val isExactInfo: Boolean,
    val installedStreamingApps: List<InstalledStreamingApp>,
    val isNotificationAccessGranted: Boolean
)

object StreamingAppDetector {

    val POPULAR_STREAMING_APPS = listOf(
        "com.spotify.music" to "Spotify",
        "com.google.android.apps.youtube.music" to "YouTube Music",
        "com.google.android.youtube" to "YouTube",
        "com.apple.android.music" to "Apple Music",
        "com.amazon.mp3" to "Amazon Music",
        "com.audible.application" to "Audible",
        "com.ivoox.app" to "iVoox",
        "deezer.android.app" to "Deezer",
        "com.aspiro.tidal" to "Tidal",
        "com.soundcloud.android" to "SoundCloud",
        "au.com.shiftyjelly.pocketcasts" to "Pocket Casts",
        "com.podbean.app.podcast" to "Podbean",
        "tunein.player" to "TuneIn Radio"
    )

    fun isNotificationAccessGranted(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(context.packageName)
    }

    fun openNotificationAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun getInstalledStreamingApps(context: Context): List<InstalledStreamingApp> {
        val pm = context.packageManager
        val list = mutableListOf<InstalledStreamingApp>()
        for ((pkg, defaultName) in POPULAR_STREAMING_APPS) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo)?.toString() ?: defaultName
                list.add(InstalledStreamingApp(packageName = pkg, name = label))
            } catch (_: Exception) {}
        }
        return list
    }

    fun detectPlaybackState(context: Context): StreamingPlaybackState {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isMusicActive = audioManager.isMusicActive
        val hasNotificationAccess = isNotificationAccessGranted(context)
        val installedApps = getInstalledStreamingApps(context)

        // 1. Comprobar si tenemos información exacta del listener
        val session = MediaNotificationListener.currentSession.value
        if (session != null && session.isPlaying) {
            return StreamingPlaybackState(
                isMusicActive = true,
                detectedAppPackage = session.packageName,
                detectedAppName = session.appName,
                trackTitle = session.title,
                trackArtist = session.artist,
                isExactInfo = true,
                installedStreamingApps = installedApps,
                isNotificationAccessGranted = hasNotificationAccess
            )
        }

        // 2. Si no hay sesión exacta pero hay música activa, buscar app instalada preferente
        val targetApp = installedApps.firstOrNull()

        return StreamingPlaybackState(
            isMusicActive = isMusicActive,
            detectedAppPackage = targetApp?.packageName,
            detectedAppName = targetApp?.name ?: if (isMusicActive) "App de Música" else null,
            trackTitle = null,
            trackArtist = null,
            isExactInfo = false,
            installedStreamingApps = installedApps,
            isNotificationAccessGranted = hasNotificationAccess
        )
    }

    fun launchApp(context: Context, packageName: String) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "No se pudo abrir la app", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al abrir la app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

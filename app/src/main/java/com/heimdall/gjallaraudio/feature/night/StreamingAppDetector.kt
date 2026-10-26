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
        "app.revanced.android.youtube" to "YouTube ReVanced",
        "app.rvx.android.youtube" to "ReVanced Extended",
        "app.revanced.android.apps.youtube.music" to "YT Music ReVanced",
        "app.rvx.android.apps.youtube.music" to "YT Music Extended",
        "com.google.android.youtube" to "YouTube",
        "com.vanced.android.youtube" to "YouTube Vanced",
        "org.schabi.newpipe" to "NewPipe",
        "com.spotify.music" to "Spotify",
        "com.google.android.apps.youtube.music" to "YouTube Music",
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

    private const val PREFS_NAME = "gjallar_streaming_prefs"
    private const val KEY_PREFERRED_PKG = "preferred_streaming_package"

    fun getPreferredApp(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PREFERRED_PKG, null)
    }

    fun setPreferredApp(context: Context, packageName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFERRED_PKG, packageName)
            .apply()
    }

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

    @Volatile
    private var cachedStreamingApps: List<InstalledStreamingApp>? = null

    fun getInstalledStreamingApps(context: Context, forceRefresh: Boolean = false): List<InstalledStreamingApp> {
        if (!forceRefresh && cachedStreamingApps != null) {
            return cachedStreamingApps!!
        }

        val pm = context.packageManager
        val list = mutableListOf<InstalledStreamingApp>()
        val seenPackages = mutableSetOf<String>()

        for ((pkg, defaultName) in POPULAR_STREAMING_APPS) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo)?.toString() ?: defaultName
                list.add(InstalledStreamingApp(packageName = pkg, name = label))
                seenPackages.add(pkg)
            } catch (_: Exception) {}
        }

        // Búsqueda dinámica de cualquier aplicación ReVanced o cliente mod instalado
        try {
            val installedPackages = pm.getInstalledApplications(0)
            for (app in installedPackages) {
                if (seenPackages.contains(app.packageName)) continue
                val pkgLower = app.packageName.lowercase()
                val label = pm.getApplicationLabel(app).toString()
                val labelLower = label.lowercase()
                if (pkgLower.contains("revanced") || pkgLower.contains("vanced") ||
                    labelLower.contains("revanced") || labelLower.contains("vanced") ||
                    labelLower.contains("newpipe") || labelLower.contains("innertune") ||
                    labelLower.contains("vimusic")
                ) {
                    list.add(InstalledStreamingApp(packageName = app.packageName, name = label))
                    seenPackages.add(app.packageName)
                }
            }
        } catch (_: Exception) {}

        // Garantizar que YouTube ReVanced esté siempre disponible entre las opciones para seleccionar
        val hasReVanced = list.any { it.packageName.contains("revanced") || it.name.contains("ReVanced", ignoreCase = true) }
        if (!hasReVanced) {
            list.add(0, InstalledStreamingApp(packageName = "app.revanced.android.youtube", name = "YouTube ReVanced"))
        }

        cachedStreamingApps = list
        return list
    }

    fun detectPlaybackState(context: Context): StreamingPlaybackState {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isMusicActive = audioManager.isMusicActive
        val hasNotificationAccess = isNotificationAccessGranted(context)
        val installedApps = getInstalledStreamingApps(context)
        val preferredPkg = getPreferredApp(context)

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

        // 2. Si hay app preferida por el usuario instalada (ej. YouTube ReVanced), priorizarla
        val preferredApp = installedApps.firstOrNull { it.packageName == preferredPkg }
        val targetApp = preferredApp ?: installedApps.firstOrNull()

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

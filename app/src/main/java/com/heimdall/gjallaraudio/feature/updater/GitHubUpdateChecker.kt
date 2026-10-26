package com.heimdall.gjallaraudio.feature.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.heimdall.gjallaraudio.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class UpToDate(val currentVersion: String) : UpdateStatus()
    data class UpdateAvailable(
        val newVersion: String,
        val releaseName: String,
        val releaseNotes: String,
        val downloadUrl: String,
        val apkSizeMb: Float
    ) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

object GitHubUpdateChecker {
    private const val GITHUB_REPO = "Hungrow/Gjallar-Audio"
    private const val LATEST_RELEASE_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(): UpdateStatus = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "GjallarAudio-App")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code == 404) {
                    return@withContext UpdateStatus.Error("No se encontraron publicaciones (releases) en el repositorio.")
                }
                return@withContext UpdateStatus.Error("Error al contactar con GitHub (HTTP ${response.code})")
            }

            val bodyString = response.body?.string() ?: return@withContext UpdateStatus.Error("Respuesta vacía de GitHub.")
            val json = JSONObject(bodyString)

            val rawTag = json.optString("tag_name", "").trim()
            val releaseName = json.optString("name", rawTag)
            val releaseNotes = json.optString("body", "Sin notas de versión disponibles.")

            val remoteVersionClean = rawTag.removePrefix("v").removePrefix("V").trim()
            val currentVersionClean = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()

            // Buscar el asset .apk
            var downloadUrl = ""
            var apkSizeBytes: Long = 0
            val assetsArray = json.optJSONArray("assets")
            if (assetsArray != null) {
                for (i in 0 until assetsArray.length()) {
                    val asset = assetsArray.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        apkSizeBytes = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            // Si no hay asset específico, usar el html_url de la release
            if (downloadUrl.isBlank()) {
                downloadUrl = json.optString("html_url", "https://github.com/$GITHUB_REPO/releases/latest")
            }

            val apkSizeMb = if (apkSizeBytes > 0) {
                apkSizeBytes / (1024f * 1024f)
            } else {
                0f
            }

            if (isNewerVersion(remoteVersionClean, currentVersionClean)) {
                UpdateStatus.UpdateAvailable(
                    newVersion = rawTag,
                    releaseName = releaseName,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    apkSizeMb = apkSizeMb
                )
            } else {
                UpdateStatus.UpToDate(currentVersion = BuildConfig.VERSION_NAME)
            }
        } catch (e: Exception) {
            UpdateStatus.Error("Error de conexión: ${e.localizedMessage ?: "No se pudo conectar a GitHub"}")
        }
    }

    fun openDownloadUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        if (remote.isBlank() || current.isBlank()) return false
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}

package com.heimdall.gjallaraudio.feature.soundboard

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

class CanterosAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    private val _currentlyPlayingId = MutableStateFlow<String?>(null)
    val currentlyPlayingId: StateFlow<String?> = _currentlyPlayingId.asStateFlow()

    fun togglePlay(sound: CanterosSound) {
        if (_currentlyPlayingId.value == sound.id) {
            stop()
            return
        }

        play(sound)
    }

    fun play(sound: CanterosSound) {
        stop()

        try {
            val afd = context.assets.openFd(sound.assetPath)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                prepare()
                setOnCompletionListener {
                    _currentlyPlayingId.value = null
                    stop()
                }
                start()
            }
            _currentlyPlayingId.value = sound.id
        } catch (e: Exception) {
            e.printStackTrace()
            _currentlyPlayingId.value = null
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _currentlyPlayingId.value = null
    }

    fun shareSound(sound: CanterosSound) {
        try {
            val cleanName = sound.title.replace(Regex("[^a-zA-Z0-9_áéíóúñÁÉÍÓÚÑ]"), "_")
            val cacheFile = File(context.cacheDir, "Canteros_${cleanName}.mp3")

            context.assets.open(sound.assetPath).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/mpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "🔊 ${sound.title} — \"${sound.quote}\"")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Compartir audio en...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

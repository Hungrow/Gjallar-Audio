package com.heimdall.gjallaraudio.feature.night

import android.content.Context
import android.media.MediaPlayer
import com.heimdall.gjallaraudio.R

enum class SoundscapeType {
    RAIN,       // Lluvia en Asgard
    WIND,       // Viento de Midgard
    FIRE,       // Hoguera Rúnica
    DELTA       // Ondas Binaurales Delta
}

class AmbientChannelPlayer(private val context: Context, private val rawResId: Int) {
    private var mediaPlayer: MediaPlayer? = null
    var volume: Float = 0f
        private set

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        if (volume > 0.01f) {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context, rawResId)?.apply {
                    isLooping = true
                }
            }
            mediaPlayer?.setVolume(volume, volume)
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } else {
            mediaPlayer?.pause()
        }
    }

    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}

class AmbientSoundEngine(context: Context) {
    private val rain = AmbientChannelPlayer(context, R.raw.rain_loop)
    private val wind = AmbientChannelPlayer(context, R.raw.wind_loop)
    private val fire = AmbientChannelPlayer(context, R.raw.fire_loop)
    private val delta = AmbientChannelPlayer(context, R.raw.delta_loop)

    fun setVolume(type: SoundscapeType, volume: Float) {
        when (type) {
            SoundscapeType.RAIN -> rain.setVolume(volume)
            SoundscapeType.WIND -> wind.setVolume(volume)
            SoundscapeType.FIRE -> fire.setVolume(volume)
            SoundscapeType.DELTA -> delta.setVolume(volume)
        }
    }

    fun stopAll() {
        rain.release()
        wind.release()
        fire.release()
        delta.release()
    }
}

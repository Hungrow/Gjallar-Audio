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

    companion object {
        @Volatile
        private var instance: AmbientSoundEngine? = null

        fun getInstance(context: Context): AmbientSoundEngine {
            return instance ?: synchronized(this) {
                instance ?: AmbientSoundEngine(context.applicationContext).also { instance = it }
            }
        }

        fun stopActiveSounds() {
            instance?.stopAll()
        }

        fun fadeVolume(factor: Float) {
            instance?.applyVolumeFactor(factor)
        }
    }

    private val rain = AmbientChannelPlayer(context, R.raw.rain_loop)
    private val wind = AmbientChannelPlayer(context, R.raw.wind_loop)
    private val fire = AmbientChannelPlayer(context, R.raw.fire_loop)
    private val delta = AmbientChannelPlayer(context, R.raw.delta_loop)

    private var baseRainVol = 0f
    private var baseWindVol = 0f
    private var baseFireVol = 0f
    private var baseDeltaVol = 0f

    fun setVolume(type: SoundscapeType, volume: Float) {
        when (type) {
            SoundscapeType.RAIN -> { baseRainVol = volume; rain.setVolume(volume) }
            SoundscapeType.WIND -> { baseWindVol = volume; wind.setVolume(volume) }
            SoundscapeType.FIRE -> { baseFireVol = volume; fire.setVolume(volume) }
            SoundscapeType.DELTA -> { baseDeltaVol = volume; delta.setVolume(volume) }
        }
    }

    fun applyVolumeFactor(factor: Float) {
        val f = factor.coerceIn(0f, 1f)
        rain.setVolume(baseRainVol * f)
        wind.setVolume(baseWindVol * f)
        fire.setVolume(baseFireVol * f)
        delta.setVolume(baseDeltaVol * f)
    }

    fun stopAll() {
        rain.release()
        wind.release()
        fire.release()
        delta.release()
        baseRainVol = 0f
        baseWindVol = 0f
        baseFireVol = 0f
        baseDeltaVol = 0f
    }
}

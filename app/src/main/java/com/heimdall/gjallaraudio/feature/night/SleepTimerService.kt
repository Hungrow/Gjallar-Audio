package com.heimdall.gjallaraudio.feature.night

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import com.heimdall.gjallaraudio.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class SleepTimerService : Service(), SensorEventListener {

    companion object {
        const val CHANNEL_ID = "gjallar_sleep_timer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_EXTEND = "ACTION_EXTEND"

        const val EXTRA_DURATION_SECONDS = "EXTRA_DURATION_SECONDS"
        const val EXTRA_FADE_OUT_MINUTES = "EXTRA_FADE_OUT_MINUTES"
        const val EXTRA_SHAKE_TO_EXTEND = "EXTRA_SHAKE_TO_EXTEND"
        const val EXTRA_SMART_SLEEP_DETECTION = "EXTRA_SMART_SLEEP_DETECTION"

        private val _remainingSeconds = MutableStateFlow<Long>(0)
        val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }

    private var countDownTimer: CountDownTimer? = null
    private var totalSeconds: Long = 0
    private var fadeOutDurationSeconds: Long = 120 // 2 minutes fade-out by default
    private var shakeEnabled = true
    private var smartSleepEnabled = false
    private var stillnessStartTime: Long = 0

    private lateinit var audioManager: AudioManager
    private var initialVolume: Int = 0
    private var maxVolume: Int = 0

    // Sensor Shake & Stillness detection
    private var sensorManager: SensorManager? = null
    private var lastShakeTime: Long = 0
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val duration = intent.getLongExtra(EXTRA_DURATION_SECONDS, 1800) // 30 min default
                fadeOutDurationSeconds = intent.getLongExtra(EXTRA_FADE_OUT_MINUTES, 2) * 60
                shakeEnabled = intent.getBooleanExtra(EXTRA_SHAKE_TO_EXTEND, true)
                smartSleepEnabled = intent.getBooleanExtra(EXTRA_SMART_SLEEP_DETECTION, false)
                startTimer(duration)
            }
            ACTION_EXTEND -> {
                extendTimer(600) // Add 10 minutes
            }
            ACTION_STOP -> {
                stopTimer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(durationSec: Long) {
        totalSeconds = durationSec
        _remainingSeconds.value = durationSec
        _isRunning.value = true
        stillnessStartTime = System.currentTimeMillis()

        initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        // Adquirir WakeLock para evitar que el modo reposo / Doze de Android pause el temporizador con la pantalla apagada
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GjallarAudio:SleepTimerWakeLock").apply {
                setReferenceCounted(false)
                acquire((durationSec + 120) * 1000L)
            }
        } catch (_: Exception) {}

        if (shakeEnabled || smartSleepEnabled) {
            val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometer?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification(durationSec))
        runCountdown(durationSec)
    }

    private fun runCountdown(durationSec: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(durationSec * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = millisUntilFinished / 1000
                _remainingSeconds.value = remaining

                // Handle volume fade out
                if (remaining <= fadeOutDurationSeconds && fadeOutDurationSeconds > 0) {
                    val progress = remaining.toFloat() / fadeOutDurationSeconds.toFloat()
                    val targetVolume = (initialVolume * progress).toInt().coerceAtLeast(0)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                }

                updateNotification(remaining)
            }

            override fun onFinish() {
                onTimerFinished()
            }
        }.start()
    }

    private fun extendTimer(extraSeconds: Long) {
        val newDuration = _remainingSeconds.value + extraSeconds
        totalSeconds = newDuration
        runCountdown(newDuration)
    }

    private fun onTimerFinished() {
        // Stop audio playback globally (pauses Spotify, YouTube, etc.)
        pauseAllMedia()

        stopTimer()
    }

    private fun pauseAllMedia() {
        // 1. Enviar eventos de teclado multimedia (PAUSE y STOP) para compatibilidad universal (Spotify, YT Music, etc.)
        try {
            val pauseDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)
            val pauseUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE)
            audioManager.dispatchMediaKeyEvent(pauseDown)
            audioManager.dispatchMediaKeyEvent(pauseUp)

            val stopDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP)
            val stopUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_STOP)
            audioManager.dispatchMediaKeyEvent(stopDown)
            audioManager.dispatchMediaKeyEvent(stopUp)
        } catch (_: Exception) {}

        // 2. Control directo de sesiones activas (MediaSession) si el listener tiene acceso
        try {
            MediaNotificationListener.pauseAll(this)
        } catch (_: Exception) {}

        // 3. Audio Focus GAIN permanente: notifica pérdida de foco definitiva a reproductores externos
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            }
        } catch (_: Exception) {}

        // 4. Silenciar volumen de música a 0 para garantizar silencio total
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        } catch (_: Exception) {}
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        sensorManager?.unregisterListener(this)
        wakeLock?.let {
            if (it.isHeld) {
                try {
                    it.release()
                } catch (_: Exception) {}
            }
        }
        wakeLock = null
        _isRunning.value = false
        _remainingSeconds.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // Detección de agitación y quietud prolongada (Dormido)
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
        val now = System.currentTimeMillis()

        if (shakeEnabled && gForce > 2.5f) { // Shake detectado
            if (now - lastShakeTime > 3000) { // Throttle 3s
                lastShakeTime = now
                stillnessStartTime = now
                extendTimer(600) // +10 minutos
            }
        } else if (smartSleepEnabled) {
            // Si el movimiento es casi nulo (gForce cerca de 1.0G = quieto en cama)
            if (kotlin.math.abs(gForce - 1.0) < 0.15) {
                // Si lleva más de 5 minutos completamente quieto y faltan más de 2 minutos
                if (now - stillnessStartTime > 300_000L && _remainingSeconds.value > fadeOutDurationSeconds) {
                    _remainingSeconds.value = fadeOutDurationSeconds
                }
            } else {
                // Se movió
                stillnessStartTime = now
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun buildNotification(remainingSec: Long): Notification {
        val minutes = remainingSec / 60
        val seconds = remainingSec % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val extendIntent = PendingIntent.getService(
            this, 1,
            Intent(this, SleepTimerService::class.java).apply { action = ACTION_EXTEND },
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, SleepTimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gjallar Night — Apagado en $timeFormatted")
            .setContentText("Atenuando el sonido hacia el descanso...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_input_add, "+10 min", extendIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopIntent)
            .build()
    }

    private fun updateNotification(remainingSec: Long) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(remainingSec))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gjallar Sleep Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones activas de cuenta atrás y descanso nocturno"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

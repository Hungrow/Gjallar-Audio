package com.heimdall.gjallaraudio.feature.night

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
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
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.heimdall.gjallaraudio.MainActivity
import com.heimdall.gjallaraudio.R
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
        const val EXTRA_TURN_OFF_SCREEN = "EXTRA_TURN_OFF_SCREEN"

        private val _remainingSeconds = MutableStateFlow<Long>(0)
        val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _isFadingOut = MutableStateFlow(false)
        val isFadingOut: StateFlow<Boolean> = _isFadingOut.asStateFlow()
    }

    private var countDownTimer: CountDownTimer? = null
    private var totalSeconds: Long = 0
    private var shakeEnabled = true
    private var smartSleepEnabled = false
    private var turnOffScreen = true
    private var stillnessStartTime: Long = 0

    private lateinit var audioManager: AudioManager
    private var initialVolume: Int = 0
    private var isFadingPhase: Boolean = false

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
                shakeEnabled = intent.getBooleanExtra(EXTRA_SHAKE_TO_EXTEND, true)
                smartSleepEnabled = intent.getBooleanExtra(EXTRA_SMART_SLEEP_DETECTION, false)
                turnOffScreen = intent.getBooleanExtra(EXTRA_TURN_OFF_SCREEN, true)
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
        _isFadingOut.value = false
        isFadingPhase = false
        stillnessStartTime = System.currentTimeMillis()

        initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        // Adquirir WakeLock para evitar que el modo reposo / Doze de Android pause el temporizador con la pantalla apagada
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GjallarAudio:SleepTimerWakeLock").apply {
                setReferenceCounted(false)
                acquire((durationSec + 180) * 1000L)
            }
        } catch (_: Exception) {}

        if (shakeEnabled || smartSleepEnabled) {
            val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometer?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    buildNotification(durationSec),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification(durationSec))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                startForeground(NOTIFICATION_ID, buildNotification(durationSec))
            } catch (_: Exception) {}
        }

        runCountdown(durationSec)
    }

    private fun runCountdown(durationSec: Long) {
        countDownTimer?.cancel()
        isFadingPhase = false
        _isFadingOut.value = false

        countDownTimer = object : CountDownTimer(durationSec * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = millisUntilFinished / 1000
                _remainingSeconds.value = remaining
                updateNotification(remaining)
            }

            override fun onFinish() {
                // Al llegar a 0, comienza la atenuación de volumen virtual durante el siguiente minuto (60s)
                startVirtualFadeOut()
            }
        }.start()
    }

    private fun startVirtualFadeOut() {
        isFadingPhase = true
        _isFadingOut.value = true
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVol > 0) {
            initialVolume = currentVol
        }

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60_000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remaining = millisUntilFinished / 1000
                _remainingSeconds.value = remaining

                // Atenuación suave progresiva a lo largo de 60 segundos
                val progress = remaining.toFloat() / 60f
                val targetVolume = (initialVolume * progress).toInt().coerceAtLeast(0)
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                } catch (_: Exception) {}
                try {
                    AmbientSoundEngine.fadeVolume(progress)
                } catch (_: Exception) {}

                updateNotificationFading(remaining)
            }

            override fun onFinish() {
                onTimerFinished()
            }
        }.start()
    }

    private fun extendTimer(extraSeconds: Long) {
        // Si estaba en fase de atenuación, restaurar el volumen del usuario
        if (isFadingPhase && initialVolume > 0) {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, initialVolume, 0)
            } catch (_: Exception) {}
            try {
                AmbientSoundEngine.fadeVolume(1f)
            } catch (_: Exception) {}
        }
        val newDuration = _remainingSeconds.value + extraSeconds
        totalSeconds = newDuration
        runCountdown(newDuration)
    }

    private fun onTimerFinished() {
        // 1. Detener toda reproducción multimedia activa (Spotify, YouTube, ReVanced, podcasts)
        pauseAllMedia()

        // 2. Restaurar virtualmente el volumen del usuario intacto
        try {
            if (initialVolume > 0) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, initialVolume, 0)
            }
        } catch (_: Exception) {}

        // 3. Apagado de pantalla (comportamiento idéntico a Sleep Timer)
        if (turnOffScreen) {
            try {
                val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val admin = GjallarDeviceAdminReceiver.getComponentName(this)
                if (dpm.isAdminActive(admin)) {
                    dpm.lockNow()
                } else {
                    // Si aún no ha activado el administrador de dispositivos, minimizar a Home
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(homeIntent)
                }
            } catch (_: Exception) {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(homeIntent)
            }
        }

        // 4. Vibración de confirmación
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(300)
            }
        } catch (_: Exception) {}

        // 5. Detener el servicio
        stopTimer()
    }

    private fun pauseAllMedia() {
        // 1. Enviar eventos de teclado multimedia (PAUSE y STOP) para compatibilidad universal
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

        // 4. Detener sonidos ambientes nórdicos propios de Gjallarhorn
        try {
            AmbientSoundEngine.stopActiveSounds()
        } catch (_: Exception) {}
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        sensorManager?.unregisterListener(this)
        // Restaurar volumen original si se cancela a medio fade
        if (isFadingPhase && initialVolume > 0) {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, initialVolume, 0)
            } catch (_: Exception) {}
        }
        wakeLock?.let {
            if (it.isHeld) {
                try {
                    it.release()
                } catch (_: Exception) {}
            }
        }
        wakeLock = null
        _isRunning.value = false
        _isFadingOut.value = false
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
                // Si lleva más de 5 minutos completamente quieto y aún no está en fase de atenuación
                if (now - stillnessStartTime > 300_000L && !isFadingPhase && _remainingSeconds.value > 10L) {
                    startVirtualFadeOut()
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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_input_add, "+10 min", extendIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopIntent)
            .build()
    }

    private fun updateNotification(remainingSec: Long) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(remainingSec))
    }

    private fun updateNotificationFading(remainingFadeSec: Long) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val timeFormatted = "${remainingFadeSec}s"

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

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gjallar Night — Atenuación suave ($timeFormatted)")
            .setContentText("Atenuando el sonido hacia el descanso...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_input_add, "+10 min", extendIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopIntent)
            .build()

        manager.notify(NOTIFICATION_ID, notif)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Temporizador de Apagado Gjallar",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones activas de cuenta atrás y descanso nocturno"
                setShowBadge(true)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

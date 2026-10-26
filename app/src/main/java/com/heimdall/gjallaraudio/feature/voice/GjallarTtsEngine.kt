package com.heimdall.gjallaraudio.feature.voice

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.util.Locale

data class CuratedVoice(
    val id: String,
    val name: String,
    val subtitle: String,
    val isEnglish: Boolean = false,
    val voice: Voice?
)

data class NaturalVoiceProfile(
    val id: String,
    val name: String,
    val description: String,
    val isFemale: Boolean,
    val speed: Float,
    val voiceMatcher: (Voice) -> Boolean
)

class GjallarTtsEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    var isInitialized = false
        private set

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _curatedVoices = MutableStateFlow<List<CuratedVoice>>(emptyList())
    val curatedVoices: StateFlow<List<CuratedVoice>> = _curatedVoices.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices.asStateFlow()

    // Voz activa en caché — solo se aplica al motor cuando realmente cambia
    private var activeVoice: Voice? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val spanishLocale = Locale("es", "ES")
            val result = tts?.setLanguage(spanishLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }

            // Descubrir todas las voces instaladas
            val allVoices = tts?.voices?.toList() ?: emptyList()

            val spanishVoices = allVoices.filter {
                it.locale.language.equals("es", ignoreCase = true) &&
                !it.features.contains("notInstalled")
            }.sortedBy { it.name }

            Log.d("GjallarTTS", "Voces ES disponibles (instaladas): ${spanishVoices.map { it.name }}")

            // Lista de candidatos femeninos y masculinos conocidos de Google TTS (España)
            // Femeninas confirmadas: eec, eea, eee (sufijos pares / vocales)
            // Masculinas confirmadas: eef, eed, eeb (sufijos consonantes / distintos)
            val femalePatterns = listOf("es-es-x-eec-local", "es-es-x-eea-local", "es-es-x-eee-local",
                                        "es-us-x-esf-local", "es-us-x-esd-local")
            val malePatterns   = listOf("es-es-x-eef-local", "es-es-x-eed-local", "es-es-x-eeb-local",
                                        "es-us-x-esc-local", "es-us-x-sfb-local")

            // 1. Elena — primera voz femenina disponible
            val voiceElena = femalePatterns.firstNotNullOfOrNull { pattern ->
                spanishVoices.find { it.name.equals(pattern, ignoreCase = true) }
            } ?: spanishVoices.firstOrNull()

            // 2. Javier — primera voz masculina disponible que NO sea la misma que Elena
            val voiceJavier = malePatterns.firstNotNullOfOrNull { pattern ->
                spanishVoices.find { it.name.equals(pattern, ignoreCase = true) }
            } ?: spanishVoices.firstOrNull { it.name != voiceElena?.name }

            // 3. Oliver (Voz nativa para textos en inglés: en-US iom)
            val voiceOliver = allVoices.find { it.name.contains("en-us-x-iom-local", ignoreCase = true) }
                ?: allVoices.find { it.name.contains("iom", ignoreCase = true) }
                ?: allVoices.find { it.name.contains("en-us", ignoreCase = true) && !it.features.contains("notInstalled") }
                ?: allVoices.find { it.locale.language.equals("en", ignoreCase = true) }

            val list = mutableListOf<CuratedVoice>()
            if (voiceElena != null) {
                list.add(
                    CuratedVoice(
                        id = "elena",
                        name = "Elena",
                        subtitle = "Español • Femenina (España)",
                        isEnglish = false,
                        voice = voiceElena
                    )
                )
            }
            if (voiceJavier != null) {
                list.add(
                    CuratedVoice(
                        id = "javier",
                        name = "Javier",
                        subtitle = "Español • Masculina (España)",
                        isEnglish = false,
                        voice = voiceJavier
                    )
                )
            }
            if (voiceOliver != null) {
                list.add(
                    CuratedVoice(
                        id = "oliver",
                        name = "Oliver",
                        subtitle = "Inglés • Voz nativa en inglés",
                        isEnglish = true,
                        voice = voiceOliver
                    )
                )
            }

            _curatedVoices.value = list
            _availableVoices.value = list.mapNotNull { it.voice }

            Log.d("GjallarTTS", "Voces seleccionadas activas: ${list.map { "${it.name} (${it.voice?.name})" }}")

            // Pre-aplicar la primera voz al motor para evitar pop en la primera reproducción
            list.firstOrNull()?.voice?.let {
                tts?.voice = it
                activeVoice = it
            }

            isInitialized = true
            setupProgressListener()
        } else {
            Log.e("GjallarTTS", "Error al inicializar TTS: $status")
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId?.startsWith("export_") == false) {
                    _isSpeaking.value = true
                }
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                _isExporting.value = false
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _isExporting.value = false
            }
        })
    }

    fun speak(
        text: String,
        speed: Float = 1.0f,
        selectedVoice: Voice? = null
    ) {
        if (!isInitialized || text.isBlank()) return

        // Solo cambiar la voz en el motor si realmente es diferente a la activa
        // Cambiar voz en cada llamada genera un pop de reinicialización del pipeline de audio
        if (selectedVoice != null && selectedVoice.name != activeVoice?.name) {
            tts?.voice = selectedVoice
            activeVoice = selectedVoice
        }
        tts?.setPitch(1.0f)
        tts?.setSpeechRate(speed.coerceIn(0.7f, 1.5f))

        val utteranceId = "live_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        _isSpeaking.value = true
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun exportToAudio(
        text: String,
        speed: Float = 1.0f,
        selectedVoice: Voice? = null,
        title: String = "Gjallar_Audio_${System.currentTimeMillis()}",
        onFinished: (Boolean, String) -> Unit
    ) {
        if (!isInitialized || text.isBlank()) {
            onFinished(false, "El motor TTS no está listo.")
            return
        }

        _isExporting.value = true

        // Detener cualquier reproducción activa limpiamente antes de exportar
        tts?.stop()

        // Aplicar configuración (solo cambiar voz si es diferente — evita el pop de reinit)
        if (selectedVoice != null && selectedVoice.name != activeVoice?.name) {
            tts?.voice = selectedVoice
            activeVoice = selectedVoice
        }
        tts?.setPitch(1.0f)
        tts?.setSpeechRate(speed.coerceIn(0.7f, 1.5f))

        val tempFile = File(context.cacheDir, "$title.wav")
        val utteranceId = "export_${System.currentTimeMillis()}"

        // Params con utteranceId correcto (obligatorio para que onDone se dispare)
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    _isExporting.value = false
                    val savedUri = saveToPublicMusic(tempFile, title)
                    onFinished(true, savedUri ?: tempFile.absolutePath)
                    setupProgressListener()
                }
            }

            override fun onError(id: String?) {
                if (id == utteranceId) {
                    _isExporting.value = false
                    onFinished(false, "Error durante la síntesis de audio.")
                    setupProgressListener()
                }
            }
        })

        tts?.synthesizeToFile(text, params, tempFile, utteranceId)
    }

    private fun saveToPublicMusic(tempFile: File, title: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.wav")
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/GjallarAudio")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it).use { output ->
                        FileInputStream(tempFile).use { input ->
                            input.copyTo(output!!)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    "Música/GjallarAudio/$title.wav"
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "GjallarAudio")
                dir.mkdirs()
                val destFile = File(dir, "$title.wav")
                tempFile.copyTo(destFile, overwrite = true)
                destFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e("GjallarTTS", "Error al guardar audio", e)
            null
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}

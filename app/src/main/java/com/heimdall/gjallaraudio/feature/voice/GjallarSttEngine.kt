package com.heimdall.gjallaraudio.feature.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

class GjallarSttEngine(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        if (!isAvailable()) {
            _error.value = "El reconocimiento de voz no está disponible en este dispositivo."
            return
        }

        stopListening()

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _error.value = null
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(errorCode: Int) {
                    _isListening.value = false
                    val msg = when (errorCode) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No se detectó voz clara."
                        SpeechRecognizer.ERROR_NETWORK -> "Error de red/servicio."
                        SpeechRecognizer.ERROR_AUDIO -> "Error al capturar audio del micrófono."
                        else -> "Código de estado: $errorCode"
                    }
                    if (errorCode != SpeechRecognizer.ERROR_NO_MATCH) {
                        _error.value = msg
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val newText = matches[0]
                        val current = _transcribedText.value
                        _transcribedText.value = if (current.isBlank()) newText else "$current $newText"
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        // Opcional para vista previa continua
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isListening.value = false
            _error.value = e.message
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isListening.value = false
    }

    fun appendText(text: String) {
        val current = _transcribedText.value
        _transcribedText.value = if (current.isBlank()) text else "$current $text"
    }

    fun clear() {
        _transcribedText.value = ""
        _error.value = null
    }

    fun saveAsTextFile(fileName: String): File? {
        val text = _transcribedText.value
        if (text.isBlank()) return null
        return try {
            val dir = File(context.filesDir, "transcriptions").apply { mkdirs() }
            val clean = fileName.trim().ifEmpty { "transcripcion_${System.currentTimeMillis()}" }
            val file = File(dir, "$clean.txt")
            file.writeText(text)
            file
        } catch (e: Exception) {
            Log.e("GjallarStt", "Error saving txt", e)
            null
        }
    }
}

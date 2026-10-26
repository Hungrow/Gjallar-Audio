package com.heimdall.gjallaraudio.feature.voice

import android.content.Context
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GjallarTranslator(private val context: Context) {

    private val modelManager = RemoteModelManager.getInstance()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus: StateFlow<String?> = _downloadStatus.asStateFlow()

    private val _downloadedLanguages = MutableStateFlow<Set<String>>(emptySet())
    val downloadedLanguages: StateFlow<Set<String>> = _downloadedLanguages.asStateFlow()

    private var activeTranslator: Translator? = null
    private var activeSourceLang: String? = null
    private var activeTargetLang: String? = null

    init {
        refreshDownloadedModels()
    }

    fun refreshDownloadedModels(onFinished: (() -> Unit)? = null) {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                val langs = models.map { it.language }.toSet()
                _downloadedLanguages.value = langs
                Log.d("GjallarTranslator", "Modelos de traducción descargados: $langs")
                onFinished?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e("GjallarTranslator", "Error consultando modelos: ${e.message}")
                onFinished?.invoke()
            }
    }

    fun isModelDownloaded(langCode: String): Boolean {
        return _downloadedLanguages.value.contains(langCode)
    }

    fun downloadModel(langCode: String, onFinished: (Boolean, String?) -> Unit) {
        val model = TranslateRemoteModel.Builder(langCode).build()
        val conditions = DownloadConditions.Builder().build()
        _isDownloading.value = true
        _downloadStatus.value = "Descargando diccionario ($langCode)..."

        modelManager.download(model, conditions)
            .addOnSuccessListener {
                _isDownloading.value = false
                _downloadStatus.value = null
                refreshDownloadedModels()
                onFinished(true, null)
            }
            .addOnFailureListener { e ->
                _isDownloading.value = false
                _downloadStatus.value = null
                onFinished(false, e.localizedMessage ?: "Error al descargar el modelo")
            }
    }

    fun deleteModel(langCode: String, onFinished: (Boolean) -> Unit) {
        val model = TranslateRemoteModel.Builder(langCode).build()
        modelManager.deleteDownloadedModel(model)
            .addOnSuccessListener {
                refreshDownloadedModels()
                onFinished(true)
            }
            .addOnFailureListener {
                onFinished(false)
            }
    }

    fun translate(
        text: String,
        sourceLang: String = TranslateLanguage.SPANISH,
        targetLang: String = TranslateLanguage.ENGLISH,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (text.isBlank()) {
            onError("El texto está vacío")
            return
        }

        _isTranslating.value = true

        // Reusar o crear translator
        if (activeTranslator == null || activeSourceLang != sourceLang || activeTargetLang != targetLang) {
            activeTranslator?.close()
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            activeTranslator = Translation.getClient(options)
            activeSourceLang = sourceLang
            activeTargetLang = targetLang
        }

        val translator = activeTranslator ?: run {
            _isTranslating.value = false
            onError("No se pudo iniciar el traductor")
            return
        }

        val conditions = DownloadConditions.Builder().build()
        _downloadStatus.value = "Verificando diccionarios sin conexión..."

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                _downloadStatus.value = null
                refreshDownloadedModels()
                translator.translate(text)
                    .addOnSuccessListener { result ->
                        _isTranslating.value = false
                        onSuccess(result)
                    }
                    .addOnFailureListener { e ->
                        _isTranslating.value = false
                        onError(e.localizedMessage ?: "Error en la traducción")
                    }
            }
            .addOnFailureListener { e ->
                _isTranslating.value = false
                _downloadStatus.value = null
                onError("Descarga de diccionario requerida: ${e.localizedMessage}")
            }
    }

    fun close() {
        activeTranslator?.close()
        activeTranslator = null
    }
}

package com.heimdall.gjallaraudio.feature.forge

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

enum class ConversionStatus {
    WAITING,
    PROCESSING,
    COMPLETED,
    ERROR
}

data class VideoConversionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val fileName: String,
    val durationFormatted: String,
    val outputName: String,
    val targetFormat: String = "MP3",
    val status: ConversionStatus = ConversionStatus.WAITING,
    val progress: Float = 0f,
    val errorMessage: String? = null
)

data class ForgedAudioHistory(
    val title: String,
    val format: String,
    val file: File,
    val shareUri: Uri,
    val durationFormatted: String,
    val publicPath: String
)

class AudioExtractorEngine(private val context: Context) {

    private val _isBatchRunning = MutableStateFlow(false)
    val isBatchRunning: StateFlow<Boolean> = _isBatchRunning.asStateFlow()

    fun getVideoMetadata(uri: Uri): Pair<String, String> {
        var duration = "00:00"
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val sec = (durationMs / 1000) % 60
            val min = (durationMs / (1000 * 60))
            duration = String.format("%02d:%02d", min, sec)
        } catch (e: Exception) {
            Log.e("AudioExtractor", "Error metadata", e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        var fileName = "video_${System.currentTimeMillis()}"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return Pair(fileName, duration)
    }

    /**
     * Extrae y codifica el audio con el nombre personalizado, formato elegido,
     * normalización de volumen inteligente EBU R128 (-14 LUFS) y corte de silencios opcional.
     */
    suspend fun extractAudio(
        item: VideoConversionItem,
        customName: String,
        targetFormat: String,
        normalizeLoudness: Boolean = true,
        trimSilence: Boolean = false
    ): Result<ForgedAudioHistory> = withContext(Dispatchers.IO) {
        val cleanName = customName.trim().ifEmpty { item.fileName.substringBeforeLast(".") }
            .replace(Regex("[^a-zA-Z0-9._ -]"), "_")

        val ext = when {
            targetFormat.contains("WAV", ignoreCase = true) -> "wav"
            targetFormat.contains("M4A", ignoreCase = true) || targetFormat.contains("AAC", ignoreCase = true) -> "m4a"
            else -> "mp3"
        }

        val tempInputFile = File(context.cacheDir, "input_${System.currentTimeMillis()}.tmp")
        val audioOutputDir = File(context.filesDir, "forged_audio").apply { mkdirs() }
        val finalOutputFile = File(audioOutputDir, "$cleanName.$ext")

        try {
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                FileOutputStream(tempInputFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("No se pudo leer el archivo de video."))

            val filters = mutableListOf<String>()
            if (trimSilence) {
                filters.add("silenceremove=start_periods=1:start_threshold=-45dB:detection=peak,areverse,silenceremove=start_periods=1:start_threshold=-45dB:detection=peak,areverse")
            }
            if (normalizeLoudness) {
                filters.add("loudnorm=I=-14:LRA=11:TP=-1.5")
            }
            val afOption = if (filters.isNotEmpty()) "-af \"${filters.joinToString(",")}\"" else ""

            val ffmpegCommand = when (ext) {
                "mp3" -> "-y -i \"${tempInputFile.absolutePath}\" -vn $afOption -c:a libmp3lame -b:a 320k -ar 44100 \"${finalOutputFile.absolutePath}\""
                "wav" -> "-y -i \"${tempInputFile.absolutePath}\" -vn $afOption -c:a pcm_s16le -ar 44100 \"${finalOutputFile.absolutePath}\""
                "m4a" -> "-y -i \"${tempInputFile.absolutePath}\" -vn $afOption -c:a aac -b:a 256k \"${finalOutputFile.absolutePath}\""
                else -> "-y -i \"${tempInputFile.absolutePath}\" -vn $afOption -c:a libmp3lame -b:a 320k \"${finalOutputFile.absolutePath}\""
            }

            val session = FFmpegKit.execute(ffmpegCommand)

            tempInputFile.delete()

            if (ReturnCode.isSuccess(session.returnCode)) {
                // Guardar también una copia en Música pública
                val mimeType = when (ext) {
                    "mp3" -> "audio/mpeg"
                    "wav" -> "audio/wav"
                    "m4a" -> "audio/mp4"
                    else -> "audio/mpeg"
                }
                val publicLocation = saveToPublicMusic(finalOutputFile, cleanName, ext, mimeType)

                // Crear URI segura mediante FileProvider para compartir
                val shareUri = try {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        finalOutputFile
                    )
                } catch (e: Exception) {
                    Log.e("AudioExtractor", "FileProvider error", e)
                    Uri.fromFile(finalOutputFile)
                }

                Result.success(
                    ForgedAudioHistory(
                        title = "$cleanName.$ext",
                        format = ext.uppercase(),
                        file = finalOutputFile,
                        shareUri = shareUri,
                        durationFormatted = item.durationFormatted,
                        publicPath = publicLocation ?: finalOutputFile.absolutePath
                    )
                )
            } else {
                val errorLog = session.failStackTrace ?: "Error en códec (código ${session.returnCode})"
                Result.failure(Exception("Fallo en conversión: $errorLog"))
            }

        } catch (e: Exception) {
            tempInputFile.delete()
            Result.failure(e)
        }
    }

    private fun saveToPublicMusic(tempFile: File, title: String, ext: String, mime: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.$ext")
                    put(MediaStore.Audio.Media.MIME_TYPE, mime)
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/GjallarForge")
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
                    "Música/GjallarForge/$title.$ext"
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "GjallarForge")
                dir.mkdirs()
                val destFile = File(dir, "$title.$ext")
                tempFile.copyTo(destFile, overwrite = true)
                destFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e("AudioExtractor", "Error guardando en Música", e)
            null
        }
    }
}

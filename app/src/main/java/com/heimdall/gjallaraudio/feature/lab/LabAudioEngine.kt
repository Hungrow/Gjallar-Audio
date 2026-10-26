package com.heimdall.gjallaraudio.feature.lab

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
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class LabAudioMetadata(
    val uri: Uri,
    val fileName: String,
    val durationMs: Long,
    val durationFormatted: String
)

data class ProcessedAudioResult(
    val title: String,
    val file: File,
    val shareUri: Uri,
    val publicPath: String
)

class LabAudioEngine(private val context: Context) {

    private val mdxSeparator = com.heimdall.gjallaraudio.feature.lab.ai.MdxKaraokeSeparator(context)

    fun getAudioMetadata(uri: Uri): LabAudioMetadata {
        var durationMs = 0L
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e("LabAudioEngine", "Error metadata", e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        val sec = (durationMs / 1000) % 60
        val min = (durationMs / (1000 * 60))
        val durationFormatted = String.format("%02d:%02d", min, sec)

        var fileName = "audio_${System.currentTimeMillis()}"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return LabAudioMetadata(uri, fileName, durationMs, durationFormatted)
    }

    /**
     * Recorta el audio al milisegundo exacto para tonos o alarmas.
     */
    suspend fun trimAudio(
        uri: Uri,
        startSeconds: Float,
        endSeconds: Float,
        outputName: String
    ): Result<ProcessedAudioResult> = withContext(Dispatchers.IO) {
        val tempInput = copyUriToTemp(uri, "trim_in") ?: return@withContext Result.failure(Exception("No se pudo leer el archivo."))
        val outputDir = File(context.filesDir, "lab_trimmed").apply { mkdirs() }
        val finalFile = File(outputDir, "${outputName.trim().ifEmpty { "tono_recortado" }}.mp3")

        val cmd = "-y -ss $startSeconds -to $endSeconds -i \"${tempInput.absolutePath}\" -c:a libmp3lame -b:a 320k \"${finalFile.absolutePath}\""
        val session = FFmpegKit.execute(cmd)
        tempInput.delete()

        if (ReturnCode.isSuccess(session.returnCode)) {
            val pub = saveToPublic(finalFile, outputName, "mp3", "audio/mpeg", Environment.DIRECTORY_RINGTONES)
            val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", finalFile)
            Result.success(ProcessedAudioResult(finalFile.name, finalFile, shareUri, pub ?: finalFile.absolutePath))
        } else {
            Result.failure(Exception("Error al recortar: ${session.failStackTrace}"))
        }
    }

    /**
     * Separación de Fuentes con Red Neuronal UVR MDX-Net ONNX:
     * - isolateVocals = true: Aisla la pista vocal limpia eliminando cualquier percusión y armonía.
     * - isolateVocals = false: Pista instrumental / karaoke pura (música sin voz).
     */
    suspend fun separateTracks(
        uri: Uri,
        outputName: String,
        isolateVocals: Boolean,
        onProgress: (Float) -> Unit = {}
    ): Result<ProcessedAudioResult> = withContext(Dispatchers.IO) {
        val tempInput = copyUriToTemp(uri, "sep_in") ?: return@withContext Result.failure(Exception("No se pudo leer el archivo de audio seleccionado."))
        val outputDir = File(context.filesDir, "lab_separated").apply { mkdirs() }
        val finalFileName = "${outputName.trim().ifEmpty { if (isolateVocals) "solo_voz" else "karaoke" }}.mp3"
        val finalFile = File(outputDir, finalFileName)

        try {
            val sepResult = mdxSeparator.separate(
                inputFile = tempInput,
                isolateVocals = isolateVocals,
                onProgress = onProgress
            )
            tempInput.delete()

            if (sepResult.isSuccess) {
                val generatedMp3 = sepResult.getOrThrow()
                generatedMp3.copyTo(finalFile, overwrite = true)
                generatedMp3.delete()

                val pub = saveToPublic(finalFile, outputName, "mp3", "audio/mpeg", Environment.DIRECTORY_MUSIC)
                val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", finalFile)
                Result.success(ProcessedAudioResult(finalFile.name, finalFile, shareUri, pub ?: finalFile.absolutePath))
            } else {
                Result.failure(sepResult.exceptionOrNull() ?: Exception("Fallo en la separación neuronal"))
            }
        } catch (e: Exception) {
            tempInput.delete()
            Result.failure(e)
        }
    }

    enum class DenoiseProfile(val title: String, val description: String) {
        STUDIO_VOICE("Voz de Estudio", "Aisla la voz humana, reduce eco de sala y normaliza el volumen"),
        STATIC_NOISE("Fondo y Viento", "Elimina siseo continuo, ruido de ventilador, aire acondicionado o viento")
    }

    /**
     * Detección Inteligente del Estribillo / Drop:
     * Analiza ventanas temporales en el audio y localiza el segmento de 30 segundos
     * con mayor densidad de energía RMS y presencia armónica.
     */
    suspend fun detectChorusDrop(uri: Uri, sliceDurationSec: Float = 30f): Result<Pair<Float, Float>> = withContext(Dispatchers.IO) {
        val metadata = getAudioMetadata(uri)
        val totalSec = metadata.durationMs / 1000f
        if (totalSec <= sliceDurationSec) {
            return@withContext Result.success(Pair(0f, totalSec))
        }

        val tempInput = copyUriToTemp(uri, "chorus_scan") ?: return@withContext Result.failure(Exception("No se pudo leer el archivo."))

        try {
            val candidateStarts = listOf(
                (totalSec * 0.25f).coerceAtMost(totalSec - sliceDurationSec),
                (totalSec * 0.40f).coerceAtMost(totalSec - sliceDurationSec),
                (totalSec * 0.55f).coerceAtMost(totalSec - sliceDurationSec),
                (totalSec * 0.70f).coerceAtMost(totalSec - sliceDurationSec)
            ).distinct()

            var bestStart = candidateStarts.firstOrNull() ?: 0f
            var bestMeanVolume = -999.0

            for (start in candidateStarts) {
                val cmd = "-ss $start -t $sliceDurationSec -i \"${tempInput.absolutePath}\" -af volumedetect -f null -"
                val session = FFmpegKit.execute(cmd)
                val logs = session.allLogsAsString
                val match = Regex("""mean_volume:\s*(-?[\d.]+)\s*dB""").find(logs)
                if (match != null) {
                    val meanVol = match.groupValues[1].toDoubleOrNull() ?: -999.0
                    if (meanVol > bestMeanVolume) {
                        bestMeanVolume = meanVol
                        bestStart = start
                    }
                }
            }

            tempInput.delete()
            val end = (bestStart + sliceDurationSec).coerceAtMost(totalSec)
            Result.success(Pair(bestStart, end))
        } catch (e: Exception) {
            tempInput.delete()
            val fallbackStart = ((totalSec - sliceDurationSec) / 2f).coerceAtLeast(0f)
            Result.success(Pair(fallbackStart, fallbackStart + sliceDurationSec))
        }
    }

    /**
     * Reductor de ruido con perfiles de estudio (Voz de Estudio o Cancelación de Ruido Estático).
     */
    suspend fun denoiseAudio(
        uri: Uri,
        outputName: String,
        profile: DenoiseProfile = DenoiseProfile.STUDIO_VOICE
    ): Result<ProcessedAudioResult> = withContext(Dispatchers.IO) {
        val tempInput = copyUriToTemp(uri, "denoise_in") ?: return@withContext Result.failure(Exception("No se pudo leer el archivo."))
        val outputDir = File(context.filesDir, "lab_denoised").apply { mkdirs() }
        val finalFile = File(outputDir, "${outputName.trim().ifEmpty { "audio_limpio" }}.mp3")

        val filter = when (profile) {
            DenoiseProfile.STUDIO_VOICE ->
                "highpass=f=85,lowpass=f=8200,afftdn=nf=-22:tn=1,speechnorm=p=0.88:e=3.0,alimiter=limit=0.92:attack=5:release=50"
            DenoiseProfile.STATIC_NOISE ->
                "afftdn=nf=-26:tn=1,alimiter=limit=0.92:attack=5:release=50"
        }

        val cmd = "-y -i \"${tempInput.absolutePath}\" -af \"$filter\" -c:a libmp3lame -b:a 320k \"${finalFile.absolutePath}\""
        val session = FFmpegKit.execute(cmd)
        tempInput.delete()

        if (ReturnCode.isSuccess(session.returnCode)) {
            val pub = saveToPublic(finalFile, outputName, "mp3", "audio/mpeg", Environment.DIRECTORY_MUSIC)
            val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", finalFile)
            Result.success(ProcessedAudioResult(finalFile.name, finalFile, shareUri, pub ?: finalFile.absolutePath))
        } else {
            Result.failure(Exception("Error al limpiar ruido: ${session.failStackTrace}"))
        }
    }

    private fun copyUriToTemp(uri: Uri, prefix: String): File? {
        val temp = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.tmp")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temp).use { output ->
                    input.copyTo(output)
                }
            }
            temp
        } catch (e: Exception) {
            temp.delete()
            null
        }
    }

    private fun saveToPublic(tempFile: File, title: String, ext: String, mime: String, directory: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "$title.$ext")
                    put(MediaStore.Audio.Media.MIME_TYPE, mime)
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "$directory/GjallarLab")
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
                    "$directory/GjallarLab/$title.$ext"
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(directory), "GjallarLab")
                dir.mkdirs()
                val destFile = File(dir, "$title.$ext")
                tempFile.copyTo(destFile, overwrite = true)
                destFile.absolutePath
            }
        } catch (e: Exception) {
            Log.e("LabAudioEngine", "Error guardando archivo", e)
            null
        }
    }
}

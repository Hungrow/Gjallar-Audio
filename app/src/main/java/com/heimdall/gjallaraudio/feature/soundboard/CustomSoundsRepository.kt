package com.heimdall.gjallaraudio.feature.soundboard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object CustomSoundsRepository {

    private const val JSON_FILE = "gjallar_custom_sounds.json"
    private const val FOLDER_NAME = "custom_sounds"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Memoria caché para evitar lecturas de disco innecesarias durante la navegación
    @Volatile
    private var memoryCache: List<CanterosSound>? = null

    fun getCustomSounds(context: Context, forceRefresh: Boolean = false): List<CanterosSound> {
        if (!forceRefresh && memoryCache != null) {
            return memoryCache!!
        }

        val file = File(context.filesDir, JSON_FILE)
        if (!file.exists()) {
            memoryCache = emptyList()
            return emptyList()
        }

        return try {
            val jsonStr = file.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<CanterosSound>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val filePath = obj.getString("filePath")
                if (File(filePath).exists()) {
                    list.add(
                        CanterosSound(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            character = obj.optString("character", "Descargas"),
                            assetPath = filePath,
                            quote = obj.optString("quote", "Añadido desde la red")
                        )
                    )
                }
            }
            memoryCache = list
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun downloadAndAddSound(
        context: Context,
        name: String,
        soundUrl: String
    ): Result<CanterosSound> = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(context.filesDir, FOLDER_NAME).apply { if (!exists()) mkdirs() }
            val cleanName = name.replace(Regex("[^a-zA-Z0-9áéíóúñÁÉÍÓÚÑ_]"), "_").take(40)
            val soundFile = File(targetDir, "${cleanName}_${System.currentTimeMillis()}.mp3")

            val request = Request.Builder().url(soundUrl).header("User-Agent", "GjallarAudio").get().build()
            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Respuesta vacía"))

            body.byteStream().use { input ->
                FileOutputStream(soundFile).use { output ->
                    input.copyTo(output)
                }
            }

            val newSound = CanterosSound(
                id = "custom_${System.currentTimeMillis()}",
                title = name.trim(),
                character = "Descargas",
                assetPath = soundFile.absolutePath,
                quote = "Añadido desde MyInstants"
            )

            // Guardar en JSON
            val existing = getCustomSounds(context).toMutableList()
            existing.add(0, newSound)
            saveAll(context, existing)

            Result.success(newSound)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteSound(context: Context, soundId: String) {
        val existing = getCustomSounds(context).toMutableList()
        val toDelete = existing.find { it.id == soundId }
        if (toDelete != null) {
            try {
                val f = File(toDelete.assetPath)
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
            existing.remove(toDelete)
            saveAll(context, existing)
        }
    }

    private fun saveAll(context: Context, list: List<CanterosSound>) {
        memoryCache = list
        try {
            val jsonArray = JSONArray()
            list.forEach { s ->
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("character", s.character)
                    put("filePath", s.assetPath)
                    put("quote", s.quote)
                }
                jsonArray.put(obj)
            }
            File(context.filesDir, JSON_FILE).writeText(jsonArray.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

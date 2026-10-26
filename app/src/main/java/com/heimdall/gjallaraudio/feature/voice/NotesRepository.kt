package com.heimdall.gjallaraudio.feature.voice

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object NotesRepository {
    private const val FILE_NAME = "gjallar_notes.json"

    fun loadNotes(context: Context): List<StickyNote> {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) {
                val initialNotes = listOf(
                    StickyNote(
                        title = "Bienvenido a Gjallar Notas",
                        content = "Escribe recordatorios y notas rápidas. Cada diseño representa una esencia nórdica.",
                        theme = NordicNoteTheme.AMBER_SUN,
                        timestamp = "Hoy, 10:00"
                    )
                )
                saveNotes(context, initialNotes)
                return initialNotes
            }

            val jsonString = file.readText(Charsets.UTF_8)
            if (jsonString.isBlank()) return emptyList()

            val jsonArray = JSONArray(jsonString)
            val result = mutableListOf<StickyNote>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id")
                val title = obj.optString("title")
                val content = obj.optString("content")
                val themeId = obj.optString("themeId")
                val timestamp = obj.optString("timestamp")

                val theme = NordicNoteTheme.values().find { it.id == themeId } ?: NordicNoteTheme.AMBER_SUN

                result.add(
                    StickyNote(
                        id = id,
                        title = title,
                        content = content,
                        theme = theme,
                        timestamp = timestamp
                    )
                )
            }
            result
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error cargando notas", e)
            emptyList()
        }
    }

    fun saveNotes(context: Context, notes: List<StickyNote>) {
        try {
            val jsonArray = JSONArray()
            for (note in notes) {
                val obj = JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("content", note.content)
                    put("themeId", note.theme.id)
                    put("timestamp", note.timestamp)
                }
                jsonArray.put(obj)
            }
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(jsonArray.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("NotesRepository", "Error guardando notas", e)
        }
    }
}

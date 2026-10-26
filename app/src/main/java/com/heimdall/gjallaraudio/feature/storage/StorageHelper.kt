package com.heimdall.gjallaraudio.feature.storage

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.widget.Toast
import java.io.File

object StorageHelper {

    val publicMusicDirPath: String
        get() = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath}/GjallarAudio"

    val displayMusicPath: String
        get() = "Almacenamiento interno > Music > GjallarAudio (y GjallarForge / GjallarLab)"

    fun copyPathToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Ruta GjallarAudio", publicMusicDirPath)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Ruta copiada al portapapeles: $publicMusicDirPath", Toast.LENGTH_SHORT).show()
    }

    fun openFileManager(context: Context) {
        // Intentar abrir el explorador de archivos en la carpeta de Música
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val gjallarDir = File(musicDir, "GjallarAudio")
        if (!gjallarDir.exists()) {
            gjallarDir.mkdirs()
        }

        // Intento 1: DocumentsContract para Android 8+ (Abre el explorador de archivos nativo Files/Archivos)
        try {
            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Music%2FGjallarAudio")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "vnd.android.document/directory")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {}

        // Intento 2: Abrir carpeta Music general mediante DocumentsContract
        try {
            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Music")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "vnd.android.document/directory")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {}

        // Intento 3: Intent genérico de gestor de archivos
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Abrir explorador de archivos"))
            return
        } catch (_: Exception) {}

        // Fallback: copiar ruta y avisar
        copyPathToClipboard(context)
        Toast.makeText(context, "Ruta copiada: Abre tu explorador de archivos y ve a Music/GjallarAudio", Toast.LENGTH_LONG).show()
    }

    data class GeneratedFileInfo(
        val name: String,
        val sizeFormatted: String,
        val file: File,
        val category: String
    )

    fun listGeneratedFiles(context: Context): List<GeneratedFileInfo> {
        val list = mutableListOf<GeneratedFileInfo>()

        // Carpetas internas
        val forgedDir = File(context.filesDir, "forged_audio")
        val trimmedDir = File(context.filesDir, "lab_trimmed")
        val separatedDir = File(context.filesDir, "lab_separated")
        val denoisedDir = File(context.filesDir, "lab_denoised")

        fun scan(dir: File, category: String) {
            if (dir.exists()) {
                dir.listFiles()?.filter { it.isFile }?.forEach { f ->
                    val sizeKb = f.length() / 1024f
                    val sizeFormatted = if (sizeKb > 1024) String.format("%.1f MB", sizeKb / 1024f) else String.format("%.0f KB", sizeKb)
                    list.add(GeneratedFileInfo(f.name, sizeFormatted, f, category))
                }
            }
        }

        scan(forgedDir, "Forge (Audio)")
        scan(trimmedDir, "Lab (Recorte)")
        scan(separatedDir, "Lab (Separación)")
        scan(denoisedDir, "Lab (Denoise)")

        // Carpetas públicas
        val pubForge = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "GjallarForge")
        val pubLab = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "GjallarLab")
        val pubAudio = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "GjallarAudio")

        scan(pubForge, "Música (Pública)")
        scan(pubLab, "Música (Pública)")
        scan(pubAudio, "Música (Pública)")

        return list.sortedByDescending { it.file.lastModified() }
    }
}

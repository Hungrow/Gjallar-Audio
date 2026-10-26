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
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val gjallarDir = File(musicDir, "GjallarAudio")
        if (!gjallarDir.exists()) {
            gjallarDir.mkdirs()
        }

        // Forzar la ventana del sistema "Abrir con" (Chooser) para permitir
        // al usuario elegir qué explorador de archivos desea usar
        val primaryUri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Music%2FGjallarAudio")
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(primaryUri, "vnd.android.document/directory")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val getContentIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        try {
            val chooser = Intent.createChooser(viewIntent, "Elige explorador de archivos").apply {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(getContentIntent))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            return
        } catch (_: Exception) {}

        try {
            val chooser = Intent.createChooser(getContentIntent, "Elige explorador de archivos").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            return
        } catch (_: Exception) {}

        copyPathToClipboard(context)
        Toast.makeText(context, "Ruta copiada al portapapeles: $publicMusicDirPath", Toast.LENGTH_LONG).show()
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

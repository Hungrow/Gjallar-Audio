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

    fun openSystemFiles(context: Context) {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val gjallarDir = File(musicDir, "GjallarAudio")
        if (!gjallarDir.exists()) gjallarDir.mkdirs()

        try {
            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Music%2FGjallarAudio")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "vnd.android.document/directory")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No se pudo abrir el explorador del sistema", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWithChooser(context: Context) {
        val intents = mutableListOf<Intent>()

        val knownPackages = listOf(
            "com.sec.android.app.myfiles",
            "com.mi.android.globalFileexplorer",
            "com.google.android.apps.nbu.files",
            "pl.solidexplorer2",
            "com.cxinventor.file.explorer",
            "com.ghisler.android.TotalCommander"
        )

        for (pkg in knownPackages) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                intents.add(launchIntent)
            }
        }

        val baseIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        val chooser = Intent.createChooser(baseIntent, "¿Con qué aplicación deseas abrir?").apply {
            if (intents.isNotEmpty()) {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            copyPathToClipboard(context)
        }
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

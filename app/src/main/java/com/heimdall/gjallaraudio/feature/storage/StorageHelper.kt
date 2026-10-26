package com.heimdall.gjallaraudio.feature.storage

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File

object StorageHelper {

    private const val PREFS_NAME = "gjallar_storage_prefs"
    private const val KEY_PREFERRED_EXPLORER = "preferred_file_explorer"

    val publicMusicDirPath: String
        get() = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).absolutePath}/GjallarAudio"

    val displayMusicPath: String
        get() = "Almacenamiento interno > Music > GjallarAudio (y GjallarForge / GjallarLab)"

    data class InstalledExplorer(
        val packageName: String,
        val name: String,
        val isSystem: Boolean = false
    )

    fun getPreferredExplorer(context: Context): String? {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_PREFERRED_EXPLORER, null)
    }

    fun setPreferredExplorer(context: Context, packageName: String?) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().apply {
            if (packageName == null) {
                remove(KEY_PREFERRED_EXPLORER)
            } else {
                putString(KEY_PREFERRED_EXPLORER, packageName)
            }
            apply()
        }
    }

    fun getPreferredExplorerLabel(context: Context): String? {
        val pkg = getPreferredExplorer(context) ?: return null
        if (pkg == "SYSTEM") return "Explorador del Sistema"
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo)?.toString() ?: pkg
        } catch (_: Exception) {
            if (pkg.contains("solidexplorer")) "Solid Explorer" else pkg
        }
    }

    @Volatile
    private var cachedExplorers: List<InstalledExplorer>? = null

    fun getInstalledExplorers(context: Context, forceRefresh: Boolean = false): List<InstalledExplorer> {
        if (!forceRefresh && cachedExplorers != null) {
            return cachedExplorers!!
        }

        val pm = context.packageManager
        val result = mutableListOf<InstalledExplorer>()

        val knownList = listOf(
            "pl.solidexplorer2" to "Solid Explorer",
            "pl.solidexplorer" to "Solid Explorer Classic",
            "com.google.android.apps.nbu.files" to "Files de Google",
            "com.sec.android.app.myfiles" to "Mis Archivos (Samsung)",
            "com.mi.android.globalFileexplorer" to "Administrador de archivos (Xiaomi)",
            "com.cxinventor.file.explorer" to "Cx Explorador de Archivos",
            "com.alphainventor.filemanager" to "File Manager Plus",
            "com.ghisler.android.TotalCommander" to "Total Commander",
            "nextapp.fx" to "FX File Explorer",
            "com.amaze.filemanager" to "Amaze File Manager",
            "me.zhanghai.android.files" to "Material Files"
        )

        for ((pkg, defaultLabel) in knownList) {
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo)?.toString() ?: defaultLabel
                result.add(InstalledExplorer(packageName = pkg, name = label))
            } catch (_: Exception) {
                // Not installed or not visible
            }
        }

        // Sistema predeterminado siempre disponible
        result.add(
            InstalledExplorer(
                packageName = "SYSTEM",
                name = "Explorador del Sistema (Android)",
                isSystem = true
            )
        )

        cachedExplorers = result
        return result
    }

    fun copyPathToClipboard(context: Context, showToast: Boolean = true) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Ruta GjallarAudio", publicMusicDirPath)
        clipboard.setPrimaryClip(clip)
        if (showToast) {
            Toast.makeText(context, "Ruta copiada al portapapeles: $publicMusicDirPath", Toast.LENGTH_SHORT).show()
        }
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

    fun openFolderWithPackage(context: Context, packageName: String) {
        if (packageName == "SYSTEM") {
            openSystemFiles(context)
            return
        }

        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val gjallarDir = File(musicDir, "GjallarAudio")
        if (!gjallarDir.exists()) gjallarDir.mkdirs()

        copyPathToClipboard(context, showToast = false)

        try {
            val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Music%2FGjallarAudio")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "vnd.android.document/directory")
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {
            // Intento 2: Iniciar la app directamente
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    Toast.makeText(context, "Abriendo gestor (Ruta copiada al portapapeles)", Toast.LENGTH_SHORT).show()
                    return
                }
            } catch (_: Exception) {}
        }

        // Intento 3: Fallback a explorador del sistema
        openSystemFiles(context)
    }

    fun openWithChooser(context: Context) {
        val intents = mutableListOf<Intent>()

        val knownPackages = listOf(
            "pl.solidexplorer2",
            "pl.solidexplorer",
            "com.sec.android.app.myfiles",
            "com.mi.android.globalFileexplorer",
            "com.google.android.apps.nbu.files",
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

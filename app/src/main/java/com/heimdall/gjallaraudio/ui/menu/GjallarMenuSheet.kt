package com.heimdall.gjallaraudio.ui.menu

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heimdall.gjallaraudio.BuildConfig
import com.heimdall.gjallaraudio.feature.storage.StorageHelper
import com.heimdall.gjallaraudio.feature.updater.GitHubUpdateChecker
import com.heimdall.gjallaraudio.feature.updater.UpdateStatus
import com.heimdall.gjallaraudio.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GjallarMenuSheet(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var showExplorerDialog by remember { mutableStateOf(false) }

    // Enlace oficial de PayPal
    val coffeeUrl = "https://paypal.me/HeimdallCorp"

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Slate950,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        modifier = Modifier.fillMaxSize(),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Slate700,
                width = 48.dp,
                height = 4.dp
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
        ) {
            // Encabezado nórdico
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate850,
                    border = BorderStroke(1.dp, GjallarCyan),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = GjallarCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GJALLAR",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp,
                            color = Slate50
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "AUDIO",
                            fontWeight = FontWeight.Light,
                            fontSize = 20.sp,
                            letterSpacing = 1.sp,
                            color = GjallarCyan
                        )
                    }
                    Text(
                        text = "ECOSISTEMA HEIMDALL • MENÚ GENERAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 0.8.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900,
                    border = BorderStroke(1.dp, Slate700)
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GjallarCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar menú",
                        tint = Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate850, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // SECCIÓN 1: ACTUALIZACIONES Y VERSIÓN
            // ==========================================
            Text(
                text = "ᛟ VERSIÓN Y ACTUALIZACIONES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = GjallarCyan,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "GjallarAudio v${BuildConfig.VERSION_NAME}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Text(
                                text = "Compilación oficial Heimdall (Build ${BuildConfig.VERSION_CODE})",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }

                        Button(
                            onClick = {
                                if (updateStatus !is UpdateStatus.Checking) {
                                    updateStatus = UpdateStatus.Checking
                                    coroutineScope.launch {
                                        updateStatus = GitHubUpdateChecker.checkForUpdates()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (updateStatus is UpdateStatus.Checking) Slate700 else GjallarCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            if (updateStatus is UpdateStatus.Checking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Slate100,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Buscando...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate100
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Slate950,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Buscar en GitHub",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate950
                                )
                            }
                        }
                    }

                    // Respuestas dinámicas de actualización
                    AnimatedVisibility(visible = updateStatus !is UpdateStatus.Idle) {
                        Spacer(modifier = Modifier.height(12.dp))
                        when (val status = updateStatus) {
                            is UpdateStatus.UpToDate -> {
                                Surface(
                                    color = Color(0xFF064E3B).copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "¡Estás al día!",
                                                color = Color(0xFF34D399),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Tienes instalada la última versión disponible (v${status.currentVersion}).",
                                                color = Slate300,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                            is UpdateStatus.UpdateAvailable -> {
                                Surface(
                                    color = GjallarCyan.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, GjallarCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.NewReleases,
                                                contentDescription = null,
                                                tint = GjallarCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Nueva versión disponible: ${status.newVersion}",
                                                color = GjallarCyan,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp
                                            )
                                        }
                                        if (status.apkSizeMb > 0) {
                                            Text(
                                                text = "Tamaño del instalador: %.1f MB".format(status.apkSizeMb),
                                                color = Slate400,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                        if (status.releaseNotes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = status.releaseNotes,
                                                color = Slate100,
                                                fontSize = 11.sp,
                                                maxLines = 4
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                GitHubUpdateChecker.openDownloadUrl(context, status.downloadUrl)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GjallarCyan),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = null,
                                                tint = Slate950,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Descargar e Instalar (.apk)",
                                                fontWeight = FontWeight.Bold,
                                                color = Slate950,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                            is UpdateStatus.Error -> {
                                Surface(
                                    color = Color(0xFF7F1D1D).copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFF87171),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = status.message,
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SECCIÓN 2: ACCESO A ARCHIVOS GENERADOS
            // ==========================================
            Text(
                text = "ᚱ ARCHIVOS Y ALMACENAMIENTO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = GjallarAmber,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GjallarAmber.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, GjallarAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.FolderOpen,
                                    contentDescription = null,
                                    tint = GjallarAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Carpeta de Exportación",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Text(
                                text = StorageHelper.displayMusicPath,
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showExplorerDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GjallarAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = Slate950,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Abrir Archivos",
                                color = Slate950,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { StorageHelper.copyPathToClipboard(context) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Slate700),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Slate100,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Copiar Ruta",
                                color = Slate100,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Archivos generados recientemente
                    val generatedFiles = remember { StorageHelper.listGeneratedFiles(context) }
                    if (generatedFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Slate800, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Archivos generados recientemente (${generatedFiles.size}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        generatedFiles.take(4).forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.AudioFile,
                                        contentDescription = null,
                                        tint = Slate500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.name,
                                        fontSize = 12.sp,
                                        color = Slate100,
                                        maxLines = 1
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Slate800
                                ) {
                                    Text(
                                        text = item.sizeFormatted,
                                        fontSize = 10.sp,
                                        color = Slate400,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SECCIÓN 3: APOYAR EL PROYECTO / CAFÉ
            // ==========================================
            Text(
                text = "ᛉ APOYAR AL CREADOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color(0xFFF472B6),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Color(0xFFF472B6).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF472B6).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFF472B6).copy(alpha = 0.5f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "☕", fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "¿Te gusta GjallarAudio?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Text(
                                text = "100% independiente, sin anuncios ni telemetría",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Puedes apoyar el desarrollo de GjallarAudio invitando al creador a un café a través de PayPal para impulsar nuevas funciones y optimizaciones de IA.",
                        fontSize = 12.sp,
                        color = Slate300,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(coffeeUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF472B6)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "☕ Invitar a un Café (PayPal)",
                            color = Slate950,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // SECCIÓN 4: REPOSITORIO Y PRIVACIDAD
            // ==========================================
            Text(
                text = "ᛋ CÓDIGO Y COMUNIDAD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Slate400,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Hungrow/Gjallar-Audio"))
                                    )
                                } catch (_: Exception) {}
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = GjallarCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Repositorio en GitHub",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Text(
                                text = "github.com/Hungrow/Gjallar-Audio",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Hungrow/Gjallar-Audio/issues"))
                                    )
                                } catch (_: Exception) {}
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = GjallarAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reportar un problema / Sugerir función",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Text(
                                text = "Abre un Issue en GitHub",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filosofía Nórdica
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Slate850),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = GjallarCyan,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Procesamiento 100% en dispositivo: Ningún archivo, voz, nota ni audio sale de tu teléfono. El modelo de IA y todos los motores funcionan sin conexión a Internet.",
                        fontSize = 11.sp,
                        color = Slate400,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showExplorerDialog) {
        AlertDialog(
            onDismissRequest = { showExplorerDialog = false },
            containerColor = Slate900,
            titleContentColor = Slate50,
            textContentColor = Slate300,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = GjallarAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Abrir Carpeta de Archivos", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Elige con qué aplicación deseas explorar los audios guardados:",
                        fontSize = 12.sp,
                        color = Slate400,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    // Opción 1: Selector de aplicaciones instaladas
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate850,
                        border = BorderStroke(1.dp, Slate700),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExplorerDialog = false
                                StorageHelper.openWithChooser(context)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Apps, contentDescription = null, tint = GjallarCyan, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Elegir aplicación instalada", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate100)
                                Text("Samsung, Xiaomi, Solid Explorer, etc.", fontSize = 10.sp, color = Slate400)
                            }
                        }
                    }

                    // Opción 2: Gestor del sistema
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate850,
                        border = BorderStroke(1.dp, Slate700),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExplorerDialog = false
                                StorageHelper.openSystemFiles(context)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = GjallarAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Explorador del Sistema", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate100)
                                Text("Gestor predeterminado (Archivos / Files)", fontSize = 10.sp, color = Slate400)
                            }
                        }
                    }

                    // Opción 3: Copiar ruta
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Slate850,
                        border = BorderStroke(1.dp, Slate700),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showExplorerDialog = false
                                StorageHelper.copyPathToClipboard(context)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Slate300, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Copiar ruta al portapapeles", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate100)
                                Text(StorageHelper.publicMusicDirPath, fontSize = 9.sp, color = Slate500, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExplorerDialog = false }) {
                    Text("Cerrar", color = Slate400)
                }
            }
        )
    }
}

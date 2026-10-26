package com.heimdall.gjallaraudio.feature.forge

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heimdall.gjallaraudio.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ForgeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val extractorEngine = remember { AudioExtractorEngine(context) }

    var selectedVideo by remember { mutableStateOf<VideoConversionItem?>(null) }
    var outputFileName by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf("MP3 Universal") }
    var normalizeLoudness by remember { mutableStateOf(true) }
    var trimSilence by remember { mutableStateOf(false) }
    var isForging by remember { mutableStateOf(false) }

    // Histórico único: último audio forjado
    var lastForgedAudio by remember { mutableStateOf<ForgedAudioHistory?>(null) }

    // Reproductor de audio integrado
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // Selector de video del almacenamiento local
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val (fileName, duration) = extractorEngine.getVideoMetadata(it)
            val baseName = fileName.substringBeforeLast(".")
            selectedVideo = VideoConversionItem(
                uri = it,
                fileName = fileName,
                durationFormatted = duration,
                outputName = baseName,
                targetFormat = selectedFormat
            )
            outputFileName = baseName
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cabecera Principal
        item {
            Text(
                text = "Gjallar Forge",
                style = MaterialTheme.typography.headlineLarge,
                color = Slate50
            )
            Text(
                text = "Extractor y Forjador de Audio Local (MP3, M4A y WAV)",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
        }

        // Formato de Salida
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Formato de Salida:",
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate100
                        )
                        Text(
                            text = if (selectedFormat.contains("MP3")) "320 kbps (LAME)" else "Estudio",
                            style = MaterialTheme.typography.labelSmall,
                            color = GjallarAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("MP3 Universal", "M4A (AAC)", "WAV (Lossless)").forEach { fmt ->
                            val isSelected = selectedFormat == fmt
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Slate850 else Slate800,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) GjallarCyan else Slate700
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFormat = fmt }
                            ) {
                                Text(
                                    text = fmt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) GjallarCyan else Slate400,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selector / Tarjeta de Video
        item {
            if (selectedVideo == null) {
                // Dropzone para elegir video
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GjallarCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { videoPickerLauncher.launch("video/*") }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Slate850,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.VideoFile,
                                    contentDescription = null,
                                    tint = GjallarCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Seleccionar Video Local",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                        Text(
                            text = "Toca para examinar videos de tu galería o almacenamiento",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                // Video seleccionado listo para forjar
                val video = selectedVideo!!
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Slate850,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Movie, contentDescription = null, tint = GjallarCyan)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = video.fileName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Slate100,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Duración: ${video.durationFormatted}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400
                                    )
                                }
                            }
                            IconButton(onClick = {
                                selectedVideo = null
                                outputFileName = ""
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = Slate400)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Nombre de archivo de salida
                        OutlinedTextField(
                            value = outputFileName,
                            onValueChange = { outputFileName = it },
                            label = { Text("Nombre del audio resultante") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GjallarCyan,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Slate50,
                                unfocusedTextColor = Slate100
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Opciones de audio inteligente
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Normalizar Volumen (EBU R128)", color = Slate300, fontSize = 13.sp)
                            Switch(
                                checked = normalizeLoudness,
                                onCheckedChange = { normalizeLoudness = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = GjallarCyan, checkedTrackColor = Slate850)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recortar silencios al inicio/fin", color = Slate300, fontSize = 13.sp)
                            Switch(
                                checked = trimSilence,
                                onCheckedChange = { trimSilence = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = GjallarAmber, checkedTrackColor = Slate850)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Botón de Forjar Audio
                        Button(
                            onClick = {
                                if (outputFileName.isBlank()) {
                                    Toast.makeText(context, "Asigna un nombre al archivo", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isForging = true
                                coroutineScope.launch {
                                    val result = extractorEngine.extractAudio(
                                        item = video,
                                        customName = outputFileName,
                                        targetFormat = selectedFormat,
                                        normalizeLoudness = normalizeLoudness,
                                        trimSilence = trimSilence
                                    )
                                    isForging = false
                                    result.onSuccess { forged ->
                                        lastForgedAudio = forged
                                        // Reset reproductor anterior
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        isPlaying = false
                                        Toast.makeText(context, "Audio forjado con éxito", Toast.LENGTH_SHORT).show()
                                    }.onFailure { err ->
                                        Toast.makeText(context, "Error: ${err.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isForging,
                            colors = ButtonDefaults.buttonColors(containerColor = GjallarCyan, contentColor = Slate950),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isForging) {
                                CircularProgressIndicator(color = Slate950, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("FORJANDO AUDIO...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = Slate950)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("FORJAR AUDIO AHORA", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Histórico Único / Reproductor Integrado
        lastForgedAudio?.let { audio ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GjallarCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = audio.file.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate50,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Guardado en carpeta pública de Música",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GjallarCyan,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate850
                            ) {
                                Text(
                                    text = audio.format,
                                    color = GjallarAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Slate700, modifier = Modifier.padding(vertical = 12.dp))

                        // Botones de Reproducir y Compartir
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isPlaying) {
                                        mediaPlayer?.pause()
                                        isPlaying = false
                                    } else {
                                        if (mediaPlayer == null) {
                                            mediaPlayer = MediaPlayer().apply {
                                                setDataSource(audio.file.absolutePath)
                                                prepare()
                                                setOnCompletionListener { isPlaying = false }
                                            }
                                        }
                                        mediaPlayer?.start()
                                        isPlaying = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) RoseRed else Slate850,
                                    contentColor = if (isPlaying) Color.White else GjallarCyan
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isPlaying) RoseRed else GjallarCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPlaying) "PAUSAR" else "REPRODUCIR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = when (audio.format.lowercase()) {
                                            "wav" -> "audio/wav"
                                            "m4a" -> "audio/mp4"
                                            else -> "audio/mpeg"
                                        }
                                        putExtra(Intent.EXTRA_STREAM, audio.shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir audio con:"))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Slate850,
                                    contentColor = GjallarAmber
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GjallarAmber.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, tint = GjallarAmber)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("COMPARTIR", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GjallarAmber)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ruta: ${audio.publicPath}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

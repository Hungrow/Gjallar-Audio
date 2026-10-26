package com.heimdall.gjallaraudio.feature.lab

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class LabTool(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TRIMMER("Recortar Tono", Icons.Filled.ContentCut),
    SEPARATION("Voz vs Música", Icons.Filled.RecordVoiceOver),
    DENOISER("Limpiar Ruido", Icons.Filled.CleaningServices)
}

@Composable
fun LabScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioEngine = remember { LabAudioEngine(context) }

    var selectedTool by remember { mutableStateOf(LabTool.TRIMMER) }
    var selectedAudio by remember { mutableStateOf<LabAudioMetadata?>(null) }
    var outputName by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    // Parámetros de recorte
    var trimStartSec by remember { mutableStateOf(0f) }
    var trimEndSec by remember { mutableStateOf(30f) }
    var isDetectingChorus by remember { mutableStateOf(false) }

    // Modo de Separación: true = Aislar Voz, false = Instrumental (Karaoke)
    var isolateVocals by remember { mutableStateOf(true) }
    var aiProgress by remember { mutableFloatStateOf(0f) }

    // Perfil de Reducción de Ruido
    var denoiseProfile by remember { mutableStateOf(LabAudioEngine.DenoiseProfile.STUDIO_VOICE) }

    // Reproductor de Preescucha de Recorte
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewCurrentSec by remember { mutableStateOf(0f) }

    // Resultado procesado final
    var processedResult by remember { mutableStateOf<ProcessedAudioResult?>(null) }
    var resultPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isResultPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.release()
            resultPlayer?.release()
        }
    }

    // Monitor de posición para la preescucha
    LaunchedEffect(isPreviewPlaying) {
        while (isPreviewPlaying && isActive) {
            previewPlayer?.let { player ->
                if (player.isPlaying) {
                    val currentPosSec = player.currentPosition / 1000f
                    previewCurrentSec = currentPosSec
                    // Si llega al final del rango seleccionado, detener
                    if (currentPosSec >= trimEndSec) {
                        player.pause()
                        player.seekTo((trimStartSec * 1000).toInt())
                        isPreviewPlaying = false
                    }
                }
            }
            delay(100)
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
            isPreviewPlaying = false

            val meta = audioEngine.getAudioMetadata(it)
            selectedAudio = meta
            val totalSec = (meta.durationMs / 1000f).coerceAtLeast(5f)
            trimStartSec = 0f
            trimEndSec = (30f).coerceAtMost(totalSec)
            outputName = meta.fileName.substringBeforeLast(".") + "_tono"
            processedResult = null

            // Inicializar reproductor de preview
            try {
                previewPlayer = MediaPlayer().apply {
                    setDataSource(context, it)
                    prepare()
                }
            } catch (e: Exception) {
                previewPlayer = null
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Gjallar Lab",
                style = MaterialTheme.typography.headlineLarge,
                color = Slate50
            )
            Text(
                text = "Estudio de Sonido: Tonos, Extractor Vocal & Cancelación de Ruido",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
        }

        // Selector de Herramienta
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate850, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LabTool.values().forEach { tool ->
                    val isSelected = selectedTool == tool
                    val bgColor by animateColorAsState(if (isSelected) Slate800 else Color.Transparent)
                    val textColor by animateColorAsState(if (isSelected) GjallarCyan else Slate400)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) Slate700 else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                selectedTool = tool
                                if (tool == LabTool.SEPARATION) {
                                    outputName = (selectedAudio?.fileName?.substringBeforeLast(".") ?: "audio") +
                                            if (isolateVocals) "_vocal" else "_instrumental"
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(tool.icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tool.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Tarjeta de Selección de Audio
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GjallarCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { audioPicker.launch("audio/*") }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedAudio == null) {
                        Surface(
                            shape = CircleShape,
                            color = Slate850,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GjallarCyan.copy(alpha = 0.6f)),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.AudioFile,
                                    contentDescription = null,
                                    tint = GjallarCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Toca para Seleccionar un Audio a Procesar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate50
                        )
                        Text(
                            text = "Selecciona una canción o nota de voz",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Slate850,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GjallarCyan),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = GjallarCyan)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedAudio!!.fileName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Slate50,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Duración total: ${selectedAudio!!.durationFormatted}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                            TextButton(onClick = { audioPicker.launch("audio/*") }) {
                                Text("Cambiar", color = GjallarCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Panel de Operaciones
        if (selectedAudio != null) {
            val totalSec = (selectedAudio!!.durationMs / 1000f).coerceAtLeast(1f)

            when (selectedTool) {
                LabTool.TRIMMER -> {
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
                                        text = "Recorte con Preescucha:",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Slate100
                                    )
                                    Text(
                                        text = "Duración: ${String.format("%.1f", (trimEndSec - trimStartSec).coerceAtLeast(0f))}s",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = GjallarAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // BOTÓN PREVISUALIZAR / PREESCUPAR
                                Button(
                                    onClick = {
                                        previewPlayer?.let { player ->
                                            if (isPreviewPlaying) {
                                                player.pause()
                                                isPreviewPlaying = false
                                            } else {
                                                player.seekTo((trimStartSec * 1000).toInt())
                                                player.start()
                                                isPreviewPlaying = true
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPreviewPlaying) RoseRed else Slate850,
                                        contentColor = if (isPreviewPlaying) Color.White else GjallarCyan
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPreviewPlaying) RoseRed else GjallarCyan),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        if (isPreviewPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPreviewPlaying) "DETENER PREVISUALIZACIÓN (${String.format("%.1f", previewCurrentSec)}s)"
                                               else "PROBAR FRAGMENTO SELECCIONADO",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Controles de Rango
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Inicio: ${String.format("%.1f", trimStartSec)}s", color = GjallarCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Fin: ${String.format("%.1f", trimEndSec)}s", color = GjallarAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                // Slider Inicio
                                Slider(
                                    value = trimStartSec,
                                    onValueChange = {
                                        if (it < trimEndSec) {
                                            trimStartSec = it
                                            if (isPreviewPlaying) previewPlayer?.seekTo((it * 1000).toInt())
                                        }
                                    },
                                    valueRange = 0f..totalSec,
                                    colors = SliderDefaults.colors(
                                        thumbColor = GjallarCyan,
                                        activeTrackColor = GjallarCyan,
                                        inactiveTrackColor = Slate700
                                    )
                                )

                                // Slider Fin
                                Slider(
                                    value = trimEndSec,
                                    onValueChange = {
                                        if (it > trimStartSec) trimEndSec = it
                                    },
                                    valueRange = 0f..totalSec,
                                    colors = SliderDefaults.colors(
                                        thumbColor = GjallarAmber,
                                        activeTrackColor = GjallarAmber,
                                        inactiveTrackColor = Slate700
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Detección Inteligente de Estribillo
                                OutlinedButton(
                                    onClick = {
                                        selectedAudio?.let { audio ->
                                            coroutineScope.launch {
                                                isDetectingChorus = true
                                                val res = audioEngine.detectChorusDrop(audio.uri, 30f)
                                                isDetectingChorus = false
                                                res.getOrNull()?.let { (start, end) ->
                                                    trimStartSec = start
                                                    trimEndSec = end
                                                    if (isPreviewPlaying) {
                                                        previewPlayer?.seekTo((start * 1000).toInt())
                                                    }
                                                    Toast.makeText(context, "¡Estribillo detectado en ${String.format("%.0f", start)}s - ${String.format("%.0f", end)}s!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isDetectingChorus,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = GjallarCyan
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GjallarCyan.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isDetectingChorus) {
                                        CircularProgressIndicator(color = GjallarCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Escaneando dinámica...", fontSize = 12.sp)
                                    } else {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = GjallarCyan)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Detectar Estribillo / Drop Automático", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                LabTool.SEPARATION -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate800),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "¿Qué pista deseas extraer?",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Slate50
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Opción 1: Solo Voz
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isolateVocals) Slate850 else Slate800,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isolateVocals) GjallarCyan else Slate700),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                isolateVocals = true
                                                outputName = (selectedAudio?.fileName?.substringBeforeLast(".") ?: "audio") + "_solo_voz"
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Filled.Mic, contentDescription = null, tint = if (isolateVocals) GjallarCyan else Slate400, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("SOLO VOZ", color = if (isolateVocals) GjallarCyan else Slate300, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }

                                    // Opción 2: Solo Música (Karaoke)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (!isolateVocals) Slate850 else Slate800,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (!isolateVocals) GjallarAmber else Slate700),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                isolateVocals = false
                                                outputName = (selectedAudio?.fileName?.substringBeforeLast(".") ?: "audio") + "_karaoke"
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = if (!isolateVocals) GjallarAmber else Slate400, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("SOLO MÚSICA", color = if (!isolateVocals) GjallarAmber else Slate300, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = GjallarCyan.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, GjallarCyan.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "IA NEURONAL UVR MDX-NET",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GjallarCyan,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isolateVocals) "Separa la voz con la red neuronal UVR MDX-Net eliminando percusiones, bajos y armonías."
                                           else "Genera la pista instrumental / karaoke de estudio aislando y restando las frecuencias vocales.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                        }
                    }
                }

                LabTool.DENOISER -> {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Slate800),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Restaurador & Filtro de Ruido",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Slate50
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Perfil 1: Voz de Estudio
                                    val isStudio = denoiseProfile == LabAudioEngine.DenoiseProfile.STUDIO_VOICE
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isStudio) Slate850 else Slate800,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isStudio) GjallarCyan else Slate700),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { denoiseProfile = LabAudioEngine.DenoiseProfile.STUDIO_VOICE }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = if (isStudio) GjallarCyan else Slate400, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("VOZ DE ESTUDIO", color = if (isStudio) GjallarCyan else Slate300, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }

                                    // Perfil 2: Fondo y Viento
                                    val isStatic = denoiseProfile == LabAudioEngine.DenoiseProfile.STATIC_NOISE
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isStatic) Slate850 else Slate800,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isStatic) GjallarAmber else Slate700),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { denoiseProfile = LabAudioEngine.DenoiseProfile.STATIC_NOISE }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(Icons.Filled.Air, contentDescription = null, tint = if (isStatic) GjallarAmber else Slate400, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("FONDO Y VIENTO", color = if (isStatic) GjallarAmber else Slate300, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = denoiseProfile.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                        }
                    }
                }
            }

            // Nombre del archivo de salida
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Nombre del Archivo Resultado:",
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate100
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = outputName,
                            onValueChange = { outputName = it },
                            placeholder = { Text("Escribe el nombre del archivo...") },
                            trailingIcon = {
                                Surface(
                                    color = Slate850,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = ".mp3",
                                        color = GjallarCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GjallarCyan,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Slate50,
                                unfocusedTextColor = Slate300
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Botón de Ejecución
            item {
                Button(
                    onClick = {
                        if (!isProcessing && selectedAudio != null) {
                            coroutineScope.launch {
                                isProcessing = true
                                previewPlayer?.pause()
                                isPreviewPlaying = false

                                resultPlayer?.stop()
                                resultPlayer?.release()
                                resultPlayer = null
                                isResultPlaying = false

                                val result = when (selectedTool) {
                                    LabTool.TRIMMER -> audioEngine.trimAudio(
                                        uri = selectedAudio!!.uri,
                                        startSeconds = trimStartSec,
                                        endSeconds = trimEndSec,
                                        outputName = outputName
                                    )
                                    LabTool.SEPARATION -> {
                                        aiProgress = 0f
                                        audioEngine.separateTracks(
                                            uri = selectedAudio!!.uri,
                                            outputName = outputName,
                                            isolateVocals = isolateVocals,
                                            onProgress = { p ->
                                                aiProgress = p
                                            }
                                        )
                                    }
                                    LabTool.DENOISER -> audioEngine.denoiseAudio(
                                        uri = selectedAudio!!.uri,
                                        outputName = outputName,
                                        profile = denoiseProfile
                                    )
                                }

                                isProcessing = false

                                if (result.isSuccess) {
                                    processedResult = result.getOrNull()
                                    Toast.makeText(context, "¡Procesado completado con éxito!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GjallarCyan,
                        contentColor = Slate950
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Slate950, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (selectedTool == LabTool.SEPARATION && aiProgress > 0f) {
                                "SEPARANDO CON IA: ${aiProgress.toInt()}%"
                            } else {
                                "PROCESANDO EN EL LABORATORIO..."
                            },
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(selectedTool.icon, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (selectedTool) {
                                LabTool.TRIMMER -> "RECORTAR TONO"
                                LabTool.SEPARATION -> if (isolateVocals) "EXTRAER SOLO VOZ" else "EXTRAER SOLO MÚSICA"
                                LabTool.DENOISER -> "LIMPIAR RUIDO"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Tarjeta de Resultado: Reproducir y Compartir
        processedResult?.let { result ->
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "RESULTADO DEL LABORATORIO",
                    style = MaterialTheme.typography.labelSmall,
                    color = GjallarAmber,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldGreen.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.AudioFile,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = result.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate50,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "MP3 320 kbps • Listo para usar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400
                                    )
                                }
                            }

                            Surface(
                                color = EmeraldGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "COMPLETADO",
                                    color = EmeraldGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Slate700, modifier = Modifier.padding(vertical = 12.dp))

                        // Botones de Acción
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isResultPlaying) {
                                        resultPlayer?.pause()
                                        isResultPlaying = false
                                    } else {
                                        if (resultPlayer == null) {
                                            resultPlayer = MediaPlayer().apply {
                                                setDataSource(result.file.absolutePath)
                                                prepare()
                                                setOnCompletionListener { isResultPlaying = false }
                                            }
                                        }
                                        resultPlayer?.start()
                                        isResultPlaying = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isResultPlaying) RoseRed else Slate850,
                                    contentColor = if (isResultPlaying) Color.White else GjallarCyan
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isResultPlaying) RoseRed else GjallarCyan),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Icon(
                                    if (isResultPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isResultPlaying) "PAUSAR" else "REPRODUCIR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/mpeg"
                                        putExtra(Intent.EXTRA_STREAM, result.shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir resultado:"))
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
                            text = "Ruta: ${result.publicPath}",
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

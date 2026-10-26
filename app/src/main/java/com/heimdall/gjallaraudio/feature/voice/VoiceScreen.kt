package com.heimdall.gjallaraudio.feature.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.heimdall.gjallaraudio.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// =========================================================================
// PALETAS NÓRDICAS CON CONTRASTE GARANTIZADO
// =========================================================================
enum class NordicNoteTheme(
    val id: String,
    val title: String,
    val rune: String,
    val cardBg: Color,
    val titleColor: Color,
    val textColor: Color,
    val placeholderColor: Color,
    val borderColor: Color,
    val pinColor: Color
) {
    AMBER_SUN(
        id = "amber",
        title = "Ámbar Solar",
        rune = "ᛟ",
        cardBg = Color(0xFFFEF3C7),
        titleColor = Color(0xFF451A03), // Marrón carbón ultra legible
        textColor = Color(0xFF78350F),  // Contraste 100% nítido
        placeholderColor = Color(0xFF92400E).copy(alpha = 0.55f),
        borderColor = Color(0xFFF59E0B),
        pinColor = Color(0xFFD97706)
    ),
    GLACIER_ICE(
        id = "ice",
        title = "Hielo Glaciar",
        rune = "ᛉ",
        cardBg = Color(0xFF0F2B48),
        titleColor = Color(0xFFF0F9FF),
        textColor = Color(0xFFBAE6FD),
        placeholderColor = Color(0xFF7DD3FC).copy(alpha = 0.55f),
        borderColor = Color(0xFF38BDF8),
        pinColor = Color(0xFF0284C7)
    ),
    FOREST_MOSS(
        id = "moss",
        title = "Musgo del Bosque",
        rune = "ᚱ",
        cardBg = Color(0xFF064E3B),
        titleColor = Color(0xFFECFDF5),
        textColor = Color(0xFFA7F3D0),
        placeholderColor = Color(0xFF6EE7B7).copy(alpha = 0.55f),
        borderColor = Color(0xFF10B981),
        pinColor = Color(0xFF059669)
    ),
    OBSIDIAN_SLATE(
        id = "obsidian",
        title = "Pizarra Nórdica",
        rune = "ᚦ",
        cardBg = Color(0xFF1E293B),
        titleColor = Color(0xFFF8FAFC),
        textColor = Color(0xFFCBD5E1),
        placeholderColor = Color(0xFF94A3B8).copy(alpha = 0.55f),
        borderColor = Color(0xFF475569),
        pinColor = Color(0xFFF59E0B)
    ),
    COPPER_DUSK(
        id = "copper",
        title = "Cobre Rúnico",
        rune = "ᛋ",
        cardBg = Color(0xFF3F1B12),
        titleColor = Color(0xFFFFEDD5),
        textColor = Color(0xFFFED7AA),
        placeholderColor = Color(0xFFFDBA74).copy(alpha = 0.55f),
        borderColor = Color(0xFFEA580C),
        pinColor = Color(0xFFC2410C)
    )
}

data class StickyNote(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val theme: NordicNoteTheme,
    val timestamp: String
)

data class Speaker(
    val id: String,
    val name: String,
    val color: Color
)

data class DialogueEntry(
    val id: String = UUID.randomUUID().toString(),
    val speakerId: String,
    val speakerName: String,
    val speakerColor: Color,
    val timestamp: String,
    var text: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sttEngine = remember { GjallarSttEngine(context) }

    DisposableEffect(Unit) {
        onDispose {
            sttEngine.stopListening()
        }
    }

    val isListening by sttEngine.isListening.collectAsState()
    val rawTranscribedText by sttEngine.transcribedText.collectAsState()

    // Entorno de transcripción: MINIMIZADO hasta su uso
    var isTranscriptionExpanded by remember { mutableStateOf(false) }

    // Muro de Notas persistidas en almacenamiento local (no se pierden al cambiar de pestaña)
    var notes by remember {
        mutableStateOf(NotesRepository.loadNotes(context))
    }
    var noteSearchQuery by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<StickyNote?>(null) }

    val filteredNotes = remember(notes, noteSearchQuery) {
        if (noteSearchQuery.isBlank()) notes
        else notes.filter {
            it.title.contains(noteSearchQuery, ignoreCase = true) ||
            it.content.contains(noteSearchQuery, ignoreCase = true)
        }
    }

    val updateNotes: (List<StickyNote>) -> Unit = { newNotes ->
        notes = newNotes
        NotesRepository.saveNotes(context, newNotes)
    }

    // Estado del nuevo lienzo nórdico
    var newTitle by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf(NordicNoteTheme.AMBER_SUN) }

    // Interlocutores (para el transcriptor minimizado)
    val defaultSpeakers = remember {
        listOf(
            Speaker("spk_1", "Interlocutor 1", GjallarCyan),
            Speaker("spk_2", "Interlocutor 2", GjallarAmber),
            Speaker("spk_3", "Interlocutor 3", Color(0xFF10B981))
        )
    }
    var speakers by remember { mutableStateOf(defaultSpeakers) }
    var activeSpeaker by remember { mutableStateOf(defaultSpeakers.first()) }
    var dialogueEntries by remember { mutableStateOf<List<DialogueEntry>>(emptyList()) }
    var lastProcessedText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            sttEngine.startListening()
        } else {
            Toast.makeText(context, "Permiso de micrófono requerido para transcribir", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(rawTranscribedText) {
        if (rawTranscribedText.isNotBlank() && rawTranscribedText != lastProcessedText) {
            val newChunk = if (rawTranscribedText.startsWith(lastProcessedText)) {
                rawTranscribedText.removePrefix(lastProcessedText).trim()
            } else {
                rawTranscribedText.trim()
            }

            if (newChunk.isNotBlank()) {
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val lastEntry = dialogueEntries.lastOrNull()

                if (lastEntry != null && lastEntry.speakerId == activeSpeaker.id) {
                    val updatedList = dialogueEntries.toMutableList()
                    val updatedText = "${lastEntry.text} $newChunk".trim()
                    updatedList[updatedList.lastIndex] = lastEntry.copy(text = updatedText)
                    dialogueEntries = updatedList
                } else {
                    val newEntry = DialogueEntry(
                        speakerId = activeSpeaker.id,
                        speakerName = activeSpeaker.name,
                        speakerColor = activeSpeaker.color,
                        timestamp = currentTime,
                        text = newChunk
                    )
                    dialogueEntries = dialogueEntries + newEntry
                }
            }
            lastProcessedText = rawTranscribedText
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera Principal
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gjallar Notas",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Slate50
                    )
                    Text(
                        text = "Muro de Post-its & Recordatorios",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                ) {
                    Text(
                        text = "${notes.size} notas",
                        color = GjallarCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // =========================================================================
        // ENTORNO DE TRANSCRIPCIÓN POR INTERLOCUTORES (MINIMIZADO HASTA SU USO)
        // =========================================================================
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isTranscriptionExpanded) Slate800 else Slate850
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isListening) RoseRed else (if (isTranscriptionExpanded) GjallarCyan.copy(alpha = 0.5f) else Slate700)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isTranscriptionExpanded = !isTranscriptionExpanded }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (isListening) RoseRed.copy(alpha = 0.2f) else Slate800,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isListening) RoseRed else GjallarCyan),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = if (isListening) RoseRed else GjallarCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isListening) "TRANSCRIBIENDO DIÁLOGO..." else "Transcriptor por Interlocutores",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isListening) RoseRed else Slate100
                                )
                                Text(
                                    text = if (isTranscriptionExpanded) "Toca para minimizar" else "Toca para abrir dictado y actas por participante",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate400,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isTranscriptionExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = if (isTranscriptionExpanded) GjallarCyan else Slate400
                        )
                    }

                    AnimatedVisibility(visible = isTranscriptionExpanded) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            HorizontalDivider(color = Slate700, modifier = Modifier.padding(bottom = 12.dp))

                            Text(
                                text = "HABLANTE ACTIVO",
                                style = MaterialTheme.typography.labelSmall,
                                color = GjallarAmber,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(speakers) { speaker ->
                                    val isSelected = activeSpeaker.id == speaker.id
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) speaker.color.copy(alpha = 0.25f) else Slate850,
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) speaker.color else Slate700
                                        ),
                                        modifier = Modifier.clickable { activeSpeaker = speaker }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(speaker.color)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = speaker.name,
                                                color = if (isSelected) speaker.color else Slate300,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (isListening) {
                                            sttEngine.stopListening()
                                        } else {
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasPermission) {
                                                sttEngine.startListening()
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isListening) RoseRed else GjallarCyan,
                                        contentColor = Slate950
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(if (isListening) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isListening) "DETENER" else "GRABAR (${activeSpeaker.name.uppercase()})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                if (dialogueEntries.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            val fullText = dialogueEntries.joinToString("\n\n") {
                                                "${it.speakerName}: ${it.text}"
                                            }
                                            val newNote = StickyNote(
                                                title = "Acta Transcrita",
                                                content = fullText,
                                                theme = NordicNoteTheme.GLACIER_ICE,
                                                timestamp = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                                            )
                                            updateNotes(listOf(newNote) + notes)
                                            Toast.makeText(context, "Convertido a Post-it en tu muro", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Slate850)
                                            .size(40.dp)
                                    ) {
                                        Icon(Icons.Filled.PostAdd, contentDescription = "Guardar como Post-it", tint = GjallarAmber, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            dialogueEntries = emptyList()
                                            sttEngine.clear()
                                            lastProcessedText = ""
                                        },
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Slate850)
                                            .size(40.dp)
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Limpiar", tint = Slate400, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // MURO DE NOTAS GUARDADAS
        // =========================================================================
        item {
            Text(
                text = "MURO DE RECUERDOS (${filteredNotes.size})",
                style = MaterialTheme.typography.labelSmall,
                color = GjallarAmber,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (notes.isNotEmpty()) {
            item {
                OutlinedTextField(
                    value = noteSearchQuery,
                    onValueChange = { noteSearchQuery = it },
                    placeholder = { Text("Buscar en tus notas...", color = Slate400, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (noteSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { noteSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Limpiar", tint = Slate400, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Slate850,
                        unfocusedContainerColor = Slate850,
                        focusedBorderColor = GjallarCyan,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Slate50,
                        unfocusedTextColor = Slate100
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }
        }

        if (filteredNotes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(Slate850, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (noteSearchQuery.isNotEmpty()) "No hay notas que coincidan con la búsqueda."
                               else "El muro está despejado. Escribe una nota en el lienzo inferior.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
            }
        }

        items(filteredNotes) { note ->
            Card(
                colors = CardDefaults.cardColors(containerColor = note.theme.cardBg),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, note.theme.borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        editingNote = note
                    }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = note.theme.pinColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = note.theme.rune,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = note.title.ifBlank { "Sin título" },
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = note.theme.titleColor
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = note.timestamp,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = note.theme.textColor.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { editingNote = note },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Editar",
                                    tint = note.theme.textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            IconButton(
                                onClick = { updateNotes(notes.filterNot { it.id == note.id }) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Borrar",
                                    tint = note.theme.textColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = note.content,
                        fontSize = 13.sp,
                        color = note.theme.textColor,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${note.theme.rune} ${note.theme.title}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = note.theme.textColor.copy(alpha = 0.7f)
                        )
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Compartir",
                            tint = note.theme.textColor.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, note.title)
                                        putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir nota:"))
                                }
                        )
                    }
                }
            }
        }

        // =========================================================================
        // LIENZO DE CREACIÓN NÓRDICO (ELEGANTE, NÍTIDO Y SIEMPRE DISPONIBLE ABAJO)
        // =========================================================================
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "TALLAR NUEVA NOTA",
                style = MaterialTheme.typography.labelSmall,
                color = GjallarAmber,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Lienzo estilizado en la esencia nórdica seleccionada
            Card(
                colors = CardDefaults.cardColors(containerColor = selectedTheme.cardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, selectedTheme.borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Encabezado del Lienzo con Runa Nórdica
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = selectedTheme.pinColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = selectedTheme.rune,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedTheme.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = selectedTheme.titleColor
                            )
                        }

                        Text(
                            text = "Lienzo Nórdico",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = selectedTheme.textColor.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Campo de Título estilizado (soporte completo de tildes y caracteres especiales)
                    TextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = {
                            Text(
                                text = "Título del recordatorio...",
                                color = selectedTheme.placeholderColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        textStyle = TextStyle(
                            color = selectedTheme.titleColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = selectedTheme.titleColor
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = true
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(
                        color = selectedTheme.borderColor.copy(alpha = 0.4f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Campo de Contenido fluido y limpio (con soporte de tildes y caracteres)
                    TextField(
                        value = newContent,
                        onValueChange = { newContent = it },
                        placeholder = {
                            Text(
                                text = "Escribe aquí los detalles del apunte o pensamiento...",
                                color = selectedTheme.placeholderColor,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        },
                        textStyle = TextStyle(
                            color = selectedTheme.textColor,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        minLines = 3,
                        maxLines = 6,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = selectedTheme.textColor
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            autoCorrectEnabled = true
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selector de Esencias Rúnicas (Orbes Nórdicos Circulares)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(NordicNoteTheme.values()) { theme ->
                                val isSelected = selectedTheme == theme
                                Surface(
                                    shape = CircleShape,
                                    color = theme.cardBg,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) selectedTheme.titleColor else theme.borderColor
                                    ),
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clickable { selectedTheme = theme }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = theme.rune,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                            color = theme.titleColor
                                        )
                                    }
                                }
                            }
                        }

                        // Botón de Fijar Nota en el Muro
                        Button(
                            onClick = {
                                if (newContent.isNotBlank() || newTitle.isNotBlank()) {
                                    val currentTime = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                                    val note = StickyNote(
                                        title = newTitle.trim(),
                                        content = newContent.trim(),
                                        theme = selectedTheme,
                                        timestamp = currentTime
                                    )
                                    updateNotes(notes + note)
                                    newTitle = ""
                                    newContent = ""
                                    Toast.makeText(context, "Nota fijada en el muro", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Escribe un título o apunte", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = selectedTheme.pinColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Filled.PushPin, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("FIJAR NOTA", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (editingNote != null) {
        val currentNote = editingNote!!
        var editTitle by remember(currentNote) { mutableStateOf(currentNote.title) }
        var editContent by remember(currentNote) { mutableStateOf(currentNote.content) }
        var editTheme by remember(currentNote) { mutableStateOf(currentNote.theme) }

        AlertDialog(
            onDismissRequest = { editingNote = null },
            title = {
                Text("Editar Nota Rúnica", color = Slate50, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Título", color = Slate400) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate50,
                            unfocusedTextColor = Slate100,
                            focusedBorderColor = GjallarCyan,
                            unfocusedBorderColor = Slate700
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Contenido", color = Slate400) },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate50,
                            unfocusedTextColor = Slate100,
                            focusedBorderColor = GjallarCyan,
                            unfocusedBorderColor = Slate700
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NordicNoteTheme.entries.forEach { theme ->
                            val isSel = editTheme == theme
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(theme.cardBg)
                                    .border(
                                        width = if (isSel) 2.5.dp else 1.dp,
                                        color = if (isSel) GjallarCyan else theme.borderColor,
                                        shape = CircleShape
                                    )
                                    .clickable { editTheme = theme },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(theme.rune, fontSize = 12.sp, color = theme.titleColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = currentNote.copy(
                            title = editTitle.trim(),
                            content = editContent.trim(),
                            theme = editTheme
                        )
                        updateNotes(notes.map { if (it.id == currentNote.id) updated else it })
                        editingNote = null
                        Toast.makeText(context, "Nota actualizada", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GjallarCyan, contentColor = Slate950)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNote = null }) {
                    Text("Cancelar", color = Slate400)
                }
            },
            containerColor = Slate850
        )
    }
}

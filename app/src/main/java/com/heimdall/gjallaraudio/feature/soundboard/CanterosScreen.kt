package com.heimdall.gjallaraudio.feature.soundboard

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanterosScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioPlayer = remember { CanterosAudioPlayer(context) }
    val currentlyPlayingId by audioPlayer.currentlyPlayingId.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }

    val filteredSounds = remember(searchQuery, selectedCategory) {
        CanterosCatalog.sounds.filter { sound ->
            val matchesCategory = if (selectedCategory == "Todos") true else sound.character.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                sound.title.contains(searchQuery, ignoreCase = true) ||
                sound.quote.contains(searchQuery, ignoreCase = true) ||
                sound.character.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Encabezado nórdico
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, GjallarAmber.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GjallarAmber.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, GjallarAmber.copy(alpha = 0.6f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = GjallarAmber,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "LOS CANTEROS",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            letterSpacing = 1.sp,
                            color = Slate50
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ᛟ SOUNDBOARD",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = GjallarAmber
                        )
                    }
                    Text(
                        text = "Botonera legendaria de frases y sonidos míticos (${filteredSounds.size} clips)",
                        fontSize = 11.sp,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (currentlyPlayingId != null) {
                    IconButton(
                        onClick = { audioPlayer.stop() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(RoseRed.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Stop,
                            contentDescription = "Detener sonido",
                            tint = RoseRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Barra de búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar frase o personaje...", color = Slate500, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Borrar", tint = Slate400, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Slate900,
                unfocusedContainerColor = Slate900,
                focusedBorderColor = GjallarCyan,
                unfocusedBorderColor = Slate800,
                focusedTextColor = Slate100,
                unfocusedTextColor = Slate100
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filtro de categorías horizontal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CanterosCatalog.categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) GjallarCyan else Slate900,
                    border = BorderStroke(1.dp, if (isSelected) GjallarCyan else Slate800),
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Slate950 else Slate300,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Rejilla de botones de sonido
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filteredSounds, key = { it.id }) { sound ->
                val isPlaying = currentlyPlayingId == sound.id

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPlaying) Slate850 else Slate900
                    ),
                    border = BorderStroke(
                        width = if (isPlaying) 1.5.dp else 1.dp,
                        color = if (isPlaying) GjallarCyan else Slate800
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { audioPlayer.togglePlay(sound) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Badge de personaje y botón compartir
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (sound.character) {
                                    "Canteros" -> GjallarAmber.copy(alpha = 0.2f)
                                    "Homer" -> Color(0xFFFACC15).copy(alpha = 0.15f)
                                    "Ralph" -> Color(0xFF60A5FA).copy(alpha = 0.15f)
                                    "Burns" -> Color(0xFFA78BFA).copy(alpha = 0.15f)
                                    else -> Slate800
                                },
                                border = BorderStroke(
                                    0.5.dp,
                                    when (sound.character) {
                                        "Canteros" -> GjallarAmber.copy(alpha = 0.5f)
                                        "Homer" -> Color(0xFFFACC15).copy(alpha = 0.5f)
                                        "Ralph" -> Color(0xFF60A5FA).copy(alpha = 0.5f)
                                        "Burns" -> Color(0xFFA78BFA).copy(alpha = 0.5f)
                                        else -> Slate700
                                    }
                                )
                            ) {
                                Text(
                                    text = sound.character,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (sound.character) {
                                        "Canteros" -> GjallarAmber
                                        "Homer" -> Color(0xFFFACC15)
                                        "Ralph" -> Color(0xFF60A5FA)
                                        "Burns" -> Color(0xFFA78BFA)
                                        else -> Slate300
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Botón para compartir por WhatsApp / Telegram
                            IconButton(
                                onClick = { audioPlayer.shareSound(sound) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = "Compartir audio",
                                    tint = Slate400,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = sound.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlaying) GjallarCyan else Slate100,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = sound.quote,
                            fontSize = 11.sp,
                            color = Slate400,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Botón de reproducción grande
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPlaying) GjallarCyan else Slate800,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isPlaying) Slate950 else Slate300,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPlaying) "DETENER" else "REPRODUCIR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = if (isPlaying) Slate950 else Slate300
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

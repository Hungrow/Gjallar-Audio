package com.heimdall.gjallaraudio.feature.soundboard

import android.content.Context
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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heimdall.gjallaraudio.ui.theme.*

enum class SoundboardTab {
    ALL,
    FAVORITES
}

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

    // Persistencia de Favoritos
    val prefs = remember { context.getSharedPreferences("gjallar_soundboard", Context.MODE_PRIVATE) }
    var favoriteIds by remember {
        mutableStateOf(prefs.getStringSet("fav_ids", emptySet()) ?: emptySet())
    }

    fun toggleFavorite(soundId: String) {
        val updated = if (favoriteIds.contains(soundId)) {
            favoriteIds - soundId
        } else {
            favoriteIds + soundId
        }
        favoriteIds = updated
        prefs.edit().putStringSet("fav_ids", updated).apply()
    }

    var selectedMainTab by remember { mutableStateOf(SoundboardTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todos") }

    val filteredSounds = remember(selectedMainTab, searchQuery, selectedCategory, favoriteIds) {
        CanterosCatalog.sounds.filter { sound ->
            val matchesTab = when (selectedMainTab) {
                SoundboardTab.ALL -> true
                SoundboardTab.FAVORITES -> favoriteIds.contains(sound.id)
            }
            val matchesCategory = if (selectedCategory == "Todos") true else sound.character.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                sound.title.contains(searchQuery, ignoreCase = true) ||
                sound.quote.contains(searchQuery, ignoreCase = true) ||
                sound.character.contains(searchQuery, ignoreCase = true)
            }
            matchesTab && matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // ========================================================
        // 1. SELECTOR SUPERIOR: [ TODOS | FAVORITOS ]
        // ========================================================
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Slate900,
            border = BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                // Pestaña TODOS
                val isAllSelected = selectedMainTab == SoundboardTab.ALL
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isAllSelected) Slate800 else Color.Transparent,
                    border = BorderStroke(
                        width = if (isAllSelected) 1.dp else 0.dp,
                        color = if (isAllSelected) GjallarCyan.copy(alpha = 0.5f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMainTab = SoundboardTab.ALL }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ᛟ Todos",
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isAllSelected) GjallarCyan else Slate400
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isAllSelected) GjallarCyan.copy(alpha = 0.2f) else Slate850
                        ) {
                            Text(
                                text = "${CanterosCatalog.sounds.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAllSelected) GjallarCyan else Slate500,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Pestaña FAVORITOS
                val isFavSelected = selectedMainTab == SoundboardTab.FAVORITES
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isFavSelected) Slate800 else Color.Transparent,
                    border = BorderStroke(
                        width = if (isFavSelected) 1.dp else 0.dp,
                        color = if (isFavSelected) GjallarCyan.copy(alpha = 0.5f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMainTab = SoundboardTab.FAVORITES }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = if (isFavSelected) GjallarCyan else Slate500,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Favoritos",
                            fontWeight = if (isFavSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isFavSelected) GjallarCyan else Slate400
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isFavSelected) GjallarCyan.copy(alpha = 0.2f) else Slate850
                        ) {
                            Text(
                                text = "${favoriteIds.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFavSelected) GjallarCyan else Slate500,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ========================================================
        // 2. BARRA DE BÚSQUEDA
        // ========================================================
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar frase o sonido...", color = Slate500, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp))
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
                .height(48.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // ========================================================
        // 3. FILTRO DE CATEGORÍAS (SCROLL HORIZONTAL)
        // ========================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CanterosCatalog.categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) GjallarCyan else Slate900,
                    border = BorderStroke(1.dp, if (isSelected) GjallarCyan else Slate800),
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) Slate950 else Slate300,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Acceso sutil a banco de sonidos externo (MyInstants)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Slate900,
            border = BorderStroke(1.dp, Slate850),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.myinstants.com/es/index/es/")).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Public,
                        contentDescription = null,
                        tint = GjallarCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "¿Quieres más efectos? Explorar en MyInstants",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }
                Icon(
                    Icons.Filled.OpenInNew,
                    contentDescription = null,
                    tint = Slate600,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ========================================================
        // 4. ESTADO VACÍO (FAVORITOS O BÚSQUEDA SIN RESULTADOS)
        // ========================================================
        if (filteredSounds.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Slate900,
                        border = BorderStroke(1.dp, Slate800),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (selectedMainTab == SoundboardTab.FAVORITES) Icons.Outlined.FavoriteBorder else Icons.Filled.SearchOff,
                                contentDescription = null,
                                tint = if (selectedMainTab == SoundboardTab.FAVORITES) GjallarCyan else Slate500,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (selectedMainTab == SoundboardTab.FAVORITES) "Sin sonidos favoritos aún" else "No se encontraron frases",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (selectedMainTab == SoundboardTab.FAVORITES)
                            "Toca el corazón de hielo en cualquier sonido para guardarlo en tu lista rápida."
                        else "Prueba con otra palabra clave o selecciona 'Todos'.",
                        fontSize = 12.sp,
                        color = Slate400,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            // ========================================================
            // 5. REJILLA DE TARJETAS DISCRETAS E INTEGRADAS
            // ========================================================
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredSounds, key = { it.id }) { sound ->
                    val isPlaying = currentlyPlayingId == sound.id
                    val isFav = favoriteIds.contains(sound.id)

                    Card(
                        shape = RoundedCornerShape(12.dp),
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
                                .padding(10.dp)
                        ) {
                            // Fila superior: Chip de personaje + Favorito + Compartir
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (sound.character) {
                                        "Canteros" -> GjallarAmber.copy(alpha = 0.15f)
                                        "Homer" -> Color(0xFFFACC15).copy(alpha = 0.12f)
                                        "Flanders" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                        "Moe" -> Color(0xFF38BDF8).copy(alpha = 0.15f)
                                        "Nelson" -> Color(0xFFFB7185).copy(alpha = 0.15f)
                                        "Milhouse" -> Color(0xFF818CF8).copy(alpha = 0.15f)
                                        "Ralph" -> Color(0xFF60A5FA).copy(alpha = 0.12f)
                                        "Burns" -> Color(0xFFA78BFA).copy(alpha = 0.12f)
                                        else -> Slate800
                                    }
                                ) {
                                    Text(
                                        text = sound.character,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (sound.character) {
                                            "Canteros" -> GjallarAmber
                                            "Homer" -> Color(0xFFFACC15)
                                            "Flanders" -> Color(0xFF34D399)
                                            "Moe" -> GjallarCyan
                                            "Nelson" -> Color(0xFFFB7185)
                                            "Milhouse" -> Color(0xFFA5B4FC)
                                            "Ralph" -> Color(0xFF93C5FD)
                                            "Burns" -> Color(0xFFC4B5FD)
                                            else -> Slate300
                                        },
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Corazón de Gigante de Hielo (Favorito)
                                    IconButton(
                                        onClick = { toggleFavorite(sound.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = if (isFav) "Quitar favorito" else "Marcar favorito",
                                            tint = if (isFav) GjallarCyan else Slate600,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(2.dp))

                                    // Compartir en WhatsApp / Telegram
                                    IconButton(
                                        onClick = { audioPlayer.shareSound(sound) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Share,
                                            contentDescription = "Compartir audio",
                                            tint = Slate500,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Título
                            Text(
                                text = sound.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPlaying) GjallarCyan else Slate100,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 16.sp
                            )

                            if (sound.quote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = sound.quote,
                                    fontSize = 10.sp,
                                    color = Slate400,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Pie discreto: estado de reproducción
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isPlaying) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = GjallarCyan.copy(alpha = 0.2f),
                                        border = BorderStroke(0.5.dp, GjallarCyan)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.GraphicEq,
                                                contentDescription = null,
                                                tint = GjallarCyan,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "SONANDO",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = GjallarCyan
                                            )
                                        }
                                    }

                                    Icon(
                                        Icons.Filled.Stop,
                                        contentDescription = "Detener",
                                        tint = RoseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Tocar para oír",
                                        fontSize = 9.sp,
                                        color = Slate600
                                    )

                                    Surface(
                                        shape = CircleShape,
                                        color = Slate800,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                tint = Slate400,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

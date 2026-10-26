package com.heimdall.gjallaraudio.feature.soundboard

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import kotlinx.coroutines.launch

enum class SoundboardTab {
    ALL,
    FAVORITES,
    ONLINE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanterosScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioPlayer = remember { CanterosAudioPlayer(context) }
    val currentlyPlayingId by audioPlayer.currentlyPlayingId.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    // 1. Sonidos personalizados descargados por el usuario
    var customSounds by remember { mutableStateOf(CustomSoundsRepository.getCustomSounds(context)) }

    // 2. Persistencia de Favoritos
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

    // Búsqueda Online (MyInstants)
    var onlineSearchQuery by remember { mutableStateOf("") }
    var onlineResults by remember { mutableStateOf<List<MyInstantsResult>>(emptyList()) }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var downloadingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var onlinePage by remember { mutableStateOf(1) }
    var hasMoreOnline by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var onlineTotalCount by remember { mutableStateOf(0) }

    // Cargar lote amplio de sonidos al abrir tab online (30 sonidos iniciales)
    LaunchedEffect(selectedMainTab) {
        if (selectedMainTab == SoundboardTab.ONLINE && onlineResults.isEmpty()) {
            isSearchingOnline = true
            val page = MyInstantsClient.fetchInitialBatch("", pagesCount = 3)
            onlineResults = page.results
            onlinePage = page.nextPage ?: 4
            hasMoreOnline = page.hasNext
            onlineTotalCount = page.totalCount
            isSearchingOnline = false
        }
    }

    // Lista combinada de sonidos locales (Built-in + Custom descargados)
    val allLocalSounds = remember(customSounds) {
        customSounds + CanterosCatalog.sounds
    }

    // Índice HashSet O(1) para verificar al instante qué sonidos están ya descargados
    val downloadedNamesSet = remember(allLocalSounds) {
        allLocalSounds.map { it.title.trim().lowercase() }.toSet()
    }

    val filteredSounds = remember(selectedMainTab, searchQuery, selectedCategory, favoriteIds, allLocalSounds) {
        allLocalSounds.filter { sound ->
            val matchesTab = when (selectedMainTab) {
                SoundboardTab.ALL -> true
                SoundboardTab.FAVORITES -> favoriteIds.contains(sound.id)
                SoundboardTab.ONLINE -> false
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
        // 1. SELECTOR SUPERIOR: [ Aullidos | Favoritos | 🌐 Online ]
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
                // Tab: AULLIDOS LOCALES
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
                            text = "ᛟ Locales",
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isAllSelected) GjallarCyan else Slate400
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isAllSelected) GjallarCyan.copy(alpha = 0.2f) else Slate850
                        ) {
                            Text(
                                text = "${allLocalSounds.size}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAllSelected) GjallarCyan else Slate500,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Tab: FAVORITOS
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
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Favoritos",
                            fontWeight = if (isFavSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isFavSelected) GjallarCyan else Slate400
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isFavSelected) GjallarCyan.copy(alpha = 0.2f) else Slate850
                        ) {
                            Text(
                                text = "${favoriteIds.size}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFavSelected) GjallarCyan else Slate500,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Tab: ONLINE (MYINSTANTS)
                val isOnlineSelected = selectedMainTab == SoundboardTab.ONLINE
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (isOnlineSelected) Slate800 else Color.Transparent,
                    border = BorderStroke(
                        width = if (isOnlineSelected) 1.dp else 0.dp,
                        color = if (isOnlineSelected) GjallarAmber.copy(alpha = 0.6f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedMainTab = SoundboardTab.ONLINE }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Public,
                            contentDescription = null,
                            tint = if (isOnlineSelected) GjallarAmber else Slate500,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Online",
                            fontWeight = if (isOnlineSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isOnlineSelected) GjallarAmber else Slate400
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ========================================================
        // MODO 1: EXPLORADOR ONLINE (DESCARGAR AL LOCAL)
        // ========================================================
        if (selectedMainTab == SoundboardTab.ONLINE) {
            OutlinedTextField(
                value = onlineSearchQuery,
                onValueChange = { onlineSearchQuery = it },
                placeholder = { Text("Buscar en millones de sonidos...", color = Slate500, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = GjallarAmber, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onlineSearchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                onlineSearchQuery = ""
                                coroutineScope.launch {
                                    isSearchingOnline = true
                                    val page = MyInstantsClient.fetchInitialBatch("", 3)
                                    onlineResults = page.results
                                    onlinePage = page.nextPage ?: 4
                                    hasMoreOnline = page.hasNext
                                    onlineTotalCount = page.totalCount
                                    isSearchingOnline = false
                                }
                            }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Borrar", tint = Slate400, modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(onClick = {
                            coroutineScope.launch {
                                isSearchingOnline = true
                                val page = MyInstantsClient.fetchInitialBatch(onlineSearchQuery, 3)
                                onlineResults = page.results
                                onlinePage = page.nextPage ?: 4
                                hasMoreOnline = page.hasNext
                                onlineTotalCount = page.totalCount
                                isSearchingOnline = false
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Buscar", tint = GjallarAmber, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900,
                    focusedBorderColor = GjallarAmber,
                    unfocusedBorderColor = Slate800,
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate100
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Chips rápidos de búsqueda y categorías para explorar el inmenso catálogo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "🔥 Populares" to "",
                    "🇪🇸 En Español" to "español",
                    "😂 Memes" to "meme",
                    "📢 Simpsons" to "simpson",
                    "🎮 Juegos" to "game",
                    "💥 Efectos" to "sound",
                    "🎬 Cine" to "movie",
                    "🥋 Anime" to "anime",
                    "👏 Aplausos" to "aplausos",
                    "🔊 Risas" to "risas",
                    "💀 Bruh" to "bruh"
                ).forEach { (label, q) ->
                    val isSelected = (q.isEmpty() && onlineSearchQuery.isEmpty()) || (q.isNotEmpty() && onlineSearchQuery == q)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Slate800 else Slate900,
                        border = BorderStroke(1.dp, if (isSelected) GjallarAmber else Slate800),
                        modifier = Modifier.clickable {
                            onlineSearchQuery = q
                            coroutineScope.launch {
                                isSearchingOnline = true
                                val page = MyInstantsClient.fetchInitialBatch(q, 3)
                                onlineResults = page.results
                                onlinePage = page.nextPage ?: 4
                                hasMoreOnline = page.hasNext
                                onlineTotalCount = page.totalCount
                                isSearchingOnline = false
                            }
                        }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (isSelected) GjallarAmber else Slate300,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isSearchingOnline) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GjallarAmber, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                }
            } else if (onlineResults.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No se encontraron sonidos online", color = Slate400, fontSize = 13.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(onlineResults, key = { it.slug + it.soundUrl }) { item ->
                        val isPlaying = currentlyPlayingId == item.slug
                        val isDownloaded = downloadedNamesSet.contains(item.name.trim().lowercase())
                        val isDownloading = downloadingIds.contains(item.slug)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isPlaying) Slate850 else Slate900),
                            border = BorderStroke(1.dp, if (isPlaying) GjallarCyan else Slate800),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isPlaying) {
                                        audioPlayer.stop()
                                    } else {
                                        audioPlayer.playUrl(item.soundUrl, item.slug)
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = GjallarAmber.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "MyInstants",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GjallarAmber,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }

                                    Icon(
                                        if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isPlaying) GjallarCyan else Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = item.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPlaying) GjallarCyan else Slate100,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Botón: Añadir a mi colección local
                                Button(
                                    onClick = {
                                        if (!isDownloaded && !isDownloading) {
                                            downloadingIds = downloadingIds + item.slug
                                            coroutineScope.launch {
                                                val res = CustomSoundsRepository.downloadAndAddSound(
                                                    context = context,
                                                    name = item.name,
                                                    soundUrl = item.soundUrl
                                                )
                                                downloadingIds = downloadingIds - item.slug
                                                if (res.isSuccess) {
                                                    customSounds = CustomSoundsRepository.getCustomSounds(context)
                                                    Toast.makeText(context, "¡Añadido a tus Aullidos locales!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Error al descargar", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDownloaded) Color(0xFF064E3B) else Slate800,
                                        contentColor = if (isDownloaded) Color(0xFF34D399) else Slate100
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth().height(30.dp)
                                ) {
                                    if (isDownloading) {
                                        CircularProgressIndicator(color = GjallarCyan, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                    } else {
                                        Icon(
                                            if (isDownloaded) Icons.Filled.Check else Icons.Filled.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isDownloaded) "En tu Local" else "+ Añadir",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (hasMoreOnline) {
                        item(span = { GridItemSpan(2) }) {
                            Button(
                                onClick = {
                                    if (!isLoadingMore && hasMoreOnline) {
                                        isLoadingMore = true
                                        coroutineScope.launch {
                                            val page = MyInstantsClient.fetchPage(onlineSearchQuery, onlinePage)
                                            onlineResults = (onlineResults + page.results).distinctBy { it.slug + it.soundUrl }
                                            onlinePage = page.nextPage ?: (onlinePage + 1)
                                            hasMoreOnline = page.hasNext
                                            isLoadingMore = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Slate850),
                                border = BorderStroke(1.dp, GjallarAmber),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                                    .height(46.dp)
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(color = GjallarAmber, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cargando catálogo...", color = GjallarAmber, fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = GjallarAmber, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cargar más sonidos (${onlineResults.size} mostrados)",
                                        color = Slate100,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ========================================================
            // MODO 2: SONIDOS LOCALES / FAVORITOS
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

            Spacer(modifier = Modifier.height(8.dp))

            // Filtro de categorías horizontal
            val allCategories = remember(customSounds) {
                if (customSounds.isNotEmpty()) listOf("Todos", "Descargas") + CanterosCatalog.categories.filter { it != "Todos" }
                else CanterosCatalog.categories
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allCategories.forEach { cat ->
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

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredSounds.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
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
                            text = if (selectedMainTab == SoundboardTab.FAVORITES) "Sin favoritos aún" else "No se encontraron frases",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedMainTab == SoundboardTab.FAVORITES)
                                "Toca el corazón de hielo en cualquier sonido para guardarlo en tu lista rápida."
                            else "Prueba con otra palabra clave o busca en la pestaña 'Online'.",
                            fontSize = 12.sp,
                            color = Slate400,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
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
                        val isCustom = sound.id.startsWith("custom_")

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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isCustom) GjallarCyan.copy(alpha = 0.15f) else when (sound.character) {
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
                                            color = if (isCustom) GjallarCyan else when (sound.character) {
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

                                        // Si es descargado por el usuario, permitir borrarlo
                                        if (isCustom) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                            IconButton(
                                                onClick = {
                                                    CustomSoundsRepository.deleteSound(context, sound.id)
                                                    customSounds = CustomSoundsRepository.getCustomSounds(context)
                                                },
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.DeleteOutline,
                                                    contentDescription = "Eliminar de local",
                                                    tint = Slate600,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

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
                                                    text = "AULLANDO",
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
                                            text = if (isCustom) "Descargado" else "Tocar para oír",
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
}

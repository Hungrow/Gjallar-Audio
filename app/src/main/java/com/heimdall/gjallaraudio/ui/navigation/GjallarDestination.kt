package com.heimdall.gjallaraudio.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.ui.graphics.vector.ImageVector

enum class GjallarDestination(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val navLabel: String
) {
    NIGHT(
        route = "night",
        title = "Gjallar Night",
        subtitle = "Descanso & Ciclos 90m",
        icon = Icons.Filled.NightsStay,
        navLabel = "Noche"
    ),
    FORGE(
        route = "forge",
        title = "Gjallar Forge",
        subtitle = "Video a Audio MP3/WAV",
        icon = Icons.Filled.VideoSettings,
        navLabel = "Forge"
    ),
    CANTEROS(
        route = "canteros",
        title = "Gjallar Aullidos",
        subtitle = "Botonera de Frases y Sonidos",
        icon = Icons.Filled.Campaign,
        navLabel = "Aullidos"
    ),
    VOICE(
        route = "voice",
        title = "Gjallar Notes",
        subtitle = "Bloc de Notas & Transcriptor",
        icon = Icons.Filled.Description,
        navLabel = "Notas"
    ),
    LAB(
        route = "lab",
        title = "Gjallar Lab",
        subtitle = "Estudio & IA Vocal",
        icon = Icons.Filled.GraphicEq,
        navLabel = "Lab"
    )
}

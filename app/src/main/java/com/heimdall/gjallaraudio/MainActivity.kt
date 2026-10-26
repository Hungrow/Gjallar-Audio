package com.heimdall.gjallaraudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heimdall.gjallaraudio.feature.forge.ForgeScreen
import com.heimdall.gjallaraudio.feature.lab.LabScreen
import com.heimdall.gjallaraudio.feature.night.NightScreen
import com.heimdall.gjallaraudio.feature.night.SleepTimerService
import com.heimdall.gjallaraudio.feature.soundboard.CanterosScreen
import com.heimdall.gjallaraudio.feature.voice.VoiceScreen
import com.heimdall.gjallaraudio.ui.menu.GjallarMenuSheet
import com.heimdall.gjallaraudio.ui.navigation.GjallarDestination
import com.heimdall.gjallaraudio.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Limpieza automática del instalador de actualización temporal (0 MB en almacenamiento)
        try {
            val updateFile = java.io.File(cacheDir, "gjallar_update.apk")
            if (updateFile.exists()) {
                updateFile.delete()
            }
        } catch (_: Exception) {}

        enableEdgeToEdge()
        setContent {
            GjallarAudioTheme {
                MainAppScaffold()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold() {
    var currentDestination by remember { mutableStateOf(GjallarDestination.NIGHT) }
    val isTimerRunning by SleepTimerService.isRunning.collectAsState()
    var showMenuSheet by remember { mutableStateOf(false) }

    if (showMenuSheet) {
        GjallarMenuSheet(onDismissRequest = { showMenuSheet = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showMenuSheet = true }
                            .padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate850,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GjallarCyan.copy(alpha = 0.7f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.GraphicEq,
                                    contentDescription = "Abrir Menú Gjallar",
                                    tint = GjallarCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GJALLAR",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    letterSpacing = 1.sp,
                                    color = Slate50
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "HORN",
                                    fontWeight = FontWeight.Light,
                                    fontSize = 18.sp,
                                    letterSpacing = 1.sp,
                                    color = GjallarCyan
                                )
                            }
                            Text(
                                text = "ECOSISTEMA HEIMDALL • MENÚ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = Slate400
                            )
                        }
                    }
                },
                actions = {
                    if (isTimerRunning) {
                        Surface(
                            color = GjallarCyan.copy(alpha = 0.15f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, GjallarCyan),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(GjallarCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VIGILIA",
                                    color = GjallarCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate900,
                    titleContentColor = Slate50
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Slate950,
                tonalElevation = 8.dp,
                modifier = Modifier.border(
                    width = 1.dp,
                    color = Slate800,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
            ) {
                GjallarDestination.values().forEach { destination ->
                    val selected = currentDestination == destination
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = destination.title,
                                tint = if (selected) GjallarCyan else Slate500
                            )
                        },
                        label = {
                            Text(
                                text = destination.navLabel,
                                color = if (selected) GjallarCyan else Slate500,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Slate800
                        )
                    )
                }
            }
        },
        containerColor = Slate900
    ) { innerPadding ->
        Crossfade(
            targetState = currentDestination,
            modifier = Modifier.padding(innerPadding),
            label = "ScreenCrossfade"
        ) { destination ->
            when (destination) {
                GjallarDestination.NIGHT -> NightScreen()
                GjallarDestination.FORGE -> ForgeScreen()
                GjallarDestination.CANTEROS -> CanterosScreen()
                GjallarDestination.VOICE -> VoiceScreen()
                GjallarDestination.LAB -> LabScreen()
            }
        }
    }
}

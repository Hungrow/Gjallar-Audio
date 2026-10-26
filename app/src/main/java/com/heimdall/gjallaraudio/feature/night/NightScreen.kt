package com.heimdall.gjallaraudio.feature.night

import android.Manifest
import android.app.TimePickerDialog
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.AlarmClock
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heimdall.gjallaraudio.ui.theme.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class NightSubTab(val title: String) {
    CYCLES("Ciclos 90 min"),
    TIMER("Temporizador"),
    SOUNDS("Ambientes")
}

@Composable
fun NightScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(NightSubTab.CYCLES) }
    val isRunning by SleepTimerService.isRunning.collectAsState()
    val remainingSeconds by SleepTimerService.remainingSeconds.collectAsState()

    // Motor de Sonidos Ambientes Nórdicos con grabaciones orgánicas en bucle
    val ambientEngine = remember { AmbientSoundEngine(context) }
    DisposableEffect(Unit) {
        onDispose {
            ambientEngine.stopAll()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Tab Selector (Heimdall Style Tabs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate850, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NightSubTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
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
                        .clickable { selectedTab = tab }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        when (selectedTab) {
            NightSubTab.CYCLES -> SleepCyclesSection()
            NightSubTab.TIMER -> SleepTimerSection(isRunning = isRunning, remainingSeconds = remainingSeconds)
            NightSubTab.SOUNDS -> SoundscapesSection(ambientEngine)
        }
    }
}

// =========================================================================
// 1. CALCULADORA DE CICLOS DE SUEÑO (90 MINUTOS) & ALARMA NATIVA
// =========================================================================

@Composable
private fun SleepCyclesSection() {
    val context = LocalContext.current
    var calculateForWakeUp by remember { mutableStateOf(false) }
    var targetWakeTime by remember { mutableStateOf(LocalTime.of(7, 0)) }

    val suggestions = remember(calculateForWakeUp, targetWakeTime) {
        if (calculateForWakeUp) {
            SleepCycleCalculator.calculateBedtimesForWakeUp(targetWakeTime)
        } else {
            SleepCycleCalculator.calculateWakeTimesForSleepNow()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Calculadora de Ciclos REM (90m)",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Slate50
                    )
                    Text(
                        text = "Despertar al final de un ciclo evita la inercia del sueño y te hace despertar lleno de energía.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !calculateForWakeUp,
                            onClick = { calculateForWakeUp = false },
                            label = { Text("Si me duermo ahora") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GjallarCyan.copy(alpha = 0.15f),
                                selectedLabelColor = GjallarCyan,
                                containerColor = Slate850,
                                labelColor = Slate300
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = !calculateForWakeUp,
                                borderColor = if (!calculateForWakeUp) GjallarCyan else Slate700
                            )
                        )

                        FilterChip(
                            selected = calculateForWakeUp,
                            onClick = { calculateForWakeUp = true },
                            label = { Text("Fijar hora de alarma") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GjallarCyan.copy(alpha = 0.15f),
                                selectedLabelColor = GjallarCyan,
                                containerColor = Slate850,
                                labelColor = Slate300
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = calculateForWakeUp,
                                borderColor = if (calculateForWakeUp) GjallarCyan else Slate700
                            )
                        )
                    }

                    if (calculateForWakeUp) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate850, RoundedCornerShape(12.dp))
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            targetWakeTime = LocalTime.of(hour, minute)
                                        },
                                        targetWakeTime.hour,
                                        targetWakeTime.minute,
                                        true
                                    ).show()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Text("Hora deseada de despertar:", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    targetWakeTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = GjallarAmber
                                )
                            }
                            Icon(Icons.Filled.AccessTime, contentDescription = null, tint = GjallarAmber)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = if (calculateForWakeUp) "Horas óptimas para irte a dormir:" else "Horas ideales para poner tu alarma (Toca el icono para programarla):",
                style = MaterialTheme.typography.titleMedium,
                color = Slate300,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }

        items(suggestions) { item ->
            CycleSuggestionCard(suggestion = item) {
                // Programar Alarma Nativa en 1 Toque
                try {
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, item.time.hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, item.time.minute)
                        putExtra(AlarmClock.EXTRA_MESSAGE, "Gjallar Night — ${item.label}")
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Configurando alarma para las ${item.formattedTime}", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "No se encontró app de reloj para configurar la alarma", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
private fun CycleSuggestionCard(
    suggestion: SleepCycleSuggestion,
    onSetAlarm: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (suggestion.isRecommended) GjallarCyan.copy(alpha = 0.6f) else Slate700
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = suggestion.formattedTime,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (suggestion.isRecommended) GjallarCyan else Slate50
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    if (suggestion.isRecommended) {
                        Surface(
                            color = GjallarCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GjallarCyan)
                        ) {
                            Text(
                                text = "RECOMENDADO",
                                color = GjallarCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${suggestion.label} • ${suggestion.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Botón de Alarma Nativa
            IconButton(
                onClick = onSetAlarm,
                modifier = Modifier
                    .size(46.dp)
                    .background(Slate850, CircleShape)
                    .border(1.dp, if (suggestion.isRecommended) GjallarCyan else Slate700, CircleShape)
            ) {
                Icon(
                    Icons.Filled.AlarmAdd,
                    contentDescription = "Poner Alarma en 1 Toque",
                    tint = if (suggestion.isRecommended) GjallarCyan else Slate300
                )
            }
        }
    }
}

// =========================================================================
// 2. TEMPORIZADOR DE APAGADO (SLEEP TIMER PRO)
// =========================================================================

@Composable
private fun SleepTimerSection(
    isRunning: Boolean,
    remainingSeconds: Long
) {
    val context = LocalContext.current
    val isFadingOut by SleepTimerService.isFadingOut.collectAsState()

    var selectedMinutes by remember { mutableStateOf(30L) }
    var fadeOutEnabled by remember { mutableStateOf(true) }
    var shakeToExtend by remember { mutableStateOf(true) }
    var smartSleepDetection by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()

    val devicePolicyManager = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { GjallarDeviceAdminReceiver.getComponentName(context) }
    var isDeviceAdminActive by remember { mutableStateOf(devicePolicyManager.isAdminActive(adminComponent)) }
    var turnOffScreen by remember { mutableStateOf(true) }

    val adminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDeviceAdminActive = devicePolicyManager.isAdminActive(adminComponent)
    }

    val mediaSessionInfo by MediaNotificationListener.currentSession.collectAsState()
    var streamingState by remember { mutableStateOf(StreamingAppDetector.detectPlaybackState(context)) }

    var selectedAppPkg by remember {
        mutableStateOf(
            StreamingAppDetector.getPreferredApp(context)
                ?: streamingState.detectedAppPackage
                ?: "app.revanced.android.youtube"
        )
    }

    val startTimerAction = {
        val intent = Intent(context, SleepTimerService::class.java).apply {
            action = SleepTimerService.ACTION_START
            putExtra(SleepTimerService.EXTRA_DURATION_SECONDS, selectedMinutes * 60)
            putExtra(SleepTimerService.EXTRA_FADE_OUT_MINUTES, if (fadeOutEnabled) 2L else 0L)
            putExtra(SleepTimerService.EXTRA_SHAKE_TO_EXTEND, shakeToExtend)
            putExtra(SleepTimerService.EXTRA_SMART_SLEEP_DETECTION, smartSleepDetection)
            putExtra(SleepTimerService.EXTRA_TURN_OFF_SCREEN, turnOffScreen)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        Toast.makeText(context, "Temporizador de $selectedMinutes min activado", Toast.LENGTH_SHORT).show()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        startTimerAction()
    }

    LaunchedEffect(Unit) {
        while (true) {
            streamingState = StreamingAppDetector.detectPlaybackState(context)
            kotlinx.coroutines.delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TARJETA PRINCIPAL DEL CONTADOR
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(22.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isRunning) GjallarCyan else Slate800
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isRunning && isFadingOut) GjallarAmber.copy(alpha = 0.15f)
                            else if (isRunning) GjallarCyan.copy(alpha = 0.15f)
                            else Slate800,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isRunning && isFadingOut) GjallarAmber
                        else if (isRunning) GjallarCyan
                        else Slate700
                    )
                ) {
                    Text(
                        text = if (isRunning && isFadingOut) "⚡ ATENUACIÓN SUAVE EN CURSO"
                               else if (isRunning) "ᛉ GUARDIÁN DE SUEÑO ACTIVO"
                               else "ᛟ DURACIÓN DEL TEMPORIZADOR",
                        fontSize = 11.sp,
                        color = if (isRunning && isFadingOut) GjallarAmber else if (isRunning) GjallarCyan else Slate300,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                val displaySeconds = if (isRunning) remainingSeconds else selectedMinutes * 60
                val hours = displaySeconds / 3600
                val min = (displaySeconds % 3600) / 60
                val sec = displaySeconds % 60

                val timeFormatted = if (hours > 0) {
                    String.format("%02d:%02d:%02d", hours, min, sec)
                } else {
                    String.format("%02d:%02d", min, sec)
                }

                Text(
                    text = if (isRunning && isFadingOut) "00:${String.format("%02d", sec)}" else timeFormatted,
                    fontSize = if (isRunning && isFadingOut) 52.sp else 58.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = if (isRunning && isFadingOut) GjallarAmber else if (isRunning) GjallarCyan else Slate50,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Text(
                    text = if (isRunning && isFadingOut) "Atenuando volumen al silencio (1 min). Tu volumen original quedará intacto."
                           else if (isRunning) "La música se atenuará y apagará al agotarse el tiempo"
                           else "${selectedMinutes} minutos programados",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRunning && isFadingOut) GjallarAmber else Slate400,
                    textAlign = TextAlign.Center
                )

                if (!isRunning) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones de ajuste fino (+/-)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { selectedMinutes = (selectedMinutes - 10).coerceAtLeast(1L) },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Slate850),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("-10m", color = Slate300, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        OutlinedButton(
                            onClick = { selectedMinutes = (selectedMinutes - 1).coerceAtLeast(1L) },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Slate850),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("-1m", color = Slate300, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedButton(
                            onClick = { selectedMinutes = (selectedMinutes + 1).coerceAtMost(240L) },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GjallarCyan.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Slate850),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("+1m", color = GjallarCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        OutlinedButton(
                            onClick = { selectedMinutes = (selectedMinutes + 10).coerceAtMost(240L) },
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GjallarCyan.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Slate850),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("+10m", color = GjallarCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Slider interactivo (1 a 180 min)
                    Slider(
                        value = selectedMinutes.toFloat(),
                        onValueChange = { selectedMinutes = it.toLong().coerceIn(1L, 240L) },
                        valueRange = 1f..180f,
                        colors = SliderDefaults.colors(
                            thumbColor = GjallarCyan,
                            activeTrackColor = GjallarCyan,
                            inactiveTrackColor = Slate800
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1m", fontSize = 11.sp, color = Slate500)
                        Text("30m", fontSize = 11.sp, color = Slate500)
                        Text("1 hora", fontSize = 11.sp, color = Slate500)
                        Text("2 horas", fontSize = 11.sp, color = Slate500)
                        Text("3 horas", fontSize = 11.sp, color = Slate500)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Accesos rápidos (Chips)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5L, 15L, 30L, 45L, 60L, 90L).forEach { mins ->
                            val isSelected = selectedMinutes == mins
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) GjallarCyan else Slate850,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) GjallarCyan else Slate700
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedMinutes = mins }
                            ) {
                                Text(
                                    text = "${mins}m",
                                    color = if (isSelected) Slate950 else Slate300,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // BOTÓN PRINCIPAL DE INICIO / DETENER
        // ==========================================
        if (!isRunning) {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        startTimerAction()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GjallarCyan,
                    contentColor = Slate950
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "INICIAR TEMPORIZADOR (${selectedMinutes} MIN)",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 0.5.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val intent = Intent(context, SleepTimerService::class.java).apply {
                            action = SleepTimerService.ACTION_STOP
                        }
                        context.startService(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoseRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.8f)
                        .height(56.dp)
                ) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DETENER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, SleepTimerService::class.java).apply {
                            action = SleepTimerService.ACTION_EXTEND
                        }
                        context.startService(intent)
                    },
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GjallarCyan),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Slate900),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(56.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = GjallarCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+10 MIN",
                        fontWeight = FontWeight.Bold,
                        color = GjallarCyan,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // ==========================================
        // DETECTOR DE STREAMING Y ACCESO RÁPIDO (YOUTUBE REVANCED, SPOTIFY, ETC.)
        // ==========================================
        val allApps = streamingState.installedStreamingApps
        val currentApp = allApps.firstOrNull { it.packageName == selectedAppPkg }
            ?: allApps.firstOrNull()
            ?: InstalledStreamingApp("app.revanced.android.youtube", "YouTube ReVanced")
        val targetPkg = currentApp.packageName
        val targetName = currentApp.name

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (streamingState.isMusicActive) GjallarCyan else Slate800
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = if (streamingState.isMusicActive) GjallarCyan else Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (streamingState.isMusicActive) "REPRODUCIENDO AHORA" else "APP DE STREAMING PREFERIDA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (streamingState.isMusicActive) GjallarCyan else Slate400,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (streamingState.isMusicActive) {
                        Surface(
                            color = GjallarCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "EN CURSO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GjallarCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (streamingState.trackTitle != null) {
                    Text(
                        text = streamingState.trackTitle!!,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate100,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${streamingState.trackArtist ?: ""} • $targetName",
                        fontSize = 12.sp,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Selector de apps con Chips desplazables
                Text(
                    text = "Selecciona la app para volver tras iniciar:",
                    fontSize = 11.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allApps) { app ->
                        val isSelected = app.packageName == targetPkg
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) GjallarCyan.copy(alpha = 0.15f) else Slate850,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) GjallarCyan else Slate700
                            ),
                            modifier = Modifier.clickable {
                                selectedAppPkg = app.packageName
                                StreamingAppDetector.setPreferredApp(context, app.packageName)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = GjallarCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = app.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) GjallarCyan else Slate300
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón para volver a la app elegida (ej. YouTube ReVanced)
                val isSpotify = targetPkg.contains("spotify", ignoreCase = true)
                val isYouTube = targetPkg.contains("youtube", ignoreCase = true) || targetPkg.contains("revanced", ignoreCase = true)
                Button(
                    onClick = {
                        StreamingAppDetector.launchApp(context, targetPkg)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSpotify) Color(0xFF1DB954) else if (isYouTube) Color(0xFFCC0000) else GjallarAmber,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VOLVER A ${targetName.uppercase()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (!streamingState.isNotificationAccessGranted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ℹ️ Conceder permiso para ver el título exacto de la canción que suena",
                        fontSize = 10.sp,
                        color = Slate500,
                        modifier = Modifier.clickable {
                            StreamingAppDetector.openNotificationAccessSettings(context)
                        }
                    )
                }
            }
        }

        // ==========================================
        // OPCIONES INTELIGENTES (AVANZADAS)
        // ==========================================
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate900),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CONFIGURACIÓN INTELIGENTE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 1. Atenuación Suave Virtual
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Atenuación suave virtual (1 minuto)", style = MaterialTheme.typography.titleMedium, color = Slate100)
                        Text("Al llegar a 0, baja suavemente el volumen durante 1 min y pausa el audio, manteniendo tu volumen intacto", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                    Switch(
                        checked = fadeOutEnabled,
                        onCheckedChange = { fadeOutEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GjallarCyan, checkedTrackColor = Slate850)
                    )
                }

                HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 12.dp))

                // 2. Apagado de Pantalla (Sleep Timer)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Apagar pantalla (Estilo Sleep Timer)", style = MaterialTheme.typography.titleMedium, color = Slate100)
                        Text(
                            text = if (isDeviceAdminActive) "Apagado y bloqueo inmediato de pantalla al terminar"
                                   else "Requiere permiso de Administrador para apagar la pantalla",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDeviceAdminActive) Slate400 else GjallarAmber
                        )
                    }
                    Switch(
                        checked = turnOffScreen,
                        onCheckedChange = { checked ->
                            turnOffScreen = checked
                            if (checked && !isDeviceAdminActive) {
                                try {
                                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Gjallarhorn requiere este permiso exclusivamente para apagar y bloquear la pantalla al finalizar el temporizador de sueño.")
                                    }
                                    adminLauncher.launch(intent)
                                } catch (_: Exception) {}
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = GjallarCyan, checkedTrackColor = Slate850)
                    )
                }

                if (turnOffScreen && !isDeviceAdminActive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Gjallarhorn requiere este permiso exclusivamente para apagar y bloquear la pantalla al finalizar el temporizador de sueño.")
                                }
                                adminLauncher.launch(intent)
                            } catch (_: Exception) {}
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GjallarAmber),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Activar permiso de Administrador para apagar", color = GjallarAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 12.dp))

                // 3. Agitar para extender
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Agitar para extender (+10 min)", style = MaterialTheme.typography.titleMedium, color = Slate100)
                        Text("Si aún estás despierto, un leve movimiento añade tiempo", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                    Switch(
                        checked = shakeToExtend,
                        onCheckedChange = { shakeToExtend = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GjallarCyan, checkedTrackColor = Slate850)
                    )
                }

                HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 12.dp))

                // 4. Detección Inteligente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Detección de Sueño Inteligente", style = MaterialTheme.typography.titleMedium, color = Slate100)
                        Text("Inicia el apagado suave si detecta reposo continuo sin esperar al fin", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                    Switch(
                        checked = smartSleepDetection,
                        onCheckedChange = { smartSleepDetection = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GjallarAmber, checkedTrackColor = Slate850)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// =========================================================================
// 3. AMBIENTES NÓRDICOS CON SÍNTESIS ACÚSTICA EN TIEMPO REAL
// =========================================================================

data class SoundscapeItem(
    val title: String,
    val subtitle: String,
    val type: SoundscapeType,
    val icon: ImageVector
)

@Composable
private fun SoundscapesSection(ambientEngine: AmbientSoundEngine) {
    val soundscapes = remember {
        listOf(
            SoundscapeItem("Lluvia en Asgard", "Lluvia real suave y relajante", SoundscapeType.RAIN, Icons.Filled.WaterDrop),
            SoundscapeItem("Viento de Midgard", "Viento real continuo entre las montañas", SoundscapeType.WIND, Icons.Filled.Air),
            SoundscapeItem("Hoguera Rúnica", "Chasquidos de leña crepitando en chimenea", SoundscapeType.FIRE, Icons.Filled.LocalFireDepartment),
            SoundscapeItem("Noche Nórdica", "Brisa nocturna y grillos serenos", SoundscapeType.DELTA, Icons.Filled.NightsStay)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Mezclador de Ambientes Nórdicos",
                style = MaterialTheme.typography.headlineSmall,
                color = Slate50
            )
            Text(
                text = "Desliza para encender y mezclar sonidos en tiempo real sin consumir internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        items(soundscapes) { item ->
            var volume by remember { mutableStateOf(0f) }

            Card(
                colors = CardDefaults.cardColors(containerColor = Slate800),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (volume > 0f) GjallarCyan else Slate700
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (volume > 0f) GjallarCyan.copy(alpha = 0.2f) else Slate850,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (volume > 0f) GjallarCyan else Slate700),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        item.icon,
                                        contentDescription = null,
                                        tint = if (volume > 0f) GjallarCyan else Slate400,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(item.title, style = MaterialTheme.typography.titleMedium, color = Slate100)
                                Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = Slate400)
                            }
                        }
                        Text(
                            text = "${(volume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (volume > 0f) GjallarCyan else Slate500
                        )
                    }

                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            ambientEngine.setVolume(item.type, it)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = GjallarCyan,
                            activeTrackColor = GjallarCyan,
                            inactiveTrackColor = Slate700
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

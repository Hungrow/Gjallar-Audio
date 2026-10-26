package com.heimdall.gjallaraudio.feature.night

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var selectedMinutes by remember { mutableStateOf(30L) }
    var fadeOutEnabled by remember { mutableStateOf(true) }
    var shakeToExtend by remember { mutableStateOf(true) }
    var smartSleepDetection by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate800),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isRunning) GjallarCyan else Slate700
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRunning) "GUARDIÁN DE SUEÑO ACTIVO" else "TIEMPO SELECCIONADO",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRunning) GjallarCyan else Slate400,
                    fontWeight = FontWeight.Bold
                )

                val displaySeconds = if (isRunning) remainingSeconds else selectedMinutes * 60
                val min = displaySeconds / 60
                val sec = displaySeconds % 60
                Text(
                    text = String.format("%02d:%02d", min, sec),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = if (isRunning) GjallarCyan else Slate50,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Text(
                    text = if (isRunning) "Pausará la música y atenuará el volumen al finalizar"
                           else "Elige los minutos antes de dormir",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (!isRunning) {
            Text(
                text = "Accesos Rápidos:",
                style = MaterialTheme.typography.titleSmall,
                color = Slate300,
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15L, 30L, 45L, 60L, 90L).forEach { mins ->
                    val isSelected = selectedMinutes == mins
                    Button(
                        onClick = { selectedMinutes = mins },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) GjallarCyan else Slate850,
                            contentColor = if (isSelected) Slate950 else Slate300
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GjallarCyan else Slate700),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(
                            text = "${mins}m",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Slate800),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Atenuación suave (Fade-out)", style = MaterialTheme.typography.titleMedium, color = Slate100)
                        Text("Baja el volumen progresivamente en los últimos 2 minutos", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                    Switch(
                        checked = fadeOutEnabled,
                        onCheckedChange = { fadeOutEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = GjallarCyan, checkedTrackColor = Slate850)
                    )
                }

                HorizontalDivider(color = Slate700, modifier = Modifier.padding(vertical = 12.dp))

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
                HorizontalDivider(color = Slate700, modifier = Modifier.padding(vertical = 12.dp))

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

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val intent = Intent(context, SleepTimerService::class.java).apply {
                    if (isRunning) {
                        action = SleepTimerService.ACTION_STOP
                    } else {
                        action = SleepTimerService.ACTION_START
                        putExtra(SleepTimerService.EXTRA_DURATION_SECONDS, selectedMinutes * 60)
                        putExtra(SleepTimerService.EXTRA_FADE_OUT_MINUTES, if (fadeOutEnabled) 2L else 0L)
                        putExtra(SleepTimerService.EXTRA_SHAKE_TO_EXTEND, shakeToExtend)
                        putExtra(SleepTimerService.EXTRA_SMART_SLEEP_DETECTION, smartSleepDetection)
                    }
                }
                context.startService(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) RoseRed else GjallarCyan,
                contentColor = if (isRunning) Color.White else Slate950
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRunning) "DETENER TEMPORIZADOR" else "ACTIVAR SLEEP TIMER",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
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

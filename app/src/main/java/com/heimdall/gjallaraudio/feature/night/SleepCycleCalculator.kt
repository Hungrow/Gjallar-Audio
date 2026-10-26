package com.heimdall.gjallaraudio.feature.night

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SleepCycleSuggestion(
    val time: LocalTime,
    val cyclesCount: Int,
    val totalSleepHours: Double,
    val isRecommended: Boolean,
    val label: String,
    val description: String
) {
    val formattedTime: String
        get() = time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))
}

object SleepCycleCalculator {
    private const val FALL_ASLEEP_TIME_MINUTES = 14L
    private const val CYCLE_DURATION_MINUTES = 90L

    /**
     * Calcula a qué horas deberías acostarte si necesitas despertar a una hora determinada.
     */
    fun calculateBedtimesForWakeUp(wakeTime: LocalTime): List<SleepCycleSuggestion> {
        val suggestions = mutableListOf<SleepCycleSuggestion>()

        // Sugerimos de 6 a 3 ciclos (9h, 7.5h, 6h, 4.5h)
        for (cycles in 6 downTo 3) {
            val totalSleepMinutes = cycles * CYCLE_DURATION_MINUTES
            val bedtime = wakeTime
                .minusMinutes(totalSleepMinutes)
                .minusMinutes(FALL_ASLEEP_TIME_MINUTES)

            val isRecommended = (cycles == 5 || cycles == 6)
            val label = when (cycles) {
                6 -> "Óptimo (9h)"
                5 -> "Recomendado (7h 30m)"
                4 -> "Aceptable (6h)"
                else -> "Mínimo / Siesta (4h 30m)"
            }
            val description = "$cycles ciclos completos de sueño (incluye 14 min para dormir)"

            suggestions.add(
                SleepCycleSuggestion(
                    time = bedtime,
                    cyclesCount = cycles,
                    totalSleepHours = totalSleepMinutes / 60.0,
                    isRecommended = isRecommended,
                    label = label,
                    description = description
                )
            )
        }
        return suggestions
    }

    /**
     * Calcula a qué horas deberías despertar si te vas a dormir en este mismo instante.
     */
    fun calculateWakeTimesForSleepNow(currentTime: LocalTime = LocalTime.now()): List<SleepCycleSuggestion> {
        val sleepStartTime = currentTime.plusMinutes(FALL_ASLEEP_TIME_MINUTES)
        val suggestions = mutableListOf<SleepCycleSuggestion>()

        // 3 a 6 ciclos
        for (cycles in 3..6) {
            val totalSleepMinutes = cycles * CYCLE_DURATION_MINUTES
            val wakeTime = sleepStartTime.plusMinutes(totalSleepMinutes)

            val isRecommended = (cycles == 5 || cycles == 6)
            val label = when (cycles) {
                6 -> "Descanso total (9h)"
                5 -> "Ideal (7h 30m)"
                4 -> "Suficiente (6h)"
                else -> "Rápido (4h 30m)"
            }
            val description = "$cycles ciclos completos de sueño"

            suggestions.add(
                SleepCycleSuggestion(
                    time = wakeTime,
                    cyclesCount = cycles,
                    totalSleepHours = totalSleepMinutes / 60.0,
                    isRecommended = isRecommended,
                    label = label,
                    description = description
                )
            )
        }
        return suggestions
    }
}

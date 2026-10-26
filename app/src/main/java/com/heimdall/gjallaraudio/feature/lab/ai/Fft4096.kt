package com.heimdall.gjallaraudio.feature.lab.ai

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Transformada rápida de Fourier (FFT) Radix-2 de 4096 puntos ultrarrápida para Android.
 * Precomputa tablas de inversión de bits y factores twiddle (seno/coseno) para evitar asignaciones
 * en tiempo de ejecución durante la inferencia neuronal.
 */
class Fft4096 {
    val size: Int = 4096
    private val bitRev: IntArray = IntArray(size)
    private val cosTableFwd: FloatArray = FloatArray(size / 2)
    private val sinTableFwd: FloatArray = FloatArray(size / 2)
    private val cosTableInv: FloatArray = FloatArray(size / 2)
    private val sinTableInv: FloatArray = FloatArray(size / 2)

    init {
        // Precalcular inversión de bits
        var j = 0
        for (i in 0 until size) {
            bitRev[i] = j
            var m = size shr 1
            while (m >= 1 && j >= m) {
                j -= m
                m = m shr 1
            }
            j += m
        }

        // Precalcular factores twiddle
        for (i in 0 until size / 2) {
            val angleFwd = (-2.0 * PI * i / size)
            cosTableFwd[i] = cos(angleFwd).toFloat()
            sinTableFwd[i] = sin(angleFwd).toFloat()

            val angleInv = (2.0 * PI * i / size)
            cosTableInv[i] = cos(angleInv).toFloat()
            sinTableInv[i] = sin(angleInv).toFloat()
        }
    }

    /**
     * Ejecuta FFT o IFFT in-place sobre los arreglos real e imag de tamaño 4096.
     */
    fun transform(real: FloatArray, imag: FloatArray, inverse: Boolean) {
        // 1. Permutación de inversión de bits
        for (i in 0 until size) {
            val r = bitRev[i]
            if (r > i) {
                val tempR = real[i]
                real[i] = real[r]
                real[r] = tempR

                val tempI = imag[i]
                imag[i] = imag[r]
                imag[r] = tempI
            }
        }

        // 2. Mariposas Cooley-Tukey
        val cosT = if (inverse) cosTableInv else cosTableFwd
        val sinT = if (inverse) sinTableInv else sinTableFwd

        var s = 1
        while (s < size) {
            val m = s shl 1
            val step = size / m
            var k = 0
            while (k < size) {
                var jIdx = 0
                for (j in 0 until s) {
                    val pos = k + j
                    val wr = cosT[jIdx]
                    val wi = sinT[jIdx]

                    val posS = pos + s
                    val tr = wr * real[posS] - wi * imag[posS]
                    val ti = wr * imag[posS] + wi * real[posS]

                    real[posS] = real[pos] - tr
                    imag[posS] = imag[pos] - ti
                    real[pos] += tr
                    imag[pos] += ti

                    jIdx += step
                }
                k += m
            }
            s = m
        }

        // 3. Normalización 1/N para la inversa
        if (inverse) {
            val scale = 1.0f / size
            for (i in 0 until size) {
                real[i] *= scale
                imag[i] *= scale
            }
        }
    }
}

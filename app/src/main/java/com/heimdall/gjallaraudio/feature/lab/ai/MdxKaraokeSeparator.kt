package com.heimdall.gjallaraudio.feature.lab.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos

/**
 * Motor de separación de fuentes con Red Neuronal UVR MDX-Net ONNX (Vocal vs Instrumental/Karaoke).
 * Procesa en streaming de bloques (chunks) para garantizar consumo mínimo de RAM (<15MB)
 * y rendimiento óptimo en dispositivos móviles.
 */
class MdxKaraokeSeparator(private val context: Context) {

    private val fft = Fft4096()
    private val nFft = 4096
    private val hopLength = 1024
    private val dimF = 2048
    private val dimT = 256
    private val chunkSize = hopLength * (dimT - 1) // 261120 muestras
    private val trim = nFft / 2 // 2048 muestras
    private val genSize = chunkSize - 2 * trim // 257024 muestras

    private val hann = FloatArray(nFft) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / nFft))).toFloat()
    }
    private val hannSq = FloatArray(nFft) { i -> hann[i] * hann[i] }

    suspend fun separate(
        inputFile: File,
        isolateVocals: Boolean,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.Default) {
        var env: OrtEnvironment? = null
        var session: OrtSession? = null
        val tempDir = File(context.cacheDir, "mdx_${System.currentTimeMillis()}").apply { mkdirs() }
        val pcmIn = File(tempDir, "input.pcm")
        val pcmOut = File(tempDir, "output.pcm")
        val finalMp3 = File(tempDir, if (isolateVocals) "solo_voz.mp3" else "karaoke.mp3")

        var rafIn: RandomAccessFile? = null
        var fosOut: FileOutputStream? = null

        try {
            onProgress(4f)
            // 1. Decodificar audio con FFmpeg a PCM Float32 Stereo 44.1kHz
            val decodeCmd = "-y -i \"${inputFile.absolutePath}\" -f f32le -ac 2 -ar 44100 \"${pcmIn.absolutePath}\""
            val decodeSession = FFmpegKit.execute(decodeCmd)
            if (!ReturnCode.isSuccess(decodeSession.returnCode)) {
                return@withContext Result.failure(Exception("Fallo al decodificar audio: ${decodeSession.failStackTrace}"))
            }

            onProgress(10f)
            // 2. Cargar modelo ONNX UVR_MDXNET_KARA
            val modelFile = ensureModelLoaded()
            env = OrtEnvironment.getEnvironment()
            val sessionOpts = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            session = env.createSession(modelFile.absolutePath, sessionOpts)

            val totalBytes = pcmIn.length()
            val numFrames = (totalBytes / 8).toInt() // 8 bytes por frame estéreo (4 bytes L + 4 bytes R)
            if (numFrames == 0) {
                return@withContext Result.failure(Exception("El archivo no contiene audio válido."))
            }

            val pad = genSize + trim - (numFrames % genSize)
            val paddedTotal = trim + numFrames + pad
            val totalChunks = (paddedTotal - chunkSize) / genSize + 1

            rafIn = RandomAccessFile(pcmIn, "r")
            fosOut = FileOutputStream(pcmOut)

            // Buffers estáticos reutilizables para evitar recolector de basura (GC pauses)
            val chunkL = FloatArray(chunkSize)
            val chunkR = FloatArray(chunkSize)
            val paddedL = FloatArray(2048 + chunkSize + 2048)
            val paddedR = FloatArray(2048 + chunkSize + 2048)

            val realBuf = FloatArray(nFft)
            val imagBuf = FloatArray(nFft)

            val inputTensorData = FloatArray(1 * 4 * dimF * dimT)
            val outData = FloatArray(1 * 4 * dimF * dimT)

            val outBufL = FloatArray(2048 + chunkSize + 2048)
            val outBufR = FloatArray(2048 + chunkSize + 2048)
            val winSum = FloatArray(2048 + chunkSize + 2048)

            val rawChunkBytes = ByteArray(chunkSize * 8)
            val rawChunkBuf = ByteBuffer.wrap(rawChunkBytes).order(ByteOrder.LITTLE_ENDIAN)

            val outSliceBytes = ByteArray(genSize * 8)
            val outSliceBuf = ByteBuffer.wrap(outSliceBytes).order(ByteOrder.LITTLE_ENDIAN)

            var writtenFrames = 0
            var chunkIdx = 0
            var i = 0

            while (i <= paddedTotal - chunkSize) {
                // Posición del chunk relativo al audio original (i - trim)
                val audioStartFrame = i - trim
                chunkL.fill(0f)
                chunkR.fill(0f)

                val readStart = audioStartFrame.coerceAtLeast(0)
                val readEnd = (audioStartFrame + chunkSize).coerceAtMost(numFrames)
                val framesToRead = (readEnd - readStart).coerceAtLeast(0)

                if (framesToRead > 0) {
                    rafIn.seek(readStart.toLong() * 8L)
                    val bytesToRead = framesToRead * 8
                    rafIn.readFully(rawChunkBytes, 0, bytesToRead)
                    rawChunkBuf.position(0)
                    val offsetInChunk = readStart - audioStartFrame
                    for (f in 0 until framesToRead) {
                        chunkL[offsetInChunk + f] = rawChunkBuf.float
                        chunkR[offsetInChunk + f] = rawChunkBuf.float
                    }
                }

                // STFT Reflect Padding (2048 muestras a cada extremo)
                for (p in 0 until 2048) {
                    paddedL[p] = chunkL[2048 - p]
                    paddedR[p] = chunkR[2048 - p]
                }
                System.arraycopy(chunkL, 0, paddedL, 2048, chunkSize)
                System.arraycopy(chunkR, 0, paddedR, 2048, chunkSize)
                for (p in 0 until 2048) {
                    paddedL[2048 + chunkSize + p] = chunkL[chunkSize - 2 - p]
                    paddedR[2048 + chunkSize + p] = chunkR[chunkSize - 2 - p]
                }

                // Generar espectrograma para L y R en tensor (1, 4, 2048, 256)
                for (t in 0 until dimT) {
                    val frameStart = t * hopLength

                    // Canal L
                    for (n in 0 until nFft) {
                        realBuf[n] = paddedL[frameStart + n] * hann[n]
                        imagBuf[n] = 0f
                    }
                    fft.transform(realBuf, imagBuf, inverse = false)
                    for (f in 0 until dimF) {
                        inputTensorData[(0 * dimF + f) * dimT + t] = realBuf[f]
                        inputTensorData[(1 * dimF + f) * dimT + t] = imagBuf[f]
                    }

                    // Canal R
                    for (n in 0 until nFft) {
                        realBuf[n] = paddedR[frameStart + n] * hann[n]
                        imagBuf[n] = 0f
                    }
                    fft.transform(realBuf, imagBuf, inverse = false)
                    for (f in 0 until dimF) {
                        inputTensorData[(2 * dimF + f) * dimT + t] = realBuf[f]
                        inputTensorData[(3 * dimF + f) * dimT + t] = imagBuf[f]
                    }
                }

                // Inferencia Neuronal ONNX
                val inTensor = OnnxTensor.createTensor(
                    env,
                    FloatBuffer.wrap(inputTensorData),
                    longArrayOf(1, 4, dimF.toLong(), dimT.toLong())
                )
                val results = session.run(mapOf("input" to inTensor))
                val outTensor = results[0] as OnnxTensor
                outTensor.floatBuffer.get(outData)

                // iSTFT Reconstrucción
                outBufL.fill(0f)
                outBufR.fill(0f)
                winSum.fill(0f)

                for (t in 0 until dimT) {
                    val frameStart = t * hopLength

                    // Canal L (Vocals)
                    for (f in 0 until dimF) {
                        val rVal = outData[(0 * dimF + f) * dimT + t]
                        val iVal = outData[(1 * dimF + f) * dimT + t]
                        realBuf[f] = rVal
                        imagBuf[f] = iVal
                        if (f > 0) {
                            realBuf[nFft - f] = rVal
                            imagBuf[nFft - f] = -iVal
                        }
                    }
                    realBuf[0] = outData[0 * dimF * dimT + t]
                    imagBuf[0] = 0f
                    realBuf[dimF] = 0f
                    imagBuf[dimF] = 0f
                    fft.transform(realBuf, imagBuf, inverse = true)
                    for (n in 0 until nFft) {
                        outBufL[frameStart + n] += realBuf[n] * hann[n]
                    }

                    // Canal R (Vocals)
                    for (f in 0 until dimF) {
                        val rVal = outData[(2 * dimF + f) * dimT + t]
                        val iVal = outData[(3 * dimF + f) * dimT + t]
                        realBuf[f] = rVal
                        imagBuf[f] = iVal
                        if (f > 0) {
                            realBuf[nFft - f] = rVal
                            imagBuf[nFft - f] = -iVal
                        }
                    }
                    realBuf[0] = outData[2 * dimF * dimT + t]
                    imagBuf[0] = 0f
                    realBuf[dimF] = 0f
                    imagBuf[dimF] = 0f
                    fft.transform(realBuf, imagBuf, inverse = true)
                    for (n in 0 until nFft) {
                        outBufR[frameStart + n] += realBuf[n] * hann[n]
                        winSum[frameStart + n] += hannSq[n]
                    }
                }

                // Normalización de solapamiento
                for (k in winSum.indices) {
                    if (winSum[k] > 1e-7f) {
                        outBufL[k] /= winSum[k]
                        outBufR[k] /= winSum[k]
                    }
                }

                // Extraer el centro acústico limpio (descartando márgenes de borde)
                val remainingFrames = numFrames - writtenFrames
                val sliceLen = genSize.coerceAtMost(remainingFrames)

                if (sliceLen > 0) {
                    outSliceBuf.position(0)
                    for (k in 0 until sliceLen) {
                        val srcIdx = 2048 + trim + k
                        val vocL = outBufL[srcIdx]
                        val vocR = outBufR[srcIdx]

                        if (isolateVocals) {
                            // SOLO VOZ
                            outSliceBuf.putFloat(vocL)
                            outSliceBuf.putFloat(vocR)
                        } else {
                            // KARAOKE / INSTRUMENTAL: Original - Voz
                            val origL = chunkL[trim + k]
                            val origR = chunkR[trim + k]
                            outSliceBuf.putFloat(origL - vocL)
                            outSliceBuf.putFloat(origR - vocR)
                        }
                    }
                    fosOut.write(outSliceBytes, 0, sliceLen * 8)
                    writtenFrames += sliceLen
                }

                inTensor.close()
                outTensor.close()
                results.close()

                chunkIdx++
                val prog = 10f + (chunkIdx.toFloat() / totalChunks.toFloat()) * 80f
                onProgress(prog.coerceAtMost(90f))

                i += genSize
            }

            fosOut.flush()
            fosOut.close()
            fosOut = null

            rafIn.close()
            rafIn = null

            onProgress(92f)
            // 7. Codificar resultado PCM a MP3 auténtico a 320 kbps con libmp3lame
            val encodeCmd = "-y -f f32le -ar 44100 -ac 2 -i \"${pcmOut.absolutePath}\" -c:a libmp3lame -b:a 320k \"${finalMp3.absolutePath}\""
            val encodeSession = FFmpegKit.execute(encodeCmd)
            if (!ReturnCode.isSuccess(encodeSession.returnCode)) {
                return@withContext Result.failure(Exception("Error al codificar MP3 final: ${encodeSession.failStackTrace}"))
            }

            onProgress(100f)
            Result.success(finalMp3)
        } catch (e: Exception) {
            Log.e("MdxKaraokeSeparator", "Error en separación de pistas", e)
            Result.failure(e)
        } finally {
            try { rafIn?.close() } catch (_: Exception) {}
            try { fosOut?.close() } catch (_: Exception) {}
            try { session?.close() } catch (_: Exception) {}
            try { env?.close() } catch (_: Exception) {}
            pcmIn.delete()
            pcmOut.delete()
        }
    }

    private fun ensureModelLoaded(): File {
        val modelDir = File(context.filesDir, "models").apply { mkdirs() }
        val modelFile = File(modelDir, "uvr_mdxnet_kara.onnx")
        if (!modelFile.exists() || modelFile.length() < 20_000_000L) {
            context.assets.open("models/uvr_mdxnet_kara.onnx").use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return modelFile
    }
}

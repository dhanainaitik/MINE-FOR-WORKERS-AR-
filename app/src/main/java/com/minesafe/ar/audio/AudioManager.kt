package com.minesafe.ar.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Offline Audio System for MineSafe AR.
 * Provides synthesized industrial sound effects (ambient mine ventilation drone,
 * electrical fire crackle, chemical leak hiss, CO2 extinguisher discharge,
 * safety confirmation chimes, and warning alerts) without requiring external media downloads.
 */
class AudioManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var ambientJob: Job? = null
    private var hazardAudioJob: Job? = null
    var isMuted: Boolean = false

    /**
     * Plays continuous low-frequency underground mine atmosphere:
     * deep ventilation shaft drone (60-80Hz) + subtle cavernous resonance.
     */
    fun startMineAmbiance() {
        if (ambientJob != null || isMuted) return

        ambientJob = scope.launch {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate)

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack.play()
            val buffer = ShortArray(bufferSize)
            var phase1 = 0.0
            var phase2 = 0.0

            try {
                while (isActive && !isMuted) {
                    for (i in buffer.indices) {
                        val drone = sin(phase1) * 0.4 + sin(phase2) * 0.3
                        val air = (Random.nextFloat() * 2f - 1f) * 0.08f
                        buffer[i] = ((drone + air) * 16000f).toInt().coerceIn(-32767, 32767).toShort()

                        phase1 += 2.0 * PI * 65.0 / sampleRate
                        phase2 += 2.0 * PI * 110.0 / sampleRate
                        if (phase1 > 2.0 * PI) phase1 -= 2.0 * PI
                        if (phase2 > 2.0 * PI) phase2 -= 2.0 * PI
                    }
                    audioTrack.write(buffer, 0, buffer.size)
                }
            } finally {
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        }
    }

    fun stopMineAmbiance() {
        ambientJob?.cancel()
        ambientJob = null
    }

    /**
     * Plays electrical fire crackle and low turbulent roar.
     */
    fun startFireSound() {
        stopHazardSound()
        if (isMuted) return
        hazardAudioJob = scope.launch {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate / 2)

            val track = createAudioTrack(sampleRate, bufferSize)
            track.play()
            val buffer = ShortArray(bufferSize)
            var phase = 0.0

            try {
                while (isActive && !isMuted) {
                    for (i in buffer.indices) {
                        val baseRoar = sin(phase) * 0.35f
                        val crackle = if (Random.nextFloat() > 0.96f) (Random.nextFloat() * 2f - 1f) * 0.85f else 0f
                        val hiss = (Random.nextFloat() * 2f - 1f) * 0.2f
                        buffer[i] = ((baseRoar + crackle + hiss) * 14000f).toInt().coerceIn(-32767, 32767).toShort()

                        phase += 2.0 * PI * 95.0 / sampleRate
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                    }
                    track.write(buffer, 0, buffer.size)
                }
            } finally {
                track.stop()
                track.release()
            }
        }
    }

    /**
     * Plays high pressure chemical gas escaping / hissing sound.
     */
    fun startChemicalLeakSound() {
        stopHazardSound()
        if (isMuted) return
        hazardAudioJob = scope.launch {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate / 2)

            val track = createAudioTrack(sampleRate, bufferSize)
            track.play()
            val buffer = ShortArray(bufferSize)
            var phaseMod = 0.0

            try {
                while (isActive && !isMuted) {
                    for (i in buffer.indices) {
                        val mod = sin(phaseMod) * 0.3f + 0.7f
                        val whiteNoise = (Random.nextFloat() * 2f - 1f) * 0.5f * mod
                        buffer[i] = (whiteNoise * 18000f).toInt().coerceIn(-32767, 32767).toShort()

                        phaseMod += 2.0 * PI * 3.5 / sampleRate
                        if (phaseMod > 2.0 * PI) phaseMod -= 2.0 * PI
                    }
                    track.write(buffer, 0, buffer.size)
                }
            } finally {
                track.stop()
                track.release()
            }
        }
    }

    fun stopHazardSound() {
        hazardAudioJob?.cancel()
        hazardAudioJob = null
    }

    /**
     * Extinguisher discharge sound (burst of compressed CO2/powder).
     */
    fun playExtinguisherDischarge(durationMs: Long = 2500) {
        if (isMuted) return
        scope.launch {
            val sampleRate = 22050
            val count = (sampleRate * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(count)

            for (i in buffer.indices) {
                val progress = i.toFloat() / count
                val envelope = if (progress < 0.1f) progress / 0.1f else (1.0f - progress).coerceAtLeast(0f)
                val noise = (Random.nextFloat() * 2f - 1f) * 0.8f
                val lowHiss = sin(2.0 * PI * 180.0 * i / sampleRate).toFloat() * 0.3f
                buffer[i] = ((noise + lowHiss) * envelope * 24000f).toInt().coerceIn(-32767, 32767).toShort()
            }

            playBuffer(buffer, sampleRate)
        }
    }

    /**
     * Two-tone positive chime (523 Hz -> 659 Hz) for milestone completion.
     */
    fun playSuccessChime() {
        if (isMuted) return
        scope.launch {
            val sampleRate = 22050
            val note1Len = (sampleRate * 0.18f).toInt()
            val note2Len = (sampleRate * 0.32f).toInt()
            val buffer = ShortArray(note1Len + note2Len)

            for (i in 0 until note1Len) {
                val env = 1f - (i.toFloat() / note1Len) * 0.4f
                buffer[i] = (sin(2.0 * PI * 523.25 * i / sampleRate) * env * 22000f).toInt().toShort()
            }
            for (i in 0 until note2Len) {
                val env = 1f - (i.toFloat() / note2Len)
                buffer[note1Len + i] = (sin(2.0 * PI * 659.25 * i / sampleRate) * env * 24000f).toInt().toShort()
            }

            playBuffer(buffer, sampleRate)
        }
    }

    /**
     * Warning / mistake buzzer (220 Hz low buzz) for incorrect action or hazard breach.
     */
    fun playMistakeBuzzer() {
        if (isMuted) return
        scope.launch {
            val sampleRate = 22050
            val len = (sampleRate * 0.28f).toInt()
            val buffer = ShortArray(len)

            for (i in 0 until len) {
                val t = (i % (sampleRate / 220)) / (sampleRate / 220f)
                val saw = (t * 2f - 1f)
                val env = 1f - (i.toFloat() / len) * 0.3f
                buffer[i] = (saw * env * 20000f).toInt().toShort()
            }

            playBuffer(buffer, sampleRate)
        }
    }

    fun playTap() {
        if (isMuted) return
        scope.launch {
            val sampleRate = 22050
            val len = (sampleRate * 0.04f).toInt()
            val buffer = ShortArray(len)
            for (i in 0 until len) {
                val env = 1f - (i.toFloat() / len)
                buffer[i] = (sin(2.0 * PI * 880.0 * i / sampleRate) * env * 14000f).toInt().toShort()
            }
            playBuffer(buffer, sampleRate)
        }
    }

    /**
     * Metallic pin pull sound: high-frequency metallic slide + chime (1400Hz - 1760Hz).
     */
    fun playPinPullSound() {
        if (isMuted) return
        scope.launch {
            val sampleRate = 22050
            val len = (sampleRate * 0.22f).toInt()
            val buffer = ShortArray(len)
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                val env = (1.0f - (i.toFloat() / len)).coerceAtLeast(0f)
                val freq = 1350.0 + (i.toFloat() / len) * 450.0
                val tone1 = sin(2.0 * PI * freq * t).toFloat()
                val tone2 = sin(2.0 * PI * (freq * 1.5) * t).toFloat() * 0.4f
                val noise = (Random.nextFloat() * 2f - 1f) * (1f - (i.toFloat() / len).coerceIn(0f, 1f)) * 0.25f
                buffer[i] = ((tone1 + tone2 + noise) * env * 22000f).toInt().coerceIn(-32767, 32767).toShort()
            }
            playBuffer(buffer, sampleRate)
        }
    }

    /**
     * Mechanical lever trigger squeeze: solid mechanical latch / click snap.
     */
    fun playLeverSqueezeSound() {
        if (isMuted) return
        scope.launch {
            val sampleRate = 22050
            val len = (sampleRate * 0.12f).toInt()
            val buffer = ShortArray(len)
            for (i in 0 until len) {
                val t = i.toFloat() / sampleRate
                val env = (1.0f - (i.toFloat() / len)).coerceAtLeast(0f)
                val click = sin(2.0 * PI * 520.0 * t).toFloat() + (Random.nextFloat() * 2f - 1f) * 0.35f
                buffer[i] = (click * env * 24000f).toInt().coerceIn(-32767, 32767).toShort()
            }
            playBuffer(buffer, sampleRate)
        }
    }

    private fun playBuffer(buffer: ShortArray, sampleRate: Int) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            scope.launch {
                delay((buffer.size.toFloat() / sampleRate * 1000).toLong() + 200)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun createAudioTrack(sampleRate: Int, bufferSize: Int): AudioTrack {
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    fun shutdown() {
        stopMineAmbiance()
        stopHazardSound()
    }
}

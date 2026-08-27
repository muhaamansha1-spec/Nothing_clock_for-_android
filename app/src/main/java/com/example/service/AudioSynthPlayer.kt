package com.example.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

object AudioSynthPlayer {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun play(ringtoneName: String) {
        stop()
        playbackJob = scope.launch {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(1024)

            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
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
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                val buffer = ShortArray(bufferSize)
                var phase = 0.0

                while (isActive) {
                    val timeMs = System.currentTimeMillis()
                    val customRt = CustomRingtoneManager.getCustomRingtone(ringtoneName)
                    val freq = if (customRt != null) {
                        when (customRt.waveform.uppercase()) {
                            "SINE" -> {
                                val mod = sin(2 * Math.PI * customRt.lfoSpeed * (timeMs / 1000.0))
                                customRt.frequency.toDouble() + (mod * (customRt.frequency * 0.15))
                            }
                            "SQUARE" -> {
                                val periodMs = if (customRt.lfoSpeed > 0) (1000 / customRt.lfoSpeed).toLong() else 200L
                                val cycle = if (periodMs > 0) (timeMs / periodMs) % 2 else 0L
                                if (cycle == 0L) customRt.frequency.toDouble() else customRt.frequency.toDouble() * 0.75
                            }
                            "TRIANGLE" -> {
                                val period = if (customRt.lfoSpeed > 0) (1000 / customRt.lfoSpeed).toInt() else 200
                                val phaseTime = (timeMs % period).toDouble() / period
                                val tri = if (phaseTime < 0.5) phaseTime * 2.0 else (1.0 - phaseTime) * 2.0
                                customRt.frequency.toDouble() * (0.8 + tri * 0.4)
                            }
                            "CHIRP" -> {
                                val cycle = (timeMs % 400) / 400.0
                                customRt.frequency.toDouble() + (cycle * 800.0)
                            }
                            else -> customRt.frequency.toDouble()
                        }
                    } else {
                        when (ringtoneName.uppercase()) {
                            "GLYPH RAPID" -> {
                                val step = (timeMs / 80) % 8
                                when (step) {
                                    0L -> 980.0
                                    1L -> 0.0
                                    2L -> 1300.0
                                    3L -> 0.0
                                    4L -> 980.0
                                    5L -> 1100.0
                                    6L -> 1300.0
                                    else -> 0.0
                                }
                            }
                            "VOX UNISON" -> {
                                val mod = sin(2 * Math.PI * 3.0 * (timeMs / 1000.0))
                                380.0 + (mod * 40.0)
                            }
                            "TEENAGE AMBIENT" -> {
                                val step = (timeMs / 600) % 4
                                val baseFreq = when (step) {
                                    0L -> 440.0 // A4
                                    1L -> 523.25 // C5
                                    2L -> 587.33 // D5
                                    else -> 349.23 // F4
                                }
                                val vibrato = sin(2 * Math.PI * 5.0 * (timeMs / 1000.0)) * 6.0
                                baseFreq + vibrato
                            }
                            "SILENT STATE" -> {
                                val cycle = timeMs % 1500
                                if (cycle < 100) 1200.0 else 0.0
                            }
                            "RETRO BEATS" -> {
                                val step = (timeMs / 120) % 6
                                when (step) {
                                    0L -> 520.0
                                    1L -> 260.0
                                    2L -> 1040.0
                                    3L -> 520.0
                                    4L -> 780.0
                                    else -> 0.0
                                }
                            }
                            "DIGITAL CHIRP" -> {
                                val cycle = (timeMs % 400) / 400.0
                                700.0 + (cycle * 900.0)
                            }
                            else -> {
                                val cycle = timeMs % 400
                                if (cycle < 200) 480.0 else 0.0
                            }
                        }
                    }

                    for (i in buffer.indices) {
                        if (freq > 0.0) {
                            buffer[i] = (sin(phase) * 12000.0).toInt().toShort()
                            val phaseIncrement = 2 * Math.PI * freq / sampleRate
                            phase = (phase + phaseIncrement) % (2 * Math.PI)
                        } else {
                            buffer[i] = 0
                            phase = 0.0
                        }
                    }

                    track.write(buffer, 0, buffer.size)
                    yield()
                }
            } catch (e: Exception) {
                Log.e("AudioSynthPlayer", "Playback error", e)
            }
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioSynthPlayer", "Error stopping track", e)
        }
        audioTrack = null
    }
}

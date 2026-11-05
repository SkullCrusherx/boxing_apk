package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.R
import com.example.model.BellSoundStyle
import com.example.model.WarningSoundStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh

class BoxingAudioEngine(private val context: Context) {

    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)

    // SoundPool for instant, low-latency playback of user audio assets
    private val soundPool: SoundPool
    private var soundIdBell: Int = 0
    private var soundIdWarning4s: Int = 0
    private var soundPoolLoaded = false

    // Cached PCM buffers for synthesized sound options
    private val audioCache = mutableMapOf<String, ShortArray>()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                soundPoolLoaded = true
            }
        }

        try {
            soundIdBell = soundPool.load(context, R.raw.boxing_bell_round, 1)
            soundIdWarning4s = soundPool.load(context, R.raw.boxing_warning_4s, 1)
        } catch (_: Exception) {}

        // Pre-warm synthesized sounds
        scope.launch {
            BellSoundStyle.entries.forEach { style ->
                if (style != BellSoundStyle.BOXING_RING_BELL) {
                    getOrCreateBellBuffer(style)
                }
            }
            WarningSoundStyle.entries.forEach { style ->
                if (style != WarningSoundStyle.BOXING_WARNING_4S) {
                    getOrCreateWarningBuffer(style)
                }
            }
            getOrCreateTickBuffer()
        }
    }

    fun playBell(
        style: BellSoundStyle,
        volume: Float = 1.0f,
        vibrate: Boolean = true,
        gymBoost: Boolean = true
    ) {
        scope.launch {
            if (vibrate) {
                triggerBellVibration(style)
            }
            val effectiveVol = if (gymBoost) 1.0f else volume.coerceIn(0.1f, 1.0f)

            if (style == BellSoundStyle.BOXING_RING_BELL) {
                playRawAudio(soundIdBell, R.raw.boxing_bell_round, effectiveVol)
            } else {
                val buffer = getOrCreateBellBuffer(style)
                playPcmBuffer(buffer, effectiveVol)
            }
        }
    }

    fun playWarning(
        style: WarningSoundStyle,
        volume: Float = 1.0f,
        vibrate: Boolean = true,
        gymBoost: Boolean = true
    ) {
        if (style == WarningSoundStyle.MUTED) {
            if (vibrate) triggerWarningVibration()
            return
        }
        scope.launch {
            if (vibrate) {
                triggerWarningVibration()
            }
            val effectiveVol = if (gymBoost) 1.0f else volume.coerceIn(0.1f, 1.0f)

            if (style == WarningSoundStyle.BOXING_WARNING_4S) {
                playRawAudio(soundIdWarning4s, R.raw.boxing_warning_4s, effectiveVol)
            } else {
                val buffer = getOrCreateWarningBuffer(style)
                if (buffer.isNotEmpty()) {
                    playPcmBuffer(buffer, effectiveVol)
                }
            }
        }
    }

    private fun playRawAudio(soundPoolId: Int, rawResId: Int, volume: Float) {
        // Try SoundPool first for zero latency
        var played = false
        if (soundPoolLoaded && soundPoolId != 0) {
            val streamId = soundPool.play(soundPoolId, volume, volume, 1, 0, 1.0f)
            if (streamId != 0) {
                played = true
            }
        }

        // If SoundPool didn't play (still loading or stream busy), use MediaPlayer as guaranteed fallback
        if (!played) {
            try {
                val mp = MediaPlayer.create(context, rawResId)
                mp?.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                            .build()
                    )
                    setVolume(volume, volume)
                    setOnCompletionListener { it.release() }
                    start()
                }
            } catch (_: Exception) {}
        }
    }

    fun playCountdownTick(volume: Float = 0.7f) {
        scope.launch {
            val buffer = getOrCreateTickBuffer()
            playPcmBuffer(buffer, volume)
        }
    }

    private fun triggerBellVibration(style: BellSoundStyle) {
        try {
            vibrator?.let { vib ->
                if (!vib.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = when (style) {
                        BellSoundStyle.DOUBLE_CLANG -> longArrayOf(0, 200, 100, 250)
                        BellSoundStyle.TRIPLE_MATCH_BELL -> longArrayOf(0, 180, 80, 180, 80, 220)
                        else -> longArrayOf(0, 350, 80, 350)
                    }
                    val amplitudes = when (style) {
                        BellSoundStyle.DOUBLE_CLANG -> intArrayOf(0, 255, 0, 255)
                        BellSoundStyle.TRIPLE_MATCH_BELL -> intArrayOf(0, 255, 0, 255, 0, 255)
                        else -> intArrayOf(0, 255, 0, 255)
                    }
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(400)
                }
            }
        } catch (_: Exception) {}
    }

    private fun triggerWarningVibration() {
        try {
            vibrator?.let { vib ->
                if (!vib.hasVibrator()) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 80, 100, 80, 100, 80, 100, 120)
                    val amplitudes = intArrayOf(0, 220, 0, 220, 0, 220, 0, 255)
                    vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(longArrayOf(0, 80, 100, 80, 100, 80, 100, 120), -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun playPcmBuffer(buffer: ShortArray, volume: Float) {
        if (buffer.isEmpty()) return
        val clampedVolume = volume.coerceIn(0f, 1f)
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val trackBufferSize = maxOf(buffer.size * 2, minBufferSize)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(trackBufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        try {
            audioTrack.setVolume(clampedVolume)
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            val durationMs = (buffer.size * 1000L) / sampleRate + 150L
            Thread.sleep(durationMs)
        } catch (_: Exception) {
        } finally {
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }

    @Synchronized
    private fun getOrCreateBellBuffer(style: BellSoundStyle): ShortArray {
        return audioCache.getOrPut(style.name) {
            when (style) {
                BellSoundStyle.BOXING_RING_BELL -> generateThunderousSteelBellBuffer()
                BellSoundStyle.THUNDEROUS_STEEL -> generateThunderousSteelBellBuffer()
                BellSoundStyle.HEAVY_RING_BELL -> generateHeavyBellBuffer()
                BellSoundStyle.DOUBLE_CLANG -> generateDoubleClangBuffer()
                BellSoundStyle.TRIPLE_MATCH_BELL -> generateTripleBellBuffer()
                BellSoundStyle.DEEP_BRONZE_GONG -> generateDeepGongBuffer()
                BellSoundStyle.ARENA_AIR_HORN -> generateAirHornBuffer()
            }
        }
    }

    @Synchronized
    private fun getOrCreateWarningBuffer(style: WarningSoundStyle): ShortArray {
        return audioCache.getOrPut(style.name) {
            when (style) {
                WarningSoundStyle.BOXING_WARNING_4S -> generateWoodenClapperBuffer()
                WarningSoundStyle.WOODEN_CLAPPER -> generateWoodenClapperBuffer()
                WarningSoundStyle.WARNING_BEEPS -> generateWarningBeepsBuffer()
                WarningSoundStyle.SHARP_WOOD_KNOCK -> generateSingleKnockBuffer()
                WarningSoundStyle.MUTED -> ShortArray(0)
            }
        }
    }

    @Synchronized
    private fun getOrCreateTickBuffer(): ShortArray {
        return audioCache.getOrPut("COUNTDOWN_TICK") {
            generateTickBuffer()
        }
    }

    private fun generateThunderousSteelBellBuffer(): ShortArray {
        val durationSec = 2.4
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        val f0 = 680.0
        val f1 = 684.0
        val f2 = 1140.0
        val f3 = 1860.0
        val f4 = 2740.0
        val f5 = 3950.0
        val f6 = 5400.0

        val twoPi = 2.0 * PI

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val attack = if (t < 0.003) (t / 0.003) else 1.0

            val envHigh = exp(-t * 11.0) * attack
            val envMid = exp(-t * 3.8) * attack
            val envLow = exp(-t * 1.4) * attack

            val impact = if (t < 0.018) {
                (Math.random() * 2.0 - 1.0) * exp(-t * 180.0) * 0.45
            } else 0.0

            val raw = (
                sin(twoPi * f0 * t) * 0.48 * envLow +
                sin(twoPi * f1 * t) * 0.42 * envLow +
                sin(twoPi * f2 * t) * 0.35 * envMid +
                sin(twoPi * f3 * t) * 0.28 * envMid +
                sin(twoPi * f4 * t) * 0.22 * envHigh +
                sin(twoPi * f5 * t) * 0.16 * envHigh +
                sin(twoPi * f6 * t) * 0.10 * envHigh +
                impact
            )

            val saturated = tanh(raw * 2.2)
            buffer[i] = (saturated.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun synthesizeSingleBellStrike(durationSec: Double = 2.0): ShortArray {
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        val f0 = 780.0
        val f1 = 784.0
        val f2 = 1220.0
        val f3 = 1960.0
        val f4 = 2840.0
        val f5 = 4150.0

        val twoPi = 2.0 * PI

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val attack = if (t < 0.006) (t / 0.006) else 1.0

            val envHigh = exp(-t * 8.5) * attack
            val envMid = exp(-t * 3.2) * attack
            val envLow = exp(-t * 1.5) * attack

            val noise = if (t < 0.012) {
                (Math.random() * 2.0 - 1.0) * exp(-t * 180.0) * 0.3
            } else 0.0

            val raw = (
                sin(twoPi * f0 * t) * 0.42 * envLow +
                sin(twoPi * f1 * t) * 0.36 * envLow +
                sin(twoPi * f2 * t) * 0.26 * envMid +
                sin(twoPi * f3 * t) * 0.20 * envMid +
                sin(twoPi * f4 * t) * 0.15 * envHigh +
                sin(twoPi * f5 * t) * 0.10 * envHigh +
                noise
            )

            val saturated = tanh(raw * 1.8)
            buffer[i] = (saturated.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateHeavyBellBuffer(): ShortArray {
        return synthesizeSingleBellStrike(2.2)
    }

    private fun generateDoubleClangBuffer(): ShortArray {
        val strike1 = synthesizeSingleBellStrike(1.8)
        val strike2 = synthesizeSingleBellStrike(2.0)
        val intervalMs = 280
        val offsetSamples = (sampleRate * (intervalMs / 1000.0)).toInt()
        val totalSamples = offsetSamples + strike2.size
        val result = ShortArray(totalSamples)

        for (i in strike1.indices) {
            result[i] = strike1[i]
        }
        for (i in strike2.indices) {
            val idx = i + offsetSamples
            if (idx < result.size) {
                val mix = result[idx].toInt() + strike2[i].toInt()
                result[idx] = mix.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return result
    }

    private fun generateTripleBellBuffer(): ShortArray {
        val strike1 = synthesizeSingleBellStrike(1.6)
        val strike2 = synthesizeSingleBellStrike(1.6)
        val strike3 = synthesizeSingleBellStrike(2.2)
        val intervalMs = 250
        val offset1 = (sampleRate * (intervalMs / 1000.0)).toInt()
        val offset2 = offset1 * 2
        val totalSamples = offset2 + strike3.size
        val result = ShortArray(totalSamples)

        fun mixIn(strike: ShortArray, offset: Int) {
            for (i in strike.indices) {
                val idx = i + offset
                if (idx < result.size) {
                    val mix = result[idx].toInt() + strike[i].toInt()
                    result[idx] = mix.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
        }

        mixIn(strike1, 0)
        mixIn(strike2, offset1)
        mixIn(strike3, offset2)
        return result
    }

    private fun generateDeepGongBuffer(): ShortArray {
        val durationSec = 2.6
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        val f0 = 360.0
        val f1 = 364.0
        val f2 = 580.0
        val f3 = 940.0
        val twoPi = 2.0 * PI

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val attack = if (t < 0.010) (t / 0.010) else 1.0
            val envLow = exp(-t * 1.2) * attack
            val envMid = exp(-t * 2.8) * attack

            val raw = (
                sin(twoPi * f0 * t) * 0.50 * envLow +
                sin(twoPi * f1 * t) * 0.42 * envLow +
                sin(twoPi * f2 * t) * 0.25 * envMid +
                sin(twoPi * f3 * t) * 0.15 * envMid
            )
            val saturated = tanh(raw * 1.7)
            buffer[i] = (saturated.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateAirHornBuffer(): ShortArray {
        val durationSec = 0.7
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        val twoPi = 2.0 * PI

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val env = when {
                t < 0.03 -> t / 0.03
                t > durationSec - 0.05 -> (durationSec - t) / 0.05
                else -> 1.0
            }
            val s1 = sin(twoPi * 240.0 * t) + 0.45 * sin(twoPi * 720.0 * t)
            val s2 = sin(twoPi * 360.0 * t) + 0.40 * sin(twoPi * 1080.0 * t)
            val raw = (s1 + s2) * 0.55 * env
            buffer[i] = (tanh(raw * 1.9) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateWoodenClapperBuffer(): ShortArray {
        val knock1 = synthesizeSingleWoodKnock()
        val knock2 = synthesizeSingleWoodKnock()
        val knock3 = synthesizeSingleWoodKnock()

        val gapMs = 210
        val offset1 = (sampleRate * (gapMs / 1000.0)).toInt()
        val offset2 = offset1 * 2
        val totalSamples = offset2 + knock3.size
        val result = ShortArray(totalSamples)

        fun mixIn(knock: ShortArray, offset: Int) {
            for (i in knock.indices) {
                val idx = i + offset
                if (idx < result.size) {
                    val mix = result[idx].toInt() + knock[i].toInt()
                    result[idx] = mix.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
        }

        mixIn(knock1, 0)
        mixIn(knock2, offset1)
        mixIn(knock3, offset2)
        return result
    }

    private fun synthesizeSingleWoodKnock(): ShortArray {
        val durationSec = 0.14
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        val twoPi = 2.0 * PI

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val pitch = 640.0 + 520.0 * exp(-t * 90.0)
            val env = exp(-t * 32.0)
            val noise = if (t < 0.008) (Math.random() * 2.0 - 1.0) * 0.55 else 0.0

            val raw = (sin(twoPi * pitch * t) * 0.80 + sin(twoPi * pitch * 1.8 * t) * 0.30 + noise) * env
            val saturated = tanh(raw * 1.9)
            buffer[i] = (saturated.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }

    private fun generateSingleKnockBuffer(): ShortArray {
        return synthesizeSingleWoodKnock()
    }

    private fun generateWarningBeepsBuffer(): ShortArray {
        val beepDuration = 0.08
        val gapDuration = 0.09
        val totalSec = beepDuration * 3 + gapDuration * 2
        val totalSamples = (sampleRate * totalSec).toInt()
        val buffer = ShortArray(totalSamples)
        val twoPi = 2.0 * PI

        for (b in 0..2) {
            val startSec = b * (beepDuration + gapDuration)
            val startIdx = (startSec * sampleRate).toInt()
            val beepSamples = (beepDuration * sampleRate).toInt()

            for (i in 0 until beepSamples) {
                val t = i.toDouble() / sampleRate
                val env = if (t < 0.004) t / 0.004 else if (t > beepDuration - 0.004) (beepDuration - t) / 0.004 else 1.0
                val raw = sin(twoPi * 1500.0 * t) * 0.95 * env
                val idx = startIdx + i
                if (idx < buffer.size) {
                    buffer[idx] = (tanh(raw * 1.8) * Short.MAX_VALUE).toInt().toShort()
                }
            }
        }
        return buffer
    }

    private fun generateTickBuffer(): ShortArray {
        val durationSec = 0.04
        val totalSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(totalSamples)
        val twoPi = 2.0 * PI

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 70.0)
            val raw = sin(twoPi * 940.0 * t) * 0.80 * env
            buffer[i] = (tanh(raw * 1.6) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }
}

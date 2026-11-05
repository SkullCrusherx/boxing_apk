package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.BellSoundStyle
import com.example.model.DarkModeOption
import com.example.model.PauseActionPreference
import com.example.model.TimerConfig
import com.example.model.WarningSoundStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("boxing_timer_prefs", Context.MODE_PRIVATE)

    private val _configFlow = MutableStateFlow(loadConfig())
    val configFlow: StateFlow<TimerConfig> = _configFlow.asStateFlow()

    private fun loadConfig(): TimerConfig {
        val totalRounds = prefs.getInt(KEY_TOTAL_ROUNDS, 3)
        val roundSec = prefs.getInt(KEY_ROUND_SEC, 180) // 3 minutes
        val restSec = prefs.getInt(KEY_REST_SEC, 60)    // 1 minute
        val prepSec = prefs.getInt(KEY_PREP_SEC, 5)
        val warnSec = prefs.getInt(KEY_WARN_SEC, 4)     // 4 seconds warning time
        val warnEnabled = prefs.getBoolean(KEY_WARN_ENABLED, true)

        val bellStyleStr = prefs.getString(KEY_BELL_STYLE, BellSoundStyle.BOXING_RING_BELL.name)
        val bellStyle = try {
            BellSoundStyle.valueOf(bellStyleStr ?: BellSoundStyle.BOXING_RING_BELL.name)
        } catch (_: Exception) {
            BellSoundStyle.BOXING_RING_BELL
        }

        val warnStyleStr = prefs.getString(KEY_WARN_STYLE, WarningSoundStyle.BOXING_WARNING_4S.name)
        val warnStyle = try {
            WarningSoundStyle.valueOf(warnStyleStr ?: WarningSoundStyle.BOXING_WARNING_4S.name)
        } catch (_: Exception) {
            WarningSoundStyle.BOXING_WARNING_4S
        }

        val volume = prefs.getFloat(KEY_VOLUME, 1.0f)
        val gymBoost = prefs.getBoolean(KEY_GYM_BOOST, true)
        val vibration = prefs.getBoolean(KEY_VIBRATION, true)

        val darkModeStr = prefs.getString(KEY_DARK_MODE, DarkModeOption.OLED_DARK.name)
        val darkMode = try {
            DarkModeOption.valueOf(darkModeStr ?: DarkModeOption.OLED_DARK.name)
        } catch (_: Exception) {
            DarkModeOption.OLED_DARK
        }

        val pauseActionStr = prefs.getString(KEY_PAUSE_ACTION, PauseActionPreference.RESUME_PRIMARY.name)
        val pauseAction = try {
            PauseActionPreference.valueOf(pauseActionStr ?: PauseActionPreference.RESUME_PRIMARY.name)
        } catch (_: Exception) {
            PauseActionPreference.RESUME_PRIMARY
        }

        val keepAwake = prefs.getBoolean(KEY_KEEP_AWAKE, true)
        val isHardRound = prefs.getBoolean(KEY_IS_HARD_ROUND, false)

        return TimerConfig(
            totalRounds = totalRounds,
            roundDurationSeconds = roundSec,
            restDurationSeconds = restSec,
            prepDurationSeconds = prepSec,
            warningSeconds = warnSec,
            warningSoundEnabled = warnEnabled,
            bellSoundStyle = bellStyle,
            warningSoundStyle = warnStyle,
            soundVolume = volume,
            gymAudioBoost = gymBoost,
            vibrationEnabled = vibration,
            darkModeOption = darkMode,
            pauseActionPreference = pauseAction,
            keepScreenAwake = keepAwake,
            isHardRoundMode = isHardRound
        )
    }

    fun updateConfig(newConfig: TimerConfig) {
        prefs.edit()
            .putInt(KEY_TOTAL_ROUNDS, newConfig.totalRounds)
            .putInt(KEY_ROUND_SEC, newConfig.roundDurationSeconds)
            .putInt(KEY_REST_SEC, newConfig.restDurationSeconds)
            .putInt(KEY_PREP_SEC, newConfig.prepDurationSeconds)
            .putInt(KEY_WARN_SEC, newConfig.warningSeconds)
            .putBoolean(KEY_WARN_ENABLED, newConfig.warningSoundEnabled)
            .putString(KEY_BELL_STYLE, newConfig.bellSoundStyle.name)
            .putString(KEY_WARN_STYLE, newConfig.warningSoundStyle.name)
            .putFloat(KEY_VOLUME, newConfig.soundVolume)
            .putBoolean(KEY_GYM_BOOST, newConfig.gymAudioBoost)
            .putBoolean(KEY_VIBRATION, newConfig.vibrationEnabled)
            .putString(KEY_DARK_MODE, newConfig.darkModeOption.name)
            .putString(KEY_PAUSE_ACTION, newConfig.pauseActionPreference.name)
            .putBoolean(KEY_KEEP_AWAKE, newConfig.keepScreenAwake)
            .putBoolean(KEY_IS_HARD_ROUND, newConfig.isHardRoundMode)
            .apply()

        _configFlow.value = newConfig
    }

    companion object {
        private const val KEY_TOTAL_ROUNDS = "total_rounds"
        private const val KEY_ROUND_SEC = "round_duration_sec"
        private const val KEY_REST_SEC = "rest_duration_sec"
        private const val KEY_PREP_SEC = "prep_duration_sec"
        private const val KEY_WARN_SEC = "warn_duration_sec"
        private const val KEY_WARN_ENABLED = "warn_sound_enabled"
        private const val KEY_BELL_STYLE = "bell_sound_style"
        private const val KEY_WARN_STYLE = "warn_sound_style"
        private const val KEY_VOLUME = "sound_volume"
        private const val KEY_GYM_BOOST = "gym_audio_boost"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_DARK_MODE = "dark_mode_option"
        private const val KEY_PAUSE_ACTION = "pause_action_preference"
        private const val KEY_KEEP_AWAKE = "keep_screen_awake"
        private const val KEY_IS_HARD_ROUND = "is_hard_round"
    }
}

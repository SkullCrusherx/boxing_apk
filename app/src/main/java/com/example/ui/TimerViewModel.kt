package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.BoxingAudioEngine
import com.example.data.TimerPreferencesRepository
import com.example.model.BellSoundStyle
import com.example.model.PauseActionPreference
import com.example.model.TimerConfig
import com.example.model.TimerPhase
import com.example.model.TimerPreset
import com.example.model.WarningSoundStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TimerUiState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val currentRound: Int = 1,
    val totalRounds: Int = 3,
    val secondsRemaining: Int = 180,
    val phaseTotalSeconds: Int = 180,
    val totalElapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isWarningActive: Boolean = false,
    val completedRounds: Int = 0,
    val isHardRoundMode: Boolean = false
) {
    val progress: Float
        get() = if (phaseTotalSeconds > 0) {
            (secondsRemaining.toFloat() / phaseTotalSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val formattedRemaining: String
        get() {
            val mins = secondsRemaining / 60
            val secs = secondsRemaining % 60
            return "%02d:%02d".format(mins, secs)
        }

    val formattedElapsed: String
        get() {
            val mins = totalElapsedSeconds / 60
            val secs = totalElapsedSeconds % 60
            return "%02d:%02d".format(mins, secs)
        }
}

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TimerPreferencesRepository(application)
    val audioEngine = BoxingAudioEngine(application)

    val configFlow: StateFlow<TimerConfig> = repository.configFlow

    private val _uiState = MutableStateFlow(
        TimerUiState(
            totalRounds = configFlow.value.totalRounds,
            secondsRemaining = configFlow.value.roundDurationSeconds,
            phaseTotalSeconds = configFlow.value.roundDurationSeconds,
            isHardRoundMode = configFlow.value.isHardRoundMode
        )
    )
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var warningPlayedForCurrentRound = false

    init {
        viewModelScope.launch {
            configFlow.collect { config ->
                if (_uiState.value.phase == TimerPhase.IDLE) {
                    _uiState.update {
                        it.copy(
                            totalRounds = config.totalRounds,
                            secondsRemaining = config.roundDurationSeconds,
                            phaseTotalSeconds = config.roundDurationSeconds,
                            isHardRoundMode = config.isHardRoundMode
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isHardRoundMode = config.isHardRoundMode)
                    }
                }
            }
        }
    }

    fun onPrimaryButtonClicked() {
        val current = _uiState.value
        when {
            current.phase == TimerPhase.IDLE || current.phase == TimerPhase.COMPLETE -> {
                startTimer()
            }
            current.isRunning && !current.isPaused -> {
                pauseTimer()
            }
            current.isPaused -> {
                val config = configFlow.value
                if (config.pauseActionPreference == PauseActionPreference.RESTART_PRIMARY) {
                    restartCurrentRound()
                } else {
                    resumeTimer()
                }
            }
        }
    }

    fun startTimer() {
        val config = configFlow.value
        timerJob?.cancel()
        warningPlayedForCurrentRound = false

        if (config.prepDurationSeconds > 0) {
            _uiState.update {
                it.copy(
                    phase = TimerPhase.PREPARE,
                    currentRound = 1,
                    completedRounds = 0,
                    secondsRemaining = config.prepDurationSeconds,
                    phaseTotalSeconds = config.prepDurationSeconds,
                    totalElapsedSeconds = 0,
                    isRunning = true,
                    isPaused = false,
                    isWarningActive = false
                )
            }
        } else {
            // Immediate Round 1
            audioEngine.playBell(
                style = config.bellSoundStyle,
                volume = config.soundVolume,
                vibrate = config.vibrationEnabled,
                gymBoost = config.gymAudioBoost
            )
            _uiState.update {
                it.copy(
                    phase = TimerPhase.ROUND,
                    currentRound = 1,
                    completedRounds = 0,
                    secondsRemaining = config.roundDurationSeconds,
                    phaseTotalSeconds = config.roundDurationSeconds,
                    totalElapsedSeconds = 0,
                    isRunning = true,
                    isPaused = false,
                    isWarningActive = false
                )
            }
        }
        startTickerLoop()
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeTimer() {
        if (!_uiState.value.isPaused) return
        _uiState.update { it.copy(isPaused = false, isRunning = true) }
        startTickerLoop()
    }

    fun restartCurrentRound() {
        timerJob?.cancel()
        warningPlayedForCurrentRound = false
        val config = configFlow.value
        audioEngine.playBell(
            style = config.bellSoundStyle,
            volume = config.soundVolume,
            vibrate = config.vibrationEnabled,
            gymBoost = config.gymAudioBoost
        )

        _uiState.update {
            it.copy(
                phase = TimerPhase.ROUND,
                secondsRemaining = config.roundDurationSeconds,
                phaseTotalSeconds = config.roundDurationSeconds,
                isRunning = true,
                isPaused = false,
                isWarningActive = false
            )
        }
        startTickerLoop()
    }

    fun resetSession() {
        timerJob?.cancel()
        warningPlayedForCurrentRound = false
        val config = configFlow.value
        _uiState.update {
            TimerUiState(
                phase = TimerPhase.IDLE,
                currentRound = 1,
                totalRounds = config.totalRounds,
                secondsRemaining = config.roundDurationSeconds,
                phaseTotalSeconds = config.roundDurationSeconds,
                totalElapsedSeconds = 0,
                isRunning = false,
                isPaused = false,
                isWarningActive = false,
                completedRounds = 0,
                isHardRoundMode = config.isHardRoundMode
            )
        }
    }

    fun skipToNextRound() {
        val current = _uiState.value
        val config = configFlow.value
        if (current.currentRound < current.totalRounds) {
            warningPlayedForCurrentRound = false
            audioEngine.playBell(
                style = config.bellSoundStyle,
                volume = config.soundVolume,
                vibrate = config.vibrationEnabled,
                gymBoost = config.gymAudioBoost
            )
            _uiState.update {
                it.copy(
                    phase = TimerPhase.ROUND,
                    currentRound = it.currentRound + 1,
                    completedRounds = it.currentRound,
                    secondsRemaining = config.roundDurationSeconds,
                    phaseTotalSeconds = config.roundDurationSeconds,
                    isPaused = false,
                    isRunning = true,
                    isWarningActive = false
                )
            }
            if (timerJob?.isActive != true) {
                startTickerLoop()
            }
        } else {
            completeWorkout()
        }
    }

    fun skipToPreviousRound() {
        val current = _uiState.value
        val config = configFlow.value
        val prevRound = maxOf(1, current.currentRound - 1)
        warningPlayedForCurrentRound = false
        audioEngine.playBell(
            style = config.bellSoundStyle,
            volume = config.soundVolume,
            vibrate = config.vibrationEnabled,
            gymBoost = config.gymAudioBoost
        )
        _uiState.update {
            it.copy(
                phase = TimerPhase.ROUND,
                currentRound = prevRound,
                completedRounds = maxOf(0, prevRound - 1),
                secondsRemaining = config.roundDurationSeconds,
                phaseTotalSeconds = config.roundDurationSeconds,
                isPaused = false,
                isRunning = true,
                isWarningActive = false
            )
        }
        if (timerJob?.isActive != true) {
            startTickerLoop()
        }
    }

    fun applyPreset(preset: TimerPreset) {
        val updated = configFlow.value.copy(
            totalRounds = preset.rounds,
            roundDurationSeconds = preset.roundDurationSec,
            restDurationSeconds = preset.restDurationSec,
            warningSeconds = preset.warningSec,
            prepDurationSeconds = preset.prepSec,
            isHardRoundMode = preset.isHardRound,
            bellSoundStyle = if (preset.isHardRound) BellSoundStyle.THUNDEROUS_STEEL else configFlow.value.bellSoundStyle,
            gymAudioBoost = if (preset.isHardRound) true else configFlow.value.gymAudioBoost
        )
        repository.updateConfig(updated)
        resetSession()
    }

    fun toggleHardRoundMode() {
        val current = configFlow.value
        if (!current.isHardRoundMode) {
            // Switch to Hard Round Championship mode
            val hardRoundPreset = TimerConfig.PRESETS.first { it.isHardRound }
            applyPreset(hardRoundPreset)
        } else {
            // Switch back to Classic Boxing
            val classicPreset = TimerConfig.PRESETS.first { it.name == "Classic Boxing" }
            applyPreset(classicPreset)
        }
    }

    fun updateConfig(config: TimerConfig) {
        repository.updateConfig(config)
    }

    fun previewBell(style: BellSoundStyle) {
        val config = configFlow.value
        audioEngine.playBell(
            style = style,
            volume = config.soundVolume,
            vibrate = config.vibrationEnabled,
            gymBoost = config.gymAudioBoost
        )
    }

    fun previewWarning(style: WarningSoundStyle) {
        val config = configFlow.value
        audioEngine.playWarning(
            style = style,
            volume = config.soundVolume,
            vibrate = config.vibrationEnabled,
            gymBoost = config.gymAudioBoost
        )
    }

    private fun startTickerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val current = _uiState.value
                if (!current.isRunning || current.isPaused) break

                val config = configFlow.value
                val newRemaining = current.secondsRemaining - 1
                val newElapsed = current.totalElapsedSeconds + 1

                when (current.phase) {
                    TimerPhase.PREPARE -> {
                        if (newRemaining <= 3 && newRemaining > 0) {
                            audioEngine.playCountdownTick(config.soundVolume)
                        }
                        if (newRemaining <= 0) {
                            // Ring Bell! Round 1 begins
                            audioEngine.playBell(
                                style = config.bellSoundStyle,
                                volume = config.soundVolume,
                                vibrate = config.vibrationEnabled,
                                gymBoost = config.gymAudioBoost
                            )
                            warningPlayedForCurrentRound = false
                            _uiState.update {
                                it.copy(
                                    phase = TimerPhase.ROUND,
                                    secondsRemaining = config.roundDurationSeconds,
                                    phaseTotalSeconds = config.roundDurationSeconds,
                                    totalElapsedSeconds = newElapsed,
                                    isWarningActive = false
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    secondsRemaining = newRemaining,
                                    totalElapsedSeconds = newElapsed
                                )
                            }
                        }
                    }

                    TimerPhase.ROUND -> {
                        // Check 10-second warning threshold
                        val isWarningZone = newRemaining in 1..config.warningSeconds
                        if (config.warningSoundEnabled && newRemaining == config.warningSeconds && !warningPlayedForCurrentRound) {
                            warningPlayedForCurrentRound = true
                            audioEngine.playWarning(
                                style = config.warningSoundStyle,
                                volume = config.soundVolume,
                                vibrate = config.vibrationEnabled,
                                gymBoost = config.gymAudioBoost
                            )
                        }

                        if (newRemaining <= 0) {
                            // Round finished! Ring Bell
                            audioEngine.playBell(
                                style = config.bellSoundStyle,
                                volume = config.soundVolume,
                                vibrate = config.vibrationEnabled,
                                gymBoost = config.gymAudioBoost
                            )
                            warningPlayedForCurrentRound = false

                            if (current.currentRound >= current.totalRounds) {
                                completeWorkout(newElapsed)
                            } else {
                                // Transition to Rest
                                _uiState.update {
                                    it.copy(
                                        phase = TimerPhase.REST,
                                        completedRounds = current.currentRound,
                                        secondsRemaining = config.restDurationSeconds,
                                        phaseTotalSeconds = config.restDurationSeconds,
                                        totalElapsedSeconds = newElapsed,
                                        isWarningActive = false
                                    )
                                }
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    secondsRemaining = newRemaining,
                                    totalElapsedSeconds = newElapsed,
                                    isWarningActive = isWarningZone
                                )
                            }
                        }
                    }

                    TimerPhase.REST -> {
                        if (newRemaining <= 3 && newRemaining > 0) {
                            audioEngine.playCountdownTick(config.soundVolume * 0.8f)
                        }
                        if (newRemaining <= 0) {
                            // Rest finished! Ring Bell & next Round begins
                            audioEngine.playBell(
                                style = config.bellSoundStyle,
                                volume = config.soundVolume,
                                vibrate = config.vibrationEnabled,
                                gymBoost = config.gymAudioBoost
                            )
                            warningPlayedForCurrentRound = false
                            _uiState.update {
                                it.copy(
                                    phase = TimerPhase.ROUND,
                                    currentRound = it.currentRound + 1,
                                    secondsRemaining = config.roundDurationSeconds,
                                    phaseTotalSeconds = config.roundDurationSeconds,
                                    totalElapsedSeconds = newElapsed,
                                    isWarningActive = false
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    secondsRemaining = newRemaining,
                                    totalElapsedSeconds = newElapsed
                                )
                            }
                        }
                    }

                    TimerPhase.IDLE, TimerPhase.COMPLETE -> {
                        break
                    }
                }
            }
        }
    }

    private fun completeWorkout(elapsed: Int = _uiState.value.totalElapsedSeconds) {
        timerJob?.cancel()
        val config = configFlow.value
        audioEngine.playBell(
            style = BellSoundStyle.TRIPLE_MATCH_BELL,
            volume = config.soundVolume,
            vibrate = config.vibrationEnabled,
            gymBoost = config.gymAudioBoost
        )
        _uiState.update {
            it.copy(
                phase = TimerPhase.COMPLETE,
                completedRounds = it.totalRounds,
                secondsRemaining = 0,
                totalElapsedSeconds = elapsed,
                isRunning = false,
                isPaused = false,
                isWarningActive = false
            )
        }
    }
}

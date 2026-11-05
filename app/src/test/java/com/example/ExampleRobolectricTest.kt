package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.BellSoundStyle
import com.example.model.WarningSoundStyle
import com.example.ui.TimerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Boxing Timer", appName)
    }

    @Test
    fun `timer viewmodel initializes with 4s warning and user audio styles`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TimerViewModel(app)
        val config = viewModel.configFlow.value
        val uiState = viewModel.uiState.value

        assertEquals(3, config.totalRounds)
        assertEquals(180, config.roundDurationSeconds)
        assertEquals(60, config.restDurationSeconds)
        assertEquals(4, config.warningSeconds)
        assertTrue(config.warningSoundEnabled)
        assertTrue(config.gymAudioBoost)
        assertEquals(BellSoundStyle.BOXING_RING_BELL, config.bellSoundStyle)
        assertEquals(WarningSoundStyle.BOXING_WARNING_4S, config.warningSoundStyle)

        assertEquals(3, uiState.totalRounds)
        assertEquals(180, uiState.secondsRemaining)
    }

    @Test
    fun `toggle hard round mode activates 5 rounds and 45s rest with 4s warning`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TimerViewModel(app)

        viewModel.toggleHardRoundMode()
        val hardConfig = viewModel.configFlow.value
        val hardUiState = viewModel.uiState.value

        assertTrue(hardConfig.isHardRoundMode)
        assertTrue(hardUiState.isHardRoundMode)
        assertEquals(5, hardConfig.totalRounds)
        assertEquals(180, hardConfig.roundDurationSeconds)
        assertEquals(45, hardConfig.restDurationSeconds)
        assertEquals(4, hardConfig.warningSeconds)
    }
}

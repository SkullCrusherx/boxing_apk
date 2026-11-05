package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.BoxingTimerScreen
import com.example.ui.TimerViewModel
import com.example.ui.theme.BoxingTimerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val config by viewModel.configFlow.collectAsState()
            val uiState by viewModel.uiState.collectAsState()

            BoxingTimerTheme(darkModeOption = config.darkModeOption) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    BoxingTimerScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        config = config
                    )
                }
            }
        }
    }
}

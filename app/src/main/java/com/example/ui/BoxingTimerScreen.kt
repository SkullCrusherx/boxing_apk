package com.example.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DarkModeOption
import com.example.model.PauseActionPreference
import com.example.model.TimerConfig
import com.example.model.TimerPhase
import com.example.ui.theme.BoxingChampionGold
import com.example.ui.theme.BoxingPrepareAmber
import com.example.ui.theme.BoxingRed
import com.example.ui.theme.BoxingRestCyan
import com.example.ui.theme.BoxingWarningOrange

@Composable
fun BoxingTimerScreen(
    viewModel: TimerViewModel,
    uiState: TimerUiState,
    config: TimerConfig
) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var showPresets by remember { mutableStateOf(false) }

    // Keep screen on during training sessions
    DisposableEffect(config.keepScreenAwake) {
        val window = (context as? Activity)?.window
        if (config.keepScreenAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Dynamic phase-based theme accent colors
    val phaseColor by animateColorAsState(
        targetValue = when {
            uiState.isWarningActive -> BoxingWarningOrange
            uiState.phase == TimerPhase.ROUND -> if (uiState.isHardRoundMode) Color(0xFFFF1744) else BoxingRed
            uiState.phase == TimerPhase.REST -> BoxingRestCyan
            uiState.phase == TimerPhase.PREPARE -> BoxingPrepareAmber
            uiState.phase == TimerPhase.COMPLETE -> BoxingChampionGold
            else -> if (uiState.isHardRoundMode) Color(0xFFFF1744) else BoxingRed
        },
        label = "phaseColorAnimation"
    )

    // Animated warning pulse for the last 10 seconds
    val infiniteTransition = rememberInfiniteTransition(label = "warningPulse")
    val warningScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (uiState.isWarningActive) 1.09f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scalePulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // App Identity & Icon Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("app_header")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, phaseColor.copy(alpha = 0.5f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.boxing_timer_icon_1793892943581),
                                    contentDescription = "Boxing Icon",
                                    modifier = Modifier.size(32.dp).clip(CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BOXING TIMER",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (uiState.isHardRoundMode) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFFFF1744),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "HARD",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${config.totalRounds} Rds × ${config.roundDurationSeconds / 60}m | ${config.restDurationSeconds}s Rest",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Top Action Icons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Presets Button
                        IconButton(
                            onClick = { showPresets = true },
                            modifier = Modifier.testTag("presets_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Presets",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Quick Dark Mode Toggle
                        IconButton(
                            onClick = {
                                val nextMode = when (config.darkModeOption) {
                                    DarkModeOption.OLED_DARK -> DarkModeOption.LIGHT
                                    DarkModeOption.LIGHT -> DarkModeOption.CARBON_DARK
                                    DarkModeOption.CARBON_DARK -> DarkModeOption.OLED_DARK
                                    DarkModeOption.SYSTEM -> DarkModeOption.OLED_DARK
                                }
                                viewModel.updateConfig(config.copy(darkModeOption = nextMode))
                            },
                            modifier = Modifier.testTag("dark_mode_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (config.darkModeOption == DarkModeOption.LIGHT) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Theme Toggle",
                                tint = phaseColor
                            )
                        }

                        // Settings Button
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // QUICK CHIPS ROW (Hard Round Mode Toggle & Standard Boxing)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.isHardRoundMode,
                        onClick = { viewModel.toggleHardRoundMode() },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = if (uiState.isHardRoundMode) Color.White else Color(0xFFFF3D00),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isHardRoundMode) "Hard Round (Active)" else "Switch to Hard Round",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF1744),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("hard_round_toggle_chip")
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BoxingChampionGold.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable {
                            viewModel.previewBell(config.bellSoundStyle)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Test Bell",
                                tint = BoxingChampionGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Max Bell Strike",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ROUND PROGRESS PILLS
                RoundPillsStrip(
                    totalRounds = uiState.totalRounds,
                    currentRound = uiState.currentRound,
                    phase = uiState.phase,
                    phaseColor = phaseColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // CIRCULAR TIMER HERO SECTION
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val circleSize = minOf(maxWidth * 0.85f, 320.dp)

                Box(
                    modifier = Modifier.size(circleSize),
                    contentAlignment = Alignment.Center
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = uiState.progress,
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                        label = "progressSweep"
                    )

                    // Dial Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        val strokeWidth = 14.dp.toPx()

                        // Background Track
                        drawCircle(
                            color = Color(0x22888888),
                            style = Stroke(width = strokeWidth)
                        )

                        // Active Progress Arc
                        val sweepAngle = animatedProgress * 360f
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    phaseColor.copy(alpha = 0.7f),
                                    phaseColor
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Content inside Dial
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Phase Header Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = phaseColor.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, phaseColor.copy(alpha = 0.6f)),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = when (uiState.phase) {
                                        TimerPhase.ROUND -> if (uiState.isHardRoundMode) Icons.Default.LocalFireDepartment else Icons.Default.SportsMma
                                        TimerPhase.REST -> Icons.Default.Pause
                                        TimerPhase.PREPARE -> Icons.Default.PlayArrow
                                        else -> Icons.Default.NotificationsActive
                                    },
                                    contentDescription = null,
                                    tint = phaseColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (uiState.phase) {
                                        TimerPhase.ROUND -> if (uiState.isHardRoundMode) "HARD ROUND ${uiState.currentRound} / ${uiState.totalRounds}" else "ROUND ${uiState.currentRound} / ${uiState.totalRounds}"
                                        TimerPhase.REST -> "REST INTERVAL"
                                        TimerPhase.PREPARE -> "GET READY"
                                        TimerPhase.COMPLETE -> "CHAMPION!"
                                        TimerPhase.IDLE -> if (uiState.isHardRoundMode) "HARD RD 1 / ${uiState.totalRounds}" else "ROUND 1 / ${uiState.totalRounds}"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp,
                                    color = phaseColor
                                )
                            }
                        }

                        // Massive Digits
                        Text(
                            text = uiState.formattedRemaining,
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (uiState.isWarningActive) BoxingWarningOrange else MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .scale(if (uiState.isWarningActive) warningScale else 1.0f)
                                .testTag("timer_display_text")
                        )

                        // Total Workout Elapsed Tracker
                        Text(
                            text = "TOTAL: ${uiState.formattedElapsed}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // 10-SECOND WARNING BANNER
            AnimatedVisibility(
                visible = uiState.isWarningActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BoxingWarningOrange.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, BoxingWarningOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .scale(warningScale)
                        .testTag("warning_10s_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Warning",
                            tint = BoxingWarningOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LAST ${config.warningSeconds} SECONDS! ${if (uiState.isHardRoundMode) "DIG DEEP & FINISH HARD!" else "FINISH STRONG!"}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = BoxingWarningOrange,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Status message when paused or complete
            if (uiState.isPaused) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "⏸ SESSION PAUSED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            } else if (uiState.phase == TimerPhase.COMPLETE) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BoxingChampionGold.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BoxingChampionGold),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "🏆 ALL ${uiState.totalRounds} ROUNDS COMPLETED!",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = BoxingChampionGold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CONTROLS SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PRIMARY ACTION BUTTON (Start, Pause, Resume/Restart)
                val isRunning = uiState.isRunning && !uiState.isPaused
                val isPaused = uiState.isPaused
                val isIdleOrComplete = uiState.phase == TimerPhase.IDLE || uiState.phase == TimerPhase.COMPLETE

                val primaryButtonText = when {
                    isIdleOrComplete -> if (uiState.isHardRoundMode) "START HARD ROUND 1" else "START ROUND 1"
                    isRunning -> "PAUSE"
                    isPaused && config.pauseActionPreference == PauseActionPreference.RESTART_PRIMARY -> "RESTART ROUND"
                    isPaused -> "RESUME"
                    else -> "START"
                }

                val primaryButtonIcon = when {
                    isIdleOrComplete -> Icons.Default.PlayArrow
                    isRunning -> Icons.Default.Pause
                    isPaused && config.pauseActionPreference == PauseActionPreference.RESTART_PRIMARY -> Icons.Default.Replay
                    isPaused -> Icons.Default.PlayArrow
                    else -> Icons.Default.PlayArrow
                }

                val primaryButtonColor = when {
                    isIdleOrComplete -> if (uiState.isHardRoundMode) Color(0xFFFF1744) else BoxingRed
                    isRunning -> BoxingWarningOrange
                    isPaused -> if (uiState.isHardRoundMode) Color(0xFFFF1744) else BoxingRed
                    else -> BoxingRed
                }

                Button(
                    onClick = { viewModel.onPrimaryButtonClicked() },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryButtonColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("primary_timer_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = primaryButtonIcon,
                            contentDescription = primaryButtonText,
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = primaryButtonText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SECONDARY CONTROLS ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Round Button
                    OutlinedButton(
                        onClick = { viewModel.skipToPreviousRound() },
                        enabled = uiState.currentRound > 1 || uiState.phase == TimerPhase.ROUND,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("prev_round_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Prev Round",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prev", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // If paused: provide complementary Resume or Restart button
                    if (isPaused) {
                        if (config.pauseActionPreference == PauseActionPreference.RESTART_PRIMARY) {
                            Button(
                                onClick = { viewModel.resumeTimer() },
                                colors = ButtonDefaults.buttonColors(containerColor = BoxingRestCyan),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("resume_secondary_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Resume",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.restartCurrentRound() },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("restart_secondary_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Restart Round",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restart Rd", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Manual Boosted Bell Trigger Button
                        OutlinedButton(
                            onClick = {
                                viewModel.previewBell(config.bellSoundStyle)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("ring_bell_manual_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Hard Bell",
                                tint = BoxingChampionGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hard Bell", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Next Round Button
                    OutlinedButton(
                        onClick = { viewModel.skipToNextRound() },
                        enabled = uiState.currentRound < uiState.totalRounds || uiState.phase == TimerPhase.ROUND,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("next_round_button")
                    ) {
                        Text("Next", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Round",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Reset Session Button
                if (uiState.phase != TimerPhase.IDLE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.resetSession() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("reset_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Timer",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset All Rounds to Start", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Settings Sheet
        if (showSettings) {
            SettingsSheet(
                config = config,
                onConfigChange = { viewModel.updateConfig(it) },
                onPreviewBell = { viewModel.previewBell(it) },
                onPreviewWarning = { viewModel.previewWarning(it) },
                onDismiss = { showSettings = false }
            )
        }

        // Presets Sheet
        if (showPresets) {
            PresetsSheet(
                onSelectPreset = { preset ->
                    viewModel.applyPreset(preset)
                },
                onDismiss = { showPresets = false }
            )
        }
    }
}

@Composable
private fun RoundPillsStrip(
    totalRounds: Int,
    currentRound: Int,
    phase: TimerPhase,
    phaseColor: Color
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed((1..totalRounds).toList()) { _, roundNumber ->
            val isCurrent = roundNumber == currentRound && phase != TimerPhase.COMPLETE
            val isDone = roundNumber < currentRound || phase == TimerPhase.COMPLETE

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when {
                    isCurrent -> phaseColor
                    isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                border = if (isCurrent) null else androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .padding(horizontal = 3.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "R$roundNumber",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                    color = when {
                        isCurrent -> Color.White
                        isDone -> MaterialTheme.colorScheme.onBackground
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

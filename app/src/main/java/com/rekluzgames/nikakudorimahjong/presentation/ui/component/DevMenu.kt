/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rekluzgames.nikakudorimahjong.domain.model.GameState
import com.rekluzgames.nikakudorimahjong.domain.model.Difficulty
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel

@Composable
fun DevMenu(
    gameViewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val autoPlayEnabled by gameViewModel.isAutoPlayEnabled.collectAsState()
    val infiniteTimeEnabled by gameViewModel.isInfiniteTimeEnabled.collectAsState()
    val skipAnimationsEnabled by gameViewModel.isSkipAnimationsEnabled.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .background(Color(0xFF1A1A1A))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Text(
                    "⚙️ SECRET DEV MENU",
                    color = Color(0xFF00FF00),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "For development eyes only",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic
                )

                HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)

                // QUICK GAME MODES
                Text(
                    "🎮 QUICK GAME MODES",
                    color = Color(0xFFFFB300),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                DevMenuButton(
                    label = "4-Tile Game (DEV)",
                    onClick = {
                        gameViewModel.startNewGame(Difficulty.DEV)
                        onDismiss()
                    }
                )

                DevMenuButton(
                    label = "Easy Game",
                    onClick = {
                        gameViewModel.startNewGame(Difficulty.EASY)
                        onDismiss()
                    }
                )

                HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)

                // GAME STATE JUMPING
                Text(
                    "⏩ FORCE STATE TRANSITIONS",
                    color = Color(0xFFFFB300),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                DevMenuButton(
                    label = "Jump to QUOTE",
                    onClick = {
                        gameViewModel.forceState(GameState.QUOTE)
                        onDismiss()
                    }
                )

                DevMenuButton(
                    label = "Jump to SCORE_ENTRY",
                    onClick = {
                        gameViewModel.forceState(GameState.SCORE_ENTRY)
                        onDismiss()
                    }
                )

                DevMenuButton(
                    label = "Jump to PLAYING",
                    onClick = {
                        gameViewModel.forceState(GameState.PLAYING)
                        onDismiss()
                    }
                )

                HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)

                // AUTO-PLAY
                Text(
                    "🤖 AUTO-PLAY",
                    color = Color(0xFFFFB300),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                DevMenuButton(
                    label = if (autoPlayEnabled) "◼ Disable Auto-Play" else "▶ Enable Auto-Play",
                    backgroundColor = if (autoPlayEnabled) Color(0xFF6B2B2B) else Color(0xFF2B6B2B),
                    onClick = { gameViewModel.toggleAutoPlay() }
                )

                HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)

                // TIMER CONTROLS
                Text(
                    "⏱️ TIMER CONTROLS",
                    color = Color(0xFFFFB300),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                DevMenuButton(
                    label = if (infiniteTimeEnabled) "◼ Disable Infinite Time" else "▶ Enable Infinite Time",
                    backgroundColor = if (infiniteTimeEnabled) Color(0xFF6B2B2B) else Color(0xFF2B6B2B),
                    onClick = { gameViewModel.toggleInfiniteTime() }
                )

                DevMenuButton(
                    label = "Jump to 5 seconds",
                    onClick = { gameViewModel.jumpToTimeRemaining(5) }
                )

                DevMenuButton(
                    label = "Jump to 30 seconds",
                    onClick = { gameViewModel.jumpToTimeRemaining(30) }
                )

                HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)

                // UI & ANIMATION
                Text(
                    "🎨 UI & ANIMATION",
                    color = Color(0xFFFFB300),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                DevMenuButton(
                    label = if (skipAnimationsEnabled) "◼ Disable Skip Animations" else "▶ Enable Skip Animations",
                    backgroundColor = if (skipAnimationsEnabled) Color(0xFF6B2B2B) else Color(0xFF2B6B2B),
                    onClick = { gameViewModel.toggleSkipAnimations() }
                )

                HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)

                // UTILITIES
                Text(
                    "🔧 UTILITIES",
                    color = Color(0xFFFFB300),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                DevMenuButton(
                    label = "Export Game State (JSON)",
                    onClick = {
                        gameViewModel.exportGameStateToLogs()
                    }
                )

                DevMenuButton(
                    label = "Force Unwinnable Board",
                    onClick = {
                        gameViewModel.forceUnwinnableBoard()
                        onDismiss()
                    }
                )

                DevMenuButton(
                    label = "Force All Pairs Available",
                    onClick = {
                        gameViewModel.forceAllPairsAvailable()
                        onDismiss()
                    }
                )

                HorizontalDivider(color = Color(0xFF333333), thickness = 1.dp)

                // CLOSE BUTTON
                DevMenuButton(
                    label = "CLOSE MENU",
                    backgroundColor = Color(0xFF2B2B2B),
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun DevMenuButton(
    label: String,
    backgroundColor: Color = Color(0xFF2B5A2B),
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
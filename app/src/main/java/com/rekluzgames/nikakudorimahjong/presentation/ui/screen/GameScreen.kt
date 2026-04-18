/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.ui.screen

import android.app.Activity
import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.rekluzgames.nikakudorimahjong.R
import com.rekluzgames.nikakudorimahjong.domain.model.GameState
import com.rekluzgames.nikakudorimahjong.presentation.effects.ParticleOverlay
import com.rekluzgames.nikakudorimahjong.presentation.ui.component.*
import com.rekluzgames.nikakudorimahjong.presentation.ui.theme.MidnightBlue
import com.rekluzgames.nikakudorimahjong.presentation.viewmodel.GameViewModel
import kotlinx.coroutines.delay

@SuppressLint("DiscouragedApi", "LocalContextResourcesRead")
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    settingsViewModel: com.rekluzgames.nikakudorimahjong.presentation.viewmodel.SettingsViewModel,
    onLanguageChange: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val isDevMenuOpen by viewModel.isDevMenuOpen.collectAsState()
    val context = LocalContext.current
    var showLanguageOverlay by remember { mutableStateOf(false) }
    var showQuoteOverlay by remember { mutableStateOf(false) }

    // --- ZOOM & PAN STATE ---
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val screenPadding = if (!settingsState.isFullScreen)
        Modifier.windowInsetsPadding(WindowInsets.systemBars)
    else
        Modifier.padding(0.dp)

    val bgResId = remember(uiState.backgroundImageName) {
        context.resources.getIdentifier(
            uiState.backgroundImageName, "drawable", context.packageName
        )
    }

    LaunchedEffect(uiState.gameState) {
        if (uiState.gameState == GameState.QUOTE) {
            showQuoteOverlay = false
            delay(2000)
            showQuoteOverlay = true
        } else {
            showQuoteOverlay = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlue)
            .then(screenPadding)
    ) {
        if (uiState.gameState == GameState.LOADING) {
            LoadingOverlay()
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // 1. GAME BOARD AREA (Left Side)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    // LAYER A: Static Background
                    val isDimmed = uiState.gameState == GameState.PLAYING
                    val backgroundAlpha by animateFloatAsState(
                        targetValue = if (isDimmed) 0.3f else 1f,
                        animationSpec = tween(durationMillis = 1000),
                        label = "BackgroundBrightness"
                    )

                    if (bgResId != 0) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(bgResId)
                                .decoderFactory(GifDecoder.Factory())
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alpha = backgroundAlpha,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // LAYER B: Location Overlay (displays location text on image)
                    if (uiState.gameState == GameState.WON ||
                        uiState.gameState == GameState.SCORE ||
                        uiState.gameState == GameState.SCORE_ENTRY ||
                        uiState.gameState == GameState.QUOTE) {
                        LocationOverlay(backgroundImageName = uiState.backgroundImageName)
                    }

                    // Reset zoom & pan immediately when zoom is disabled so the
                    // board never stays stuck in a zoomed-in state after toggle.
                    LaunchedEffect(settingsState.isZoomEnabled) {
                        if (!settingsState.isZoomEnabled) {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    }

                    // LAYER C: Zoomable Board
                    // Single unified pointerInput block handles double-tap reset,
                    // zoom, and pan. Using one block avoids pointer-input collision
                    // where competing modifiers fight over the same touch stream.
                    //
                    // Double-tap strategy: record timestamp on pointer UP (not down).
                    // This matches how the Android View system's GestureDetector works
                    // and avoids the edge case of triggering a reset while the finger
                    // is still held on the second tap press.
                    //
                    // Position check: both taps must land within doubleTapSlop of each
                    // other. This prevents two fast tile selections on different tiles
                    // from being mistaken for a double-tap reset.
                    //
                    // isZoomEnabled is the pointerInput key so the gesture handler
                    // restarts cleanly whenever the setting changes.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(settingsState.isZoomEnabled) {
                                val touchSlop    = viewConfiguration.touchSlop
                                val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis

                                // Tracks the time and position of the last confirmed tap-up.
                                // Both are required to distinguish a double-tap-to-reset
                                // from two fast tile selections on different tiles.
                                // Declared outside awaitEachGesture so they persist
                                // across gesture cycles.
                                var lastTapUpTime     = 0L
                                var lastTapUpPosition = Offset.Zero

                                // How far apart two taps can be and still count as a
                                // double-tap. Using 2× touchSlop gives ~16dp of tolerance,
                                // matching Android's own DoubleTapSlop constant.
                                val doubleTapSlop = touchSlop * 2

                                awaitEachGesture {
                                    // Wait for first finger down on the Initial pass so we
                                    // intercept before the tile grid sees the event.
                                    val down         = awaitFirstDown(pass = PointerEventPass.Initial)
                                    val downPosition = down.position

                                    var isTransforming = false
                                    var totalPan       = Offset.Zero

                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val zoom  = event.calculateZoom()
                                        val pan   = event.calculatePan()
                                        totalPan += pan

                                        val allUp = event.changes.all { !it.pressed }

                                        // Only apply zoom/pan transforms when the feature
                                        // is enabled. The gesture loop still runs when
                                        // disabled so single taps pass through normally.
                                        if (settingsState.isZoomEnabled) {
                                            if (!isTransforming &&
                                                (zoom != 1f || totalPan.getDistance() > touchSlop)
                                            ) {
                                                isTransforming = true
                                            }

                                            if (isTransforming) {
                                                // Apply zoom, clamped between 1× and 3×.
                                                scale = (scale * zoom).coerceIn(1f, 3f)
                                                offset = if (scale > 1f) offset + pan else Offset.Zero
                                                // Consume so tiles don't receive pan/zoom events.
                                                event.changes.forEach { it.consume() }
                                            }
                                        }

                                        // All fingers lifted — evaluate double-tap on UP.
                                        // Checking on UP (not down) is critical:
                                        //   • The tile's click handler fires on up, so consuming
                                        //     here still suppresses the spurious tile click.
                                        //   • We never consume a down that hasn't fully resolved,
                                        //     so single taps flow through to tiles with zero delay.
                                        if (allUp && !isTransforming) {
                                            val now  = System.currentTimeMillis()
                                            val dist = (downPosition - lastTapUpPosition).getDistance()
                                            val isWithinTime  = now - lastTapUpTime < doubleTapTimeout
                                            val isWithinSpace = dist < doubleTapSlop

                                            if (settingsState.isZoomEnabled && isWithinTime && isWithinSpace) {
                                                // Confirmed double-tap on roughly the same spot:
                                                // reset zoom & pan.
                                                scale  = 1f
                                                offset = Offset.Zero
                                                // Consume this up-event so the tile grid does not
                                                // register it as a tile selection.
                                                event.changes.forEach { it.consume() }
                                                // Prevent triple-tap from triggering again.
                                                lastTapUpTime = 0L
                                            } else {
                                                // Either too slow, too far apart, or zoom is
                                                // disabled — treat as a fresh first tap.
                                                // Do NOT consume — let it fall through to the
                                                // tile grid on the Main pass so tile selection
                                                // works with zero latency.
                                                lastTapUpTime     = now
                                                lastTapUpPosition = downPosition
                                            }
                                            break
                                        }

                                        if (allUp) break
                                    }
                                }
                            }
                            .graphicsLayer(
                                scaleX      = scale,
                                scaleY      = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        BoardGrid(
                            uiState            = uiState,
                            onTileClick        = { r, c -> viewModel.handleTileClick(r, c) },
                            onLayeredTileClick = { id -> viewModel.handleLayeredTileClick(id) }
                        )
                    }
                } // End Game Board Area Box

                // 2. SIDE MENU (Right Side)
                Column(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .width(125.dp)
                        .fillMaxHeight()
                        .background(Color(0x99000000), RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    MenuPillButton(
                        text  = stringResource(R.string.btn_menu),
                        color = Color(0xFF2A2A2A)
                    ) { viewModel.changeState(GameState.PAUSED) }

                    val hText  = if (uiState.canFinish) stringResource(R.string.btn_finish) else stringResource(R.string.btn_hint)
                    val hColor = if (uiState.canFinish) Color(0xFFCC2200) else Color(0xFF00BFFF)
                    MenuPillButton(text = hText, color = hColor) { viewModel.getHint() }

                    MenuPillButton(
                        text    = stringResource(R.string.btn_shuffle_format, uiState.shufflesRemaining),
                        enabled = uiState.shufflesRemaining > 0,
                        color   = Color(0xFF708090)
                    ) { viewModel.shuffle() }

                    MenuPillButton(
                        text    = stringResource(R.string.btn_undo),
                        enabled = uiState.canUndo,
                        color   = Color(0xFF708090)
                    ) { viewModel.undo() }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))

                    MenuPillButton(text = stringResource(R.string.btn_settings), color = Color(0xFF2A2A2A)) { viewModel.changeState(GameState.OPTIONS) }
                    MenuPillButton(text = stringResource(R.string.btn_boards),   color = Color(0xFF2A2A2A)) { viewModel.changeState(GameState.BOARDS) }
                    MenuPillButton(
                        text = stringResource(R.string.setting_zoom) + ": " +
                                if (settingsState.isZoomEnabled)
                                    stringResource(R.string.status_on)
                                else
                                    stringResource(R.string.status_off),
                        color = if (settingsState.isZoomEnabled) Color(0xFF00BFFF) else Color(0xFF708090)
                    ) { settingsViewModel.toggleZoom() }

                    Spacer(modifier = Modifier.weight(1f))

                    if (uiState.remainingTilesCount > 0) {
                        Text(
                            text       = stringResource(R.string.remaining_tiles_format, uiState.remainingTilesCount),
                            color      = Color.White.copy(alpha = 0.4f),
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TimerDisplay(viewModel = viewModel)
                }
            } // End Row

            // ===================================================================
            // DEV MENU OVERLAY
            // ===================================================================
            if (isDevMenuOpen) {
                DevMenu(
                    gameViewModel = viewModel,
                    onDismiss = { viewModel.closeDevMenu() }
                )
            }

            // ===================================================================
            // GAME STATE OVERLAYS
            // ===================================================================
            when (uiState.gameState) {
                GameState.PAUSED      -> PauseOverlay(viewModel) { (context as? Activity)?.finish() }
                GameState.BOARDS      -> BoardsOverlay(viewModel)
                GameState.OPTIONS     -> SettingsOverlay(viewModel, settingsViewModel, onShowLanguage = { showLanguageOverlay = true })
                GameState.ABOUT       -> AboutScreen(viewModel)
                GameState.SCORE_ENTRY -> ScoreEntryOverlay(viewModel)
                GameState.SCORE       -> ScoreboardOverlay(viewModel)
                GameState.NO_MOVES    -> StalemateOverlay(viewModel)
                GameState.QUOTE       -> if (showQuoteOverlay) QuoteOverlay(viewModel)
                else -> {}
            }

            ParticleOverlay(
                triggerVictoryStorm = uiState.gameState == GameState.WON || uiState.gameState == GameState.SCORE_ENTRY,
                selectionPositions  = emptyList(),
                isScoreEntryActive  = uiState.gameState == GameState.SCORE_ENTRY
            )

            if (showLanguageOverlay) {
                LanguageOverlay(
                    onSelect = { lang ->
                        showLanguageOverlay = false
                        onLanguageChange(lang)
                        viewModel.refreshQuote()
                    },
                    onClose = { showLanguageOverlay = false }
                )
            }
        }
    }
}
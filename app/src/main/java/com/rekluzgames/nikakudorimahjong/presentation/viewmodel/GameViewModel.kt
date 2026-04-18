/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rekluzgames.nikakudorimahjong.data.audio.SoundManager
import com.rekluzgames.nikakudorimahjong.data.audio.MusicManager
import com.rekluzgames.nikakudorimahjong.data.haptic.HapticManager
import com.rekluzgames.nikakudorimahjong.domain.engine.*
import com.rekluzgames.nikakudorimahjong.domain.model.*
import com.rekluzgames.nikakudorimahjong.domain.rules.HintFinder
import com.rekluzgames.nikakudorimahjong.domain.rules.LayeredHintFinder
import com.rekluzgames.nikakudorimahjong.presentation.score.ScoreManager
import com.rekluzgames.nikakudorimahjong.presentation.timer.GameTimer
import com.rekluzgames.nikakudorimahjong.presentation.ui.component.AboutInteractionHandler
import com.rekluzgames.nikakudorimahjong.presentation.ui.component.TileInteractionHandler
import com.rekluzgames.nikakudorimahjong.presentation.usecase.ShuffleUseCase
import com.rekluzgames.nikakudorimahjong.presentation.usecase.HintUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import com.rekluzgames.nikakudorimahjong.presentation.usecase.UndoUseCase
import com.rekluzgames.nikakudorimahjong.presentation.usecase.InteractionCoordinator
import com.rekluzgames.nikakudorimahjong.presentation.usecase.AutoCompleteUseCase
import com.rekluzgames.nikakudorimahjong.presentation.usecase.AutoCompleteStep
import com.rekluzgames.nikakudorimahjong.presentation.usecase.LayeredAutoCompleteStep
import android.util.Log

@HiltViewModel
class GameViewModel @Inject constructor(
    private val controller: GameSessionController,
    private val soundManager: SoundManager,
    private val musicManager: MusicManager,
    private val hapticManager: HapticManager,
    private val engine: GameEngine,
    private val layeredEngine: LayeredGameEngine,
    private val scoreManager: ScoreManager,
    private val gameTimer: GameTimer,
    private val postMatchProcessor: PostMatchProcessor,
    private val shuffleUseCase: ShuffleUseCase,
    private val hintUseCase: HintUseCase,
    private val undoUseCase: UndoUseCase,
    private val interactionCoordinator: InteractionCoordinator,
    private val autoCompleteUseCase: AutoCompleteUseCase,
) : ViewModel() {

    private val aboutHandler = AboutInteractionHandler(soundManager, hapticManager)

    private val _uiState = MutableStateFlow(GameUIState())
    val uiState = _uiState.asStateFlow()

    val timeSeconds: StateFlow<Int> = gameTimer.timeSeconds
    val timeFormatted: StateFlow<String> = gameTimer.timeSeconds
        .map { s -> "%02d:%02d".format(s / 60, s % 60) }
        .stateIn(viewModelScope, SharingStarted.Lazily, "00:00")

    private var gameJob: Job? = null
    private var quoteJob: Job? = null
    private var matchLineJob: Job? = null
    private var autoHintJob: Job? = null
    private var lastActivityTime = 0L

    // =========================================================================
    // DEV MENU STATE & CONTROL
    // =========================================================================

    private val _isDevMenuOpen = MutableStateFlow(false)
    val isDevMenuOpen: StateFlow<Boolean> = _isDevMenuOpen.asStateFlow()

    private val _isAutoPlayEnabled = MutableStateFlow(false)
    val isAutoPlayEnabled: StateFlow<Boolean> = _isAutoPlayEnabled.asStateFlow()

    private val _isInfiniteTimeEnabled = MutableStateFlow(false)
    val isInfiniteTimeEnabled: StateFlow<Boolean> = _isInfiniteTimeEnabled.asStateFlow()

    private val _isSkipAnimationsEnabled = MutableStateFlow(false)
    val isSkipAnimationsEnabled: StateFlow<Boolean> = _isSkipAnimationsEnabled.asStateFlow()

    private var autoPlayJob: Job? = null

    init {
        _uiState.update {
            it.copy(
                highScores = scoreManager.getAllHighScores(),
                gameState = GameState.WELCOME
            )
        }
    }

    // -------------------------------------------------------------------------
    // Navigation & State Modifiers
    // -------------------------------------------------------------------------

    fun changeState(newState: GameState) {
        _uiState.update {
            if (it.gameState == GameState.PLAYING && newState != GameState.PLAYING) {
                gameTimer.pause()
                autoHintJob?.cancel()
            } else if (it.gameState != GameState.PLAYING && newState == GameState.PLAYING) {
                gameTimer.start(viewModelScope)
                lastActivityTime = System.currentTimeMillis()
                startAutoHintTimer()
            }
            it.copy(previousState = it.gameState, gameState = newState)
        }
    }

    fun goBack() {
        val s = _uiState.value
        when (s.gameState) {
            GameState.OPTIONS, GameState.BOARDS, GameState.SCORE, GameState.ABOUT -> changeState(s.previousState)
            GameState.PAUSED -> changeState(GameState.PLAYING)
            else -> {}
        }
    }

    fun startFromWelcome() = startNewGame(Difficulty.NORMAL)

    fun recordActivity() {
        lastActivityTime = System.currentTimeMillis()
    }

    private fun startAutoHintTimer() {
        autoHintJob?.cancel()
        autoHintJob = viewModelScope.launch {
            delay(AUTO_HINT_DELAY_MS)
            val state = _uiState.value
            if (state.gameState == GameState.PLAYING) {
                getHint()
            }
        }
    }

    private fun resetAutoHintTimer() {
        lastActivityTime = System.currentTimeMillis()
        startAutoHintTimer()
    }

    // -------------------------------------------------------------------------
    // Core Game Flow
    // -------------------------------------------------------------------------

    fun startNewGame(diff: Difficulty) {
        cancelActiveJobs()
        gameTimer.reset()

        val statePrep = controller.prepareNewGameState(
            _uiState.value.backgroundImageName,
            isLayered = false,
            diff = diff
        )

        _uiState.update {
            it.copy(
                gameState = GameState.LOADING,
                difficulty = diff,
                isLayeredMode = false,
                undoHistory = emptyList(),
                layeredUndoHistory = emptyList(),
                usedHint = false,
                usedShuffle = false,
                playerName = "",
                backgroundImageName = statePrep.backgroundImageName,
                currentQuote = statePrep.currentQuote
            )
        }

        gameJob = viewModelScope.launch {
            val board: List<List<Tile>> = controller.generateFlatBoard(diff)
            _uiState.update {
                it.copy(
                    board = board,
                    originalBoard = board,
                    gameState = GameState.PLAYING,
                    shufflesRemaining = diff.shuffles
                )
            }
            finalizeStart()
        }
    }

    private fun finalizeStart() {
        gameTimer.start(viewModelScope)
        musicManager.start()
        startAutoHintTimer()
    }

    private fun handleWin() {
        cancelActiveJobs()
        gameTimer.pause()
        soundManager.play("tile_tada")
        hapticManager.gameWin()

        _uiState.update {
            it.copy(gameState = GameState.QUOTE)
        }

        quoteJob = viewModelScope.launch {
            delay(controller.getQuoteScreenDuration())
            if (_uiState.value.gameState == GameState.QUOTE) {
                dismissQuote()
            }
        }
    }

    fun dismissQuote() {
        quoteJob?.cancel()
        _uiState.update { it.copy(playerName = "", gameState = GameState.SCORE_ENTRY) }
    }

    private fun cancelActiveJobs() {
        gameJob?.cancel()
        quoteJob?.cancel()
        matchLineJob?.cancel()
    }

    // -------------------------------------------------------------------------
    // Tile Interaction — Flat Mode
    // -------------------------------------------------------------------------

    fun handleTileClick(r: Int, c: Int) {
        resetAutoHintTimer()

        val result = interactionCoordinator.handleFlatTileClick(r, c, _uiState.value)
        _uiState.value = result.newState

        when (result.hapticFeedback) {
            "select" -> hapticManager.tileSelect()
            "match" -> hapticManager.tileMatch()
            "error" -> hapticManager.tileError()
        }

        result.soundToPlay?.let { soundManager.play(it) }

        if (result.matchPath != null && result.matchedPair != null) {
            showMatchLine(result.matchPath, result.matchedPair.first, result.matchedPair.second)
        }

        result.matchedBoard?.let { board ->
            viewModelScope.launch {
                postMatchProcessor.process(
                    board = board,
                    getBoard = { _uiState.value.board },
                    updateBoard = { nb -> _uiState.update { it.copy(board = nb) } },
                    onStalemate = { changeState(GameState.NO_MOVES) },
                    onWin = { handleWin() }
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Tile Interaction — Layered Mode
    // -------------------------------------------------------------------------

    fun handleLayeredTileClick(id: Int) {
        resetAutoHintTimer()

        val result = interactionCoordinator.handleLayeredTileClick(id, _uiState.value)
        _uiState.value = result.newState

        when (result.hapticFeedback) {
            "select" -> hapticManager.tileSelect()
            "match" -> hapticManager.tileMatch()
            "error" -> hapticManager.tileError()
        }

        result.soundToPlay?.let { soundManager.play(it) }

        if (result.shouldCheckWin || result.shouldCheckStalemate) {
            viewModelScope.launch {
                delay(HANDLE_WIN_ANIMATION_DELAY_MS)
                val tiles = _uiState.value.layeredTiles
                when {
                    result.shouldCheckWin && layeredEngine.isGameOver(tiles) -> handleWin()
                    result.shouldCheckStalemate && layeredEngine.isStalemate(tiles) -> changeState(GameState.NO_MOVES)
                }
            }
        }
    }

    private fun showMatchLine(path: List<Pair<Int, Int>>, p1: Pair<Int, Int>, p2: Pair<Int, Int>) {
        matchLineJob?.cancel()
        _uiState.update { it.copy(lastMatchPath = path, lastMatchedPair = p1 to p2) }
        matchLineJob = viewModelScope.launch {
            delay(500L)
            _uiState.update { it.copy(lastMatchPath = null, lastMatchedPair = null) }
        }
    }

    // -------------------------------------------------------------------------
    // Score & Settings Logic
    // -------------------------------------------------------------------------

    fun selectScoreTab(tab: String) { _uiState.update { it.copy(selectedScoreTab = tab) } }
    fun updatePlayerName(input: String) { _uiState.update { it.copy(playerName = input.trim().take(3)) } }

    fun saveScoreAndShowBoard() {
        val state = _uiState.value
        val difficulty = if (state.isLayeredMode) state.currentLayeredLayout?.difficulty ?: Difficulty.NORMAL else state.difficulty
        // Map DEV to EASY for score saving since DEV isn't displayed in the scoreboard
        val scoreDifficulty = if (difficulty == Difficulty.DEV) Difficulty.EASY else difficulty
        val newScore = scoreManager.processWin(state.playerName, gameTimer.timeSeconds.value, scoreDifficulty, state.usedHint, state.usedShuffle)

        _uiState.update {
            it.copy(
                highScores = scoreManager.getAllHighScores(),
                selectedScoreTab = scoreDifficulty.label,
                lastSavedScore = newScore,
                gameState = GameState.SCORE
            )
        }
    }

    fun clearScores(diffLabel: String) {
        scoreManager.clearScores(diffLabel)
        _uiState.update { it.copy(highScores = scoreManager.getAllHighScores()) }
    }

    fun clearLastSavedScore() { _uiState.update { it.copy(lastSavedScore = null) } }

    // -------------------------------------------------------------------------
    // 3D / Layered Game
    // -------------------------------------------------------------------------

    fun startNewLayeredGame(layout: LayeredLayout) {
        cancelActiveJobs()
        gameTimer.reset()

        val statePrep = controller.prepareNewGameState(
            _uiState.value.backgroundImageName,
            isLayered = true,
            layout = layout
        )

        _uiState.update {
            it.copy(
                gameState = GameState.LOADING,
                isLayeredMode = true,
                currentLayeredLayout = layout,
                layeredTiles = emptyList(),
                originalLayeredTiles = emptyList(),
                selectedLayeredTileId = null,
                layeredHints = emptyList(),
                currentLayeredHintIndex = -1,
                layeredUndoHistory = emptyList(),
                usedHint = false,
                usedShuffle = false,
                playerName = "",
                backgroundImageName = statePrep.backgroundImageName,
                currentQuote = statePrep.currentQuote
            )
        }

        gameJob = viewModelScope.launch {
            val tiles = controller.generateLayeredBoard(layout)
            _uiState.update {
                it.copy(
                    layeredTiles = tiles,
                    originalLayeredTiles = tiles,
                    gameState = GameState.PLAYING,
                    shufflesRemaining = 5
                )
            }
            finalizeStart()
        }
    }

    private fun retryLayeredGame() {
        cancelActiveJobs()
        gameTimer.reset()

        val original = _uiState.value.originalLayeredTiles
        _uiState.update {
            it.copy(
                layeredTiles = original,
                gameState = GameState.PLAYING,
                selectedLayeredTileId = null,
                layeredHints = emptyList(),
                currentLayeredHintIndex = -1,
                layeredUndoHistory = emptyList(),
                usedHint = false,
                usedShuffle = false,
                shufflesRemaining = 5,
                playerName = ""
            )
        }
        finalizeStart()
    }

    fun retryGame() {
        if (_uiState.value.isLayeredMode) { retryLayeredGame(); return }

        cancelActiveJobs()
        gameTimer.reset()

        val originalBoard = _uiState.value.originalBoard
        _uiState.update {
            it.copy(
                board = originalBoard,
                gameState = GameState.PLAYING,
                shufflesRemaining = it.difficulty.shuffles,
                undoHistory = emptyList(),
                selectedTile = null,
                allAvailableHints = emptyList(),
                currentHintIndex = -1,
                usedHint = false,
                usedShuffle = false,
                playerName = ""
            )
        }
        finalizeStart()
    }

    // -------------------------------------------------------------------------
    // Shuffle
    // -------------------------------------------------------------------------

    fun shuffle() {
        resetAutoHintTimer()

        val state = _uiState.value

        val newState = if (state.isLayeredMode) {
            shuffleUseCase.shuffleLayered(state)
        } else {
            shuffleUseCase.shuffleFlat(state)
        }

        if (newState != state) {
            _uiState.value = newState
            soundManager.play("shuffle")
            hapticManager.shuffle()
        }
    }

    // -------------------------------------------------------------------------
    // Undo
    // -------------------------------------------------------------------------

    fun undo() {
        resetAutoHintTimer()

        val state = _uiState.value
        val newState = if (state.isLayeredMode) {
            undoUseCase.undoLayered(state)
        } else {
            undoUseCase.undoFlat(state)
        }

        if (newState != state) {
            _uiState.value = newState
            soundManager.play("tile_click")
        }
    }

    // -------------------------------------------------------------------------
    // Hint
    // -------------------------------------------------------------------------

    fun getHint() {
        val state = _uiState.value
        if (state.gameState != GameState.PLAYING) return

        if (state.isLayeredMode) {
            if (state.canFinish) { autoComplete(); return }
            getLayeredHint()
            return
        }

        if (state.canFinish) { autoComplete(); return }

        val newState = hintUseCase.applyFlatHint(state)
        _uiState.value = newState
    }

    private fun getLayeredHint() {
        val state = _uiState.value
        if (state.gameState != GameState.PLAYING) return

        val newState = hintUseCase.applyLayeredHint(state)
        _uiState.value = newState
    }

    private fun autoComplete() {
        val isLayered = _uiState.value.isLayeredMode
        viewModelScope.launch {
            while (isActive && _uiState.value.canFinish) {
                val step: Any = if (isLayered) {
                    autoCompleteUseCase.performLayeredStep(_uiState.value) ?: break
                } else {
                    autoCompleteUseCase.performFlatStep(_uiState.value) ?: break
                }

                soundManager.play("tile_match")
                hapticManager.tileMatch()

                when (step) {
                    is LayeredAutoCompleteStep -> {
                        _uiState.update { it.copy(lastMatchedLayeredPair = step.matchPair) }
                    }
                    is AutoCompleteStep -> {
                        _uiState.update { it.copy(lastMatchPath = step.matchPath, lastMatchedPair = step.pair) }
                    }
                }

                delay(300L)

                when (step) {
                    is LayeredAutoCompleteStep -> {
                        _uiState.value = step.newState.copy(lastMatchedLayeredPair = null)
                    }
                    is AutoCompleteStep -> {
                        _uiState.value = step.newState.copy(lastMatchPath = null, lastMatchedPair = null)
                    }
                }

                delay(AUTO_COMPLETE_DELAY_MS - 300L)

                when (step) {
                    is LayeredAutoCompleteStep -> { if (step.isGameOver) break }
                    is AutoCompleteStep -> { if (step.isGameOver) break }
                }
            }

            if (isActive) {
                if (isLayered) {
                    val remaining = _uiState.value.layeredTiles.count { !it.isRemoved }
                    if (remaining == 0) {
                        delay(HANDLE_WIN_ANIMATION_DELAY_MS)
                        handleWin()
                    }
                } else if (engine.isGameOver(_uiState.value.board)) {
                    delay(HANDLE_WIN_ANIMATION_DELAY_MS)
                    handleWin()
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Settings
    // -------------------------------------------------------------------------

    fun applySettingsAndResume(
        modeChanged: Boolean,
        boardTypeChanged: Boolean,
        acknowledgeModeChange: () -> Unit,
        acknowledgeBoardTypeChange: () -> Unit,
        currentDifficulty: Difficulty,
        isLayeredMode: Boolean
    ) {
        acknowledgeBoardTypeChange()

        when {
            boardTypeChanged && isLayeredMode -> {
                acknowledgeModeChange()
                startNewLayeredGame(LayeredLayouts.PYRAMID)
            }
            boardTypeChanged && !isLayeredMode -> {
                acknowledgeModeChange()
                startNewGame(currentDifficulty)
            }
            modeChanged -> {
                startNewGame(currentDifficulty)
                acknowledgeModeChange()
            }
            else -> changeState(GameState.PLAYING)
        }
    }

    // -------------------------------------------------------------------------
    // About Screen
    // -------------------------------------------------------------------------

    fun onAboutTileClick(index: Int, threshold: Int) {
        _uiState.update { aboutHandler.onTileClick(it, index, threshold) }
    }

    fun closeAbout() {
        _uiState.update { aboutHandler.close(it) }
    }

    // -------------------------------------------------------------------------
    // Quote
    // -------------------------------------------------------------------------

    fun refreshQuote() {
        _uiState.update {
            it.copy(currentQuote = controller.prepareNewGameState(it.backgroundImageName, it.isLayeredMode).currentQuote)
        }
    }

    // =========================================================================
    // DEV MENU METHODS
    // =========================================================================

    fun openDevMenu() {
        _isDevMenuOpen.value = true
    }

    fun closeDevMenu() {
        _isDevMenuOpen.value = false
    }

    // =========================================================================
    // GAME STATE FORCING
    // =========================================================================

    fun forceState(newState: GameState) {
        // Handle state transitions with proper cleanup
        val currentState = _uiState.value.gameState

        when {
            currentState == GameState.PLAYING && newState != GameState.PLAYING -> {
                gameTimer.pause()
                autoHintJob?.cancel()
            }
            currentState != GameState.PLAYING && newState == GameState.PLAYING -> {
                gameTimer.start(viewModelScope)
                lastActivityTime = System.currentTimeMillis()
                startAutoHintTimer()
            }
        }

        _uiState.update {
            it.copy(
                previousState = it.gameState,
                gameState = newState
            )
        }

        Log.d("DevMenu", "Forced state to: $newState")
    }

    // =========================================================================
    // AUTO-PLAY - SIMPLIFIED
    // =========================================================================

    fun toggleAutoPlay() {
        _isAutoPlayEnabled.value = !_isAutoPlayEnabled.value
        Log.d("DevMenu", "Auto-play: ${_isAutoPlayEnabled.value}")

        if (_isAutoPlayEnabled.value && _uiState.value.gameState == GameState.PLAYING) {
            startAutoPlay()
        } else {
            autoPlayJob?.cancel()
        }
    }

    private fun startAutoPlay() {
        autoPlayJob?.cancel()
        autoPlayJob = viewModelScope.launch {
            while (_isAutoPlayEnabled.value && _uiState.value.gameState == GameState.PLAYING) {
                delay(800)

                val state = _uiState.value

                if (!state.isLayeredMode) {
                    // Flat board auto-play
                    val board = state.board
                    var foundPair = false

                    outer@ for (r in board.indices) {
                        for (c in board[r].indices) {
                            val tile = board[r][c]
                            if (!tile.isRemoved) {
                                // Found first tile, click it
                                handleTileClick(r, c)
                                delay(400)

                                // Find matching tile with same type
                                inner@ for (r2 in board.indices) {
                                    for (c2 in board[r2].indices) {
                                        val tile2 = board[r2][c2]
                                        if (!tile2.isRemoved && tile2.type == tile.type && !(r == r2 && c == c2)) {
                                            handleTileClick(r2, c2)
                                            delay(400)
                                            foundPair = true
                                            break@outer
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!foundPair) break // No more pairs available
                } else {
                    // Layered board auto-play
                    val tiles = state.layeredTiles.filter { !it.isRemoved }
                    if (tiles.size >= 2) {
                        val first = tiles[0]
                        val matching = tiles.drop(1).firstOrNull { it.type == first.type }
                        if (matching != null) {
                            handleLayeredTileClick(first.id)
                            delay(400)
                            handleLayeredTileClick(matching.id)
                            delay(400)
                        } else {
                            break // No matching pair found
                        }
                    } else {
                        break // Not enough tiles
                    }
                }
            }
        }
    }

    // =========================================================================
    // TIMER CONTROLS
    // =========================================================================

    fun toggleInfiniteTime() {
        _isInfiniteTimeEnabled.value = !_isInfiniteTimeEnabled.value
        Log.d("DevMenu", "Infinite time: ${_isInfiniteTimeEnabled.value}")

        if (_isInfiniteTimeEnabled.value) {
            gameTimer.pause()
        } else {
            if (_uiState.value.gameState == GameState.PLAYING) {
                gameTimer.start(viewModelScope)
            }
        }
    }

    fun jumpToTimeRemaining(seconds: Int) {
        gameTimer.setTimeRemaining(seconds)
        Log.d("DevMenu", "Jumped to $seconds seconds")
    }

    // =========================================================================
    // ANIMATION CONTROLS
    // =========================================================================

    fun toggleSkipAnimations() {
        _isSkipAnimationsEnabled.value = !_isSkipAnimationsEnabled.value
        Log.d("DevMenu", "Skip animations: ${_isSkipAnimationsEnabled.value}")
    }

    // =========================================================================
    // BOARD TESTING
    // =========================================================================

    fun forceUnwinnableBoard() {
        Log.d("DevMenu", "Resetting board to starting state")

        val state = _uiState.value
        _uiState.update {
            it.copy(
                board = state.originalBoard,
                gameState = GameState.PLAYING,
                selectedTile = null
            )
        }
    }

    fun forceAllPairsAvailable() {
        Log.d("DevMenu", "Removing half the tiles for easy play")

        val state = _uiState.value
        val newBoard = state.originalBoard.mapIndexed { r, row ->
            row.mapIndexed { c, tile ->
                // Remove every other tile to make remaining tiles solvable
                if ((r + c) % 2 == 0) tile.copy(isRemoved = true) else tile
            }
        }

        _uiState.update {
            it.copy(
                board = newBoard,
                gameState = GameState.PLAYING,
                selectedTile = null
            )
        }
    }

    // =========================================================================
    // STATE EXPORT FOR DEBUGGING
    // =========================================================================

    fun exportGameStateToLogs() {
        val state = _uiState.value
        val exportData: Map<String, Any> = mapOf(
            "gameState" to state.gameState.name,
            "isLayeredMode" to state.isLayeredMode,
            "difficulty" to state.difficulty.label,
            "boardRows" to state.board.size,
            "boardCols" to (state.board.getOrNull(0)?.size ?: 0),
            "tilesRemaining" to state.board.flatten().count { !it.isRemoved },
            "totalTiles" to state.board.flatten().size,
            "timeRemaining" to gameTimer.timeSeconds.value,
            "usedHint" to state.usedHint,
            "usedShuffle" to state.usedShuffle,
            "shufflesRemaining" to state.shufflesRemaining
        )

        Log.d("DevMenu_State", exportData.toString())
    }

    companion object {
        const val BOARD_GRAVITY_ANIMATION_DELAY_MS = 100L
        const val HANDLE_WIN_ANIMATION_DELAY_MS = 500L
        private const val AUTO_COMPLETE_DELAY_MS = 700L
        private const val AUTO_HINT_DELAY_MS = 15000L // 15 seconds
    }
}
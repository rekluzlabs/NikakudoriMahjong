package com.rekluzgames.nikakudorimahjong.domain.engine

import com.rekluzgames.nikakudorimahjong.data.repository.GameRepository
import com.rekluzgames.nikakudorimahjong.domain.model.Difficulty
import com.rekluzgames.nikakudorimahjong.domain.model.GameState
import com.rekluzgames.nikakudorimahjong.domain.model.GameUIState
import com.rekluzgames.nikakudorimahjong.domain.model.LayeredLayout
import com.rekluzgames.nikakudorimahjong.domain.model.LayeredTile
import com.rekluzgames.nikakudorimahjong.domain.model.Tile
import com.rekluzgames.nikakudorimahjong.domain.rules.BoardGenerator
import com.rekluzgames.nikakudorimahjong.domain.rules.LayeredBoardGenerator
import com.rekluzgames.nikakudorimahjong.presentation.quote.QuoteManager
import com.rekluzgames.nikakudorimahjong.data.audio.MusicManager
import com.rekluzgames.nikakudorimahjong.presentation.timer.GameTimer
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class to hold state preparation values.
 * Returned by prepareNewGameState() and used by ViewModel to build GameUIState.
 */
data class GameSessionStatePrep(
    val backgroundImageName: String,
    val currentQuote: String
)

/**
 * Data class to hold the result of a game session lifecycle operation.
 * Returned by startFlatGame, startLayeredGame, retryFlat, retryLayered.
 */
data class GameSessionResult(
    val state: GameUIState,
    val shouldStartTimer: Boolean = true,
    val shouldStartMusic: Boolean = true
)

@Singleton
class GameSessionController @Inject constructor(
    private val backgroundManager: BackgroundManager,
    private val quoteManager: QuoteManager,
    private val repository: GameRepository,
    private val gameTimer: GameTimer,
    private val musicManager: MusicManager
) {
    companion object {
        private const val BOARD_GENERATION_MIN_DELAY_MS = 600L
        private const val QUOTE_SCREEN_DURATION_MS = 8000L
    }

    fun getQuoteScreenDuration(): Long = QUOTE_SCREEN_DURATION_MS

    /**
     * Prepares state values for a new game without creating GameUIState.
     * The ViewModel will use these values to construct the full state.
     */
    fun prepareNewGameState(
        currentImage: String,
        isLayered: Boolean,
        diff: Difficulty? = null,
        layout: LayeredLayout? = null
    ): GameSessionStatePrep {
        return GameSessionStatePrep(
            backgroundImageName = backgroundManager.next(currentImage),
            currentQuote = quoteManager.next(repository.getLanguage())
        )
    }

    suspend fun generateFlatBoard(diff: Difficulty): List<List<Tile>> {
        val startTime = System.currentTimeMillis()
        val board = BoardGenerator.createBoard(diff)
        ensureMinimumDelay(startTime)
        return board
    }

    suspend fun generateLayeredBoard(layout: LayeredLayout): List<LayeredTile> {
        val startTime = System.currentTimeMillis()
        val tiles = LayeredBoardGenerator.generate(layout)
        ensureMinimumDelay(startTime)
        return tiles
    }

    private suspend fun ensureMinimumDelay(startTime: Long) {
        val elapsed = System.currentTimeMillis() - startTime
        val remaining = BOARD_GENERATION_MIN_DELAY_MS - elapsed
        if (remaining > 0) delay(remaining)
    }




    /**
     * Build the initial state for a flat game.
     * Called by ViewModel after board generation.
     */
    fun buildFlatGameState(
        currentState: GameUIState,
        difficulty: Difficulty,
        statePrep: GameSessionStatePrep,
        board: List<List<Tile>>
    ): GameUIState {
        return currentState.copy(
            gameState = GameState.PLAYING,
            difficulty = difficulty,
            isLayeredMode = false,
            board = board,
            originalBoard = board,
            shufflesRemaining = difficulty.shuffles,
            undoHistory = emptyList(),
            layeredUndoHistory = emptyList(),
            selectedTile = null,
            allAvailableHints = emptyList(),
            currentHintIndex = -1,
            lastMatchPath = null,
            lastMatchedPair = null,
            usedHint = false,
            usedShuffle = false,
            playerName = "",
            backgroundImageName = statePrep.backgroundImageName,
            currentQuote = statePrep.currentQuote
        )
    }

    /**
     * Build the loading state for a flat game.
     * Called by ViewModel before board generation.
     */
    fun buildFlatGameLoadingState(
        currentState: GameUIState,
        difficulty: Difficulty,
        statePrep: GameSessionStatePrep
    ): GameUIState {
        return currentState.copy(
            gameState = GameState.LOADING,
            difficulty = difficulty,
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

    /**
     * Build the state for retrying a flat game.
     * Resets to original board but keeps difficulty.
     */
    fun buildFlatGameRetryState(currentState: GameUIState): GameUIState {
        return currentState.copy(
            board = currentState.originalBoard,
            gameState = GameState.PLAYING,
            shufflesRemaining = currentState.difficulty.shuffles,
            undoHistory = emptyList(),
            selectedTile = null,
            allAvailableHints = emptyList(),
            currentHintIndex = -1,
            lastMatchPath = null,
            lastMatchedPair = null,
            usedHint = false,
            usedShuffle = false,
            playerName = ""
        )
    }




    /**
     * Build the initial state for a layered game.
     * Called by ViewModel after board generation.
     */
    fun buildLayeredGameState(
        currentState: GameUIState,
        layout: LayeredLayout,
        statePrep: GameSessionStatePrep,
        tiles: List<LayeredTile>
    ): GameUIState {
        return currentState.copy(
            gameState = GameState.PLAYING,
            isLayeredMode = true,
            currentLayeredLayout = layout,
            layeredTiles = tiles,
            originalLayeredTiles = tiles,
            shufflesRemaining = 5,
            selectedLayeredTileId = null,
            layeredHints = emptyList(),
            currentLayeredHintIndex = -1,
            layeredUndoHistory = emptyList(),
            undoHistory = emptyList(),
            lastMatchPath = null,
            lastMatchedPair = null,
            usedHint = false,
            usedShuffle = false,
            playerName = "",
            backgroundImageName = statePrep.backgroundImageName,
            currentQuote = statePrep.currentQuote
        )
    }

    /**
     * Build the loading state for a layered game.
     * Called by ViewModel before board generation.
     */
    fun buildLayeredGameLoadingState(
        currentState: GameUIState,
        layout: LayeredLayout,
        statePrep: GameSessionStatePrep
    ): GameUIState {
        return currentState.copy(
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

    /**
     * Build the state for retrying a layered game.
     * Resets to original tiles but keeps layout.
     */
    fun buildLayeredGameRetryState(currentState: GameUIState): GameUIState {
        return currentState.copy(
            layeredTiles = currentState.originalLayeredTiles,
            gameState = GameState.PLAYING,
            shufflesRemaining = 5,
            selectedLayeredTileId = null,
            layeredHints = emptyList(),
            currentLayeredHintIndex = -1,
            layeredUndoHistory = emptyList(),
            lastMatchPath = null,
            lastMatchedPair = null,
            usedHint = false,
            usedShuffle = false,
            playerName = ""
        )
    }




    /**
     * Build the state when game is won.
     * Transitions to QUOTE state.
     */
    fun buildWinState(currentState: GameUIState): GameUIState {
        return currentState.copy(
            gameState = GameState.QUOTE,
            lastMatchPath = null,
            lastMatchedPair = null
        )
    }
}
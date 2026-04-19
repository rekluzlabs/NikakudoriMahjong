/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.usecase

import com.rekluzgames.nikakudorimahjong.domain.engine.GameEngine
import com.rekluzgames.nikakudorimahjong.domain.engine.LayeredGameEngine
import com.rekluzgames.nikakudorimahjong.domain.model.GameUIState
import com.rekluzgames.nikakudorimahjong.domain.rules.HintFinder
import com.rekluzgames.nikakudorimahjong.domain.rules.LayeredHintFinder
import com.rekluzgames.nikakudorimahjong.domain.rules.PathFinder
import javax.inject.Inject

/**
 * Result of one auto-complete step.
 * Contains the next state and info about the move made.
 */
data class AutoCompleteStep(
    val newState: GameUIState,
    val matchPath: List<Pair<Int, Int>>,
    val pair: Pair<Pair<Int, Int>, Pair<Int, Int>>,
    val shouldContinue: Boolean,
    val isGameOver: Boolean
)

data class LayeredAutoCompleteStep(
    val newState: GameUIState,
    val matchPair: Pair<Int, Int>,
    val shouldContinue: Boolean,
    val isGameOver: Boolean
)

class AutoCompleteUseCase @Inject constructor(
    private val engine: GameEngine,
    private val layeredEngine: LayeredGameEngine
) {

    /**
     * Perform one step of auto-complete for flat mode.
     * Finds the next match, builds the new state, and returns all info.
     *
     * Returns null if:
     * - canFinish is false
     * - no matches available
     * - attemptMatch fails
     */
    fun performFlatStep(state: GameUIState): AutoCompleteStep? {

        if (!state.canFinish) return null

        val board = state.board
        val matches = HintFinder.findAllMatches(board)

        if (matches.isEmpty()) {
            return null
        }

        val (p1, p2) = matches.first()
        val path = PathFinder.getPath(p1, p2, board) ?: listOf(p1, p2)

        val matchedBoard = engine.attemptMatch(p1, p2, board) ?: return null

        val isGameOver = engine.isGameOver(matchedBoard)

        val newState = state.copy(
            board = matchedBoard,
            selectedTile = null,
            allAvailableHints = emptyList(),
            currentHintIndex = -1
        )

        return AutoCompleteStep(
            newState = newState,
            matchPath = path,
            pair = p1 to p2,
            shouldContinue = !isGameOver,
            isGameOver = isGameOver
        )
    }

    /**
     * Perform one step of auto-complete for layered (3D) mode.
     */
    fun performLayeredStep(state: GameUIState): LayeredAutoCompleteStep? {
        if (!state.canFinish) return null

        val tiles = state.layeredTiles
        val matches = LayeredHintFinder.findAllMatches(tiles, layeredEngine)

        if (matches.isEmpty()) return null

        val (id1, id2) = matches.first()
        val tile1 = tiles.find { it.id == id1 } ?: return null
        val tile2 = tiles.find { it.id == id2 } ?: return null

        val matchedTiles = layeredEngine.attemptMatch(id1, id2, tiles) ?: return null
        val remainingTiles = matchedTiles.count { !it.isRemoved }
        val isGameOver = remainingTiles == 0

        val newState = state.copy(
            layeredTiles = matchedTiles,
            selectedLayeredTileId = null,
            layeredHints = emptyList(),
            currentLayeredHintIndex = -1
        )

        return LayeredAutoCompleteStep(
            newState = newState,
            matchPair = id1 to id2,
            shouldContinue = !isGameOver,
            isGameOver = isGameOver
        )
    }
}
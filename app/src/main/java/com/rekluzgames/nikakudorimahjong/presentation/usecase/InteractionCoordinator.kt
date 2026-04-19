/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.usecase

import com.rekluzgames.nikakudorimahjong.domain.engine.LayeredGameEngine
import com.rekluzgames.nikakudorimahjong.domain.model.GameUIState
import com.rekluzgames.nikakudorimahjong.domain.model.Tile
import com.rekluzgames.nikakudorimahjong.presentation.ui.component.TileInteractionHandler
import javax.inject.Inject

/**
 * Result of a tile interaction in flat mode.
 * The ViewModel decides what to do with these results.
 */
data class FlatInteractionResult(
    val newState: GameUIState,
    val soundToPlay: String? = null,
    val hapticFeedback: String? = null,
    val matchPath: List<Pair<Int, Int>>? = null,
    val matchedPair: Pair<Pair<Int, Int>, Pair<Int, Int>>? = null,
    val matchedBoard: List<List<Tile>>? = null
)

/**
 * Result of a tile interaction in layered mode.
 * The ViewModel decides what to do with these results.
 */
data class LayeredInteractionResult(
    val newState: GameUIState,
    val soundToPlay: String? = null,
    val hapticFeedback: String? = null,
    val shouldCheckWin: Boolean = false,
    val shouldCheckStalemate: Boolean = false
)

class InteractionCoordinator @Inject constructor(
    private val tileHandler: TileInteractionHandler,
    private val layeredEngine: LayeredGameEngine
) {

    /**
     * Handle a flat mode tile click.
     * Returns all information needed for the ViewModel to orchestrate effects.
     */
    fun handleFlatTileClick(
        r: Int,
        c: Int,
        state: GameUIState
    ): FlatInteractionResult {

        val result = tileHandler.handleClick(state, r, c)

        val hapticFeedback = when (result.playSound) {
            "tile_error" -> "error"
            "tile_match" -> "match"
            else -> "select"
        }

        val (matchPath, matchedPair) = if (result.matchPath != null && result.matchedPair != null) {
            result.matchPath to result.matchedPair
        } else if (result.matchPath != null) {
            val first = result.matchPath.firstOrNull()
            val last = result.matchPath.lastOrNull()
            if (first != null && last != null) {
                result.matchPath to (first to last)
            } else {
                null to null
            }
        } else {
            null to null
        }

        return FlatInteractionResult(
            newState = result.newState,
            soundToPlay = result.playSound,
            hapticFeedback = hapticFeedback,
            matchPath = matchPath,
            matchedPair = matchedPair,
            matchedBoard = result.matchedBoard
        )
    }

    /**
     * Handle a layered mode tile click.
     * Returns all information needed for the ViewModel to orchestrate effects.
     */
    fun handleLayeredTileClick(
        id: Int,
        state: GameUIState
    ): LayeredInteractionResult {

        val tapped = state.layeredTiles.firstOrNull { it.id == id && !it.isRemoved }
            ?: return LayeredInteractionResult(
                newState = state,
                soundToPlay = null,
                hapticFeedback = null
            )

        if (!layeredEngine.isFree(tapped, state.layeredTiles)) {
            return LayeredInteractionResult(
                newState = state,
                soundToPlay = "tile_error",
                hapticFeedback = "error"
            )
        }

        return when (state.selectedLayeredTileId) {
            id -> {

                LayeredInteractionResult(
                    newState = state.copy(selectedLayeredTileId = null),
                    soundToPlay = null,
                    hapticFeedback = null
                )
            }
            null -> {

                LayeredInteractionResult(
                    newState = state.copy(selectedLayeredTileId = id),
                    soundToPlay = null,
                    hapticFeedback = "select"
                )
            }
            else -> {

                val newTiles = layeredEngine.attemptMatch(
                    state.selectedLayeredTileId,
                    id,
                    state.layeredTiles
                )

                if (newTiles != null) {

                    val snapshot = state.layeredTiles
                    LayeredInteractionResult(
                        newState = state.copy(
                            layeredTiles = newTiles,
                            selectedLayeredTileId = null,
                            layeredHints = emptyList(),
                            currentLayeredHintIndex = -1,
                            layeredUndoHistory = state.layeredUndoHistory + listOf(snapshot)
                        ),
                        soundToPlay = "tile_match",
                        hapticFeedback = "match",
                        shouldCheckWin = true,
                        shouldCheckStalemate = true
                    )
                } else {

                    LayeredInteractionResult(
                        newState = state.copy(selectedLayeredTileId = id),
                        soundToPlay = null,
                        hapticFeedback = "select"
                    )
                }
            }
        }
    }
}
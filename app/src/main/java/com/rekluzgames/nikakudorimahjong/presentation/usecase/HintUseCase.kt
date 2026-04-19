/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.presentation.usecase

import com.rekluzgames.nikakudorimahjong.domain.engine.LayeredGameEngine
import com.rekluzgames.nikakudorimahjong.domain.model.GameUIState
import com.rekluzgames.nikakudorimahjong.domain.rules.HintFinder
import com.rekluzgames.nikakudorimahjong.domain.rules.LayeredHintFinder
import javax.inject.Inject

class HintUseCase @Inject constructor(
    private val layeredEngine: LayeredGameEngine
) {

    /**
     * Apply hint logic for flat mode.
     * Returns updated state with hints populated or NO_MOVES state.
     *
     * Does NOT handle autoComplete() — that stays in ViewModel.
     */
    fun applyFlatHint(state: GameUIState): GameUIState {

        if (state.gameState != com.rekluzgames.nikakudorimahjong.domain.model.GameState.PLAYING) {
            return state
        }

        var newState = state.copy(usedHint = true)

        if (newState.allAvailableHints.isEmpty()) {
            val hints = HintFinder.findAllMatches(state.board)
            if (hints.isNotEmpty()) {
                newState = newState.copy(
                    allAvailableHints = hints,
                    currentHintIndex = 0
                )
            } else {

                newState = newState.copy(
                    gameState = com.rekluzgames.nikakudorimahjong.domain.model.GameState.NO_MOVES
                )
            }
        } else {

            newState = newState.copy(
                currentHintIndex = (newState.currentHintIndex + 1) % newState.allAvailableHints.size
            )
        }

        return newState
    }

    /**
     * Apply hint logic for layered mode.
     * Returns updated state with hints populated or NO_MOVES state.
     *
     * Does NOT handle autoComplete() — that stays in ViewModel.
     */
    fun applyLayeredHint(state: GameUIState): GameUIState {

        if (state.gameState != com.rekluzgames.nikakudorimahjong.domain.model.GameState.PLAYING) {
            return state
        }

        var newState = state.copy(usedHint = true)

        if (newState.layeredHints.isEmpty()) {
            val hints = LayeredHintFinder.findAllMatches(state.layeredTiles, layeredEngine)
            if (hints.isNotEmpty()) {
                newState = newState.copy(
                    layeredHints = hints,
                    currentLayeredHintIndex = 0
                )
            } else {

                newState = newState.copy(
                    gameState = com.rekluzgames.nikakudorimahjong.domain.model.GameState.NO_MOVES
                )
            }
        } else {

            newState = newState.copy(
                currentLayeredHintIndex = (newState.currentLayeredHintIndex + 1) % newState.layeredHints.size
            )
        }

        return newState
    }
}
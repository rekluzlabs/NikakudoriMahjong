/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.domain.engine

import com.rekluzgames.nikakudorimahjong.domain.model.LayeredTile
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class LayeredGameEngine @Inject constructor() {

    fun isFree(tile: LayeredTile, all: List<LayeredTile>): Boolean {
        if (tile.isRemoved) return false

        val blockedFromAbove = all.any { other ->
            !other.isRemoved &&
                    other.layer > tile.layer &&
                    abs(other.col - tile.col) < 2 &&
                    abs(other.row - tile.row) < 2
        }
        if (blockedFromAbove) return false

        val blockedLeft = all.any { other ->
            !other.isRemoved &&
                    other.layer == tile.layer &&
                    other.col == tile.col - 2 &&
                    abs(other.row - tile.row) < 2
        }

        val blockedRight = all.any { other ->
            !other.isRemoved &&
                    other.layer == tile.layer &&
                    other.col == tile.col + 2 &&
                    abs(other.row - tile.row) < 2
        }

        return !(blockedLeft && blockedRight)
    }

    fun attemptMatch(id1: Int, id2: Int, all: List<LayeredTile>): List<LayeredTile>? {
        val t1 = all.firstOrNull { it.id == id1 } ?: return null
        val t2 = all.firstOrNull { it.id == id2 } ?: return null
        if (t1.type != t2.type) return null
        if (!isFree(t1, all) || !isFree(t2, all)) return null

        return all.map { tile ->
            if (tile.id == id1 || tile.id == id2) tile.copy(isRemoved = true) else tile
        }
    }

    fun isGameOver(all: List<LayeredTile>): Boolean = all.all { it.isRemoved }

    fun isStalemate(all: List<LayeredTile>): Boolean {
        val freeTiles = all.filter { !it.isRemoved && isFree(it, all) }
        return freeTiles.groupBy { it.type }.none { it.value.size >= 2 }
    }
}
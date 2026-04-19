/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.domain.model

data class LayeredLayout(
    val id: String,
    val displayName: String,
    val positions: List<TilePosition>,
    val difficulty: Difficulty = Difficulty.NORMAL
) {
    val tileCount: Int get() = positions.size
}

object LayeredLayouts {

    /**
     * PYRAMID — 228 tiles, 4 layers, NORMAL difficulty.
     * Each layer is a filled rectangle centered on the layer below.
     * All layers use step 2 (tiles touching sides without overlapping).
     * Layer 0 (base): 12x12 = 144 tiles - filled rectangle
     * Layer 1: 8x8 = 64 tiles - centered on top of layer 0
     * Layer 2: 4x4 = 16 tiles - centered on top of layer 1
     * Layer 3 (top): 2x2 = 4 tiles - centered on top of layer 2
     * 
     * Total: 144 + 64 + 16 + 4 = 228 tiles (even) ✓
     */
    val PYRAMID = LayeredLayout(
        id = "PYRAMID",
        displayName = "Pyramid",
        difficulty = Difficulty.NORMAL,
        positions = buildList {

            for (r in 0..22 step 2) {
                for (c in 0..22 step 2) {
                    add(TilePosition(c, r, 0))
                }
            }

            for (r in 4..18 step 2) {
                for (c in 4..18 step 2) {
                    add(TilePosition(c, r, 1))
                }
            }

            for (r in 8..14 step 2) {
                for (c in 8..14 step 2) {
                    add(TilePosition(c, r, 2))
                }
            }

            for (r in 10..12 step 2) {
                for (c in 10..12 step 2) {
                    add(TilePosition(c, r, 3))
                }
            }
        }
    )

    /**
     * FORTRESS — 104 tiles, 3 layers, HARD difficulty.
     */
    val FORTRESS = LayeredLayout(
        id = "FORTRESS",
        displayName = "Fortress",
        difficulty = Difficulty.HARD,
        positions = buildList {

            for (r in 0..14 step 2) for (c in 0..14 step 2) add(TilePosition(c, r, 0))

            for (r in 0..14 step 2) {
                for (c in 0..14 step 2) {
                    if (r == 0 || r == 14 || c == 0 || c == 14) {
                        add(TilePosition(c, r, 1))
                        if ((r == 0 || r == 14) && (c == 0 || c == 14)) {
                            add(TilePosition(c, r, 2))
                        }
                    }
                }
            }

            for (r in 6..8 step 2) for (c in 6..8 step 2) {
                add(TilePosition(c, r, 1))
                add(TilePosition(c, r, 2))
            }

        }
    )

    /**
     * DRAGON — 126 tiles, 4 layers, EXTREME difficulty.
     * A traditional, fully symmetrical majestic dragon layout 
     * featuring sweeping wings, a ridged spine, and a long tail.
     */
    val DRAGON = LayeredLayout(
        id = "DRAGON",
        displayName = "Dragon",
        difficulty = Difficulty.EXTREME,
        positions = buildList {


            for (c in 14..18 step 2) add(TilePosition(c, 0, 0))
            for (c in 12..20 step 2) add(TilePosition(c, 2, 0))
            for (c in 14..18 step 2) add(TilePosition(c, 4, 0))

            for (r in 6..14 step 2) for (c in 12..20 step 2) add(TilePosition(c, r, 0))

            for (c in 14..18 step 2) add(TilePosition(c, 16, 0))
            for (r in 18..22 step 2) add(TilePosition(16, r, 0))

            for (c in listOf(8, 10, 22, 24)) add(TilePosition(c, 6, 0))
            for (c in 4..10 step 2) add(TilePosition(c, 8, 0))
            for (c in 22..28 step 2) add(TilePosition(c, 8, 0))
            for (c in 0..10 step 2) add(TilePosition(c, 10, 0))
            for (c in 22..32 step 2) add(TilePosition(c, 10, 0))
            for (c in 4..10 step 2) add(TilePosition(c, 12, 0))
            for (c in 22..28 step 2) add(TilePosition(c, 12, 0))
            for (c in listOf(8, 10, 22, 24)) add(TilePosition(c, 14, 0))


            for (c in 14..18 step 2) add(TilePosition(c, 2, 1))
            add(TilePosition(16, 4, 1))

            for (r in 6..14 step 2) for (c in 14..18 step 2) add(TilePosition(c, r, 1))
            add(TilePosition(16, 16, 1))

            for (c in listOf(8, 10, 22, 24)) add(TilePosition(c, 8, 1))
            for (c in 4..10 step 2) add(TilePosition(c, 10, 1))
            for (c in 22..28 step 2) add(TilePosition(c, 10, 1))
            for (c in listOf(8, 10, 22, 24)) add(TilePosition(c, 12, 1))


            add(TilePosition(16, 2, 2))
            for (r in 6..14 step 2) add(TilePosition(16, r, 2))

            for (c in listOf(8, 10, 22, 24)) add(TilePosition(c, 10, 2))

            add(TilePosition(16, 8, 3))
            add(TilePosition(16, 12, 3))
        }
    )

    /**
     * TURTLE — 136 tiles, 7 layers, EXTREME difficulty.
     */
    val TURTLE = LayeredLayout(
        id = "TURTLE",
        displayName = "Turtle",
        difficulty = Difficulty.EXTREME,
        positions = buildList {

            for (r in 2..12 step 2) for (c in 4..22 step 2) add(TilePosition(c, r, 0))
            val extras = listOf(12 to 0, 12 to 14, 2 to 4, 2 to 10, 24 to 4, 24 to 10, 0 to 7, 26 to 7)
            extras.forEach { add(TilePosition(it.first, it.second, 0)) }

            for (r in 3..11 step 2) for (c in 7..19 step 2) add(TilePosition(c, r, 1))

            for (r in 4..10 step 2) for (c in 10..18 step 2) add(TilePosition(c, r, 2))

            for (r in 5..9 step 2) for (c in 12..16 step 2) add(TilePosition(c, r, 3))

            add(TilePosition(14, 6, 4)); add(TilePosition(14, 8, 4))

            add(TilePosition(14, 7, 5)); add(TilePosition(14, 7, 6))

        }
    )

    /**
     * BRIDGE — 126 tiles, 4 layers, NORMAL difficulty.
     * Traditional twin-island configuration securely connected
     * by a towering center bridge spanning across the board.
     */
    val BRIDGE = LayeredLayout(
        id = "BRIDGE",
        displayName = "Bridge",
        difficulty = Difficulty.NORMAL,
        positions = buildList {


            for (c in 0..10 step 2) for (r in 0..10 step 2) add(TilePosition(c, r, 0))

            for (c in 22..32 step 2) for (r in 0..10 step 2) add(TilePosition(c, r, 0))


            for (c in 2..6 step 2) for (r in 2..8 step 2) add(TilePosition(c, r, 1))

            for (c in 26..30 step 2) for (r in 2..8 step 2) add(TilePosition(c, r, 1))

            for (c in 8..24 step 2) for (r in 4..6 step 2) add(TilePosition(c, r, 1))


            add(TilePosition(4, 4, 2)); add(TilePosition(4, 6, 2))

            add(TilePosition(28, 4, 2)); add(TilePosition(28, 6, 2))

            for (c in 14..18 step 2) for (r in 4..6 step 2) add(TilePosition(c, r, 2))


            add(TilePosition(16, 4, 3)); add(TilePosition(16, 6, 3))
        }
    )

    /**
     * CASTLE — 140 tiles, 5 layers, EXTREME difficulty.
     */
    val CASTLE = LayeredLayout(
        id = "CASTLE",
        displayName = "Castle",
        difficulty = Difficulty.EXTREME,
        positions = buildList {

            for (r in 0..14 step 2) for (c in 0..18 step 2) add(TilePosition(c, r, 0))

            for (r in 0..14 step 2) {
                for (c in 0..18 step 2) {
                    if (r == 0 || r == 14 || c == 0 || c == 18) add(TilePosition(c, r, 1))
                    if (r in 4..10 && c in 6..12) add(TilePosition(c, r, 1))
                }
            }

            val towers = listOf(0 to 0, 18 to 0, 0 to 14, 18 to 14)
            towers.forEach { (c, r) ->
                add(TilePosition(c, r, 2))
                add(TilePosition(c, r, 3))
            }

            add(TilePosition(9, 6, 3)); add(TilePosition(9, 8, 3))
            add(TilePosition(9, 7, 3)); add(TilePosition(9, 7, 4))

        }
    )

    val ALL: List<LayeredLayout> = listOf(PYRAMID, FORTRESS, DRAGON, TURTLE, BRIDGE, CASTLE)

    fun byId(id: String): LayeredLayout? = ALL.firstOrNull { it.id == id }
}
/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 * This code and its assets are the exclusive property of Rekluz Games.
 * Unauthorized copying, distribution, or commercial use is strictly prohibited.
 */

package com.rekluzgames.nikakudorimahjong.domain.model

data class BackgroundMetadata(
    val resourceName: String,
    val locationName: String,
    val description: String
)

object BackgroundLocations {
    private val locations = mapOf(
        "bg_001" to BackgroundMetadata("bg_001", "Osaka, Japan", "Dotonbori"),
        "bg_002" to BackgroundMetadata("bg_002", "Osaka, Japan", "Dotonbori"),
        "bg_003" to BackgroundMetadata("bg_003", "Kyoto, Japan", "Kinkaju-ji Temple of the Golden Pavillion"),
        "bg_004" to BackgroundMetadata("bg_004", "Asakusa, Tokyo, Japan", "Senso-ji Temple"),
        "bg_005" to BackgroundMetadata("bg_005", "Tokyo, Japan", "Tokyo Tower"),
        "bg_006" to BackgroundMetadata("bg_006", "Uji, Japan", "Byodoin Temple"),
        "bg_007" to BackgroundMetadata("bg_007", "Wakayama, Japan", "Ohasiroka Bridge"),
        "bg_008" to BackgroundMetadata("bg_008", "Uji, Japan", "Byodoin Temple"),
        "bg_009" to BackgroundMetadata("bg_009", "Okayama, Japan", "Okayama Castle"),
        "bg_010" to BackgroundMetadata("bg_010", "Tokyo, Japan", "Vending Machines"),
        "bg_011" to BackgroundMetadata("bg_011", "Himeji, Japan", "Himeji Castle"),
        "bg_012" to BackgroundMetadata("bg_012", "Fukuoka, Japan", "Nanzoin Temple"),
        "bg_013" to BackgroundMetadata("bg_013", "Kamakura, Japan", "The Great Buddha of Kamakura"),
        "bg_014" to BackgroundMetadata("bg_014", "Osaka, Japan", "Osaka Castle"),
        "bg_015" to BackgroundMetadata("bg_015", "Nara, Japan", "Todai-ji Temple"),
        "bg_016" to BackgroundMetadata("bg_016", "Kyoto, Japan", "Kinkaju-ji Temple of the Golden Pavillion")
    )

    fun getLocationByResourceName(resourceName: String): BackgroundMetadata? {
        return locations[resourceName]
    }

    fun getAllLocations(): Map<String, BackgroundMetadata> {
        return locations
    }
}
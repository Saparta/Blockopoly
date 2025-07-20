package com.gameservice.util

import com.gameservice.models.Card
import com.gameservice.models.PlayerState

fun playerToStealCardFrom(thievingPlayer: String, targetCard: Card.Property, playerStates: Map<String, PlayerState>) : String? {
    return playerStates.firstNotNullOfOrNull { entry ->
        if (entry.key == thievingPlayer) return@firstNotNullOfOrNull null
        val propertySet = entry.value.getSetOfProperty(targetCard)
        if (propertySet == null) return@firstNotNullOfOrNull null
        if (!propertySet.isComplete) return@firstNotNullOfOrNull entry.key else null
    }
}
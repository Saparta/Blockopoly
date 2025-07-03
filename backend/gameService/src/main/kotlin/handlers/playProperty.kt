package com.gameservice.handlers

import com.gameservice.cardMapping
import com.gameservice.models.Card
import com.gameservice.models.GameState
import com.gameservice.models.PlayProperty
import kotlinx.coroutines.flow.MutableStateFlow

fun playProperty(game: MutableStateFlow<GameState>, playerId: String, playProperty : PlayProperty) : GameState {
    val current = game.value
    val card = cardMapping[playProperty.id] ?: return current
    if (card !is Card.Property) return current
    if (current.playerAtTurn != playerId || current.cardsLeftToPlay <= 0 ||
        !current.isCardInHand(playerId, card) ||
        !card.colors.contains(playProperty.color)) return current

    return current.let { state ->
        state.playerState[playerId]?.hand?.removeIf { it.id == card.id }
        state.playerState[playerId]?.properties?.addProperty(card, playProperty.color)
        val cardsLeft = current.cardsLeftToPlay - 1
        return@let state.copy(cardsLeftToPlay = cardsLeft)
    }
}
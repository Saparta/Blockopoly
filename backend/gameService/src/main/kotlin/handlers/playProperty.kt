package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.NUM_COMPLETE_SETS_TO_WIN
import com.gameservice.cardMapping
import com.gameservice.models.Card
import com.gameservice.models.GameState
import com.gameservice.models.PlacePropertyMessage
import com.gameservice.models.PlayProperty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

suspend fun playProperty(room: DealGame, game: MutableStateFlow<GameState>, playerId: String, playProperty : PlayProperty) : GameState {
    return game.updateAndGet { current ->
        if (current.pendingInteractions.isNotEmpty()) return current
        val card = cardMapping[playProperty.id] ?: return current
        if (card !is Card.Property) return current
        val playerState = current.playerState[playerId] ?: return current
        if (current.playerAtTurn != playerId || current.cardsLeftToPlay <= 0 ||
            !current.isCardInHand(playerId, card) ||
            !card.colors.contains(playProperty.color)) return current

        playerState.hand.removeIf { it.id == card.id }
        val propertySetId = playerState.addProperty(card, playProperty.color)
        room.sendBroadcast(PlacePropertyMessage(playerId, card, propertySetId!!))
        val cardsLeft = current.cardsLeftToPlay - 1
        var winner: String? = null
        if (playerState.numCompleteSets() == NUM_COMPLETE_SETS_TO_WIN) winner = playerId
        return@updateAndGet current.copy(winningPlayer = winner, cardsLeftToPlay = cardsLeft)
    }
}
package com.gameservice.handlers

import com.gameservice.DEVELOPMENT_ACTION_CARDS
import com.gameservice.DealGame
import com.gameservice.cardMapping
import com.gameservice.models.Card
import com.gameservice.models.DevelopmentAddedMessage
import com.gameservice.models.GameState
import com.gameservice.models.PlayDevelopment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

suspend fun playDevelopment(room: DealGame, game: MutableStateFlow<GameState>, playerId: String, action : PlayDevelopment) : GameState {
    return game.updateAndGet { current ->
        if (current.pendingInteractions.isNotEmpty()) return current
        val card = cardMapping[action.id] ?: return current
        if (card !is Card.Action || card.actionType !in DEVELOPMENT_ACTION_CARDS) return current
        val playerState = current.playerState[playerId] ?: return current
        if (current.playerAtTurn != playerId || current.cardsLeftToPlay <= 0 ||
            !current.isCardInHand(playerId, card)) return current
        playerState.addDevelopment(card, action.propertySetId) ?: return current
        room.sendBroadcast(DevelopmentAddedMessage(card, action.propertySetId))
        playerState.hand.removeIf { it.id == card.id }
        return@updateAndGet current.copy(cardsLeftToPlay = current.cardsLeftToPlay - 1)
    }
}
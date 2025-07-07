package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.MAX_CARDS_PER_TURN
import com.gameservice.models.Card
import com.gameservice.models.DiscardMessage
import com.gameservice.models.GameState
import kotlinx.coroutines.flow.MutableStateFlow

suspend fun endTurn(room: DealGame, game: MutableStateFlow<GameState>, playerId: String) : GameState {
    val current = game.value
    if (current.playerAtTurn != playerId) return current
    val nextPlayer = current.playerOrder[(current.playerOrder.indexOf(playerId) + 1) % current.playerOrder.size]
    val removedCards = mutableListOf<Card>()
    current.playerState[playerId]?.hand?.size?.let { handSize ->
        if (handSize > 7) {
            while (current.playerState.getValue(playerId).hand.size > 7) {
                val removedCard = current.playerState.getValue(playerId).hand.removeLast()
                room.sendBroadcast(DiscardMessage(playerId, removedCard))
                removedCards.add(removedCard)
            }
        }
    }
    current.discardPile.addAll(removedCards)
    current.playerAtTurn = nextPlayer
    current.cardsLeftToPlay = MAX_CARDS_PER_TURN
    return startTurn(room, game, nextPlayer)
}
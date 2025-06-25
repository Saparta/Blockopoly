package com.gameservice.handlers

import com.gameservice.MAX_CARDS_PER_TURN
import com.gameservice.models.Card
import com.gameservice.models.GameState
import kotlinx.coroutines.flow.MutableStateFlow

fun endTurn(game: MutableStateFlow<GameState>, playerId: String) : GameState {
    val current = game.value
    if (current.playerAtTurn != playerId) return current
    val nextPlayer = current.playerOrder[(current.playerOrder.indexOf(playerId) + 1) % current.playerOrder.size]
    val removedCards = mutableListOf<Card>()
    current.playerState[playerId]?.hand?.size?.let {
        if (it > 7) {
            while (current.playerState.getValue(playerId).hand.size > 7) {
                removedCards.add(current.playerState.getValue(playerId).hand.removeLast())
            }
        }
    }
    current.discardPile.addAll(removedCards)
    return current.copy(playerAtTurn = nextPlayer, cardsLeftToPlay = MAX_CARDS_PER_TURN, discardPile = current.discardPile)
}
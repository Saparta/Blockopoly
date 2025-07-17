package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.cardMapping
import com.gameservice.models.ActionType
import com.gameservice.models.Card
import com.gameservice.models.DrawMessage
import com.gameservice.models.GameState
import com.gameservice.models.PassGo
import com.gameservice.models.PlayUnstoppableActionMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

suspend fun passGo(room: DealGame, gameState: MutableStateFlow<GameState>, playerId: String, action: PassGo) : GameState {
    return gameState.updateAndGet { current ->
        if (current.pendingInteractions.isNotEmpty() || current.playerAtTurn != playerId) return current
        val card = cardMapping[action.id] ?: return current
        val playerState = current.playerState[playerId] ?: return current
        if (card !is Card.Action || card.actionType != ActionType.PASS_GO || !current.isCardInHand(playerId, card) || current.cardsLeftToPlay <= 0) return current
        room.sendBroadcast(PlayUnstoppableActionMessage(playerId, card))
        current.discardPile.add(card)
        val drawnCards = current.draw(true)
        playerState.hand.removeIf { it.id == card.id }
        room.sendBroadcast(DrawMessage(playerId, drawnCards))
        return@updateAndGet current.copy(cardsLeftToPlay = current.cardsLeftToPlay - 1)
    }
}
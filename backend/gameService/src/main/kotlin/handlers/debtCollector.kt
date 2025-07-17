package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.cardMapping
import com.gameservice.models.ActionType
import com.gameservice.models.Card
import com.gameservice.models.DebtCollect
import com.gameservice.models.DebtCollectMessage
import com.gameservice.models.GameState
import com.gameservice.models.PendingInteraction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

suspend fun debtCollect(room: DealGame, gameState: MutableStateFlow<GameState>, playerId: String, action: DebtCollect) : GameState {
    return gameState.updateAndGet { current ->
        if (current.pendingInteractions.isNotEmpty() || current.playerAtTurn != playerId) return current
        val playerState = current.playerState[playerId] ?: return current
        val card = cardMapping[action.id] ?: return current
        if (current.playerState[action.target] == null) return current
        if (card !is Card.Action || card.actionType != ActionType.DEBT_COLLECTOR || !current.isCardInHand(playerId, card) || current.cardsLeftToPlay <= 0) return current
        val debtCollectMessage = DebtCollectMessage(playerId, action.target, card.id)

        current.pendingInteractions.add(
            PendingInteraction(
                fromPlayer = playerId,
                toPlayer = action.target,
                action = debtCollectMessage,
                initial = listOf(card.id),
                awaitingResponseFrom = action.target,
            )
        ) ?: return current

        room.sendBroadcast(debtCollectMessage)
        current.discardPile.add(card)
        playerState.hand.removeIf { it.id == card.id }
        return@updateAndGet current.copy(cardsLeftToPlay = current.cardsLeftToPlay - 1)
    }
}
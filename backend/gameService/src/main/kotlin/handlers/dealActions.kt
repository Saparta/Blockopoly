package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.cardMapping
import com.gameservice.models.ActionType
import com.gameservice.models.Card
import com.gameservice.models.GameState
import com.gameservice.models.PendingInteraction
import com.gameservice.models.SlyDeal
import com.gameservice.models.SlyDealMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

suspend fun slyDeal(room: DealGame, gameState: MutableStateFlow<GameState>, playerId: String, action: SlyDeal) : GameState {
    return gameState.updateAndGet { current ->
        if (current.pendingInteractions.isNotEmpty() || current.playerAtTurn != playerId) return current
        val playerState = current.playerState[playerId] ?: return current
        val card = cardMapping[action.id] ?: return current
        if (card !is Card.Action || card.actionType != ActionType.SLY_DEAL || !current.isCardInHand(playerId, card) || current.cardsLeftToPlay <= 0) return current
        val targetCard = cardMapping[action.targetCard] ?: return current
        val targetPlayer = current.playerState.firstNotNullOfOrNull { entry ->
            if (entry.key == playerId) return@firstNotNullOfOrNull null
            val propertySet = when (targetCard) {
                is Card.Property -> entry.value.getSetOfProperty(targetCard)
                is Card.Action -> entry.value.getSetOfDevelopment(targetCard)
                else -> null
            }
            if (propertySet == null) return@firstNotNullOfOrNull null
            if (!propertySet.isComplete) return@firstNotNullOfOrNull entry.key else null
        } ?: return current
        val slyDealMessage = SlyDealMessage(playerId, targetPlayer, action.targetCard, action.colorToReceiveAs)
        current.pendingInteractions.add(
            PendingInteraction(
                fromPlayer = playerId,
                toPlayer = targetPlayer,
                action = slyDealMessage,
                initial = listOf(action.id),
                awaitingResponseFrom = targetPlayer,
            )
        )
        room.sendBroadcast(slyDealMessage)
        current.discardPile.add(card)
        playerState.hand.removeIf { it.id == card.id }
        return@updateAndGet current.copy(cardsLeftToPlay = current.cardsLeftToPlay - 1)
    }
}
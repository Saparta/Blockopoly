package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.cardMapping
import com.gameservice.models.ActionType
import com.gameservice.models.Card
import com.gameservice.models.ForcedDeal
import com.gameservice.models.ForcedDealMessage
import com.gameservice.models.GameState
import com.gameservice.models.PendingInteraction
import com.gameservice.models.SlyDeal
import com.gameservice.models.SlyDealMessage
import com.gameservice.util.playerToStealCardFrom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

suspend fun slyDeal(room: DealGame, gameState: MutableStateFlow<GameState>, playerId: String, action: SlyDeal) : GameState {
    return gameState.updateAndGet { current ->
        if (current.pendingInteractions.isNotEmpty() || current.playerAtTurn != playerId) return current
        val playerState = current.playerState[playerId] ?: return current
        val card = cardMapping[action.id] ?: return current
        if (card !is Card.Action || card.actionType != ActionType.SLY_DEAL || !current.isCardInHand(playerId, card) || current.cardsLeftToPlay <= 0) return current
        val targetCard = cardMapping[action.targetCard] ?: return current
        if (targetCard !is Card.Property) return current
        val targetPlayer = playerToStealCardFrom(playerId, targetCard, current.playerState) ?: return current
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

suspend fun forcedDeal(room: DealGame, gameState: MutableStateFlow<GameState>, playerId: String, action: ForcedDeal) : GameState {
    return gameState.updateAndGet { current ->
        if (current.pendingInteractions.isNotEmpty() || current.playerAtTurn != playerId) return current
        val playerState = current.playerState[playerId] ?: return current
        val forcedDealCard = cardMapping[action.id] ?: return current
        if (forcedDealCard !is Card.Action || forcedDealCard.actionType != ActionType.FORCED_DEAL || !current.isCardInHand(playerId, forcedDealCard) || current.cardsLeftToPlay <= 0) return current
        val cardToGive = cardMapping[action.cardToGive] ?: return current
        if (cardToGive !is Card.Property) return current
        playerState.getSetOfProperty(cardToGive) ?: return current
        val targetCard = cardMapping[action.targetCard] ?: return current
        if (targetCard !is Card.Property) return current
        val targetPlayer = playerToStealCardFrom(playerId, targetCard, current.playerState) ?: return current
        val forcedDealMessage = ForcedDealMessage(playerId, targetPlayer, action.cardToGive, action.colorToReceiveAs, action.targetCard)
        current.pendingInteractions.add(
            PendingInteraction(
                fromPlayer = playerId,
                toPlayer = targetPlayer,
                action = forcedDealMessage,
                initial = listOf(action.id),
                awaitingResponseFrom = targetPlayer,
            )
        )
        room.sendBroadcast(forcedDealMessage)
        current.discardPile.add(forcedDealCard)
        playerState.hand.removeIf { it.id == forcedDealCard.id }
        return@updateAndGet current.copy(cardsLeftToPlay = current.cardsLeftToPlay - 1)
    }
}
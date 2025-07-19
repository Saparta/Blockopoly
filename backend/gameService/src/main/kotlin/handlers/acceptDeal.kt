package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.cardMapping
import com.gameservice.models.AcceptDeal
import com.gameservice.models.Card
import com.gameservice.models.Color
import com.gameservice.models.GameState
import com.gameservice.models.PlayerState
import com.gameservice.models.SlyDealAcceptedMessage
import com.gameservice.models.SlyDealMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

suspend fun acceptDeal(room: DealGame, game: MutableStateFlow<GameState>, playerId: String, action: AcceptDeal) : GameState {
    return game.updateAndGet { current ->
        val interaction = current.pendingInteractions.getTargetedInteraction(playerId) ?: return current
        if (interaction.awaitingResponseFrom != playerId) return current
        if (interaction.initial.size + interaction.offense.size == interaction.defense.size) return current
        val receiverState = current.playerState[interaction.fromPlayer] ?: return current
        val giverState = current.playerState[interaction.toPlayer] ?: return current
        val request = interaction.action
        when (request) {
            is SlyDealMessage -> {
                val targetCardInstance = cardMapping[request.targetCard] ?: return current
                val destination = transferCard(receiverState, giverState, request.receivingAs, targetCardInstance) ?: return current
                current.pendingInteractions.remove(interaction)
                room.sendBroadcast(
                    SlyDealAcceptedMessage(
                        interaction.fromPlayer,
                        interaction.toPlayer,
                        request.targetCard,
                        destination
                    )
                )
            }
            else -> return current
        }
        return@updateAndGet current.copy()
    }
}

fun transferCard(receiverState: PlayerState, giverState: PlayerState, receiveAs: Color?, card: Card) : String? {
    when (card) {
        is Card.Property -> {
            giverState.removeProperty(card) ?: return null
            return receiverState.addProperty(card, receiveAs)
        }
        is Card.Action -> {
            if (receiveAs == null) return null
            giverState.removeDevelopment(card) ?: return null
            return receiverState.addDevelopment(card, receiveAs)
        }
        else -> return null
    }
}
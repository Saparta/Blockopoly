package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.cardMapping
import com.gameservice.models.ALL_COLOR_SET
import com.gameservice.models.ActionType
import com.gameservice.models.Card
import com.gameservice.models.GameState
import com.gameservice.models.PendingInteraction
import com.gameservice.models.RentRequestMessage
import com.gameservice.models.RequestRent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

suspend fun requestRent(room: DealGame, game: MutableStateFlow<GameState>, playerId: String, rentRequest: RequestRent) : GameState {
    return game.updateAndGet { current ->
        if (current.pendingInteractions.isNotEmpty()) return current
        val rentCard = cardMapping[rentRequest.rentCardId] ?: return current
        if (rentCard !is Card.Rent) return current
        val playerState = current.playerState[playerId] ?: return current
        val numCardsConsumed = 1 + rentRequest.rentDoublers.size
        val doublers = rentRequest.rentDoublers.map {
            val doubleRentCard = cardMapping[it]
            if (doubleRentCard !is Card.ActionCard || doubleRentCard.actionType != ActionType.DOUBLE_RENT) {
                return current
            }
            return@map doubleRentCard
        }
        val isWildRent = rentCard.colors == ALL_COLOR_SET
        val validTargeting = (isWildRent && rentRequest.target != null) || (!isWildRent && rentRequest.target == null)
        if (doublers.any { !current.isCardInHand(playerId, it) }) return current
        val setBeingCharged = playerState.getPropertySet(rentRequest.rentingSetId) ?: return current
        if (current.playerAtTurn != playerId || current.cardsLeftToPlay < numCardsConsumed || !validTargeting ||
            !current.isCardInHand(playerId, rentCard) ||
            !rentCard.colors.contains(setBeingCharged.color) ||
            (rentRequest.target != null && current.playerState[rentRequest.target] == null)) {
            return current
        }

        val cardsUsed = listOf(rentCard.id) + rentRequest.rentDoublers
        val rentRequestMessage = RentRequestMessage(
            playerId,
            if (rentRequest.target == null) current.playerState.keys.filter { it != playerId } else listOf(rentRequest.target),
            cardsUsed,
            setBeingCharged.calculateRent()
        )

        if (rentRequest.target != null) {
            current.pendingInteractions.add(
                PendingInteraction(
                    playerId,
                    rentRequest.target,
                    rentRequestMessage,
                    cardsUsed.toMutableList(),
                    rentRequest.target
                )
            ) ?: return current
        } else {
            current.playerState.keys.forEach { victim ->
                if (victim == playerId) return@forEach
                current.pendingInteractions.add(
                    PendingInteraction(
                        playerId,
                        victim,
                        rentRequestMessage,
                        cardsUsed.toMutableList(),
                        victim,
                    )
                ) ?: return current
            }
        }
        room.sendBroadcast(rentRequestMessage)
        playerState.hand.removeIf { it.id in cardsUsed }
        current.discardPile.addAll(doublers + rentCard)
        return@updateAndGet current.copy(cardsLeftToPlay = current.cardsLeftToPlay - numCardsConsumed)
    }
}
package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.cardMapping
import com.gameservice.models.ActionType
import com.gameservice.models.Card
import com.gameservice.models.GameState
import com.gameservice.models.PendingInteraction
import com.gameservice.models.RentRequestMessage
import com.gameservice.models.RequestRent
import kotlinx.coroutines.flow.MutableStateFlow

suspend fun requestRent(room: DealGame, game: MutableStateFlow<GameState>, playerId: String, rentRequest: RequestRent) : GameState{
    val current = game.value
    val rentCard = cardMapping[rentRequest.rentCardId] ?: return current
    if (rentCard !is Card.Rent) return current
    val numCardsConsumed = 1 + rentRequest.rentDoublers.size
    val doublers = rentRequest.rentDoublers.map {
        val doubleRentCard = cardMapping[it]
        if (doubleRentCard !is Card.ActionCard || doubleRentCard.actionType != ActionType.DOUBLE_RENT) {
            return current
        }
        return@map doubleRentCard
    }
    if (doublers.any { !current.isCardInHand(playerId, it) }) return current
    val setBeingCharged = current.playerState[playerId]?.propertyCollection?.getPropertySet(rentRequest.rentingSetId) ?: return current
    if (current.playerAtTurn != playerId || current.cardsLeftToPlay < numCardsConsumed ||
        !current.isCardInHand(playerId, rentCard) ||
        !rentCard.colors.contains(setBeingCharged.color) ||
        (rentRequest.target != null && current.playerState[rentRequest.target] != null)) {
        return current
    }


    val rentRequestMessage = RentRequestMessage(
        playerId,
        if (rentRequest.target == null) current.playerState.keys.filter { it != playerId } else listOf(rentRequest.target),
        (listOf(rentCard.id) + rentRequest.rentDoublers),
        setBeingCharged.calculateRent())

    if (rentRequest.target != null) {
        current.pendingInteractions.add(PendingInteraction(playerId, rentRequest.target, rentRequestMessage, rentRequest.target, mutableListOf()))
    } else {
        current.playerState.keys.forEach { victim ->
            if (victim == playerId) return@forEach
            current.pendingInteractions.add(PendingInteraction(playerId, victim, rentRequestMessage, victim, mutableListOf()))
        }
    }
    room.sendBroadcast(rentRequestMessage)
    current.discardPile.addAll(doublers + rentCard)
    return current.copy(cardsLeftToPlay = current.cardsLeftToPlay - numCardsConsumed)
}
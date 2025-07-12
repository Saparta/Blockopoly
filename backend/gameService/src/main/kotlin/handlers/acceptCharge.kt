package com.gameservice.handlers

import com.gameservice.DEVELOPMENT_ACTION_CARDS
import com.gameservice.DealGame
import com.gameservice.cardMapping
import com.gameservice.models.AcceptCharge
import com.gameservice.models.ActionType
import com.gameservice.models.Card
import com.gameservice.models.GameState
import com.gameservice.models.PaymentEarningsMessage
import com.gameservice.models.PendingInteraction
import com.gameservice.models.RentRequestMessage
import kotlinx.coroutines.flow.MutableStateFlow

suspend fun acceptCharge(room: DealGame, game: MutableStateFlow<GameState>, playerId: String, payment: AcceptCharge) : GameState {
    val current = game.value
    val interaction = current.pendingInteractions.find { it.toPlayer == playerId } ?: return current
    if (interaction.awaitingResponseFrom != playerId) return current
    return when (interaction.action) {
        is RentRequestMessage -> handleRentPayment(room, current, playerId, payment, interaction)
        else -> return current
    }
}

suspend fun handleRentPayment(room: DealGame, gameState: GameState, playerId: String, payment: AcceptCharge, interaction: PendingInteraction) : GameState {
    val paymentCards = payment.payment.map { cardMapping[it] ?: return gameState}
    val playerState = gameState.playerState[playerId] ?: return gameState
    val request = interaction.action as? RentRequestMessage ?: return gameState
    val receiverPlayerState = gameState.playerState[request.requester] ?: return gameState
    if (paymentCards.sumOf { it.value ?: return gameState } < resolveRentJSNStack(interaction)) return gameState
    // Validate each card as belonging to paying player before starting payment
    paymentCards.forEach { card ->
        when (card) {
            is Card.Property -> {
                if (!playerState.propertyCollection.isPropertyInCollection(card)) return gameState
            }
            is Card.ActionCard -> {
                if (!playerState.bank.contains(card)) {
                    if (card.actionType in DEVELOPMENT_ACTION_CARDS) {
                        if (card !is Card.Action) return gameState
                        if (!playerState.propertyCollection.isDevelopmentInCollection(card)) return gameState
                    } else return gameState
                }
            }
            is Card.Money -> {
                if (!playerState.bank.contains(card)) return gameState
            }
        }
    }
    // Complete payment
    val propertyToDestinations = mutableMapOf<Int, String>()
    val bankCards = mutableSetOf<Int>()
    paymentCards.forEach { card ->
        when (card) {
            is Card.Property -> {
                playerState.propertyCollection.removeProperty(card)
                // Players can always Move property after they receive it since it's their turn so color isn't too important
                val destination = receiverPlayerState.propertyCollection.addProperty(card, card.colors.first())!!
                propertyToDestinations[card.id] = destination
            }
            is Card.ActionCard -> {
                if (!playerState.bank.remove(card)) {
                    if (card.actionType in DEVELOPMENT_ACTION_CARDS) {
                        playerState.propertyCollection.removeDevelopment(card as Card.Action)
                    }
                }
                receiverPlayerState.bank.add(card)
                bankCards.add(card.id)
            }
            is Card.Money -> {
                playerState.bank.remove(card)
                receiverPlayerState.bank.add(card)
                bankCards.add(card.id)
            }
        }
    }
    room.sendBroadcast(
        PaymentEarningsMessage(
            request.requester,
            playerId,
            propertyToDestinations,
            bankCards
        )
    )
    return gameState
}

fun resolveRentJSNStack(interaction: PendingInteraction) : Int {
    val request = interaction.action as RentRequestMessage
    val jsnStack = interaction.jsnStack
    val allCards = (request.cardsUsed + jsnStack).toMutableList()
    var amount = request.amount
    var lastCardId = allCards.last()
    while (cardMapping[lastCardId] is Card.Action && allCards.size > 1 && (cardMapping[lastCardId] as? Card.Action)?.actionType == ActionType.JUST_SAY_NO) {
        allCards.removeLast()
        allCards.removeLast()
        lastCardId = allCards.last()
    }
    val difference = allCards - request.cardsUsed
    if (difference.isEmpty()) return 0
    while (difference.size > 1) {
        amount /= 2
    }
    return amount
}
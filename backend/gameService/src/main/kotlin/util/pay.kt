package com.gameservice.util

import com.gameservice.DEVELOPMENT_ACTION_CARDS
import com.gameservice.cardMapping
import com.gameservice.models.Card
import com.gameservice.models.GameState

data class PayResponse(val success: Boolean = false, val propertyToDestinations: Map<Int, String> = emptyMap(), val bankCards: Set<Int> = emptySet())

fun pay(gameState: GameState, giver: String, receiver: String, payment: List<Int>) : PayResponse {
    val paymentCards = payment.map { cardMapping[it] ?: return PayResponse() }
    val playerState = gameState.playerState[giver] ?: return PayResponse()
    val receiverPlayerState = gameState.playerState[receiver] ?: return PayResponse()
    // Validate each card as belonging to paying player before starting payment
    paymentCards.forEach { card ->
        when (card) {
            is Card.Property -> {
                if (!playerState.propertyCollection.isPropertyInCollection(card)) return PayResponse()
            }
            is Card.ActionCard -> {
                if (!playerState.bank.contains(card)) {
                    if (card.actionType in DEVELOPMENT_ACTION_CARDS) {
                        if (card !is Card.Action) return PayResponse()
                        if (!playerState.propertyCollection.isDevelopmentInCollection(card)) return PayResponse()
                    } else return PayResponse()
                }
            }
            is Card.Money -> {
                if (!playerState.bank.contains(card)) return PayResponse()
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
    return PayResponse(true, propertyToDestinations, bankCards)
}
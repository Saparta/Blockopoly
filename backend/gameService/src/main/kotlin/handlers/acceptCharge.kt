package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.cardMapping
import com.gameservice.models.AcceptCharge
import com.gameservice.models.GameState
import com.gameservice.models.PaymentEarningsMessage
import com.gameservice.models.PendingInteraction
import com.gameservice.models.RentRequestMessage
import com.gameservice.util.pay
import kotlinx.coroutines.flow.MutableStateFlow

suspend fun acceptCharge(room: DealGame, game: MutableStateFlow<GameState>, playerId: String, payment: AcceptCharge) : GameState {
    val current = game.value
    val interaction = current.pendingInteractions.getTargetedInteraction(playerId) ?: return current
    if (interaction.awaitingResponseFrom != playerId) return current
    return when (interaction.action) {
        is RentRequestMessage -> handleRentPayment(room, current, playerId, payment, interaction)
        else -> return current
    }
}

suspend fun handleRentPayment(room: DealGame, gameState: GameState, playerId: String, payment: AcceptCharge, interaction: PendingInteraction) : GameState {
    val paymentCards = payment.payment.map { cardMapping[it] ?: return gameState}
    val request = interaction.action as? RentRequestMessage ?: return gameState
    if (paymentCards.sumOf { it.value ?: return gameState } <
        if (interaction.resolved) request.amount else resolveRentJSNStack(interaction)
        ) return gameState

    val (success, propertyDestinations, bankCards) = pay(gameState, playerId, request.requester, payment.payment)
    if (!success) return gameState
    gameState.pendingInteractions.remove(interaction)
    room.sendBroadcast(
        PaymentEarningsMessage(
            request.requester,
            playerId,
            propertyDestinations,
            bankCards
        )
    )
    return gameState.copy()
}

fun resolveRentJSNStack(interaction: PendingInteraction) : Int {
    val request = interaction.action as RentRequestMessage
    if (interaction.resolved) return request.amount
    val offense = interaction.offense
    val defense = interaction.defense
    var amount = request.amount
    if (defense.size >= offense.size + interaction.initial.size) {
        return 0 // Illegal State, should not be attempting to pay a canceled action
    }
    if (defense.size > offense.size) {
        val sizeDiff = defense.size - offense.size
        val remainingCards = interaction.initial.subList(0, interaction.initial.size - sizeDiff)
        repeat(interaction.initial.size - remainingCards.size) {
            amount /= 2
        }
    }
    return amount
}
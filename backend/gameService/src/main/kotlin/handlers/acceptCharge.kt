package com.gameservice.handlers

import com.gameservice.BIRTHDAY_PAYMENT_AMOUNT
import com.gameservice.DEBT_COLLECTOR_PAYMENT_AMOUNT
import com.gameservice.DealGame
import com.gameservice.NUM_COMPLETE_SETS_TO_WIN
import com.gameservice.models.AcceptCharge
import com.gameservice.models.BirthdayMessage
import com.gameservice.models.DebtCollectMessage
import com.gameservice.models.GameState
import com.gameservice.models.PaymentEarningsMessage
import com.gameservice.models.PendingInteraction
import com.gameservice.models.RentRequestMessage
import com.gameservice.util.pay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

suspend fun acceptCharge(room: DealGame, game: MutableStateFlow<GameState>, playerId: String, payment: AcceptCharge) : GameState {
    return game.updateAndGet { current ->
        val interaction = current.pendingInteractions.getTargetedInteraction(playerId) ?: return current
        if (interaction.awaitingResponseFrom != playerId) return current
        return@updateAndGet handlePayment(room, current, playerId, payment, interaction)
    }
}

suspend fun handlePayment(room: DealGame, gameState: GameState, playerId: String, payment: AcceptCharge, interaction: PendingInteraction) : GameState {
    if (interaction.initial.size + interaction.offense.size == interaction.defense.size) return gameState
    val request = interaction.action
    val amountRequested = when (interaction.action) {
        is BirthdayMessage -> BIRTHDAY_PAYMENT_AMOUNT
        is DebtCollectMessage -> DEBT_COLLECTOR_PAYMENT_AMOUNT
        is RentRequestMessage -> resolveRentJSNStack(interaction)
        else -> return gameState
    }
    val receiverState = gameState.playerState[request.requester] ?: return gameState

    val (success, propertyDestinations, bankCards) = pay(gameState, playerId, request.requester, payment.payment, amountRequested)
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
    var winner: String? = null
    if (receiverState.numCompleteSets() == NUM_COMPLETE_SETS_TO_WIN) {
        winner = request.requester
    }
    return gameState.copy(winningPlayer = winner)
}

fun resolveRentJSNStack(interaction: PendingInteraction) : Int {
    val request = interaction.action as RentRequestMessage
    if (interaction.resolved) return request.amount
    val offense = interaction.offense
    val defense = interaction.defense
    var amount = request.amount
    if (defense.size >= offense.size + interaction.initial.size) throw IllegalStateException() // Interaction should've been canceled
    if (defense.size > offense.size) {
        val sizeDiff = defense.size - offense.size
        val remainingCards = interaction.initial.subList(0, interaction.initial.size - sizeDiff)
        repeat(interaction.initial.size - remainingCards.size) {
            amount /= 2
        }
    }
    return amount
}
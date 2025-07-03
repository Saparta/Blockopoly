package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.cardMapping
import com.gameservice.models.Card
import com.gameservice.models.GameState
import com.gameservice.models.PlaceInBankMessage
import com.gameservice.models.PlayMoney
import kotlinx.coroutines.flow.MutableStateFlow

suspend fun playForMoney(room: DealGame, game: MutableStateFlow<GameState>, playerId: String, playMoney: PlayMoney) : GameState{
    val current = game.value
    val card = cardMapping[playMoney.id] ?: return current
    if (card is Card.Property) return current

    if (current.playerAtTurn != playerId ||
        current.cardsLeftToPlay <= 0 ||
        !current.isCardInHand(playerId, card)) return current

    return current.let { state ->
        state.playerState[playerId]!!.hand.removeIf { it.id == card.id }
        state.playerState[playerId]!!.bank.add(card)
        room.sendBroadcast(PlaceInBankMessage(playerId, card))
        val cardsLeft = state.cardsLeftToPlay - 1
        return@let state.copy(cardsLeftToPlay = cardsLeft)
    }
}
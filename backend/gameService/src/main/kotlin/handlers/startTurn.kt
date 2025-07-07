package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.models.DrawMessage
import com.gameservice.models.GameState
import com.gameservice.models.StartMessage
import kotlinx.coroutines.flow.MutableStateFlow

suspend fun startTurn(room: DealGame, game: MutableStateFlow<GameState>, playerId: String) : GameState {
    if (game.value.playerAtTurn == playerId) {
        val cardsDrawn = game.value.draw()
        room.sendBroadcast(StartMessage(playerId))
        room.sendBroadcast(DrawMessage(playerId, cardsDrawn))
        return game.value.copy()
    }
    return game.value
}
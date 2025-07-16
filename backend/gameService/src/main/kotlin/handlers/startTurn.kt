package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.models.DrawMessage
import com.gameservice.models.GameState
import com.gameservice.models.StartMessage

suspend fun startTurn(room: DealGame, gameState: GameState, playerId: String) : GameState {
    return gameState.let { current ->
        if (current.pendingInteractions.isNotEmpty() || current.turnStarted) return current
        if (current.playerAtTurn != playerId) return current
        val cardsDrawn = current.draw()
        room.sendBroadcast(StartMessage(playerId))
        room.sendBroadcast(DrawMessage(playerId, cardsDrawn))
        return@let current.copy(turnStarted = true)
    }
}
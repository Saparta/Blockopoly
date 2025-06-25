package com.gameservice.handlers

import com.gameservice.models.GameState
import kotlinx.coroutines.flow.MutableStateFlow

fun endTurn(game: MutableStateFlow<GameState>, playerId: String) : GameState {
    val current = game.value
    if (current.playerAtTurn != playerId) return current
    val nextPlayer = current.playerOrder[(current.playerOrder.indexOf(playerId) + 1) % current.playerOrder.size]
    return current.copy(playerAtTurn = nextPlayer)
}
package com.gameservice.handlers

import com.gameservice.models.GameState
import kotlinx.coroutines.flow.MutableStateFlow

fun startTurn(game: MutableStateFlow<GameState>, playerId: String) : GameState {
    if (game.value.playerAtTurn == playerId) {
        return game.value.draw()
    }
    return game.value
}
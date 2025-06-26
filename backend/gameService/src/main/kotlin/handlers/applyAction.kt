package com.gameservice.handlers

import com.gameservice.models.EndTurn
import com.gameservice.models.GameAction
import com.gameservice.models.GameState
import com.gameservice.models.PlayMoney
import com.gameservice.models.PlayProperty
import com.gameservice.models.StartTurn
import kotlinx.coroutines.flow.MutableStateFlow

fun applyAction(game: MutableStateFlow<GameState>, playerId: String, action: GameAction) : GameState {
    return when (action) {
        is StartTurn -> startTurn(game, playerId)
        is PlayProperty -> playProperty(game, playerId, action)
        is PlayMoney -> playForMoney(game, playerId, action)
        is EndTurn -> endTurn(game, playerId)
    }
}
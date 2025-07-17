package com.gameservice.handlers

import com.gameservice.DealGame
import com.gameservice.models.AcceptCharge
import com.gameservice.models.AcceptJsn
import com.gameservice.models.Birthday
import com.gameservice.models.DebtCollect
import com.gameservice.models.EndTurn
import com.gameservice.models.GameAction
import com.gameservice.models.GameState
import com.gameservice.models.JustSayNo
import com.gameservice.models.PassGo
import com.gameservice.models.PlayMoney
import com.gameservice.models.PlayProperty
import com.gameservice.models.RequestRent
import com.gameservice.models.StartTurn
import kotlinx.coroutines.flow.MutableStateFlow

suspend fun applyAction(room: DealGame, game: MutableStateFlow<GameState>, playerId: String, action: GameAction) : GameState {
    return when (action) {
        is StartTurn -> startTurn(room, game.value, playerId)
        is PlayProperty -> playProperty(room, game, playerId, action)
        is PlayMoney -> playForMoney(room, game, playerId, action)
        is RequestRent -> requestRent(room, game, playerId, action)
        is AcceptCharge -> acceptCharge(room, game, playerId, action)
        is JustSayNo -> justSayNo(room, game, playerId, action)
        is AcceptJsn -> acceptJsn(room, game, playerId, action)
        is PassGo -> passGo(room, game, playerId, action)
        is EndTurn -> endTurn(room, game, playerId)
        is Birthday -> TODO()
        is DebtCollect -> TODO()
    }
}
package com.gameservice

import com.gameservice.models.Card
import com.gameservice.models.GameState
import com.gameservice.models.PlayerState
import com.gameservice.models.PropertyCollection
import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap

// Manages 1 game room
class RoomManager(val roomId: String, val players: List<String>) {
    private lateinit var state : MutableStateFlow<GameState>
    private val playerSockets = ConcurrentHashMap<String, WebSocketSession>()

    fun connectPlayer(playerId: String, session: WebSocketSession) {
        if (playerId in players && !playerSockets.containsKey(playerId)) {
            playerSockets[playerId] = session
        }
        if (playerSockets.size == players.size) {
            val game = GameState()
            game.playerAtTurn = players.random()
            players.forEach {
                val playerHand = mutableListOf<Card>()
                repeat(INITIAL_DRAW_COUNT) { playerHand.add(game.drawPile.removeFirst())}
                game.playerState.put(it, PlayerState(playerHand, PropertyCollection(), mutableListOf()))
            }
        }
    }
}
package com.gameservice

import com.gameservice.models.GameState
import com.gameservice.models.PlayerState
import com.gameservice.models.PropertyCollection
import com.gameservice.models.SocketMessage
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

// Manages 1 game room
class RoomManager(val roomId: String, val players: List<String>) {
    private lateinit var state : MutableStateFlow<GameState>
    private val playerSockets = ConcurrentHashMap<String, WebSocketSession>()

    suspend fun broadcast(message: SocketMessage) {
        for ((_, session) in playerSockets) {
            session.send(Json.encodeToString(message))
        }
    }

    suspend fun broadcastState() {
        for ((id, session) in playerSockets) {
            try {
                session.send(
                    Json.encodeToString(
                        SocketMessage(
                            MessageType.STATE.toString(),
                            Json.encodeToString(state.value.stateVisibleToPlayer(id))
                        )
                    )
                )
            } catch (e: Exception) {
                playerSockets.remove(id)
                broadcast(SocketMessage(MessageType.LEAVE.toString(), id))
            }
        }
    }

    suspend fun connectPlayer(playerId: String, session: WebSocketSession) {
        if (playerId in players && !playerSockets.containsKey(playerId)) {
            playerSockets[playerId] = session
        }
        if (playerSockets.size == players.size) {
            val game = GameState()
            game.playerAtTurn = players.random()
            players.forEach { id ->
                val hand = MutableList(INITIAL_DRAW_COUNT) { game.drawPile.removeFirst() }
                game.playerState[id] = PlayerState(hand, PropertyCollection(), mutableListOf())
            }
            state = MutableStateFlow(game)
            broadcastState()
        }
    }
}
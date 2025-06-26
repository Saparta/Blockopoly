package com.gameservice

import com.gameservice.handlers.applyAction
import com.gameservice.models.Command
import com.gameservice.models.GameState
import com.gameservice.models.PlayerState
import com.gameservice.models.PropertyCollection
import com.gameservice.models.SocketMessage
import io.ktor.websocket.CloseReason
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

// Manages 1 game room
class DealGame(val roomId: String, val players: List<String>) {
    private val state = CompletableDeferred<MutableStateFlow<GameState>>()
    private val commandChannel = Channel<Command>(capacity = Channel.UNLIMITED)
    private val playerSockets = ConcurrentHashMap<String, WebSocketSession>()
    private val gameScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // Process commands from websocket sequentially.
        // Don't need highly concurrent access to GameState and prevents Concurrent Modifications Errors
        gameScope.launch {
            for (command in commandChannel) {
                val newState = applyAction(state.await(), command.playerId, command.command )
                state.await().tryEmit(newState)
            }
        }
    }

    suspend fun sendCommand(command: Command) {
        commandChannel.send(command)
    }

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
                            Json.encodeToString(state.await().value.stateVisibleToPlayer(id))
                        )
                    )
                )
            } catch (_: Exception) {
                playerSockets.remove(id)
                broadcast(SocketMessage(MessageType.LEAVE.toString(), id))
            }
        }
    }

     suspend fun connectPlayer(playerId: String, session: WebSocketSession) : CompletableDeferred<MutableStateFlow<GameState>>? {
        if (playerId in players) {
            if (playerSockets.containsKey(playerId)) {
                playerSockets.get(playerId)?.close(CloseReason(CloseReason.Codes.NORMAL, "Player started new connection"))
                if (state.isCompleted) {
                    val playOrder = state.await().value.playerOrder
                    session.send(Json.encodeToString(SocketMessage(MessageType.PLAY_ORDER.toString(), Json.encodeToString(playOrder))))
                    session.send(Json.encodeToString(SocketMessage(MessageType.STATE.toString(),
                                Json.encodeToString(state.await().value.stateVisibleToPlayer(playerId)))))
                }
            }
            playerSockets[playerId] = session
        } else return null

        if (playerSockets.size == players.size && !state.isCompleted) {
            val game = GameState()
            game.playerOrder = players.shuffled()
            game.playerAtTurn = game.playerOrder.first()
            players.forEach { id ->
                val hand = MutableList(INITIAL_DRAW_COUNT) { game.drawPile.removeFirst() }
                game.playerState[id] = PlayerState(hand, PropertyCollection(), mutableListOf())
            }
            state.complete(MutableStateFlow(game))
            broadcast(SocketMessage(MessageType.PLAY_ORDER.toString(), Json.encodeToString(game.playerOrder)))
            gameScope.launch {
                state.await()
                    .collect { newState ->
                        broadcastState()
                    }
            }
        }
         return state
    }
}
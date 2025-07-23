package com.gameservice

import com.gameservice.handlers.applyAction
import com.gameservice.models.Command
import com.gameservice.models.DrawMessage
import com.gameservice.models.GameState
import com.gameservice.models.SocketMessage
import com.gameservice.models.StartTurn
import com.gameservice.models.StateMessage
import io.ktor.websocket.CloseReason
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.send
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

// Manages 1 game room
class DealGame(val roomId: String, val players: List<String>) {
    val state = CompletableDeferred<MutableStateFlow<GameState>>()
    private val commandChannel = Channel<Command>(capacity = Channel.UNLIMITED)
    private val broadcastChannel = Channel<SocketMessage>(capacity = Channel.UNLIMITED)
    private val playerSockets = ConcurrentHashMap<String, WebSocketSession>()
    private val gameScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val initialBroadcastDone = CompletableDeferred<Unit>()


    init {
        // Process commands from websocket sequentially.
        // Don't need highly concurrent access to GameState and prevents Concurrent Modifications Errors
        gameScope.launch {
            for (command in commandChannel) {
                val newState = applyAction(this@DealGame, state.await(), command.playerId, command.command )
                state.await().tryEmit(newState)
            }
        }

        gameScope.launch {
            for (msg in broadcastChannel) {
                broadcast(msg)
            }
        }
    }

    suspend fun sendCommand(command: Command) = commandChannel.send(command)

    suspend fun sendBroadcast(message: SocketMessage) = broadcastChannel.send(message)

    // These broadcasts are mainly to help with animations on client. Authoritative GameState is broadcasted by updates to state
    private suspend fun broadcast(message: SocketMessage) {
        when (message) {
            is StateMessage -> {} // STATE is automatically broadcasted by coroutine in connectPlayer.
            is DrawMessage -> {
                val fakeCards = List(message.cards.size) {FAKE_CARD}
                for ((player, session) in playerSockets) {
                    val cards = if (player == message.playerId) message.cards else fakeCards
                    session.send(DrawMessage(message.playerId, cards).toJson())
                }
            }
            else -> for ((_, session) in playerSockets) {
                session.send(message.toJson())
            }
        }
    }

    private suspend fun broadcastState(newState: GameState) {
        for ((id, session) in playerSockets) {
            session.send(StateMessage(newState.getVisibleGameState(id)).toJson())
        }
    }

     @OptIn(FlowPreview::class)
     suspend fun connectPlayer(playerId: String, session: WebSocketSession) : CompletableDeferred<MutableStateFlow<GameState>>? {
        if (playerId in players) {
            if (playerSockets.containsKey(playerId)) {
                playerSockets.get(playerId)?.close(CloseReason(CloseReason.Codes.NORMAL, "Player started new connection"))
                if (state.isCompleted) {
                    session.send(StateMessage(state.await().value.getVisibleGameState(playerId)).toJson())
                }
            }
            playerSockets[playerId] = session
        } else return null

        if (playerSockets.size == players.size && !state.isCompleted) {
            val game = GameState(players)
            state.complete(MutableStateFlow(game))

            gameScope.launch {
                state.await()
                    .debounce(100)
                    .collect { newState ->
                        broadcastState(newState)
                        if (!initialBroadcastDone.isCompleted) {
                            initialBroadcastDone.complete(Unit)
                        }
                    }
            }

            gameScope.launch {
                initialBroadcastDone.await()
                sendCommand(Command(game.playerAtTurn!!, StartTurn()))
            }

        }
         return state
    }
}
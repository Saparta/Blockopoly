package com.gameservice

import com.gameservice.models.Command
import com.gameservice.models.GameAction
import com.gameservice.models.StartTurn
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    routing {
        get("/") {call.respond(HttpStatusCode.OK)}
        route("/ws/play/{roomId}/{playerId}") {
            webSocket{
                val roomId = call.parameters["roomId"] ?: return@webSocket
                val playerId = call.parameters["playerId"] ?: return@webSocket
                ServerManager.connectToRoom(roomId, playerId, this)?.await() ?: return@webSocket
                val game = ServerManager.getRoom(roomId)!!
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val actionCommand = Json.decodeFromString<GameAction>(frame.readText())
                        when (actionCommand) {
                            is StartTurn -> {}
                            else -> game.sendCommand(Command(playerId, actionCommand))
                        }
                    }
                }
            }
        }
    }
}

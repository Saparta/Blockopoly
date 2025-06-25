package com.gameservice

import com.gameservice.handlers.applyAction
import com.gameservice.models.GameAction
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.future.await
import kotlinx.serialization.json.Json

fun Application.configureRouting() {
    routing {
        get("/") {call.respond(HttpStatusCode.OK)}
        post("/start/{roomId}") {
            // TODO: Use JWT + playerId from request body to guarantee the user starting the room is part of the room
            call.application.environment.log.info("Starting game")
            val redis = call.application.attributes[REDIS_COMMANDS_KEY]
            val roomId = call.parameters["roomId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val roomStartable = redis.llen(ROOM_TO_PLAYERS_PREFIX + roomId).await()
            if (roomStartable < 2) {
                call.respond(HttpStatusCode.BadRequest)
            }
            val roomStarted = redis.setnx(ROOM_START_STATUS_PREFIX + roomId, "true").await()
            redis.expire(ROOM_START_STATUS_PREFIX + roomId, SECONDS_IN_DAY.toLong())
            if (!roomStarted) {
                return@post call.respond(HttpStatusCode.InternalServerError)
            }
            ServerManager.addRoom(DealGame(roomId, redis.lrange(ROOM_TO_PLAYERS_PREFIX + roomId, 0, -1).await()))
            redis.publish(roomId, "START#")
            call.respond(HttpStatusCode.OK)
        }

        route("/ws/play/{roomId}/{playerId}") {
            webSocket{
                val roomId = call.parameters["roomId"] ?: return@webSocket
                val playerId = call.parameters["playerId"] ?: return@webSocket
                val blockopolyGame = ServerManager.connectToRoom(roomId, playerId, this) ?: return@webSocket
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        applyAction(blockopolyGame.await(), playerId, Json.decodeFromString<GameAction>(frame.readText()))
                    }
                }
            }
        }
    }
}

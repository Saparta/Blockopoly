package com.gameservice

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.future.await

fun Application.configureRouting() {
    routing {
        get("/") {call.respond(HttpStatusCode.OK)}
        post("/start/{roomId}") {
            val redis = call.application.attributes[REDIS_COMMANDS_KEY]
            val roomId = call.parameters["roomId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val roomStarted = redis.setnx(Constants.ROOM_START_STATUS_PREFIX + roomId, "true")
            redis.expire(Constants.ROOM_START_STATUS_PREFIX + roomId, Constants.SECONDS_IN_DAY.toLong())
            if (!roomStarted.await()) {
                return@post call.respond(HttpStatusCode.InternalServerError)
            }
            redis.publish(roomId, "START#")
        }

        route("/ws/play/{playerId}") {
            webSocket{

            }
        }
    }
}

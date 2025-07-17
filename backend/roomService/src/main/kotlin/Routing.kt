package com.roomservice

import com.roomservice.routes.closeRoomHandler
import com.roomservice.routes.createRoomHandler
import com.roomservice.routes.joinRoomHandler
import com.roomservice.routes.leaveRoomHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import kotlinx.coroutines.future.await
import kotlin.time.Duration.Companion.seconds


fun Application.configureRouting() {
    routing {
        sse("/createRoom/{username}") {
            heartbeat { period = 20.seconds }
            call.application.environment.log.info("Room Create Begun")
            createRoomHandler(call, this)
        }
        sse("/joinRoom/{roomCode}/{username}") {
            heartbeat { period = 20.seconds }
            call.application.environment.log.info("Room Join Begun")
            joinRoomHandler(call, this)
        }
        post("/leaveRoom/{playerId}") {
            call.application.environment.log.info("Room Leave Begun")
            leaveRoomHandler(call)
        }
        post("/closeRoom/{roomId}") {
            call.application.environment.log.info("Room Close Begun")
            closeRoomHandler(call)
        }
        post("/start/{roomId}") {
            // TODO: Use JWT + playerId from request body to guarantee the user starting the room is part of the room
            // TODO: Address race condition(room marked as startable but player count then drops below 2) with Redis Multi and Exec
            call.application.environment.log.info("Starting game")
            val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
            val roomId = call.parameters["roomId"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val roomStartable = redis.llen(ROOM_TO_PLAYERS_PREFIX + roomId).await()
            if (roomStartable < 2) {
                call.respond(HttpStatusCode.BadRequest)
            }
            val roomStarted = redis.setnx(ROOM_START_STATUS_PREFIX + roomId, "true").await()
            redis.expire(ROOM_START_STATUS_PREFIX + roomId, SECS_IN_HOUR * 24)
            if (!roomStarted) {
                return@post call.respond(HttpStatusCode.InternalServerError)
            }
            redis.publish(roomId, "START${ROOM_BROADCAST_TYPE_DELIMITER}")
            val players = redis.lrange(ROOM_TO_PLAYERS_PREFIX + roomId, 0, -1).await()
            val msg = (listOf(roomId) + players).toTypedArray().joinToString(separator = ROOM_BROADCAST_MSG_DELIMITER)
            redis.publish(ROOM_START_CHANNEL, msg)
            call.respond(HttpStatusCode.OK)
        }
    }
}

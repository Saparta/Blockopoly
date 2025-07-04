package com.roomservice

import com.roomservice.routes.closeRoomHandler
import com.roomservice.routes.createRoomHandler
import com.roomservice.routes.joinRoomHandler
import com.roomservice.routes.leaveRoomHandler
import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
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
    }
}

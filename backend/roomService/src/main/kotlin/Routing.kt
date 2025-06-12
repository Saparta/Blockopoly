package com.roomservice

import com.roomservice.routes.closeRoomHandler
import com.roomservice.routes.createRoomHandler
import com.roomservice.routes.joinRoomHandler
import com.roomservice.routes.leaveRoomHandler
import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        post("/createRoom") { createRoomHandler(call) }
        post("/joinRoom/{roomCode}") { joinRoomHandler(call) }
        post("/leaveRoom/{playerId}") { leaveRoomHandler(call)  }
        post("/closeRoom/{roomId}") { closeRoomHandler(call) }
    }
}

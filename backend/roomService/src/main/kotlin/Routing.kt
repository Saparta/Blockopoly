package com.roomservice

import com.roomservice.routes.closeRoomHandler
import com.roomservice.routes.createRoomHandler
import com.roomservice.routes.joinRoomHandler
import com.roomservice.routes.leaveRoomHandler
import com.roomservice.routes.roomConnect
import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.sse


fun Application.configureRouting() {
    routing {
        post("/createRoom") { createRoomHandler(call) }
        post("/joinRoom/{roomCode}") { joinRoomHandler(call) }
        post("/leaveRoom/{playerId}") { leaveRoomHandler(call) }
        post("/closeRoom/{roomId}") { closeRoomHandler(call) }
        sse("/room/{roomCode}") { roomConnect(call, this) }
    }
}

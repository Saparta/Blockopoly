package com.roomservice

import com.roomservice.routes.closeRoomHandler
import com.roomservice.routes.createRoom
import com.roomservice.routes.joinRoomHandler
import com.roomservice.routes.leaveRoomHandler
import com.roomservice.routes.roomConnect
import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.sse


fun Application.configureRouting() {
    routing {
        sse("/createRoom/{username}") { createRoom(call, this) }
        post("/joinRoom/{roomCode}") { joinRoomHandler(call) }
        post("/leaveRoom/{playerId}") { leaveRoomHandler(call) }
        post("/closeRoom/{roomCode}") { closeRoomHandler(call) }
        sse("/room/{roomCode}") { roomConnect(call, this) }
    }
}

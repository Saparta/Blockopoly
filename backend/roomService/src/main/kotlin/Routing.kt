package com.roomservice

import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        post("/createRoom") { createRoomHandler(call) }
        post("/joinRoom/{id}") { joinRoomHandler(call) }
        post("/leaveRoom/{id}") { leaveRoomHandler(call)  }
        post("/closeRoom/{id}") { closeRoomHandler(call) }
    }
}

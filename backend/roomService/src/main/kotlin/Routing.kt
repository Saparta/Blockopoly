package com.roomservice

import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        post("/createRoom") {  }
        post("/joinRoom/{id}") {  }
        post("/leaveRoom/{id}") {  }
        post("/closeRoom/{id}") {  }
    }
}

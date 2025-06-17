package com.gameservice

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket

fun Application.configureRouting() {
    routing {
        get("/") {call.respond(HttpStatusCode.OK)}
        route("/ws/play") {
            webSocket{

            }
        }
    }
}

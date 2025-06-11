package com.roomservice

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

suspend fun createRoomHandler(call: ApplicationCall) {
    call.application.environment.log.info("the room has been created")
}
suspend fun joinRoomHandler(call: ApplicationCall) {
    call.application.environment.log.info("the room has been joined")
}
suspend fun leaveRoomHandler(call: ApplicationCall) {
    call.application.environment.log.info("the room has been leaved")
}
suspend fun closeRoomHandler(call: ApplicationCall) {
    call.application.environment.log.info("the room has been closed")
}
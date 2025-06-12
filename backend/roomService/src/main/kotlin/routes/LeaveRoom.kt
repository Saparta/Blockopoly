package com.roomservice.routes

import io.ktor.server.application.ApplicationCall

suspend fun leaveRoomHandler(call: ApplicationCall) {
    call.application.environment.log.info("the room has been leaved")
}
package com.roomservice

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.coroutines.future.await
import java.util.UUID

suspend fun createRoomHandler(call: ApplicationCall) {
    val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val hostID = UUID.randomUUID().toString()
    val roomID = UUID.randomUUID().toString()
    val hostFuture = redis.set(hostID, roomID)
    val roomFuture = redis.lpush(roomID, hostID).await()
    if (hostFuture.await() == "OK" && roomFuture == 1L) {
        call.run {
            application.environment.log.info("the room has been created")
            call.respond(HttpStatusCode.OK)
        }
    }
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
package com.roomservice

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asDeferred
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CreateRoomResponse(val playerID: String, val roomId: String)

suspend fun createRoomHandler(call: ApplicationCall) {
    val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val hostID = UUID.randomUUID().toString()
    val roomID = UUID.randomUUID().toString()
    val hostFuture = redis.set(hostID, roomID).asDeferred()
    val roomFuture = redis.lpush(roomID, hostID).asDeferred()
    val (hostStatus, roomStatus) = awaitAll(hostFuture, roomFuture)

    if (hostStatus == "OK" && roomStatus == 1L) {
        call.respond(HttpStatusCode.OK, CreateRoomResponse(hostID, roomID))
    } else {
        call.respond(HttpStatusCode.BadRequest, "Failed to create room")
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
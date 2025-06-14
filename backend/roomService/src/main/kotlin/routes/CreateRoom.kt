package com.roomservice.routes

import com.roomservice.Constants
import com.roomservice.Constants.JOIN_CODE_ALPHABET
import com.roomservice.Constants.JOIN_CODE_TO_ROOM_PREFIX
import com.roomservice.Constants.PLAYER_TO_NAME_PREFIX
import com.roomservice.Constants.PLAYER_TO_ROOM_PREFIX
import com.roomservice.Constants.ROOM_TO_JOIN_CODE_PREFIX
import com.roomservice.Constants.ROOM_TO_PLAYERS_PREFIX
import com.roomservice.LETTUCE_REDIS_COMMANDS_KEY
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CreateRoomRequest(val name: String)

@Serializable
data class CreateRoomResponse(val playerID: String, val name: String, val roomID: String, val roomCode: String)

suspend fun createRoomHandler(call: ApplicationCall) {
    val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val userName = call.receive<CreateRoomRequest>().name
    val hostID = UUID.randomUUID().toString()
    val roomID = UUID.randomUUID().toString()

    val hostFuture = redis.set(PLAYER_TO_ROOM_PREFIX + hostID, roomID).asDeferred()
    val nameFuture = redis.set(PLAYER_TO_NAME_PREFIX + hostID, userName).asDeferred()
    val roomFuture = redis.lpush(ROOM_TO_PLAYERS_PREFIX + roomID, hostID).asDeferred()

    val maxRetries = 3
    var genCodeAttempts = 0
    var successfulCode = false
    var code = NanoId.generate(Constants.JOIN_CODE_SIZE, JOIN_CODE_ALPHABET)
    while (!successfulCode && genCodeAttempts < maxRetries) {
        genCodeAttempts++
        val codeFuture = redis.setnx(JOIN_CODE_TO_ROOM_PREFIX + code,roomID).await()
        if (codeFuture) {
            successfulCode = true
        } else {
            code = NanoId.generate(Constants.JOIN_CODE_SIZE, JOIN_CODE_ALPHABET)
        }
    }
    if (!successfulCode) {
        return call.respond(HttpStatusCode.InternalServerError, JoinRoomResponse("", "", "",""))
    }

    val roomToCodeFuture = redis.set(ROOM_TO_JOIN_CODE_PREFIX + roomID, code).asDeferred()

    val (hostStatus, nameStatus, roomStatus, roomToCodeStatus) = awaitAll(
        hostFuture,
        nameFuture,
        roomFuture,
        roomToCodeFuture
    )

    if (null !in arrayOf(hostStatus, nameStatus, roomToCodeStatus) && roomStatus == 1L) {
        return call.respond(
            status = HttpStatusCode.OK,
            message = CreateRoomResponse(
                playerID = hostID,
                name = userName,
                roomID = roomID,
                roomCode = code
            )
        )
    } else {
        redis.del(
            PLAYER_TO_ROOM_PREFIX + hostID,
            ROOM_TO_PLAYERS_PREFIX + roomID,
            JOIN_CODE_TO_ROOM_PREFIX + code,
            ROOM_TO_JOIN_CODE_PREFIX + roomID
        )
        return call.respond(HttpStatusCode.InternalServerError, CreateRoomResponse(
            "",
            "",
            "",
            ""))
    }
}
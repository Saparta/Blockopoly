package com.roomservice.routes

import com.roomservice.Constants.JOIN_CODE_ALPHABET
import com.roomservice.Constants.JOIN_CODE_TO_ROOM_PREFIX
import com.roomservice.Constants.PLAYER_TO_ROOM_PREFIX
import com.roomservice.Constants.ROOM_TO_JOIN_CODE_PREFIX
import com.roomservice.Constants.ROOM_TO_PLAYERS_PREFIX
import com.roomservice.LETTUCE_REDIS_COMMANDS_KEY
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asDeferred
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CreateRoomResponse(val playerID: String, val roomId: String, val roomCode: String)

suspend fun createRoomHandler(call: ApplicationCall) {
    val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val hostID = UUID.randomUUID().toString()
    val roomID = UUID.randomUUID().toString()

    val hostFuture = redis.set(PLAYER_TO_ROOM_PREFIX + hostID, roomID).asDeferred()

    val roomFuture = redis.lpush(ROOM_TO_PLAYERS_PREFIX + roomID, hostID).asDeferred()

    val code = NanoId.generate(6, JOIN_CODE_ALPHABET)
    val codeFuture = redis.set(JOIN_CODE_TO_ROOM_PREFIX + code,roomID).asDeferred()

    val roomToCodeFuture = redis.set(ROOM_TO_JOIN_CODE_PREFIX + roomID, code).asDeferred()

    val (hostStatus, roomStatus, codeStatus, roomToCodeStatus) = awaitAll(
        hostFuture,
        roomFuture,
        codeFuture,
        roomToCodeFuture
    )

    if (null !in arrayOf(hostStatus, codeStatus, roomToCodeStatus) && roomStatus == 1L) {
        return call.respond(
            HttpStatusCode.OK,
            CreateRoomResponse(
                hostID,
                roomID,
                code
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
            ""))
    }
}
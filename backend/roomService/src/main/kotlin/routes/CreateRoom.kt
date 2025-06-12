package com.roomservice.routes

import com.roomservice.Constants.JOIN_CODE_ALPHABET
import com.roomservice.Constants.JOIN_CODE_TO_ROOM_PREFIX
import com.roomservice.Constants.PLAYER_TO_ROOM_PREFIX
import com.roomservice.Constants.ROOM_DATA_TTL
import com.roomservice.Constants.ROOM_TO_JOIN_CODE_PREFIX
import com.roomservice.Constants.ROOM_TO_PLAYERS_PREFIX
import com.roomservice.LETTUCE_REDIS_COMMANDS_KEY
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.viascom.nanoid.NanoId
import java.util.*
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asDeferred
import kotlinx.serialization.Serializable

@Serializable
data class CreateRoomResponse(val playerID: String, val roomId: String, val roomCode: String)

suspend fun createRoomHandler(call: ApplicationCall) {
    val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val hostID = UUID.randomUUID().toString()
    val roomID = UUID.randomUUID().toString()

    val hostFuture = redis.setex(PLAYER_TO_ROOM_PREFIX + hostID,ROOM_DATA_TTL, roomID).asDeferred()

    val roomFuture = redis.lpush(ROOM_TO_PLAYERS_PREFIX + roomID, hostID).asDeferred()
    val ttlFuture = redis.expire(ROOM_TO_PLAYERS_PREFIX + roomID, ROOM_DATA_TTL).asDeferred()

    val code = NanoId.generate(6, JOIN_CODE_ALPHABET)
    val codeFuture = redis.setex(JOIN_CODE_TO_ROOM_PREFIX + code, ROOM_DATA_TTL,roomID).asDeferred()

    val roomToCodeFuture = redis.setex(ROOM_TO_JOIN_CODE_PREFIX + roomID, ROOM_DATA_TTL, code).asDeferred()

    val (hostStatus, roomStatus, codeStatus, ttlStatus, roomToCodeStatus) = awaitAll(
        hostFuture,
        roomFuture,
        codeFuture,
        ttlFuture,
        roomToCodeFuture
    )

    if (null !in arrayOf(hostStatus, codeStatus, roomToCodeStatus) && roomStatus == 1L && ttlStatus == true) {
        call.respond(HttpStatusCode.OK, CreateRoomResponse(hostID, roomID, code))
    } else {
        redis.del(
            PLAYER_TO_ROOM_PREFIX + hostID,
            ROOM_TO_PLAYERS_PREFIX + roomID,
            JOIN_CODE_TO_ROOM_PREFIX + code,
            ROOM_TO_JOIN_CODE_PREFIX + roomID)
        call.respond(HttpStatusCode.InternalServerError, "Failed to create room")
    }
}
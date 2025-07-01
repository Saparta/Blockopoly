package com.roomservice.routes

import com.roomservice.ErrorType
import com.roomservice.JOIN_CODE_ALPHABET
import com.roomservice.JOIN_CODE_SIZE
import com.roomservice.JOIN_CODE_TO_ROOM_PREFIX
import com.roomservice.LETTUCE_REDIS_COMMANDS_KEY
import com.roomservice.PLAYER_TO_NAME_PREFIX
import com.roomservice.PLAYER_TO_ROOM_PREFIX
import com.roomservice.PUBSUB_MANAGER_KEY
import com.roomservice.ROOM_TO_JOIN_CODE_PREFIX
import com.roomservice.ROOM_TO_PLAYERS_PREFIX
import com.roomservice.RoomBroadcastType
import com.roomservice.models.Player
import com.roomservice.models.RedisConnections
import com.roomservice.models.RoomSubChannel
import com.roomservice.util.format
import com.roomservice.util.forwardSSe
import com.roomservice.util.reconnect
import io.ktor.server.application.ApplicationCall
import io.ktor.server.sse.ServerSSESession
import io.viascom.nanoid.NanoId
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class CreateRoomResponse(val playerId: String = "", val name: String = "", val roomId: String = "", val roomCode: String = "")

suspend fun createRoomHandler(call: ApplicationCall, session : ServerSSESession) {
    val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val pubSubManager = call.application.attributes[PUBSUB_MANAGER_KEY]
    val userName = call.parameters["username"]
    val playerId = call.request.queryParameters["playerId"]
    if (userName.isNullOrBlank()) {
        session.send(ErrorType.BAD_REQUEST.toString(), RoomBroadcastType.ERROR.toString())
        return session.close()
    }
    // Player is reconnecting
    if (playerId != null) {
        return reconnect(playerId, session, RedisConnections(pubSubManager, redis))
    }

    val hostID = UUID.randomUUID().format()
    val roomID = UUID.randomUUID().format()

    val hostFuture = redis.set(PLAYER_TO_ROOM_PREFIX + hostID, roomID).asDeferred()
    val nameFuture = redis.set(PLAYER_TO_NAME_PREFIX + hostID, userName).asDeferred()
    val roomFuture = redis.lpush(ROOM_TO_PLAYERS_PREFIX + roomID, hostID).asDeferred()

    val maxRetries = 3
    var genCodeAttempts = 0
    var successfulCode = false
    var code = NanoId.generate(JOIN_CODE_SIZE, JOIN_CODE_ALPHABET)
    while (!successfulCode && genCodeAttempts < maxRetries) {
        genCodeAttempts++
        val codeFuture = redis.setnx(JOIN_CODE_TO_ROOM_PREFIX + code,roomID).await()
        if (codeFuture) {
            successfulCode = true
        } else {
            code = NanoId.generate(JOIN_CODE_SIZE, JOIN_CODE_ALPHABET)
        }
    }
    if (!successfulCode) {
        session.send(ErrorType.SERVICE_UNAVAILABLE.toString(), RoomBroadcastType.ERROR.toString())
        return session.close()
    }

    val channel = pubSubManager.subscribe(roomID)

    val roomToCodeFuture = redis.set(ROOM_TO_JOIN_CODE_PREFIX + roomID, code).asDeferred()

    val (hostStatus, nameStatus, roomStatus, roomToCodeStatus) = awaitAll(
        hostFuture,
        nameFuture,
        roomFuture,
        roomToCodeFuture
    )

    if (null !in arrayOf(hostStatus, nameStatus, roomToCodeStatus) && roomStatus == 1L) {
        session.send(
                Json.encodeToString(CreateRoomResponse(
                            playerId = hostID,
                            name = userName,
                            roomId = roomID,
                            roomCode = code
                        )
                ), RoomBroadcastType.INITIAL.toString())
        session.send(Player(hostID, userName).toString(), RoomBroadcastType.HOST.toString())
        return forwardSSe(RoomSubChannel(channel, roomID, UUID.randomUUID().toString(), hostID), RedisConnections(pubSubManager, redis), session)
    } else {
        redis.del(
            PLAYER_TO_ROOM_PREFIX + hostID,
            ROOM_TO_PLAYERS_PREFIX + roomID,
            JOIN_CODE_TO_ROOM_PREFIX + code,
            ROOM_TO_JOIN_CODE_PREFIX + roomID
        )
        session.send(ErrorType.INTERNAL_SERVER_ERROR.toString(), RoomBroadcastType.ERROR.toString())
        return session.close()
    }
}
package com.roomservice.routes

import com.roomservice.Constants.JOIN_CODE_TO_ROOM_PREFIX
import com.roomservice.Constants.PLAYER_TO_NAME_PREFIX
import com.roomservice.Constants.PLAYER_TO_ROOM_PREFIX
import com.roomservice.Constants.ROOM_TO_JOIN_CODE_PREFIX
import com.roomservice.Constants.ROOM_TO_PLAYERS_PREFIX
import com.roomservice.Constants.RoomBroadcastType.CLOSED
import com.roomservice.LETTUCE_REDIS_COMMANDS_KEY
import com.roomservice.models.RoomBroadcast
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await

suspend fun closeRoomHandler(call: ApplicationCall, roomId: String? = null) {
    val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val roomCode =  call.parameters["roomCode"] ?: redis.get(ROOM_TO_JOIN_CODE_PREFIX + roomId).await()
    val players =
        if (roomId == null) {
            val id = redis.get(JOIN_CODE_TO_ROOM_PREFIX + roomCode).await()
            redis.lrange(ROOM_TO_PLAYERS_PREFIX + id, 0, -1).asDeferred()
        } else {
            redis.lrange(ROOM_TO_PLAYERS_PREFIX + roomId, 0, -1).asDeferred()
        }

    redis.del(ROOM_TO_PLAYERS_PREFIX + roomId, ROOM_TO_JOIN_CODE_PREFIX + roomId)
    for (player in players.await()) {
        redis.del(PLAYER_TO_ROOM_PREFIX + player, PLAYER_TO_NAME_PREFIX + player)
    }
    redis.publish(roomCode, RoomBroadcast(CLOSED, "").toString())
    call.respond(HttpStatusCode.OK)
}
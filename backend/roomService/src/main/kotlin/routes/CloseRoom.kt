package com.roomservice.routes

import com.roomservice.Constants.PLAYER_TO_NAME_PREFIX
import com.roomservice.Constants.PLAYER_TO_ROOM_PREFIX
import com.roomservice.Constants.ROOM_TO_JOIN_CODE_PREFIX
import com.roomservice.Constants.ROOM_TO_PLAYERS_PREFIX
import com.roomservice.Constants.RoomBroadcastType.CLOSED
import com.roomservice.LETTUCE_REDIS_COMMANDS_KEY
import com.roomservice.models.RoomBroadcast
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await

suspend fun closeRoomHandler(call: ApplicationCall, playerId: String? = null) {
    val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val roomId =  call.parameters["roomId"] ?: redis.get(PLAYER_TO_ROOM_PREFIX + playerId!!).await()
    val players = redis.lrange(ROOM_TO_PLAYERS_PREFIX + roomId, 0, -1).asDeferred()
    redis.del(ROOM_TO_PLAYERS_PREFIX + roomId, ROOM_TO_JOIN_CODE_PREFIX + roomId)
    for (player in players.await()) {
        redis.del(PLAYER_TO_ROOM_PREFIX + player, PLAYER_TO_NAME_PREFIX + player)
    }
    redis.publish(roomId, RoomBroadcast(CLOSED, "").toString())
}
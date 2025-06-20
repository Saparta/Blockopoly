package com.roomservice.routes

import com.roomservice.LETTUCE_REDIS_COMMANDS_KEY
import com.roomservice.PLAYER_TO_NAME_PREFIX
import com.roomservice.PLAYER_TO_ROOM_PREFIX
import com.roomservice.ROOM_TO_JOIN_CODE_PREFIX
import com.roomservice.ROOM_TO_PLAYERS_PREFIX
import com.roomservice.RoomBroadcastType
import com.roomservice.models.RoomBroadcast
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.coroutines.future.asDeferred

suspend fun closeRoomHandler(call: ApplicationCall, roomId: String? = null) {
    val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val roomId =  roomId ?: call.parameters["roomId"]
    val players = redis.lrange(ROOM_TO_PLAYERS_PREFIX + roomId, 0, -1).asDeferred()

    redis.del(ROOM_TO_PLAYERS_PREFIX + roomId, ROOM_TO_JOIN_CODE_PREFIX + roomId)
    for (player in players.await()) {
        redis.del(PLAYER_TO_ROOM_PREFIX + player, PLAYER_TO_NAME_PREFIX + player)
    }
    redis.publish(roomId, RoomBroadcast(RoomBroadcastType.CLOSED, "").toString())
    call.respond(HttpStatusCode.OK)
}
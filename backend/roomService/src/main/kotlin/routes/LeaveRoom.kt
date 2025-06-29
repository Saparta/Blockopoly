package com.roomservice.routes

import com.roomservice.LETTUCE_REDIS_COMMANDS_KEY
import com.roomservice.PLAYER_TO_NAME_PREFIX
import com.roomservice.PLAYER_TO_ROOM_PREFIX
import com.roomservice.ROOM_TO_JOIN_CODE_PREFIX
import com.roomservice.ROOM_TO_PLAYERS_PREFIX
import com.roomservice.RoomBroadcastType
import com.roomservice.models.Player
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await


suspend fun leaveRoomHandler(call: ApplicationCall) {
    val playerID = call.parameters["playerId"]
        ?: return call.respond(HttpStatusCode.BadRequest)
    val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val roomID = redis.get(PLAYER_TO_ROOM_PREFIX + playerID).await()

    if (roomID == null) {
        return call.respond(HttpStatusCode.InternalServerError)
    }

    val hostLeaving = redis.lindex(ROOM_TO_PLAYERS_PREFIX + roomID, -1).await() == playerID
    val removedCount = redis.lrem(ROOM_TO_PLAYERS_PREFIX + roomID, 1, playerID).await()

    if (removedCount == 0L) {
        call.application.environment.log.warn("Failed to remove player $playerID from room $roomID — player not found in list.")
        return call.respond(
            HttpStatusCode.InternalServerError,
            mapOf("error" to "Failed to leave room: player was not in the room.")
        )
    }
    val roomCodeFuture = redis.get(ROOM_TO_JOIN_CODE_PREFIX + roomID).asDeferred()
    val playerNameFuture = redis.get(PLAYER_TO_NAME_PREFIX + playerID).asDeferred()
    val (roomCode, playerName) = awaitAll(roomCodeFuture, playerNameFuture)


    redis.del(
        PLAYER_TO_ROOM_PREFIX + playerID,
        PLAYER_TO_NAME_PREFIX + playerID
    ).await()


    if (roomCode != null && playerName != null) {
        redis.publish(
            roomID,
            com.roomservice.models.RoomBroadcast(
                RoomBroadcastType.LEAVE,
                Player(playerID, playerName).toString()
            ).toString()
        ).await()
    }

    val numberRemaining = redis.llen(ROOM_TO_PLAYERS_PREFIX + roomID).await()
    if (numberRemaining == 0L) {
        closeRoomHandler(call, roomID)
    }
    else if (hostLeaving) {
        val newHostID = redis.lindex(ROOM_TO_PLAYERS_PREFIX + roomID, -1).await()
        val newHostName = redis.get(PLAYER_TO_NAME_PREFIX + newHostID).await()
        redis.publish(roomID,
            com.roomservice.models.RoomBroadcast(
                RoomBroadcastType.HOST, Player(newHostID, newHostName).toString()
            ).toString()
        ).await()
    }

    call.respond(HttpStatusCode.OK, message =  "$playerName has left the room.")




}

package com.roomservice.routes

import com.roomservice.Constants
import com.roomservice.Constants.JOIN_CODE_TO_ROOM_PREFIX
import com.roomservice.Constants.MAX_PLAYERS
import com.roomservice.Constants.PLAYER_TO_ROOM_PREFIX
import com.roomservice.Constants.ROOM_TO_PLAYERS_PREFIX
import com.roomservice.LETTUCE_REDIS_COMMANDS_KEY
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.future.await
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class JoinRoomResponse(val roomId: String, val roomCode: String, val playerID: String)

suspend fun joinRoomHandler(call: ApplicationCall) {
    val roomCode = call.parameters["roomCode"]
        ?: return call.respond(
            HttpStatusCode.BadRequest,
            JoinRoomResponse(roomId = "", roomCode = "", playerID = ""))
    val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val roomID = redis.get(JOIN_CODE_TO_ROOM_PREFIX + roomCode).await()
    if (roomID == null) {
        call.respond(HttpStatusCode.NotFound, JoinRoomResponse(
            roomId = "",
            roomCode = "",
            playerID = ""
            )
        )
            return
        }

        val maxRetry = 3
        repeat(maxRetry) { attempt ->
            redis.watch(ROOM_TO_PLAYERS_PREFIX + roomID).await()
            val numPlayers = redis.llen(ROOM_TO_PLAYERS_PREFIX + roomID).await()
            if (numPlayers >= MAX_PLAYERS) {
                redis.unwatch().await()
                return call.respond(
                    Constants.ROOM_FULL_STATUS, JoinRoomResponse(
                        roomId = "",
                        roomCode = "",
                        playerID = ""
                    )
                )
            }

            val playerID = UUID.randomUUID().toString()
            val successfulUpdate = updateDatastore(playerID, roomID, call, redis)
            if (successfulUpdate) {
                return call.respond(HttpStatusCode.OK, JoinRoomResponse(roomId = roomID, roomCode = roomCode, playerID = playerID))
            }
         }

    return call.respond(
        HttpStatusCode.ServiceUnavailable,
        JoinRoomResponse("","","")
    )
}

suspend fun updateDatastore(playerID: String, roomID: String, call: ApplicationCall, redis: RedisAsyncCommands<String, String>) : Boolean{
    redis.multi().await()
    redis.lpush(ROOM_TO_PLAYERS_PREFIX + roomID, playerID)
    redis.lrange(ROOM_TO_PLAYERS_PREFIX + roomID, 0, 0)
    redis.set(PLAYER_TO_ROOM_PREFIX + playerID, roomID)

    val transactionResult = redis.exec().await() ?: return false

    if ((transactionResult[1] as ArrayList<String>).firstOrNull() != playerID ||
        (transactionResult[2] as String?) == null) {
            call.respond(HttpStatusCode.InternalServerError, JoinRoomResponse(
                roomId = "",
                roomCode = "",
                playerID = ""
            ))
            redis.lrem(ROOM_TO_PLAYERS_PREFIX + roomID, 1, playerID)
            redis.del(PLAYER_TO_ROOM_PREFIX + playerID)
        return false
    }
    return true
}
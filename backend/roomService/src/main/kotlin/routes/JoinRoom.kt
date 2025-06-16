package com.roomservice.routes

import com.roomservice.Constants
import com.roomservice.Constants.JOIN_CODE_TO_ROOM_PREFIX
import com.roomservice.Constants.MAX_PLAYERS
import com.roomservice.Constants.PLAYER_TO_NAME_PREFIX
import com.roomservice.Constants.PLAYER_TO_ROOM_PREFIX
import com.roomservice.Constants.ROOM_TO_PLAYERS_PREFIX
import com.roomservice.LETTUCE_REDIS_COMMANDS_KEY
import com.roomservice.PUBSUB_MANAGER_KEY
import com.roomservice.models.Player
import com.roomservice.models.RoomBroadcast
import com.roomservice.util.forwardSSe
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.sse.ServerSSESession
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class JoinRoomRequest(val name: String)

@Serializable
data class JoinRoomResponse(val playerID: String = "", val name: String = "", val roomId: String = "", val roomCode: String = "", val players: List<Player> = emptyList())

@Serializable
data class JoinRoomBroadcast(val playerID: String, val name: String) {
    override fun toString(): String {
        return "$playerID${Constants.ROOM_BROADCAST_MSG_DELIMITER}$name"
    }
}

suspend fun joinRoomHandler(call: ApplicationCall, session: ServerSSESession) {
    val roomCode = call.parameters["roomCode"]
        ?: return call.respond(HttpStatusCode.BadRequest)
    val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val pubSubManager = call.application.attributes[PUBSUB_MANAGER_KEY]
    val userName = call.parameters["username"] ?: return call.respond(HttpStatusCode.BadRequest)
    val roomID = redis.get(JOIN_CODE_TO_ROOM_PREFIX + roomCode).await()
    if (roomID == null) {
        call.respond(HttpStatusCode.NotFound)
        return
    }

    val maxRetry = 3
    repeat(maxRetry) { attempt ->
        redis.watch(ROOM_TO_PLAYERS_PREFIX + roomID).await()
        val numPlayers = redis.llen(ROOM_TO_PLAYERS_PREFIX + roomID).await()
        if (numPlayers >= MAX_PLAYERS) {
            redis.unwatch().await()
            return call.respond(Constants.ROOM_FULL_STATUS)
        }

        val playerID = UUID.randomUUID().toString()
        val successfulUpdate = updateDatastore(playerID, userName, roomID, call, redis)
        if (successfulUpdate.first) {
            redis.publish(roomCode,
                    RoomBroadcast(
                        Constants.RoomBroadcastType.JOIN,
                        JoinRoomBroadcast(playerID, userName).toString()
                ).toString()
            )
            val channel = pubSubManager.subscribe(roomCode)
            val playerNamesFuture = successfulUpdate.second.map {  redis.get(PLAYER_TO_NAME_PREFIX + it).asDeferred() }

            val players = playerNamesFuture.awaitAll().zip(successfulUpdate.second).map {
                (playerName, playerId) -> Player(playerId, playerName)
            }

            session.send(Json.encodeToString(
                JoinRoomResponse(playerID = playerID,
                    name = userName,
                    roomId = roomID,
                    roomCode = roomCode,
                    players = players)
            ), Constants.RoomBroadcastType.INITIAL.toString())

            session.send(players.last().playerId, Constants.RoomBroadcastType.HOST.toString())
            forwardSSe(channel, roomCode, session, pubSubManager, playerID)
        }
     }
    return call.respond(HttpStatusCode.ServiceUnavailable)
}

suspend fun updateDatastore(playerID: String, userName: String, roomID: String, call: ApplicationCall, redis: RedisAsyncCommands<String, String>) : Pair<Boolean, List<String>> {
    redis.multi().await()
    redis.lpush(ROOM_TO_PLAYERS_PREFIX + roomID, playerID)
    redis.lrange(ROOM_TO_PLAYERS_PREFIX + roomID, 0, -1)
    redis.set(PLAYER_TO_ROOM_PREFIX + playerID, roomID)
    redis.set(PLAYER_TO_NAME_PREFIX + playerID, userName)
    val transactionResult = redis.exec().await() ?: return Pair(false, emptyList())

    if ((transactionResult[1] as ArrayList<String>).firstOrNull() != playerID ||
        (transactionResult[2] as String?) == null) {
            call.respond(HttpStatusCode.InternalServerError)
            redis.lrem(ROOM_TO_PLAYERS_PREFIX + roomID, 1, playerID)
            redis.del(PLAYER_TO_ROOM_PREFIX + playerID, PLAYER_TO_NAME_PREFIX + userName)
        return Pair(false, emptyList())
    }
    return Pair(true, transactionResult[1])
}
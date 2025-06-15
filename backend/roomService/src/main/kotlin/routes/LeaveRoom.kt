package com.roomservice.routes

import com.roomservice.Constants.PLAYER_TO_ROOM_PREFIX
import com.roomservice.Constants.ROOM_TO_PLAYERS_PREFIX
import com.roomservice.LETTUCE_REDIS_COMMANDS_KEY
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await

suspend fun leaveRoomHandler(call: ApplicationCall) {
    val playerId = call.parameters["playerId"]
        ?: return call.respond(HttpStatusCode.BadRequest)
    val redis = call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val roomId = redis.get(PLAYER_TO_ROOM_PREFIX + playerId).await()

    val lRemFuture = redis.lrem(ROOM_TO_PLAYERS_PREFIX + roomId, 1, playerId).asDeferred()
    val delPlayerFuture = redis.del(PLAYER_TO_ROOM_PREFIX + roomId).asDeferred()
    val delNameFuture = redis.del(PLAYER_TO_ROOM_PREFIX + playerId).asDeferred()

    val futures = awaitAll(lRemFuture, delPlayerFuture, delNameFuture)
    if (futures.any( {it -> it != 1L})) {

    }
}
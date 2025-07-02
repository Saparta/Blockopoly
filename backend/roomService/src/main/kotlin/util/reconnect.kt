package com.roomservice.util

import com.roomservice.ErrorType
import com.roomservice.LETTUCE_REDIS_COMMANDS_KEY
import com.roomservice.PLAYER_TO_NAME_PREFIX
import com.roomservice.PLAYER_TO_ROOM_PREFIX
import com.roomservice.PUBSUB_MANAGER_KEY
import com.roomservice.ROOM_BROADCAST_MSG_DELIMITER
import com.roomservice.ROOM_TO_PLAYERS_PREFIX
import com.roomservice.RoomBroadcastType
import com.roomservice.models.Player
import com.roomservice.models.RoomBroadcast
import com.roomservice.models.RoomSubChannel
import io.ktor.server.sse.ServerSSESession
import kotlinx.coroutines.future.await
import java.util.UUID

suspend fun reconnect(playerId: String, session : ServerSSESession) {
    val redis = session.call.application.attributes[LETTUCE_REDIS_COMMANDS_KEY]
    val pubSubManager = session.call.application.attributes[PUBSUB_MANAGER_KEY]
    val roomId = redis.get(PLAYER_TO_ROOM_PREFIX + playerId).await()
    if (roomId == null) {
        session.send(ErrorType.BAD_REQUEST.toString(), RoomBroadcastType.ERROR.toString())
        return session.close()
    }
    val hostId = redis.lindex(ROOM_TO_PLAYERS_PREFIX + roomId, -1).await()
    val hostName = redis.get(PLAYER_TO_NAME_PREFIX + hostId).await()
    val channelId = UUID.randomUUID().toString()
    redis.publish(
        roomId,
        RoomBroadcast(
            RoomBroadcastType.RECONNECT,
            "$playerId$ROOM_BROADCAST_MSG_DELIMITER${channelId}"
        ).toString()).await()
    val channel = pubSubManager.subscribe(roomId)
    session.send(Player(hostId, hostName).toString(), RoomBroadcastType.HOST.toString())
    return forwardSSe(RoomSubChannel(channel, roomId, channelId, playerId), session)
}
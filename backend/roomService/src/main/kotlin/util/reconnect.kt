package com.roomservice.util

import com.roomservice.ErrorType
import com.roomservice.PLAYER_TO_ROOM_PREFIX
import com.roomservice.ROOM_BROADCAST_MSG_DELIMITER
import com.roomservice.RedisPubSubManager
import com.roomservice.RoomBroadcastType
import com.roomservice.models.RoomBroadcast
import com.roomservice.models.RoomSubChannel
import io.ktor.server.sse.ServerSSESession
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.future.await
import java.util.UUID

suspend fun reconnect(playerId: String, session : ServerSSESession, redis: RedisAsyncCommands<String, String>, pubSubManager: RedisPubSubManager) {
    val roomId = redis.get(PLAYER_TO_ROOM_PREFIX + playerId).await()
    if (roomId == null) {
        session.send(ErrorType.BAD_REQUEST.toString(), RoomBroadcastType.ERROR.toString())
        return session.close()
    }
    val channelId = UUID.randomUUID().toString()
    redis.publish(
        roomId,
        RoomBroadcast(
            RoomBroadcastType.RECONNECT,
            "$playerId$ROOM_BROADCAST_MSG_DELIMITER${channelId}"
        ).toString()).await()
    val channel = pubSubManager.subscribe(roomId)
    return forwardSSe(RoomSubChannel(channel, roomId, channelId, playerId), session, pubSubManager)
}
package com.roomservice.util

import com.roomservice.ErrorType
import com.roomservice.PLAYER_TO_NAME_PREFIX
import com.roomservice.PLAYER_TO_ROOM_PREFIX
import com.roomservice.ROOM_BROADCAST_MSG_DELIMITER
import com.roomservice.ROOM_TO_PLAYERS_PREFIX
import com.roomservice.RoomBroadcastType
import com.roomservice.models.Player
import com.roomservice.models.RedisConnections
import com.roomservice.models.RoomBroadcast
import com.roomservice.models.RoomSubChannel
import io.ktor.server.sse.ServerSSESession
import kotlinx.coroutines.future.await
import java.util.UUID

suspend fun reconnect(playerId: String, session : ServerSSESession, redisConnections: RedisConnections) {
    val roomId = redisConnections.redis.get(PLAYER_TO_ROOM_PREFIX + playerId).await()
    if (roomId == null) {
        session.send(ErrorType.BAD_REQUEST.toString(), RoomBroadcastType.ERROR.toString())
        return session.close()
    }
    val hostId = redisConnections.redis.lindex(ROOM_TO_PLAYERS_PREFIX + roomId, -1).await()
    val hostName = redisConnections.redis.get(PLAYER_TO_NAME_PREFIX + hostId).await()
    val channelId = UUID.randomUUID().toString()
    redisConnections.redis.publish(
        roomId,
        RoomBroadcast(
            RoomBroadcastType.RECONNECT,
            "$playerId$ROOM_BROADCAST_MSG_DELIMITER${channelId}"
        ).toString()).await()
    val channel = redisConnections.pubSubManager.subscribe(roomId)
    session.send(Player(hostId, hostName).toString(), RoomBroadcastType.HOST.toString())
    return forwardSSe(RoomSubChannel(channel, roomId, channelId, playerId),  redisConnections, session)
}
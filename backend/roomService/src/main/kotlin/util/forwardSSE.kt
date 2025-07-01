package com.roomservice.util

import com.roomservice.ROOM_BROADCAST_MSG_DELIMITER
import com.roomservice.ROOM_BROADCAST_TYPE_DELIMITER
import com.roomservice.RoomBroadcastType
import com.roomservice.models.RedisConnections
import com.roomservice.models.RoomSubChannel
import com.roomservice.routes.leaveRoomHelper
import io.ktor.server.sse.ServerSSESession
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun forwardSSe(roomSubChannel: RoomSubChannel, redisConnections: RedisConnections, session: ServerSSESession) {
    withContext(Dispatchers.IO) {
        for (msg in roomSubChannel.channel) {
            try {
                val (type, msg) = msg.split(ROOM_BROADCAST_TYPE_DELIMITER)
                if (type == "START") {
                    session.send(ServerSentEvent("", type))
                    redisConnections.pubSubManager.unsubscribe(roomSubChannel.channelKey, roomSubChannel.channel)
                    session.close()
                } else if (type == RoomBroadcastType.RECONNECT.toString()) {
                    val (playerId, channelId) = msg.split(ROOM_BROADCAST_MSG_DELIMITER)
                    if (roomSubChannel.playerId == playerId && roomSubChannel.channelId != channelId) {
                        session.send(ServerSentEvent("", type))
                        redisConnections.pubSubManager.unsubscribe(roomSubChannel.channelKey, roomSubChannel.channel)
                        session.close()
                    }
                } else if (type == RoomBroadcastType.CLOSED.toString()) {
                    session.send(ServerSentEvent("", type))
                    redisConnections.pubSubManager.unsubscribe(roomSubChannel.channelKey, roomSubChannel.channel)
                    session.close()
                } else if (type == RoomBroadcastType.LEAVE.toString()
                    && msg.split(ROOM_BROADCAST_MSG_DELIMITER).first() == roomSubChannel.playerId
                ) {
                    redisConnections.pubSubManager.unsubscribe(roomSubChannel.channelKey, roomSubChannel.channel)
                    session.close()
                } else if (type !in arrayOf(RoomBroadcastType.JOIN.toString(), RoomBroadcastType.LEAVE.toString())
                    || msg.split(ROOM_BROADCAST_MSG_DELIMITER).first() != roomSubChannel.playerId
                ) {
                    session.send(ServerSentEvent(msg, type))
                }
            } catch (_: Exception) {
                leaveRoomHelper(playerId = roomSubChannel.playerId, redis = redisConnections.redis)
            }
        }
    }
}
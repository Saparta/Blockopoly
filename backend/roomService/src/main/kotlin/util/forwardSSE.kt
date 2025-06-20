package com.roomservice.util

import com.roomservice.ROOM_BROADCAST_MSG_DELIMITER
import com.roomservice.ROOM_BROADCAST_TYPE_DELIMITER
import com.roomservice.RedisPubSubManager
import com.roomservice.RoomBroadcastType
import io.ktor.server.sse.ServerSSESession
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.channels.Channel

suspend fun forwardSSe(channel: Channel<String>, channelKey: String, session: ServerSSESession, pubSubManager: RedisPubSubManager, playerId: String) {
    for (msg in channel) {
        val (type, msg) = msg.split(ROOM_BROADCAST_TYPE_DELIMITER)
        if (type == "START") {
            session.send(ServerSentEvent("", type))
            pubSubManager.unsubscribe(channelKey, channel)
            session.close()
        } else if (type == RoomBroadcastType.CLOSED.toString()) {
            session.send(ServerSentEvent("", type))
            pubSubManager.unsubscribe(channelKey, channel)
            session.close()
        } else if (type == RoomBroadcastType.LEAVE.toString()
            && msg.split(ROOM_BROADCAST_MSG_DELIMITER).first() == playerId) {
            pubSubManager.unsubscribe(channelKey, channel)
            session.close()
        } else if (type !in arrayOf(RoomBroadcastType.JOIN.toString(), RoomBroadcastType.LEAVE.toString())
            || msg.split(ROOM_BROADCAST_MSG_DELIMITER).first() != playerId) {
            session.send(ServerSentEvent(msg, type))
        }
    }
}